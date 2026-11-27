export interface DemandBidItem {
  id: number
  demandId: number
  bidderId: number
  bidderName?: string
  bidderAvatar?: string
  bidderProfessional?: string
  bidderEduVerified?: number
  bidderCreditScore?: number
  bidPrice: number
  estimatedHours: number
  bidPlan: string
  bidStatus: number // 0=待选标, 1=已中标转单, 2=未中标
  demandTitle?: string
  matchedOrderId?: number
  createTime: string
}

export interface DemandItem {
  id: number
  demandNo: string
  publisherId: number
  publisherName?: string
  publisherAvatar?: string
  publisherEduVerified?: number
  publisherCreditScore?: number
  campusId: number
  campusName?: string
  tagId?: number
  tagName?: string
  title: string
  description: string
  budgetMin: number
  budgetMax: number
  tradeType: number // 1=OFFLINE, 2=ONLINE
  urgencyLevel: number // 1=常规, 2=加急, 3=十万火急
  deadlineTime?: string
  meetupLat?: number
  meetupLng?: number
  meetupPoiName?: string
  meetupAddress?: string
  meetupRadiusMeters?: number
  bidCount: number
  demandStatus: number // 0=招募竞标中, 1=已选标转担保单, 2=已取消, 3=管理员关闭
  acceptedBidId?: number
  matchedServiceId?: number
  matchedOrderId?: number
  closeReason?: string
  createTime: string
  bids?: DemandBidItem[]
}

export interface DemandCreatePayload {
  campusId?: number
  tagId?: number
  title: string
  description: string
  budgetMin: number
  budgetMax: number
  tradeType: number
  urgencyLevel: number
  deadlineTime?: string
  meetupLat?: number
  meetupLng?: number
  meetupPoiName?: string
  meetupAddress?: string
  meetupRadiusMeters?: number
}

export interface CreatorProfileData {
  userId: number
  displayName: string
  avatar?: string
  grade?: string
  professionalName?: string
  campusName?: string
  bio?: string
  eduVerified: number
  creditScore: number
  creditTier: string
  publishPermission: number
  sellerDeposit?: number
  completedOrderCount: number
  activeServiceCount: number
  reviewCount: number
  averageRating?: number
  services: Array<{
    id: number
    serviceTitle: string
    serviceDescription: string
    coverImage?: string
    tagName?: string
    tradeType: number
    price: number
    currentPurchaseCount: number
    averageRating?: number
  }>
  recentReviews: Array<{
    id: number
    serviceId: number
    serviceTitle: string
    buyerDisplayName: string
    rating: number
    content: string
    createTime: string
  }>
}
