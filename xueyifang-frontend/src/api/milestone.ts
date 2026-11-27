import request from './request'

export interface OrderMilestoneItem {
  id: number
  orderId: number
  stageIndex: number
  title: string
  amount: number
  milestoneStatus: number // 0=待提交, 1=已提交待验收, 2=已验收释放资金
  proofNote?: string
  submittedTime?: string
  releasedTime?: string
  platformFee?: number
  sellerNetAmount?: number
  createTime: string
}

export interface SellerDepositLogItem {
  id: number
  ledgerNo: string
  actionType: number // 1=缴纳, 2=退还, 3=违约划扣
  amount: number
  depositBefore: number
  depositAfter: number
  remark?: string
  createTime: string
}

export function listOrderMilestones(orderId: number): Promise<OrderMilestoneItem[]> {
  return request<OrderMilestoneItem[]>({
    url: `/order/${orderId}/milestones`,
    method: 'get'
  })
}

export function saveOrderMilestonePlan(
  orderId: number,
  stages: Array<{ title: string; amount: number }>
): Promise<OrderMilestoneItem[]> {
  return request<OrderMilestoneItem[]>({
    url: `/order/${orderId}/milestones/plan`,
    method: 'post',
    data: { stages }
  })
}

export function submitOrderMilestoneProof(milestoneId: number, proofNote: string): Promise<boolean> {
  return request<boolean>({
    url: `/order/milestone/${milestoneId}/submit`,
    method: 'post',
    data: { proofNote }
  })
}

export function releaseOrderMilestone(milestoneId: number): Promise<boolean> {
  return request<boolean>({
    url: `/order/milestone/${milestoneId}/release`,
    method: 'post'
  })
}

export function getSellerDepositInfo(): Promise<{
  sellerDeposit: number
  walletBalance: number
  logs: SellerDepositLogItem[]
}> {
  return request({
    url: '/wallet/deposit/info',
    method: 'get'
  })
}

export function paySellerDeposit(amount: number): Promise<boolean> {
  return request<boolean>({
    url: '/wallet/deposit/pay',
    method: 'post',
    data: { amount }
  })
}

export function withdrawSellerDeposit(amount: number): Promise<boolean> {
  return request<boolean>({
    url: '/wallet/deposit/withdraw',
    method: 'post',
    data: { amount }
  })
}
