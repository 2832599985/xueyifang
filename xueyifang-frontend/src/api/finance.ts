import request from './request'

export interface FinancialReconciliationDailyItem {
  id: number
  reportNo: string
  reportDate: string
  paidOrderCount: number
  completedOrderCount: number
  refundOrderCount: number
  noshowCompensatedCount: number
  grossEscrowInflow: number
  sellerSettledAmount: number
  platformCommissionAmount: number
  refundOutflowAmount: number
  noshowPenaltyAmount: number
  activeEscrowFrozenPool: number
  discrepancyCount: number
  discrepancyAmount: number
  reconcileStatus: number // 1=总分账平, 2=存在差额预警
  auditNote?: string
  createTime: string
}

export interface AppointmentNoshowClaimItem {
  id: number
  claimNo: string
  orderId: number
  orderNumber?: string
  serviceId?: number
  buyerId: number
  sellerId: number
  campusId?: number
  noshowType: number
  scheduledStart?: string
  scheduledEnd?: string
  orderAmount: number
  refundToBuyerAmount: number
  compensationAmount: number
  compensationSource: string
  creditDeducted: number
  claimStatus: number
  evidenceNote: string
  createTime: string
}

export interface FinancialOverviewData {
  totalPlatformCommission: number
  totalSellerSettled: number
  currentEscrowFrozenPool: number
  currentSellerDepositPool: number
  totalNoshowClaims: number
  totalNoshowCompensationAmount: number
  totalReconcileReports: number
  discrepancyReportCount: number
  recentReports: FinancialReconciliationDailyItem[]
  recentNoshowClaims: AppointmentNoshowClaimItem[]
}

export function getFinanceOverview(): Promise<FinancialOverviewData> {
  return request<FinancialOverviewData>({
    url: '/admin/finance/overview',
    method: 'get'
  })
}

export function reconcileDailyFinance(reportDate?: string): Promise<FinancialReconciliationDailyItem> {
  return request<FinancialReconciliationDailyItem>({
    url: '/admin/finance/reconcile',
    method: 'post',
    params: reportDate ? { reportDate } : undefined
  })
}

export function exportFinanceCsv(params?: {
  startDate?: string
  endDate?: string
}): Promise<{ fileName: string; csvContent: string }> {
  return request<{ fileName: string; csvContent: string }>({
    url: '/admin/finance/export-csv',
    method: 'get',
    params
  })
}

export function scanOverdueNoShowOrders(): Promise<number> {
  return request<number>({
    url: '/admin/finance/noshow/scan',
    method: 'post'
  })
}

export function adminCompensateNoShowOrder(
  orderId: number,
  evidenceNote?: string
): Promise<AppointmentNoshowClaimItem> {
  return request<AppointmentNoshowClaimItem>({
    url: `/admin/finance/noshow/${orderId}/compensate`,
    method: 'post',
    data: { evidenceNote }
  })
}

export function getOrderNoShowClaim(orderId: number): Promise<AppointmentNoshowClaimItem | null> {
  return request<AppointmentNoshowClaimItem | null>({
    url: `/order/noshow/${orderId}`,
    method: 'get'
  })
}

export function buyerClaimSellerNoShow(
  orderId: number,
  evidenceNote?: string
): Promise<AppointmentNoshowClaimItem> {
  return request<AppointmentNoshowClaimItem>({
    url: `/order/noshow/${orderId}/claim`,
    method: 'post',
    data: { evidenceNote }
  })
}
