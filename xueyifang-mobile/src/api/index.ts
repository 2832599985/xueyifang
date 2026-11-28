import { httpRequest } from '@/utils/request'
import type { MobileUser } from '@/stores/user'

export interface CampusItem {
  id: number
  campusCode: string
  campusName: string
  city: string
  centerLat: number
  centerLng: number
  safeRadiusMeters: number
  eduEmailDomains: string
}

export interface TimeSlotItem {
  id: number
  serviceId: number
  slotStart: string
  slotEnd: string
  remainingCapacity: number
}

export interface BleSessionPayload {
  orderId: number
  sessionId: string
  advShortToken: string
  serviceUuid: string
  characteristicUuid: string
  buyerNonce: string
  roleSigningKey: string
  role: 'BUYER' | 'SELLER'
  minRssiDbm: number
  expireSeconds: number
}

export const mobileApi = {
  login(studentId: string, password: string) {
    return httpRequest<{ token: string; userInfo?: MobileUser } & Partial<MobileUser>>({
      url: '/auth/login',
      method: 'POST',
      data: { studentId, password }
    })
  },

  getCurrentUser() {
    return httpRequest<MobileUser>({ url: '/auth/currentUser' })
  },

  logout() {
    return httpRequest<void>({
      url: '/auth/logout',
      method: 'POST'
    })
  },

  sendEduCode(eduEmail: string) {
    return httpRequest<{ eduEmail: string; expireSeconds: number; devPreviewCode?: string }>({
      url: '/auth/edu-verify/send',
      method: 'POST',
      data: { eduEmail }
    })
  },

  confirmEduCode(eduEmail: string, verifyCode: string) {
    return httpRequest<void>({
      url: '/auth/edu-verify/confirm',
      method: 'POST',
      data: { eduEmail, verifyCode }
    })
  },

  listCampuses() {
    return httpRequest<CampusItem[]>({ url: '/campus/list' })
  },

  checkFence(campusId: number, lat: number, lng: number) {
    return httpRequest<{ inside: boolean; message: string }>({
      url: `/campus/check-fence?campusId=${campusId}&lat=${lat}&lng=${lng}`
    })
  },

  listServices(params: { keyword?: string; tagId?: number; pageNum?: number; pageSize?: number }) {
    const q: string[] = [`pageNum=${params.pageNum || 1}`, `pageSize=${params.pageSize || 12}`]
    if (params.keyword) q.push(`keyword=${encodeURIComponent(params.keyword)}`)
    if (params.tagId) q.push(`tagId=${params.tagId}`)
    return httpRequest<{ records: any[]; total: number }>({
      url: `/service/list?${q.join('&')}`
    })
  },

  getServiceDetail(id: number) {
    return httpRequest<any>({ url: `/service/${id}` })
  },

  listDemands(params?: { keyword?: string; pageNum?: number; pageSize?: number }) {
    const q: string[] = [`pageNum=${params?.pageNum || 1}`, `pageSize=${params?.pageSize || 15}`]
    if (params?.keyword) q.push(`keyword=${encodeURIComponent(params.keyword)}`)
    return httpRequest<{ records: any[]; total: number }>({
      url: `/demand/list?${q.join('&')}`
    })
  },

  getDemandDetail(demandId: number) {
    return httpRequest<any>({ url: `/demand/${demandId}` })
  },

  publishDemand(payload: Record<string, any>) {
    return httpRequest<number>({
      url: '/demand/publish',
      method: 'POST',
      data: payload
    })
  },

  submitDemandBid(demandId: number, payload: { bidPrice: number; estimatedHours: number; bidPlan: string }) {
    return httpRequest<number>({
      url: `/demand/${demandId}/bid`,
      method: 'POST',
      data: payload
    })
  },

  listGroupSessions(params?: { keyword?: string; pageNum?: number; pageSize?: number }) {
    const q: string[] = [`pageNum=${params?.pageNum || 1}`, `pageSize=${params?.pageSize || 15}`]
    if (params?.keyword) q.push(`keyword=${encodeURIComponent(params.keyword)}`)
    return httpRequest<{ records: any[]; total: number }>({
      url: `/group-session/list?${q.join('&')}`
    })
  },

  joinGroupSession(sessionId: number) {
    return httpRequest<number>({
      url: `/group-session/${sessionId}/join`,
      method: 'POST'
    })
  },

  getPricingBenchmark(params: { tagId?: number; campusId?: number; tradeType?: number; proposedPrice?: number }) {
    const q: string[] = []
    if (params.tagId) q.push(`tagId=${params.tagId}`)
    if (params.campusId) q.push(`campusId=${params.campusId}`)
    if (params.tradeType) q.push(`tradeType=${params.tradeType}`)
    if (params.proposedPrice !== undefined) q.push(`proposedPrice=${params.proposedPrice}`)
    return httpRequest<any>({
      url: `/service/pricing-benchmark${q.length ? '?' + q.join('&') : ''}`
    })
  },

  getRiskRadarOverview() {
    return httpRequest<any>({
      url: '/admin/risk-radar/overview'
    })
  },

  getOrderNoShowClaim(orderId: number) {
    return httpRequest<any>({
      url: `/order/noshow/${orderId}`
    })
  },

  claimSellerNoShow(orderId: number, evidenceNote?: string) {
    return httpRequest<any>({
      url: `/order/noshow/${orderId}/claim`,
      method: 'POST',
      data: { evidenceNote }
    })
  },

  getOrderCertificate(orderId: number) {
    return httpRequest<any>({
      url: `/order/certificate/${orderId}`
    })
  },

  verifyOrderCertificateSeal(orderId: number, sealHash: string) {
    return httpRequest<any>({
      url: `/order/certificate/${orderId}/verify?sealHash=${encodeURIComponent(sealHash)}`
    })
  },

  getCreatorLeaderboard(params?: { campusId?: number; tagId?: number; rankBy?: string; limit?: number }) {
    const q: string[] = []
    if (params?.campusId) q.push(`campusId=${params.campusId}`)
    if (params?.tagId) q.push(`tagId=${params.tagId}`)
    if (params?.rankBy) q.push(`rankBy=${encodeURIComponent(params.rankBy)}`)
    if (params?.limit) q.push(`limit=${params.limit}`)
    return httpRequest<any[]>({
      url: `/creator/leaderboard${q.length ? '?' + q.join('&') : ''}`
    })
  },

  createOrder(payload: Record<string, any>) {
    return httpRequest<number>({
      url: '/order/create',
      method: 'POST',
      data: payload
    })
  },

  escrowPrepay(orderId: number, payChannel = 'WECHAT_MINI') {
    return httpRequest<{
      paymentNo: string
      orderId: number
      amount: number
      sdkPayParams: Record<string, string>
      sandboxCallbackSign?: string
    }>({
      url: '/payment/escrow/prepay',
      method: 'POST',
      data: { orderId, payChannel }
    })
  },

  sandboxEscrowCallback(paymentNo: string, paidAmount: number, signature: string) {
    return httpRequest<void>({
      url: '/payment/escrow/webhook/callback',
      method: 'POST',
      data: { paymentNo, paidAmount, signature }
    })
  },

  listMyOrders(role: 'BUYER' | 'SELLER', pageNum = 1) {
    const path = role === 'SELLER' ? '/order/mySellingOrders' : '/order/myOrders'
    return httpRequest<{ records: any[]; total: number }>({
      url: `${path}?pageNum=${pageNum}&pageSize=20`
    })
  },

  getOrderDetail(orderId: number) {
    return httpRequest<any>({ url: `/order/${orderId}` })
  },

  proposeMeetup(orderId: number, data: Record<string, any>) {
    return httpRequest<number>({
      url: `/order/${orderId}/meetup/propose`,
      method: 'POST',
      data
    })
  },

  respondMeetup(proposalId: number, accept: boolean, rejectReason?: string) {
    return httpRequest<void>({
      url: `/order/meetup/${proposalId}/respond`,
      method: 'POST',
      data: { accept, rejectReason }
    })
  },

  listMeetupProposals(orderId: number) {
    return httpRequest<any[]>({ url: `/order/${orderId}/meetup/proposals` })
  },

  getOfflineCredential(orderId: number) {
    return httpRequest<any>({ url: `/order/${orderId}/verify-credential` })
  },

  verifyQrToken(qrToken: string, expectedOrderId?: number, verifierLat?: number, verifierLng?: number) {
    return httpRequest<number>({
      url: '/order/verify-qr',
      method: 'POST',
      data: { qrToken, expectedOrderId, verifierLat, verifierLng }
    })
  },

  openBleSession(orderId: number) {
    return httpRequest<BleSessionPayload>({
      url: `/order/${orderId}/ble/session`,
      method: 'POST'
    })
  },

  submitBleVerify(orderId: number, data: Record<string, any>) {
    return httpRequest<void>({
      url: `/order/${orderId}/ble/verify`,
      method: 'POST',
      data
    })
  }
}
