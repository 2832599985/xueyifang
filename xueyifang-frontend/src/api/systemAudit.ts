import request from './request'

export interface SecurityCheckItemData {
  checkCode: string
  checkTitle: string
  passed: boolean
  severity: 'PASS' | 'WARNING' | 'CRITICAL' | string
  detail: string
  recommendation: string
}

export interface SystemHealthAuditData {
  auditBatchNo: string
  inspectTime: string
  overallHealthScore: number
  healthLevel: 'EXCELLENT' | 'WARNING' | 'CRITICAL' | string
  negativeWalletCount: number
  escrowMismatchCount: number
  unverifiedActiveSellerCount: number
  pendingHighRiskAlertCount: number
  overdueUncompensatedOrderCount: number
  totalUsers: number
  totalActiveServices: number
  totalCompletedOrders: number
  totalGroupSessions: number
  totalDemands: number
  compositeIndexOptimized: boolean
  securityChecks: SecurityCheckItemData[]
}

export interface OrderDeliveryCertificateData {
  certificateNo: string
  orderId: number
  orderNumber: string
  serviceId: number
  serviceTitle: string
  serviceTagName: string
  campusId?: number
  campusName: string
  sellerId: number
  sellerName: string
  sellerStudentIdMasked: string
  sellerEduVerified: number
  buyerId: number
  buyerName: string
  buyerStudentIdMasked: string
  totalAmount: number
  tradeType: number
  verifyChannel: string
  meetupPoiName?: string
  practiceCreditHours: number
  completedTime: string
  sha256Seal: string
  verificationStatus: string
}

export function inspectSystemHealth(): Promise<SystemHealthAuditData> {
  return request<SystemHealthAuditData>({
    url: '/admin/system-audit/inspect',
    method: 'get'
  })
}

export function getOrderCertificate(orderId: number): Promise<OrderDeliveryCertificateData> {
  return request<OrderDeliveryCertificateData>({
    url: `/order/certificate/${orderId}`,
    method: 'get'
  })
}

export function verifyOrderCertificateSeal(
  orderId: number,
  sealHash: string
): Promise<{
  orderId: number
  certificateNo: string
  authentic: boolean
  expectedSeal: string
  message: string
}> {
  return request({
    url: `/order/certificate/${orderId}/verify`,
    method: 'get',
    params: { sealHash }
  })
}
