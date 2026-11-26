import request from './request'
import type { PageResult, PageParams } from '@/types/http'
import type {
  OrderListItem,
  OrderDetail,
  CreateOrderRequest,
  PayOrderRequest,
  SellerHandleRefundRequest,
  SellerShipPayload,
  OrderVerifyCredential,
  OrderVerifyQrPayload,
  MeetupProposal,
  EscrowPrepayResult
} from '@/types/order'

export function createOrder(data: CreateOrderRequest): Promise<number> {
  return request<number>({
    url: '/order/create',
    method: 'post',
    data
  })
}

export function payOrder(orderId: number, data?: PayOrderRequest): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/pay`,
    method: 'post',
    data: data || { orderId }
  })
}

export function createEscrowPrepay(orderId: number, payChannel = 'WECHAT_MINI'): Promise<EscrowPrepayResult> {
  return request<EscrowPrepayResult>({
    url: '/payment/escrow/prepay',
    method: 'post',
    data: { orderId, payChannel }
  })
}

export function triggerSandboxEscrowWebhook(payload: {
  paymentNo: string
  paidAmount: number
  signature: string
  channelTradeNo?: string
}): Promise<void> {
  return request<void>({
    url: '/payment/escrow/webhook/callback',
    method: 'post',
    data: {
      ...payload,
      timestamp: Math.floor(Date.now() / 1000)
    }
  })
}

export function proposeMeetupLocation(
  orderId: number,
  data: {
    latitude: number
    longitude: number
    poiName: string
    detailAddress?: string
    radiusMeters?: number
    proposedTime?: string
    remark?: string
  }
): Promise<number> {
  return request<number>({
    url: `/order/${orderId}/meetup/propose`,
    method: 'post',
    data
  })
}

export function respondMeetupProposal(
  proposalId: number,
  accept: boolean,
  rejectReason?: string
): Promise<void> {
  return request<void>({
    url: `/order/meetup/${proposalId}/respond`,
    method: 'post',
    data: { accept, rejectReason }
  })
}

export function listMeetupProposals(orderId: number): Promise<MeetupProposal[]> {
  return request<MeetupProposal[]>({
    url: `/order/${orderId}/meetup/proposals`,
    method: 'get'
  })
}

export function getOrderDetail(orderId: number | string): Promise<OrderDetail> {
  return request<OrderDetail>({
    url: `/order/${orderId}`,
    method: 'get'
  })
}

export function listBuyerOrders(
  params?: PageParams & { orderStatus?: number }
): Promise<PageResult<OrderListItem>> {
  return request<PageResult<OrderListItem>>({
    url: '/order/myOrders',
    method: 'get',
    params: {
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 10,
      orderStatus: params?.orderStatus
    }
  })
}

export function listSellerOrders(
  params?: PageParams & { orderStatus?: number }
): Promise<PageResult<OrderListItem>> {
  return request<PageResult<OrderListItem>>({
    url: '/order/mySellingOrders',
    method: 'get',
    params: {
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 10,
      orderStatus: params?.orderStatus
    }
  })
}

export function cancelOrder(orderId: number): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/cancel`,
    method: 'post'
  })
}

export function buyerConfirm(orderId: number): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/confirm`,
    method: 'post'
  })
}

export function sellerShip(orderId: number, data?: SellerShipPayload): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/ship`,
    method: 'post',
    data: data || {}
  })
}

export function getOfflineVerifyCredential(orderId: number): Promise<OrderVerifyCredential> {
  return request<OrderVerifyCredential>({
    url: `/order/${orderId}/verify-credential`,
    method: 'get'
  })
}

export function verifyOfflineCode(
  orderId: number,
  verifyCode: string,
  coords?: { verifierLat?: number; verifierLng?: number }
): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/verify-code`,
    method: 'post',
    data: {
      verifyCode,
      verifierLat: coords?.verifierLat,
      verifierLng: coords?.verifierLng
    }
  })
}

export function verifyOfflineQr(data: OrderVerifyQrPayload): Promise<number> {
  return request<number>({
    url: '/order/verify-qr',
    method: 'post',
    data
  })
}

export function sellerHandleRefund(orderId: number, data: SellerHandleRefundRequest): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/handleRefund`,
    method: 'post',
    data
  })
}

export function sellerConfirm(orderId: number): Promise<void> {
  return sellerShip(orderId)
}

export function applyRefund(orderId: number, reason: string, remark?: string): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/refund`,
    method: 'post',
    data: { reason, remark }
  })
}

export function buildOrderDetailUrl(orderId: number | string): string {
  return `/orders/${orderId}`
}

export function formatOrderList(orders: OrderListItem[]): OrderListItem[] {
  return orders.map(order => ({ ...order }))
}