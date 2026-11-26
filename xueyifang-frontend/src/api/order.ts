/**
 * Order API module
 * Handles all order-related API calls
 */

import request from './request'
import type { PageResult, PageParams } from '@/types/http'
import type {
  OrderListItem,
  OrderDetail,
  CreateOrderRequest,
  PayOrderRequest,
  RefundOrderRequest,
  SellerHandleRefundRequest,
  SellerShipPayload,
  OrderVerifyCredential,
  OrderVerifyQrPayload,
  OrderStatus
} from '@/types/order'

// ==================== Order APIs ====================

/**
 * Create a new order
 * @param data Order creation data
 * @returns Created order ID
 */
export function createOrder(data: CreateOrderRequest): Promise<number> {
  return request<number>({
    url: '/order/create',
    method: 'post',
    data
  })
}

/**
 * Pay for an order
 * @param orderId Order ID
 * @param data Payment data (optional)
 * @returns void
 */
export function payOrder(orderId: number, data?: PayOrderRequest): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/pay`,
    method: 'post',
    data: data || { orderId }
  })
}

/**
 * Get order detail
 * @param orderId Order ID
 * @returns Order detail information
 */
export function getOrderDetail(orderId: number | string): Promise<OrderDetail> {
  return request<OrderDetail>({
    url: `/order/${orderId}`,
    method: 'get'
  })
}

/**
 * Get buyer's order list (我的订单)
 * @param params Query parameters with pagination and status filter
 * @returns Paginated order list
 */
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

/**
 * Get seller's order list (我的售出订单)
 * @param params Query parameters with pagination and status filter
 * @returns Paginated order list
 */
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

/**
 * Cancel an order
 * @param orderId Order ID
 * @returns Success status
 */
export function cancelOrder(orderId: number): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/cancel`,
    method: 'post'
  })
}

/**
 * Buyer confirm order completion (买家确认收货)
 * @param orderId Order ID
 * @returns Success status
 */
export function buyerConfirm(orderId: number): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/confirm`,
    method: 'post'
  })
}

/**
 * Seller ship order (卖家发货，可选携带交付说明与凭证截图)
 */
export function sellerShip(orderId: number, data?: SellerShipPayload): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/ship`,
    method: 'post',
    data: data || {}
  })
}

/**
 * 买方获取线下订单实时动态核销凭证（30s动态数字码 + 10s防截图动态二维码载荷）
 */
export function getOfflineVerifyCredential(orderId: number): Promise<OrderVerifyCredential> {
  return request<OrderVerifyCredential>({
    url: `/order/${orderId}/verify-credential`,
    method: 'get'
  })
}

/**
 * 校内线下交易当面核销码结单（卖家输入买家30秒动态6位核销码）
 */
export function verifyOfflineCode(orderId: number, verifyCode: string): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/verify-code`,
    method: 'post',
    data: { verifyCode }
  })
}

/**
 * 校内线下交易实时摄像头动态二维码核销结单（10秒有效期，禁用截图识别）
 */
export function verifyOfflineQr(data: OrderVerifyQrPayload): Promise<number> {
  return request<number>({
    url: '/order/verify-qr',
    method: 'post',
    data
  })
}

/**
 * Seller handle refund request (卖家处理退款) - v2.0 new
 * @param orderId Order ID
 * @param data Handle refund data (approve/reject)
 * @returns Success status
 */
export function sellerHandleRefund(orderId: number, data: SellerHandleRefundRequest): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/handleRefund`,
    method: 'post',
    data
  })
}

/**
 * @deprecated Use sellerShip instead (v2.0)
 * Seller confirm order (卖家确认订单) - 已废弃
 * @param orderId Order ID
 * @returns Success status
 */
export function sellerConfirm(orderId: number): Promise<void> {
  console.warn('sellerConfirm is deprecated, use sellerShip instead')
  return sellerShip(orderId)
}

/**
 * Apply for refund
 * @param orderId Order ID
 * @param reason Refund reason
 * @param remark Optional remark
 * @returns void
 */
export function applyRefund(orderId: number, reason: string, remark?: string): Promise<void> {
  return request<void>({
    url: `/order/${orderId}/refund`,
    method: 'post',
    data: { reason, remark }
  })
}

// ==================== Helper Functions ====================

/**
 * Build order detail URL
 * @param orderId Order ID
 * @returns Full URL for order detail page
 */
export function buildOrderDetailUrl(orderId: number | string): string {
  return `/orders/${orderId}`
}

/**
 * Format order list for display
 * Adds computed fields for UI display
 * @param orders Original order list
 * @returns Enhanced order list
 */
export function formatOrderList(orders: OrderListItem[]): OrderListItem[] {
  return orders.map(order => ({
    ...order,
    // Add any computed fields if needed
  }))
}