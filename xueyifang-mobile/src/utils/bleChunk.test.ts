import test from 'node:test'
import assert from 'node:assert/strict'
import { encodeBleFrames, BleFrameAssembler, BLE_DEFAULT_MAX_CHUNK_BYTES } from './bleChunk'
import { stringToArrayBuffer } from './bleCrypto'

test('encodeBleFrames splits ~160-byte JSON reply into <=20-byte frames and reassembles identically', () => {
  const payload = JSON.stringify({
    buyerNonce: 'a1b2c3d4e5f60708',
    buyerSig: '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
    lat: 30.513621,
    lng: 114.355812
  })

  const frames = encodeBleFrames(payload)
  assert.ok(frames.length > 5, `expected multiple chunks, got ${frames.length}`)
  for (const frame of frames) {
    assert.ok(frame.byteLength <= BLE_DEFAULT_MAX_CHUNK_BYTES, `frame exceeded 20 bytes: ${frame.byteLength}`)
  }

  const assembler = new BleFrameAssembler()
  let result: string | null = null
  for (let i = 0; i < frames.length; i++) {
    result = assembler.push(frames[i])
    if (i < frames.length - 1) {
      assert.equal(result, null)
    }
  }
  assert.equal(result, payload)
  assert.deepEqual(JSON.parse(result!), JSON.parse(payload))
})

test('BleFrameAssembler reassembles out-of-order and duplicate frames cleanly', () => {
  const payload = '{"buyerNonce":"1234567890abcdef","buyerSig":"deadbeefcafebabe0011223344556677","lat":30.5,"lng":114.3}'
  const frames = encodeBleFrames(payload, 20)
  const reversed = [...frames].reverse()

  const assembler = new BleFrameAssembler()
  let result: string | null = null
  // Feed a duplicate frame first
  assert.equal(assembler.push(reversed[0]), null)
  for (const f of reversed) {
    result = assembler.push(f)
  }
  assert.equal(result, payload)
})

test('BleFrameAssembler accepts legacy single-packet unframed JSON', () => {
  const legacy = JSON.stringify({ buyerNonce: 'abc', buyerSig: 'def' })
  const assembler = new BleFrameAssembler()
  const result = assembler.push(stringToArrayBuffer(legacy))
  assert.equal(result, legacy)
})
