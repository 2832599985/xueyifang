export type { ServiceTag, Professional, TradeLocation, CampusInfo } from '@/types/dict'
import type { TradeLocation } from '@/types/dict'
import { formatPrice as sharedFormatPrice, formatCompactNumber } from '@/utils/format'

export const formatPrice = sharedFormatPrice

export interface ServiceTimeSlot {
  id: number
  serviceId: number
  slotStart: string
  slotEnd: string
  maxCapacity: number
  bookedCount: number
  remainingCapacity: number
  status: number
}

export interface ServiceListItem {
  id: number
  serviceTitle: string
  price: number
  tagId: number
  tagName?: string
  tradeType: number
  coverImage: string
  viewCount: number
  collectionCount?: number
  salesCount?: number
  averageRating?: number
  sellerName?: string
  sellerAvatar?: string
  sellerId?: number
  createTime: string
}

export interface ServiceDetail {
  id: number
  serviceTitle: string
  serviceDescription: string
  sellerId: number
  sellerName?: string
  sellerAvatar?: string
  sellerRating?: number
  sellerProfessional?: string
  sellerGrade?: string
  sellerBio?: string
  sellerCompletedOrders?: number
  sellerEduVerified?: number
  sellerCreditScore?: number
  campusId?: number
  campusName?: string
  tagId: number
  tagName?: string
  tradeType: number
  tradeTypeText?: string
  tradeLocationId?: number
  tradeLocationName?: string
  tradeLocationLat?: number
  tradeLocationLng?: number
  tradeLocation?: TradeLocation
  price: number
  maxPurchases: number
  currentPurchaseCount: number
  isPhysical: number
  requireSlot?: number
  cancelFreeHours?: number
  requireBleVerify?: number
  availableSlots?: ServiceTimeSlot[]
  status: number
  reviewStatus: number
  reviewReason?: string
  viewCount: number
  collectionCount: number
  salesCount?: number
  reviewCount?: number
  averageRating?: number
  createTime: string
  updateTime?: string
  images: string[]
  isCollected?: boolean
  deliveryTime?: string
  requirements?: string
  coverImage?: string
}

export type ServiceSortBy = 'latest' | 'create_desc' | 'price_asc' | 'price_desc' | 'view_desc'

export interface ServiceQueryParams {
  keyword?: string
  tagId?: number
  tradeType?: number
  sortBy?: ServiceSortBy
  sellerId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ServiceFormData {
  serviceTitle: string
  serviceDescription: string
  campusId?: number
  tagId: number
  price: number
  tradeType: number
  tradeLocationId?: number
  maxPurchases: number
  isPhysical: number
  requireSlot?: number
  cancelFreeHours?: number
  requireBleVerify?: number
  images: string[]
}

export type ServicePublishRequest = ServiceFormData
export type ServiceUpdateRequest = ServiceFormData

export interface MyServiceItem extends ServiceListItem {
  status: number
  reviewStatus: number
  reviewReason?: string
  updateTime?: string
  maxPurchases?: number
  currentPurchaseCount?: number
  canEdit?: boolean
  canOnline?: boolean
  canOffline?: boolean
}

export interface FavoriteListItem {
  favoriteId: number
  service: ServiceListItem
  sellerName: string
  createTime: string
}

export interface FavoriteRequest {
  serviceId: number
}

export enum ServiceStatus {
  ONLINE = 1,
  OFFLINE = 2,
  PENDING_REVIEW = 3,
  REJECTED = 4
}

export enum ReviewStatus {
  APPROVED = 1,
  PENDING = 2,
  REJECTED = 3
}

export enum TradeType {
  OFFLINE = 1,
  ONLINE_ESCROW = 2
}

export enum PhysicalType {
  VIRTUAL = 0,
  PHYSICAL = 1
}

export function getTradeTypeText(tradeType: number): string {
  switch (tradeType) {
    case TradeType.OFFLINE:
      return '线下交易'
    case TradeType.ONLINE_ESCROW:
      return '线上担保'
    default:
      return '未知'
  }
}

export function getServiceStatusText(status: number): string {
  switch (status) {
    case ServiceStatus.ONLINE:
      return '已上架'
    case ServiceStatus.OFFLINE:
      return '已下架'
    case ServiceStatus.PENDING_REVIEW:
      return '审核中'
    case ServiceStatus.REJECTED:
      return '已驳回'
    default:
      return '未知'
  }
}

export function getServiceStatusType(status: number): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case ServiceStatus.ONLINE:
      return 'success'
    case ServiceStatus.OFFLINE:
      return 'info'
    case ServiceStatus.PENDING_REVIEW:
      return 'warning'
    case ServiceStatus.REJECTED:
      return 'danger'
    default:
      return 'info'
  }
}

export function getReviewStatusText(reviewStatus: number): string {
  switch (reviewStatus) {
    case ReviewStatus.APPROVED:
      return '已通过'
    case ReviewStatus.PENDING:
      return '审核中'
    case ReviewStatus.REJECTED:
      return '已驳回'
    default:
      return '未知'
  }
}

export function getReviewStatusType(reviewStatus: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (reviewStatus) {
    case ReviewStatus.APPROVED:
      return 'success'
    case ReviewStatus.PENDING:
      return 'warning'
    case ReviewStatus.REJECTED:
      return 'danger'
    default:
      return 'info'
  }
}

export function canEditService(status: number): boolean {
  return status === ServiceStatus.OFFLINE || status === ServiceStatus.REJECTED
}

export function canOnlineService(status: number, _reviewStatus?: number): boolean {
  return status === ServiceStatus.OFFLINE || status === ServiceStatus.REJECTED
}

export function canOfflineService(status: number): boolean {
  return status === ServiceStatus.ONLINE || status === ServiceStatus.PENDING_REVIEW
}

export function formatSalesCount(count?: number): string {
  if (!count) return '暂无销量'
  return `${formatCompactNumber(count)}人购买`
}
