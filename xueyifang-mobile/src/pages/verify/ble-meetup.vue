<template>
  <view class="ble-page">
    <view class="radar-stage">
      <view class="ring ring-1" />
      <view class="ring ring-2" />
      <view class="core-node">
        <text class="rssi-val">{{ latestRssi !== null ? `${latestRssi} dBm` : 'BLE' }}</text>
        <text class="role-tag">{{ session?.role === 'BUYER' ? '买方广播端' : '卖方感应端' }}</text>
      </view>
    </view>

    <view class="status-panel">
      <text class="stage-title">{{ stageTitle }}</text>
      <text class="stage-sub">{{ stageLog }}</text>

      <view v-if="session" class="crypto-meta">
        <text>会话凭证：{{ session.advShortToken }}</text>
        <text>最低信号要求：不低于 {{ session.minRssiDbm }} dBm（约 3~5 米内）</text>
      </view>

      <view
        v-if="!verified && !running"
        class="start-btn"
        @tap="startBleHandshake"
      >
        启动低功耗蓝牙双向挑战握手
      </view>

      <view v-if="verified" class="done-btn" @tap="goBackOrder">
        碰面核销已完成 · 返回查看结算单
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { mobileApi, type BleSessionPayload } from '@/api'
import {
  generateSellerNonce,
  signBleHandshake,
  stringToArrayBuffer,
  arrayBufferToString
} from '@/utils/bleCrypto'
import { encodeBleFrames, BleFrameAssembler } from '@/utils/bleChunk'

const orderId = ref(0)
const session = ref<BleSessionPayload | null>(null)
const running = ref(false)
const verified = ref(false)
const latestRssi = ref<number | null>(null)
const stageTitle = ref('准备就绪 · 请将两部手机靠近至 3 米内')
const stageLog = ref('点击下方按钮向服务端申请一次性挑战随机数并启动 BLE 广播/扫描')

let peripheralServer: any = null
let connectingDevice = false

function handleBleError(title: string, err?: any) {
  running.value = false
  connectingDevice = false
  stageTitle.value = title
  stageLog.value = err?.errMsg || err?.message || '蓝牙连接或特征值交互失败，请保持两部手机靠近后重试'
}

async function getCurrentCoords(): Promise<{ lat?: number; lng?: number }> {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'gcj02',
      success: (res) => resolve({ lat: Number(res.latitude.toFixed(6)), lng: Number(res.longitude.toFixed(6)) }),
      fail: () => resolve({})
    })
  })
}

async function startBleHandshake() {
  if (!orderId.value || running.value) return
  running.value = true
  try {
    stageTitle.value = '正在获取服务端一次性挑战会话...'
    const sess = await mobileApi.openBleSession(orderId.value)
    session.value = sess

    await new Promise<void>((resolve, reject) => {
      uni.openBluetoothAdapter({
        success: () => resolve(),
        fail: (err) => reject(err)
      })
    })

    if (sess.role === 'BUYER') {
      await runBuyerPeripheralMode(sess)
    } else {
      await runSellerCentralMode(sess)
    }
  } catch (err: any) {
    running.value = false
    stageTitle.value = '蓝牙感应中断'
    stageLog.value = err?.errMsg || err?.message || '请确认已开启手机系统蓝牙与定位权限'
  }
}

/**
 * 买方（Peripheral 外围广播模式）：
 * 广播短令牌 XYFB:xxxxxxxx，接收卖方近场写入的 sellerNonce (Ns)，
 * 使用买方会话子密钥计算 HMAC(orderId|sessionId|Nb|Ns|BUYER) 并通过特征值回传给卖方。
 */
async function runBuyerPeripheralMode(sess: BleSessionPayload) {
  stageTitle.value = '买方广播已开启 · 等待卖方手机靠近感应'
  stageLog.value = `广播帧标识 ${sess.advShortToken}，请保持屏幕常亮`

  const wxAny = (globalThis as any).wx
  if (wxAny && typeof wxAny.createBLEPeripheralServer === 'function') {
    wxAny.createBLEPeripheralServer({
      success: (res: any) => {
        peripheralServer = res.server
        peripheralServer.addService({
          service: {
            uuid: sess.serviceUuid,
            characteristics: [
              {
                uuid: sess.characteristicUuid,
                properties: { write: true, read: true, notify: true },
                permission: { readable: true, writeable: true }
              }
            ]
          },
          success: () => {
            peripheralServer.startAdvertising({
              advertiseRequest: {
                connectable: true,
                deviceName: sess.advShortToken,
                serviceUuids: [sess.serviceUuid]
              },
              fail: (err: any) => handleBleError('启动买方蓝牙广播失败', err)
            })
            peripheralServer.onCharacteristicWriteRequest(async (req: any) => {
              try {
                const sellerNonce = arrayBufferToString(req.value).trim()
                const coords = await getCurrentCoords()
                const buyerSig = signBleHandshake({
                  roleSigningKey: sess.roleSigningKey,
                  orderId: sess.orderId,
                  sessionId: sess.sessionId,
                  buyerNonce: sess.buyerNonce,
                  sellerNonce,
                  role: 'BUYER'
                })
                const replyPayload = JSON.stringify({
                  buyerNonce: sess.buyerNonce,
                  buyerSig,
                  lat: coords.lat,
                  lng: coords.lng
                })
                const frames = encodeBleFrames(replyPayload)
                for (let i = 0; i < frames.length; i++) {
                  await new Promise<void>((resolve, reject) => {
                    peripheralServer.writeCharacteristicValue({
                      serviceId: sess.serviceUuid,
                      characteristicId: sess.characteristicUuid,
                      value: frames[i],
                      needNotify: true,
                      callbackId: i === 0 ? req.callbackId : undefined,
                      success: () => resolve(),
                      fail: (err: any) => reject(err)
                    })
                  })
                  if (i < frames.length - 1) {
                    await new Promise((r) => setTimeout(r, 25))
                  }
                }
                stageTitle.value = '已完成近场双向挑战签名交换'
                stageLog.value = '卖方正在提交双端签名至服务端结算担保资金...'
              } catch (err: any) {
                handleBleError('回传加密签名分包失败', err)
              }
            })
          },
          fail: (err: any) => handleBleError('注册蓝牙 GATT 服务失败', err)
        })
      },
      fail: (err: any) => handleBleError('初始化外围广播服务失败', err)
    })
  } else {
    stageLog.value = '请在微信小程序或 Android 真机环境运行 BLE 外围广播模式'
    running.value = false
  }
}

/**
 * 卖方（Central 中心扫描连接模式）：
 * 扫描携带 advShortToken 的买方外围广播，读取实时物理 RSSI 信号强度，
 * 连接 GATT 写入随机挑战码 Ns，读取买方分包签名后叠加卖方签名联合上报后端验签完结订单。
 */
async function runSellerCentralMode(sess: BleSessionPayload) {
  stageTitle.value = '正在扫描 3 米内买方加密蓝牙信标...'
  stageLog.value = `目标信标：${sess.advShortToken}`
  connectingDevice = false

  uni.onBluetoothDeviceFound((res) => {
    if (connectingDevice || verified.value) return
    for (const dev of res.devices) {
      const name = dev.name || dev.localName || ''
      if (name.includes(sess.advShortToken)) {
        latestRssi.value = dev.RSSI
        if (dev.RSSI < sess.minRssiDbm) {
          stageLog.value = `已发现买方手机，当前信号 ${dev.RSSI} dBm 偏弱，请再靠近一些`
          return
        }
        connectingDevice = true
        uni.stopBluetoothDevicesDiscovery({})
        connectAndExchangeWithBuyer(sess, dev.deviceId, dev.RSSI)
        break
      }
    }
  })

  uni.startBluetoothDevicesDiscovery({
    allowDuplicatesKey: true,
    fail: (err) => handleBleError('启动蓝牙扫描失败', err)
  })
}

async function connectAndExchangeWithBuyer(sess: BleSessionPayload, deviceId: string, rssi: number) {
  stageTitle.value = '已捕获近场信标 · 正在交换加密挑战签名...'
  const sellerNonce = generateSellerNonce()
  const sellerCoords = await getCurrentCoords()
  const assembler = new BleFrameAssembler()

  uni.createBLEConnection({
    deviceId,
    success: () => {
      if (typeof (uni as any).setBLEMTU === 'function') {
        try {
          ;(uni as any).setBLEMTU({ deviceId, mtu: 240 })
        } catch {
          // ignore on unsupported platforms; 20-byte chunking handles default MTU
        }
      }
      uni.getBLEDeviceServices({
        deviceId,
        success: () => {
          uni.getBLEDeviceCharacteristics({
            deviceId,
            serviceId: sess.serviceUuid,
            success: () => {
              uni.notifyBLECharacteristicValueChange({
                state: true,
                deviceId,
                serviceId: sess.serviceUuid,
                characteristicId: sess.characteristicUuid,
                success: () => {
                  uni.onBLECharacteristicValueChange(async (ev) => {
                    try {
                      const assembled = assembler.push(ev.value)
                      if (!assembled) {
                        return
                      }
                      const buyerReply = JSON.parse(assembled)
                      const sellerSignature = signBleHandshake({
                        roleSigningKey: sess.roleSigningKey,
                        orderId: sess.orderId,
                        sessionId: sess.sessionId,
                        buyerNonce: sess.buyerNonce,
                        sellerNonce,
                        role: 'SELLER'
                      })

                      await mobileApi.submitBleVerify(sess.orderId, {
                        sessionId: sess.sessionId,
                        buyerNonce: sess.buyerNonce,
                        sellerNonce,
                        rssiDbm: rssi,
                        buyerLat: buyerReply.lat,
                        buyerLng: buyerReply.lng,
                        sellerLat: sellerCoords.lat,
                        sellerLng: sellerCoords.lng,
                        buyerSignature: buyerReply.buyerSig,
                        sellerSignature
                      })

                      verified.value = true
                      running.value = false
                      connectingDevice = false
                      stageTitle.value = '蓝牙近场加密碰面核销成功'
                      stageLog.value = '双端挑战签名与校园围栏定位均已存证，担保资金已结算入账'
                      uni.closeBLEConnection({ deviceId })
                    } catch (e: any) {
                      handleBleError('服务端验签未通过', e)
                      uni.closeBLEConnection({ deviceId })
                    }
                  })

                  uni.writeBLECharacteristicValue({
                    deviceId,
                    serviceId: sess.serviceUuid,
                    characteristicId: sess.characteristicUuid,
                    value: stringToArrayBuffer(sellerNonce) as any,
                    fail: (err) => {
                      handleBleError('写入卖方挑战随机数失败', err)
                      uni.closeBLEConnection({ deviceId })
                    }
                  })
                },
                fail: (err) => {
                  handleBleError('开启特征值通知失败', err)
                  uni.closeBLEConnection({ deviceId })
                }
              })
            },
            fail: (err) => {
              handleBleError('读取蓝牙特征值失败', err)
              uni.closeBLEConnection({ deviceId })
            }
          })
        },
        fail: (err) => {
          handleBleError('读取蓝牙服务列表失败', err)
          uni.closeBLEConnection({ deviceId })
        }
      })
    },
    fail: (err) => handleBleError('建立低功耗蓝牙连接失败', err)
  })
}

function goBackOrder() {
  uni.navigateBack()
}

onLoad((q) => {
  orderId.value = Number(q?.orderId || 0)
})

onUnload(() => {
  try {
    if (peripheralServer) {
      peripheralServer.stopAdvertising()
      peripheralServer.close()
    }
    uni.stopBluetoothDevicesDiscovery({})
    uni.closeBluetoothAdapter({})
  } catch {
    // ignore cleanup errors
  }
})
</script>

<style scoped lang="scss">
.ble-page {
  min-height: 100vh;
  background: #09090b;
  color: #ffffff;
  padding: 48rpx 36rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
}
.radar-stage {
  position: relative;
  height: 520rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  .ring {
    position: absolute;
    border-radius: 50%;
    border: 2rpx dashed rgba(16, 185, 129, 0.35);
  }
  .ring-1 {
    width: 340rpx;
    height: 340rpx;
  }
  .ring-2 {
    width: 480rpx;
    height: 480rpx;
  }
  .core-node {
    width: 210rpx;
    height: 210rpx;
    border-radius: 50%;
    background: rgba(16, 185, 129, 0.16);
    border: 3rpx solid #18181b;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    .rssi-val {
      font-size: 34rpx;
      font-weight: 800;
      color: #e5c79e;
    }
    .role-tag {
      font-size: 22rpx;
      color: #a1a1aa;
      margin-top: 6rpx;
    }
  }
}
.status-panel {
  background: #18181b;
  border-radius: 32rpx;
  padding: 36rpx;
  .stage-title {
    display: block;
    font-size: 32rpx;
    font-weight: 700;
    margin-bottom: 12rpx;
  }
  .stage-sub {
    display: block;
    font-size: 25rpx;
    color: #a1a1aa;
    margin-bottom: 24rpx;
  }
  .crypto-meta {
    background: #27272a;
    border-radius: 18rpx;
    padding: 20rpx;
    margin-bottom: 28rpx;
    text {
      display: block;
      font-size: 23rpx;
      color: #d4d4d8;
      margin-bottom: 6rpx;
    }
  }
  .start-btn,
  .done-btn {
    height: 88rpx;
    line-height: 88rpx;
    border-radius: 999rpx;
    text-align: center;
    font-size: 28rpx;
    font-weight: 700;
  }
  .start-btn {
    background: #e5c79e;
    color: #09090b;
  }
  .done-btn {
    background: #ffffff;
    color: #09090b;
  }
}
</style>
