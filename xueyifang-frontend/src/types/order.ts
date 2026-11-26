/**
 * Order-related type definitions aligned with backend OrderConstant and VOs
 */
import type { TradeLocation } from '@/types/dict'

export enum OrderStatus {
  UNPAID = 1,
  PENDING_SHIP = 2,
  PENDING_RECEIPT = 3,
  COMPLETED = 4,
  CANCELLED = 5,
  FAILED = 6
}

export enum RefundStatus {
  NONE = 0,
  PENDING = 1,
  APPROVED = 2,
  REJECTED = 3,
  COMPLETED = 4
}

export enum PaymentStatus {
  UNPAID = 1,
  PAID = 2,
  REFUNDED = 3
}

export enum PaymentMethod {
  WALLET = 1
}

export enum TradeType {
  OFFLINE = 1,
  ONLINE = 2
}

export type OrderRole = 'BUYER' | 'SELLER'

export interface OrderListItem {
  id: number
  orderNumber: string
  serviceId: number
  serviceTitle: string
  serviceImage?: string
  unitPrice: number
  quantity: number
  totalAmount: number
  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  sellerId?: number
  sellerName?: string
  sellerAvatar?: string
  buyerId?: number
  buyerName?: string
  buyerAvatar?: string
  createTime: string
  remark?: string
  tradeType?: TradeType
  refundStatus?: RefundStatus
}

export type { TradeLocation }

export interface OrderLog {
  id?: number
  orderId: number
  operatorId: number
  operatorName?: string
  action: string
  actionDesc?: string
  createTime: string
}

export interface OrderDetail {
  id: number
  orderNumber: string
  serviceId: number
  serviceTitle: string
  serviceDescription?: string
  serviceImage?: string
  serviceTagName?: string
  unitPrice: number
  quantity: number
  totalAmount: number
  tradeType: TradeType
  tradeLocationId?: number
  tradeLocationName?: string
  tradeLocation?: TradeLocation
  orderStatus: OrderStatus
  paymentStatus: PaymentStatus
  paymentMethod?: PaymentMethod
  frozenAmount?: number
  buyerId: number
  buyerName: string
  buyerAvatar?: string
  buyerProfessional?: string
  sellerId: number
  sellerName: string
  sellerAvatar?: string
  sellerProfessional?: string
  paymentTime?: string
  sellerShipTime?: string
  buyerConfirmTime?: string
  createTime: string
  remark?: string
  deliveryNote?: string
  deliveryImages?: string[]
  verifyCode?: string
  refundStatus?: RefundStatus
  refundReason?: string
  refundRequestTime?: string
  logs?: OrderLog[]
  isReviewed?: boolean
}

export interface SellerShipPayload {
  deliveryNote?: string
  deliveryImages?: string[]
}

export interface OrderVerifyCredential {
  orderId: number
  orderNumber: string
  orderStatus: OrderStatus
  verifyCode?: string | null
  codePeriodSeconds: number
  codeRemainingSeconds: number
  qrToken?: string | null
  qrPeriodSeconds: number
  qrRemainingSeconds: number
  serverTimestamp: number
}

export interface OrderVerifyQrPayload {
  qrToken: string
  expectedOrderId?: number
}

export interface CreateOrderRequest {
  serviceId: number
  quantity: number
  tradeType: TradeType
  tradeLocationId?: number
  remark?: string
}

export interface PayOrderRequest {
  orderId: number
  paymentMethod?: PaymentMethod
}

export interface RefundOrderRequest {
  orderId: number
  reason: string
}

export interface SellerHandleRefundRequest {
  approve: boolean
  rejectReason?: string
}

export function getOrderStatusText(status: OrderStatus): string {
  switch (status) {
    case OrderStatus.UNPAID:
      return '待支付'
    case OrderStatus.PENDING_SHIP:
      return '待发货'
    case OrderStatus.PENDING_RECEIPT:
      return '待收货'
    case OrderStatus.COMPLETED:
      return '已完成'
    case OrderStatus.CANCELLED:
      return '已取消'
    case OrderStatus.FAILED:
      return '交易失败'
    default:
      return '未知'
  }
}

export function getOrderStatusType(status: OrderStatus): 'warning' | 'primary' | 'info' | 'success' | 'danger' {
  switch (status) {
    case OrderStatus.UNPAID:
      return 'warning'
    case OrderStatus.PENDING_SHIP:
      return 'primary'
    case OrderStatus.PENDING_RECEIPT:
      return 'primary'
    case OrderStatus.COMPLETED:
      return 'success'
    case OrderStatus.CANCELLED:
      return 'info'
    case OrderStatus.FAILED:
      return 'danger'
    default:
      return 'info'
  }
}

export function getOrderStatusColor(status: OrderStatus): string {
  const colorMap: Record<OrderStatus, string> = {
    [OrderStatus.UNPAID]: '#B45309',
    [OrderStatus.PENDING_SHIP]: '#18181B',
    [OrderStatus.PENDING_RECEIPT]: '#0369A1',
    [OrderStatus.COMPLETED]: '#15803D',
    [OrderStatus.CANCELLED]: '#838A97',
    [OrderStatus.FAILED]: '#B91C1C'
  }
  return colorMap[status] || '#838A97'
}

export function getPaymentStatusText(status: PaymentStatus): string {
  switch (status) {
    case PaymentStatus.UNPAID:
      return '未支付'
    case PaymentStatus.PAID:
      return '已支付'
    case PaymentStatus.REFUNDED:
      return '已退款'
    default:
      return '未知'
  }
}

export function getPaymentStatusType(status: PaymentStatus): 'warning' | 'success' | 'danger' | 'info' {
  switch (status) {
    case PaymentStatus.UNPAID:
      return 'warning'
    case PaymentStatus.PAID:
      return 'success'
    case PaymentStatus.REFUNDED:
      return 'danger'
    default:
      return 'info'
  }
}

export function getRefundStatusText(status?: RefundStatus): string {
  switch (status) {
    case RefundStatus.PENDING:
      return '退款申请中'
    case RefundStatus.APPROVED:
      return '退款已同意'
    case RefundStatus.REJECTED:
      return '退款被拒绝'
    case RefundStatus.COMPLETED:
      return '已退款'
    default:
      return ''
  }
}

export function getRefundStatusType(status?: RefundStatus): 'info' | 'warning' | 'success' | 'danger' {
  switch (status) {
    case RefundStatus.PENDING:
      return 'warning'
    case RefundStatus.APPROVED:
      return 'success'
    case RefundStatus.REJECTED:
      return 'danger'
    case RefundStatus.COMPLETED:
      return 'danger'
    default:
      return 'info'
  }
}

export function getTradeTypeText(type: TradeType): string {
  switch (type) {
    case TradeType.OFFLINE:
      return '线下交易'
    case TradeType.ONLINE:
      return '线上担保'
    default:
      return '未知'
  }
}

export function canPayOrder(status: OrderStatus): boolean {
  return status === OrderStatus.UNPAID
}

export function canCancelOrder(status: OrderStatus): boolean {
  return status === OrderStatus.UNPAID
}

export function canBuyerConfirm(status: OrderStatus, refundStatus?: RefundStatus): boolean {
  return (
    status === OrderStatus.PENDING_RECEIPT &&
    (refundStatus == null || refundStatus === RefundStatus.NONE || refundStatus === RefundStatus.REJECTED)
  )
}

export function canSellerShip(status: OrderStatus): boolean {
  return status === OrderStatus.PENDING_SHIP
}

export function canSellerHandleRefund(status: OrderStatus, refundStatus?: RefundStatus): boolean {
  return status === OrderStatus.PENDING_RECEIPT && refundStatus === RefundStatus.PENDING
}

export function canApplyRefund(status: OrderStatus, refundStatus?: RefundStatus): boolean {
  if (status === OrderStatus.PENDING_SHIP) {
    return true
  }
  if (status === OrderStatus.PENDING_RECEIPT) {
    return refundStatus == null || refundStatus === RefundStatus.NONE || refundStatus === RefundStatus.REJECTED
  }
  return false
}

export function canInitiateDispute(status: OrderStatus): boolean {
  return (
    status === OrderStatus.PENDING_SHIP ||
    status === OrderStatus.PENDING_RECEIPT ||
    status === OrderStatus.COMPLETED
  )
}

export function formatOrderNumber(orderNumber: string): string {
  if (!orderNumber) return ''
  if (orderNumber.length === 12) {
    return `${orderNumber.slice(0, 4)} ${orderNumber.slice(4, 8)} ${orderNumber.slice(8)}`
  }
  return orderNumber
}

export function formatCurrency(amount: number): string {
  return `¥${(amount || 0).toFixed(2)}`
}
