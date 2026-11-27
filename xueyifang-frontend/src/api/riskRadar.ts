import request from './request'
import type { PageResult } from '@/types/http'

export interface PricingBenchmarkData {
  tagId?: number
  tagName: string
  campusId?: number
  tradeType?: number
  completedOrderSampleCount: number
  activeListingSampleCount: number
  totalSampleCount: number
  minPrice: number
  p25Price: number
  p50Price: number
  p75Price: number
  p90Price: number
  maxPrice: number
  avgPrice: number
  recommendedMinPrice: number
  recommendedMaxPrice: number
  proposedPrice: number
  percentileRank: number
  pricePositionCode: 'BELOW_P25_VALUE' | 'SWEET_SPOT_P25_P75' | 'PREMIUM_P75_P90' | 'ABOVE_P90_OUTLIER' | string
  pricePositionLabel: string
  conversionAdvice: string
}

export interface RiskRadarOverviewData {
  totalAlerts: number
  pendingAlerts: number
  highSeverityAlerts: number
  confirmedPenalties: number
  dismissedAlerts: number
  scannedOrders7d: number
  bidirectionalWashCount: number
  highFreqPairCount: number
  ultraFastCompletionCount: number
}

export interface RiskAuditAlertItem {
  id: number
  alertNo: string
  ruleCode: string
  ruleName: string
  severity: number
  riskScore: number
  orderId: number
  orderNumber?: string
  serviceId?: number
  serviceTitle?: string
  buyerId: number
  buyerName?: string
  buyerStudentId?: string
  buyerCreditScore?: number
  sellerId: number
  sellerName?: string
  sellerStudentId?: string
  sellerCreditScore?: number
  campusId?: number
  orderAmount: number
  pairOrderCount7d?: number
  reverseOrderCount7d?: number
  fulfillDurationSeconds?: number
  evidenceSummary: string
  handleStatus: number // 0=待核查, 1=确认违规并处罚, 2=排除误报
  penaltyAction?: string
  creditDeducted?: number
  handlerId?: number
  handleRemark?: string
  handleTime?: string
  createTime: string
}

export function getPricingBenchmark(params: {
  tagId?: number
  campusId?: number
  tradeType?: number
  proposedPrice?: number
}): Promise<PricingBenchmarkData> {
  return request<PricingBenchmarkData>({
    url: '/service/pricing-benchmark',
    method: 'get',
    params
  })
}

export function getRiskRadarOverview(): Promise<RiskRadarOverviewData> {
  return request<RiskRadarOverviewData>({
    url: '/admin/risk-radar/overview',
    method: 'get'
  })
}

export function listRiskAlerts(params: {
  pageNum?: number
  pageSize?: number
  ruleCode?: string
  severity?: number
  handleStatus?: number
  keyword?: string
}): Promise<PageResult<RiskAuditAlertItem>> {
  return request<PageResult<RiskAuditAlertItem>>({
    url: '/admin/risk-radar/list',
    method: 'get',
    params
  })
}

export function scanRiskRadarOrders(days = 7): Promise<number> {
  return request<number>({
    url: `/admin/risk-radar/scan?days=${days}`,
    method: 'post'
  })
}

export function evaluateOrderRisk(orderId: number): Promise<RiskAuditAlertItem[]> {
  return request<RiskAuditAlertItem[]>({
    url: `/admin/risk-radar/evaluate/${orderId}`,
    method: 'post'
  })
}

export function resolveRiskAlert(
  id: number,
  data: {
    handleStatus: number
    creditDeductPoints?: number
    offlineService?: boolean
    handleRemark?: string
  }
): Promise<RiskAuditAlertItem> {
  return request<RiskAuditAlertItem>({
    url: `/admin/risk-radar/${id}/resolve`,
    method: 'post',
    data
  })
}
