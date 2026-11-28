import express from 'express'
import type { Request, Response, NextFunction, Router } from 'express'
import crypto from 'node:crypto'
import { createInitialSeedData } from './seedData.ts'
import type {
  SeedUser,
  SeedService,
  SeedOrder,
  SeedDemand,
  SeedGroupSession
} from './seedData.ts'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function paginate<T>(items: T[], pageNum = 1, pageSize = 10): PageResult<T> {
  const safePage = Math.max(1, Number(pageNum) || 1)
  const safeSize = Math.max(1, Math.min(100, Number(pageSize) || 10))
  const total = items.length
  const pages = Math.max(1, Math.ceil(total / safeSize))
  const start = (safePage - 1) * safeSize
  return {
    records: items.slice(start, start + safeSize),
    total,
    size: safeSize,
    current: safePage,
    pages
  }
}

function nowStr(): string {
  return new Date().toISOString().replace('T', ' ').slice(0, 19)
}

function ok<T>(res: Response, data: T, message = 'ok') {
  return res.json({ code: 0, message, data })
}

function fail(res: Response, code: number, message: string, httpStatus = 200) {
  return res.status(httpStatus).json({ code, message, data: null })
}

export function createMockStore() {
  const state = createInitialSeedData()
  const tokens = new Map<string, number>() // token -> userId
  const eduVerifyCodes = new Map<number, { email: string; code: string }>()
  const creditLogs = [
    {
      id: 1,
      userId: 3,
      delta: 12,
      scoreBefore: 740,
      scoreAfter: 752,
      reasonCode: 'ORDER_COMPLETED_ON_TIME',
      reasonDesc: '按期验收完成担保订单 XYF202611220001',
      relatedOrderId: 1001,
      createTime: state.orders[0].buyerConfirmTime || nowStr()
    },
    {
      id: 2,
      userId: 2,
      delta: 16,
      scoreBefore: 770,
      scoreAfter: 786,
      reasonCode: 'FIVE_STAR_DELIVERY',
      reasonDesc: '获得买家五星好评交付认证',
      relatedOrderId: 1001,
      createTime: state.orders[0].buyerConfirmTime || nowStr()
    }
  ]
  const depositLogs: Array<{
    id: number
    userId: number
    ledgerNo: string
    actionType: number
    amount: number
    depositBefore: number
    depositAfter: number
    remark: string
    createTime: string
  }> = [
    {
      id: 1,
      userId: 2,
      ledgerNo: 'DEP202611010001',
      actionType: 1,
      amount: 500,
      depositBefore: 0,
      depositAfter: 500,
      remark: '创作者诚信保证金缴纳',
      createTime: state.users[1].createTime
    },
    {
      id: 2,
      userId: 3,
      ledgerNo: 'DEP202611050002',
      actionType: 1,
      amount: 200,
      depositBefore: 0,
      depositAfter: 200,
      remark: '创作者诚信保证金缴纳',
      createTime: state.users[2].createTime
    }
  ]

  const reconcileReports = [
    {
      id: 1,
      reportNo: 'REC20261127001',
      reportDate: new Date(Date.now() - 86400000).toISOString().slice(0, 10),
      paidOrderCount: 14,
      completedOrderCount: 11,
      refundOrderCount: 1,
      noshowCompensatedCount: 0,
      grossEscrowInflow: 1846,
      sellerSettledAmount: 1492.5,
      platformCommissionAmount: 78.5,
      refundOutflowAmount: 85,
      noshowPenaltyAmount: 0,
      activeEscrowFrozenPool: 367,
      discrepancyCount: 0,
      discrepancyAmount: 0,
      reconcileStatus: 1,
      auditNote: '总分账核对一致，无差额异常',
      createTime: nowStr()
    }
  ]

  const noshowClaims: Array<Record<string, unknown>> = []

  let nextId = 5000
  const genId = () => ++nextId

  const sanitizeUser = (u: SeedUser) => {
    const { password: _pw, ...rest } = u
    return rest
  }

  const getUserById = (id: number) => state.users.find((u) => u.id === id)
  const getTagById = (tagId?: number) => state.serviceTags.find((t) => t.tagId === tagId)
  const getCampusById = (campusId?: number) => state.campuses.find((c) => c.id === campusId)
  const getLocationById = (locId?: number) => state.tradeLocations.find((l) => l.id === locId)

  const loginUser = (studentId: string, password: string) => {
    const user = state.users.find((u) => u.studentId === studentId.trim())
    if (!user) return { error: '学号不存在，可使用演示账号 20240003 / password123 登录' }
    if (user.accountStatus === 0) return { error: '该账号已被冻结，请联系管理员' }
    if (user.password !== password) return { error: '密码错误（演示账号默认密码为 password123）' }
    const token = `xyf_${user.id}_${crypto.randomBytes(12).toString('hex')}`
    tokens.set(token, user.id)
    return { token, userInfo: sanitizeUser(user) }
  }

  const getUserByToken = (token?: string): SeedUser | undefined => {
    if (!token) return undefined
    const uid = tokens.get(token)
    if (uid) return getUserById(uid)
    // Allow deterministic fallback for dev tokens formatted as xyf_<id>_...
    const match = /^xyf_(\d+)_/.exec(token)
    if (match) {
      const parsedId = Number(match[1])
      return getUserById(parsedId)
    }
    return undefined
  }

  const toServiceListItem = (s: SeedService) => {
    const seller = getUserById(s.sellerId)
    const tag = getTagById(s.tagId)
    return {
      id: s.id,
      serviceTitle: s.serviceTitle,
      price: s.price,
      tagId: s.tagId,
      tagName: tag?.tagName || '校园技能',
      tradeType: s.tradeType,
      coverImage: s.images[0] || '',
      viewCount: s.viewCount,
      collectionCount: s.collectionCount,
      salesCount: s.salesCount,
      averageRating: s.averageRating,
      sellerName: seller?.nickname || seller?.realName || '认证创作者',
      sellerAvatar: seller?.avatar || '',
      sellerId: s.sellerId,
      status: s.status,
      reviewStatus: s.reviewStatus,
      reviewReason: s.reviewReason,
      maxPurchases: s.maxPurchases,
      currentPurchaseCount: s.currentPurchaseCount,
      createTime: s.createTime,
      updateTime: s.updateTime
    }
  }

  const toServiceDetail = (s: SeedService, currentUserId?: number) => {
    const seller = getUserById(s.sellerId)
    const tag = getTagById(s.tagId)
    const campus = getCampusById(s.campusId)
    const location = getLocationById(s.tradeLocationId)
    const slots = state.serviceSlots.filter((sl) => sl.serviceId === s.id)
    const isCollected = currentUserId
      ? state.favorites.some((f) => f.userId === currentUserId && f.serviceId === s.id)
      : false
    return {
      ...s,
      coverImage: s.images[0] || '',
      sellerName: seller?.nickname || seller?.realName || '认证创作者',
      sellerAvatar: seller?.avatar || '',
      sellerRating: s.averageRating,
      sellerProfessional: seller?.professionalName || '',
      sellerGrade: seller?.grade || '',
      sellerBio: seller?.bio || '',
      sellerCompletedOrders: state.orders.filter((o) => o.sellerId === s.sellerId && o.orderStatus === 4).length + 12,
      sellerEduVerified: seller?.eduVerified ?? 1,
      sellerCreditScore: seller?.creditScore ?? 750,
      campusName: campus?.campusName || '主校区（清河园）',
      tagName: tag?.tagName || '校园技能',
      tradeTypeText: s.tradeType === 1 ? '线下交易' : '线上担保',
      tradeLocationName: location?.locationName,
      tradeLocationLat: location?.latitude,
      tradeLocationLng: location?.longitude,
      tradeLocation: location,
      availableSlots: slots,
      isCollected
    }
  }

  const toDemandVO = (d: SeedDemand) => {
    const pub = getUserById(d.publisherId)
    const campus = getCampusById(d.campusId)
    const tag = getTagById(d.tagId)
    return {
      ...d,
      publisherName: pub?.nickname || pub?.realName || '校园同学',
      publisherAvatar: pub?.avatar || '',
      publisherEduVerified: pub?.eduVerified ?? 1,
      publisherCreditScore: pub?.creditScore ?? 740,
      campusName: campus?.campusName || '主校区',
      tagName: tag?.tagName || '技能定制',
      bidCount: d.bids.length,
      bids: d.bids.map((b) => {
        const bidder = getUserById(b.bidderId)
        return {
          ...b,
          bidderName: bidder?.nickname || bidder?.realName || '校园行家',
          bidderAvatar: bidder?.avatar || '',
          bidderProfessional: bidder?.professionalName || '',
          bidderEduVerified: bidder?.eduVerified ?? 1,
          bidderCreditScore: bidder?.creditScore ?? 760,
          demandTitle: d.title
        }
      })
    }
  }

  const toGroupSessionVO = (g: SeedGroupSession) => {
    const creator = getUserById(g.creatorId)
    const campus = getCampusById(g.campusId)
    const tag = getTagById(g.tagId)
    return {
      ...g,
      creatorName: creator?.nickname || creator?.realName || '认证导师',
      creatorAvatar: creator?.avatar || '',
      creatorProfessional: creator?.professionalName || '',
      creatorEduVerified: creator?.eduVerified ?? 1,
      creatorCreditScore: creator?.creditScore ?? 770,
      creatorDeposit: creator?.sellerDeposit ?? 300,
      campusName: campus?.campusName || '主校区',
      tagName: tag?.tagName || '拼班团课',
      joinedCount: g.participants.length,
      participants: g.participants.map((p) => {
        const buyer = getUserById(p.buyerId)
        return {
          ...p,
          buyerName: buyer?.nickname || buyer?.realName || '同学',
          buyerAvatar: buyer?.avatar || ''
        }
      })
    }
  }

  const toOrderDetailVO = (o: SeedOrder) => {
    const buyer = getUserById(o.buyerId)
    const seller = getUserById(o.sellerId)
    const location = getLocationById(o.tradeLocationId)
    const isReviewed = state.reviews.some((r) => r.orderId === o.id)
    return {
      ...o,
      tradeLocation: location,
      buyerName: buyer?.nickname || buyer?.realName || '买家',
      buyerAvatar: buyer?.avatar || '',
      buyerProfessional: buyer?.professionalName || '',
      sellerName: seller?.nickname || seller?.realName || '卖家',
      sellerAvatar: seller?.avatar || '',
      sellerProfessional: seller?.professionalName || '',
      isReviewed
    }
  }

  const toOrderListItemVO = (o: SeedOrder) => {
    const buyer = getUserById(o.buyerId)
    const seller = getUserById(o.sellerId)
    return {
      id: o.id,
      orderNumber: o.orderNumber,
      serviceId: o.serviceId,
      serviceTitle: o.serviceTitle,
      serviceImage: o.serviceImage,
      unitPrice: o.unitPrice,
      quantity: o.quantity,
      totalAmount: o.totalAmount,
      orderStatus: o.orderStatus,
      paymentStatus: o.paymentStatus,
      sellerId: o.sellerId,
      sellerName: seller?.nickname || seller?.realName || '卖家',
      sellerAvatar: seller?.avatar || '',
      buyerId: o.buyerId,
      buyerName: buyer?.nickname || buyer?.realName || '买家',
      buyerAvatar: buyer?.avatar || '',
      createTime: o.createTime,
      remark: o.remark,
      tradeType: o.tradeType,
      refundStatus: o.refundStatus
    }
  }

  const buildLeaderboard = (params?: { campusId?: number; tagId?: number; rankBy?: string; limit?: number }) => {
    let creators = state.users.filter((u) => u.role === 1 && u.publishPermission === 1)
    if (params?.campusId) {
      creators = creators.filter((u) => u.campusId === Number(params.campusId))
    }
    const items = creators.map((u) => {
      const userServices = state.services.filter((s) => s.sellerId === u.id && s.status === 1)
      const totalSales = userServices.reduce((acc, s) => acc + s.salesCount, 0)
      const avgRating =
        userServices.length > 0
          ? Number((userServices.reduce((acc, s) => acc + s.averageRating, 0) / userServices.length).toFixed(1))
          : 4.9
      const reviewCount = userServices.reduce((acc, s) => acc + s.reviewCount, 0)
      const topSkillTags = Array.from(
        new Set(userServices.map((s) => getTagById(s.tagId)?.tagName).filter(Boolean) as string[])
      )
      const compositeInfluenceScore = Math.round(u.creditScore * 0.65 + totalSales * 4.5 + avgRating * 35)
      return {
        rank: 0,
        userId: u.id,
        displayName: u.nickname || u.realName,
        avatar: u.avatar,
        grade: u.grade,
        professionalName: u.professionalName,
        campusId: u.campusId,
        campusName: u.campusName,
        bio: u.bio,
        eduVerified: u.eduVerified,
        creditScore: u.creditScore,
        creatorLevelTitle: u.creditScore >= 785 ? '校园金牌行家' : u.creditScore >= 765 ? '认证实力导师' : '校园新锐行家',
        sellerDeposit: u.sellerDeposit,
        activeServiceCount: userServices.length,
        completedOrderCount: totalSales,
        totalPracticeHours: totalSales * 2.5,
        averageRating: avgRating,
        reviewCount,
        compositeInfluenceScore,
        topSkillTags: topSkillTags.length ? topSkillTags : ['学业辅导'],
        badges: [
          { badgeCode: 'EDU_VERIFIED', badgeName: '高校实名', badgeTone: 'blue', badgeDesc: '已通过校园邮箱与学籍认证' },
          ...(u.sellerDeposit >= 300
            ? [{ badgeCode: 'DEPOSIT_GUARD', badgeName: '保证金担保', badgeTone: 'gold', badgeDesc: `已缴纳 ¥${u.sellerDeposit} 诚信保证金` }]
            : [])
        ],
        tagIds: userServices.map((s) => s.tagId)
      }
    })

    const filtered = params?.tagId
      ? items.filter((item) => item.tagIds.includes(Number(params.tagId)))
      : items

    const rankBy = params?.rankBy || 'COMPREHENSIVE'
    filtered.sort((a, b) => {
      if (rankBy === 'COMPLETED_ORDERS') return b.completedOrderCount - a.completedOrderCount
      if (rankBy === 'CREDIT_SCORE') return b.creditScore - a.creditScore
      if (rankBy === 'RATING') return b.averageRating - a.averageRating
      return b.compositeInfluenceScore - a.compositeInfluenceScore
    })

    const limit = Number(params?.limit) || 20
    return filtered.slice(0, limit).map((item, idx) => {
      const { tagIds: _tagIds, ...rest } = item
      return { ...rest, rank: idx + 1 }
    })
  }

  const createOrder = (buyer: SeedUser, payload: {
    serviceId: number
    quantity?: number
    tradeType?: number
    tradeLocationId?: number
    slotId?: number
    scheduledStart?: string
    meetupLat?: number
    meetupLng?: number
    meetupPoiName?: string
    meetupAddress?: string
    meetupRadiusMeters?: number
    remark?: string
  }) => {
    const svc = state.services.find((s) => s.id === Number(payload.serviceId))
    if (!svc) return { error: '服务不存在或已下架' }
    if (svc.sellerId === buyer.id) return { error: '不能购买自己发布的服务' }
    const quantity = Math.max(1, Number(payload.quantity) || 1)
    const totalAmount = Number((svc.price * quantity).toFixed(2))
    const orderId = genId()
    const orderNumber = `XYF${new Date().toISOString().slice(0, 10).replace(/-/g, '')}${String(orderId).slice(-4)}`
    const loc = getLocationById(payload.tradeLocationId || svc.tradeLocationId)
    const tag = getTagById(svc.tagId)
    const order: SeedOrder = {
      id: orderId,
      orderNumber,
      serviceId: svc.id,
      serviceTitle: svc.serviceTitle,
      serviceDescription: svc.serviceDescription,
      serviceImage: svc.images[0] || '',
      serviceTagName: tag?.tagName || '校园技能',
      unitPrice: svc.price,
      quantity,
      totalAmount,
      tradeType: Number(payload.tradeType) || svc.tradeType,
      campusId: svc.campusId,
      tradeLocationId: loc?.id,
      tradeLocationName: loc?.locationName,
      slotId: payload.slotId,
      scheduledStart: payload.scheduledStart,
      meetupLat: payload.meetupLat ?? loc?.latitude,
      meetupLng: payload.meetupLng ?? loc?.longitude,
      meetupPoiName: payload.meetupPoiName ?? loc?.locationName,
      meetupAddress: payload.meetupAddress ?? loc?.locationAddress,
      meetupRadiusMeters: payload.meetupRadiusMeters ?? loc?.validRadiusMeters ?? 150,
      meetupConfirmed: 0,
      requireBleVerify: svc.requireBleVerify,
      orderStatus: 1, // UNPAID
      paymentStatus: 1,
      paymentMethod: 1,
      frozenAmount: 0,
      platformFee: Number((totalAmount * 0.05).toFixed(2)),
      sellerSettleAmount: Number((totalAmount * 0.95).toFixed(2)),
      buyerId: buyer.id,
      sellerId: svc.sellerId,
      createTime: nowStr(),
      remark: payload.remark || '',
      deliveryImages: [],
      verifyCode: String(Math.floor(100000 + Math.random() * 900000)),
      refundStatus: 0,
      logs: [
        {
          id: genId(),
          orderId,
          operatorId: buyer.id,
          operatorName: buyer.nickname || buyer.realName,
          action: 'CREATE',
          actionDesc: '创建担保交易订单',
          createTime: nowStr()
        }
      ],
      meetupProposals: [],
      milestones: []
    }
    state.orders.unshift(order)
    return { order }
  }

  const payOrder = (buyer: SeedUser, orderId: number, paymentMethod = 1) => {
    const order = state.orders.find((o) => o.id === orderId)
    if (!order) return { error: '订单不存在' }
    if (order.buyerId !== buyer.id) return { error: '无权支付他人订单' }
    if (order.orderStatus !== 1) return { error: '订单当前状态不可支付' }
    if (paymentMethod === 1 && buyer.walletBalance < order.totalAmount) {
      return { error: '钱包余额不足，请先充值后再支付' }
    }
    const balBefore = buyer.walletBalance
    const frozenBefore = buyer.frozenAmount
    if (paymentMethod === 1) {
      buyer.walletBalance = Number((buyer.walletBalance - order.totalAmount).toFixed(2))
    }
    buyer.frozenAmount = Number((buyer.frozenAmount + order.totalAmount).toFixed(2))
    order.orderStatus = 2 // PENDING_SHIP
    order.paymentStatus = 2 // PAID
    order.paymentMethod = paymentMethod
    order.frozenAmount = order.totalAmount
    order.paymentTime = nowStr()
    order.logs.push({
      id: genId(),
      orderId: order.id,
      operatorId: buyer.id,
      operatorName: buyer.nickname || buyer.realName,
      action: 'PAY',
      actionDesc: `支付成功 ¥${order.totalAmount.toFixed(2)}，平台担保托管中`,
      createTime: nowStr()
    })
    state.walletTransactions.unshift({
      transactionId: genId(),
      userId: buyer.id,
      transactionType: 3,
      typeName: '支付',
      amount: order.totalAmount,
      balanceBefore: balBefore,
      balanceAfter: buyer.walletBalance,
      frozenBefore,
      frozenAfter: buyer.frozenAmount,
      relatedOrderId: order.id,
      relatedOrderNumber: order.orderNumber,
      transactionNo: `WT${Date.now()}`,
      remark: `订单 ${order.orderNumber} 担保托管支付`,
      createTime: nowStr()
    })
    return { order }
  }

  const confirmOrder = (buyer: SeedUser, orderId: number) => {
    const order = state.orders.find((o) => o.id === orderId)
    if (!order) return { error: '订单不存在' }
    if (order.buyerId !== buyer.id) return { error: '仅买家可确认验收' }
    if (order.orderStatus !== 3 && order.orderStatus !== 2) return { error: '订单状态不允许确认收货' }
    const seller = getUserById(order.sellerId)
    buyer.frozenAmount = Math.max(0, Number((buyer.frozenAmount - order.frozenAmount).toFixed(2)))
    if (seller) {
      const sellerBalBefore = seller.walletBalance
      seller.walletBalance = Number((seller.walletBalance + order.sellerSettleAmount).toFixed(2))
      state.walletTransactions.unshift({
        transactionId: genId(),
        userId: seller.id,
        transactionType: 5,
        typeName: '收入',
        amount: order.sellerSettleAmount,
        balanceBefore: sellerBalBefore,
        balanceAfter: seller.walletBalance,
        frozenBefore: seller.frozenAmount,
        frozenAfter: seller.frozenAmount,
        relatedOrderId: order.id,
        relatedOrderNumber: order.orderNumber,
        transactionNo: `WT${Date.now()}`,
        remark: `订单 ${order.orderNumber} 验收结算入账`,
        createTime: nowStr()
      })
    }
    order.orderStatus = 4 // COMPLETED
    order.frozenAmount = 0
    order.buyerConfirmTime = nowStr()
    order.logs.push({
      id: genId(),
      orderId: order.id,
      operatorId: buyer.id,
      operatorName: buyer.nickname || buyer.realName,
      action: 'CONFIRM',
      actionDesc: '买家确认验收，担保资金已结算给创作者',
      createTime: nowStr()
    })
    const svc = state.services.find((s) => s.id === order.serviceId)
    if (svc) {
      svc.salesCount += order.quantity
      svc.currentPurchaseCount += order.quantity
    }
    return { order }
  }

  const joinGroupSession = (buyer: SeedUser, sessionId: number) => {
    const session = state.groupSessions.find((g) => g.id === sessionId)
    if (!session) return { error: '拼班团课不存在' }
    if (session.sessionStatus >= 2) return { error: '该拼班已满员或已结束' }
    if (session.creatorId === buyer.id) return { error: '不能报名自己发起的团课' }
    if (session.participants.some((p) => p.buyerId === buyer.id)) return { error: '您已报名该拼班课程' }

    const lockPrice = session.currentUnitPrice
    if (buyer.walletBalance < lockPrice) return { error: '钱包余额不足，请先充值' }

    buyer.walletBalance = Number((buyer.walletBalance - lockPrice).toFixed(2))
    buyer.frozenAmount = Number((buyer.frozenAmount + lockPrice).toFixed(2))
    const orderId = genId()
    session.participants.push({
      id: genId(),
      buyerId: buyer.id,
      orderId,
      lockedPrice: lockPrice,
      effectivePrice: lockPrice,
      rebateAmount: 0,
      createTime: nowStr()
    })

    // Trigger tiered discount rebate if threshold reached
    if (session.participants.length >= session.tierTargetCount && session.tierRebateApplied === 0) {
      session.tierRebateApplied = 1
      session.currentUnitPrice = session.tierDiscountPrice
      for (const p of session.participants) {
        const diff = Number((p.lockedPrice - session.tierDiscountPrice).toFixed(2))
        if (diff > 0) {
          p.effectivePrice = session.tierDiscountPrice
          p.rebateAmount = diff
          const member = getUserById(p.buyerId)
          if (member) {
            member.walletBalance = Number((member.walletBalance + diff).toFixed(2))
            member.frozenAmount = Math.max(0, Number((member.frozenAmount - diff).toFixed(2)))
          }
        }
      }
    }
    if (session.participants.length >= session.maxParticipants) {
      session.sessionStatus = 2
    } else if (session.participants.length >= session.minParticipants) {
      session.sessionStatus = 1
    }

    return { orderId, session }
  }

  return {
    state,
    tokens,
    eduVerifyCodes,
    creditLogs,
    depositLogs,
    reconcileReports,
    noshowClaims,
    genId,
    sanitizeUser,
    getUserById,
    getTagById,
    getCampusById,
    getLocationById,
    loginUser,
    getUserByToken,
    toServiceListItem,
    toServiceDetail,
    toDemandVO,
    toGroupSessionVO,
    toOrderDetailVO,
    toOrderListItemVO,
    buildLeaderboard,
    createOrder,
    payOrder,
    confirmOrder,
    joinGroupSession
  }
}

export type MockStore = ReturnType<typeof createMockStore>

export function createApiRouter(store: MockStore): Router {
  const router = express.Router()
  const { state } = store

  const extractToken = (req: Request): string | undefined => {
    const auth = req.headers.authorization
    if (auth && auth.startsWith('Bearer ')) return auth.slice(7).trim()
    return undefined
  }

  const getAuthUser = (req: Request): SeedUser | undefined => store.getUserByToken(extractToken(req))

  const requireAuth = (req: Request, res: Response, next: NextFunction) => {
    const user = getAuthUser(req)
    if (!user) {
      fail(res, 40103, '请先登录校园账号', 401)
      return
    }
    ;(req as Request & { authUser?: SeedUser }).authUser = user
    next()
  }

  const requireAdmin = (req: Request, res: Response, next: NextFunction) => {
    const user = getAuthUser(req)
    if (!user) {
      fail(res, 40103, '请先登录管理员账号', 401)
      return
    }
    if (user.role !== 2) {
      fail(res, 40003, '需要管理员权限', 403)
      return
    }
    ;(req as Request & { authUser?: SeedUser }).authUser = user
    next()
  }

  const reqUser = (req: Request): SeedUser => (req as Request & { authUser: SeedUser }).authUser

  // ==================== Health & File Proxy ====================
  router.get('/health', (_req, res) => ok(res, { status: 'UP', mode: 'in-memory-node' }))

  router.post('/file/upload', requireAuth, (_req, res) => {
    const sampleUrl = 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1000&q=80'
    return ok(res, sampleUrl)
  })

  router.post('/file/upload/batch', requireAuth, (_req, res) => {
    return ok(res, ['https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1000&q=80'])
  })

  router.delete('/file/delete', requireAuth, (_req, res) => ok(res, true))

  router.get('/file/view/*', (_req, res) => {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">
      <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#f4f4f5"/><stop offset="100%" stop-color="#e4e4e7"/></linearGradient></defs>
      <rect width="800" height="600" fill="url(#g)"/>
      <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#71717a" font-family="sans-serif" font-size="24">学艺坊 · 校园精选服务</text>
    </svg>`
    res.setHeader('Content-Type', 'image/svg+xml')
    res.send(svg)
  })

  // ==================== Auth ====================
  router.post('/auth/login', (req, res) => {
    const { studentId = '', password = '' } = req.body || {}
    const result = store.loginUser(String(studentId), String(password))
    if ('error' in result) return fail(res, 40001, result.error)
    return ok(res, result)
  })

  router.post('/auth/register', (req, res) => {
    const { studentId, password, realName, phone, professionalId, email, dormitory } = req.body || {}
    if (!studentId || !password || !realName) return fail(res, 40001, '请完整填写学号、姓名和密码')
    if (state.users.some((u) => u.studentId === String(studentId).trim())) {
      return fail(res, 40001, '该学号已被注册')
    }
    const pro = state.professionals.find((p) => p.id === Number(professionalId)) || state.professionals[0]
    const newUser: SeedUser = {
      id: store.genId(),
      studentId: String(studentId).trim(),
      password: String(password),
      realName: String(realName).trim(),
      nickname: String(realName).trim(),
      phone: String(phone || ''),
      email: String(email || ''),
      dormitory: String(dormitory || '主校区宿舍'),
      grade: '大二',
      professionalId: pro.id,
      professionalName: pro.professionalName,
      campusId: 1,
      campusName: state.campuses[0].campusName,
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
      bio: '欢迎来到学艺坊校园技能互助社区。',
      role: 1,
      publishPermission: 0,
      permissionReviewStatus: 2,
      walletBalance: 500,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 0,
      creditScore: 700,
      sellerDeposit: 0,
      createTime: nowStr()
    }
    state.users.push(newUser)
    return ok(res, store.sanitizeUser(newUser))
  })

  router.get('/auth/currentUser', requireAuth, (req, res) => ok(res, store.sanitizeUser(reqUser(req))))

  router.put('/auth/updateProfile', requireAuth, (req, res) => {
    const user = reqUser(req)
    const body = req.body || {}
    if (body.nickname !== undefined) user.nickname = String(body.nickname)
    if (body.email !== undefined) user.email = String(body.email)
    if (body.dormitory !== undefined) user.dormitory = String(body.dormitory)
    if (body.grade !== undefined) user.grade = String(body.grade)
    if (body.avatar !== undefined) user.avatar = String(body.avatar)
    if (body.bio !== undefined) user.bio = String(body.bio)
    if (body.phone !== undefined) user.phone = String(body.phone)
    if (body.professionalId !== undefined) {
      const pro = state.professionals.find((p) => p.id === Number(body.professionalId))
      if (pro) {
        user.professionalId = pro.id
        user.professionalName = pro.professionalName
      }
    }
    return ok(res, null)
  })

  router.put('/auth/changePassword', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { oldPassword, newPassword } = req.body || {}
    if (user.password !== oldPassword) return fail(res, 40001, '原密码不正确')
    if (!newPassword || String(newPassword).length < 6) return fail(res, 40001, '新密码长度不能少于 6 位')
    user.password = String(newPassword)
    return ok(res, null)
  })

  router.post('/auth/logout', (req, res) => {
    const token = extractToken(req)
    if (token) store.tokens.delete(token)
    return ok(res, true)
  })

  router.post('/auth/edu-verify/send', requireAuth, (req, res) => {
    const user = reqUser(req)
    const eduEmail = String(req.body?.eduEmail || '').trim()
    if (!eduEmail.endsWith('.edu.cn') && !eduEmail.includes('@')) {
      return fail(res, 40001, '请输入有效的高校 edu.cn 邮箱')
    }
    const code = '668899'
    store.eduVerifyCodes.set(user.id, { email: eduEmail, code })
    return ok(res, { eduEmail, expireSeconds: 300, devPreviewCode: code })
  })

  router.post('/auth/edu-verify/confirm', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { eduEmail, verifyCode } = req.body || {}
    const saved = store.eduVerifyCodes.get(user.id)
    if (saved && saved.code !== String(verifyCode)) {
      return fail(res, 40001, '验证码不正确（演示验证码为 668899）')
    }
    user.eduVerified = 1
    user.verifiedEduEmail = String(eduEmail || saved?.email || `${user.studentId}@campus.edu.cn`)
    user.creditScore = Math.min(800, user.creditScore + 20)
    return ok(res, null)
  })

  router.get('/auth/credit-logs', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(res, store.creditLogs.filter((l) => l.userId === user.id))
  })

  // ==================== Public Dictionaries & Config ====================
  router.get('/sys-config/register-status', (_req, res) => {
    const cfg = state.sysConfigs.find((c) => c.configKey === 'REGISTER_ENABLED')
    return ok(res, { registerEnabled: cfg ? cfg.configValue === '1' : true })
  })

  router.get('/service/tags', (_req, res) => ok(res, state.serviceTags))
  router.get('/professional/list', (_req, res) => ok(res, state.professionals))
  router.get('/professional/:id', (req, res) => {
    const pro = state.professionals.find((p) => p.id === Number(req.params.id))
    return pro ? ok(res, pro) : fail(res, 40400, '专业不存在')
  })

  router.get('/trade-location/list', (_req, res) => ok(res, state.tradeLocations.filter((l) => l.isAvailable === 1)))
  router.get('/trade-location/list/all', (_req, res) => ok(res, state.tradeLocations))
  router.get('/trade-location/:id', (req, res) => {
    const loc = store.getLocationById(Number(req.params.id))
    return loc ? ok(res, loc) : fail(res, 40400, '地点不存在')
  })

  router.get('/campus/list', (_req, res) => ok(res, state.campuses.filter((c) => c.status === 1)))
  router.get('/campus/check-fence', (_req, res) => ok(res, { inside: true, message: '当前位置位于校内安全核销围栏内' }))

  // ==================== Services ====================
  router.get('/service/pricing-benchmark', (req, res) => {
    const tagId = req.query.tagId ? Number(req.query.tagId) : undefined
    const proposedPrice = Number(req.query.proposedPrice) || 100
    const subset = state.services.filter((s) => (!tagId || s.tagId === tagId) && s.status === 1)
    const prices = (subset.length ? subset : state.services).map((s) => s.price).sort((a, b) => a - b)
    const pick = (p: number) => prices[Math.min(prices.length - 1, Math.floor(prices.length * p))] || 88
    const p25 = pick(0.25)
    const p50 = pick(0.5)
    const p75 = pick(0.75)
    const p90 = pick(0.9)
    const avg = Number((prices.reduce((a, b) => a + b, 0) / prices.length).toFixed(1))
    const lowerCount = prices.filter((p) => p <= proposedPrice).length
    const percentileRank = Math.round((lowerCount / prices.length) * 100)
    const pricePositionCode =
      proposedPrice < p25
        ? 'BELOW_P25_VALUE'
        : proposedPrice <= p75
          ? 'SWEET_SPOT_P25_P75'
          : proposedPrice <= p90
            ? 'PREMIUM_P75_P90'
            : 'ABOVE_P90_OUTLIER'
    const labelMap: Record<string, string> = {
      BELOW_P25_VALUE: '高性价比引流区间',
      SWEET_SPOT_P25_P75: '黄金成交甜点区间',
      PREMIUM_P75_P90: '高品质溢价区间',
      ABOVE_P90_OUTLIER: '偏高定价预警'
    }
    return ok(res, {
      tagId,
      tagName: store.getTagById(tagId)?.tagName || '全校技能',
      completedOrderSampleCount: 128,
      activeListingSampleCount: prices.length,
      totalSampleCount: 128 + prices.length,
      minPrice: prices[0] || 20,
      p25Price: p25,
      p50Price: p50,
      p75Price: p75,
      p90Price: p90,
      maxPrice: prices[prices.length - 1] || 200,
      avgPrice: avg,
      recommendedMinPrice: p25,
      recommendedMaxPrice: p75,
      proposedPrice,
      percentileRank,
      pricePositionCode,
      pricePositionLabel: labelMap[pricePositionCode],
      conversionAdvice: `当前定价处于同类目第 ${percentileRank} 百分位，推荐定价区间 ¥${p25} - ¥${p75}，兼顾成交转化与创作者收益。`
    })
  })

  router.get('/service/list', (req, res) => {
    const { keyword, tagId, tradeType, sortBy, sellerId, status, pageNum = 1, pageSize = 12 } = req.query
    let list = state.services.filter((s) => (status ? s.status === Number(status) : s.status === 1))
    if (tagId) list = list.filter((s) => s.tagId === Number(tagId))
    if (tradeType) list = list.filter((s) => s.tradeType === Number(tradeType))
    if (sellerId) list = list.filter((s) => s.sellerId === Number(sellerId))
    if (keyword) {
      const kw = String(keyword).trim().toLowerCase()
      list = list.filter(
        (s) =>
          s.serviceTitle.toLowerCase().includes(kw) ||
          s.serviceDescription.toLowerCase().includes(kw) ||
          (store.getTagById(s.tagId)?.tagName || '').toLowerCase().includes(kw)
      )
    }
    list = [...list]
    if (sortBy === 'price_asc') list.sort((a, b) => a.price - b.price)
    else if (sortBy === 'price_desc') list.sort((a, b) => b.price - a.price)
    else if (sortBy === 'view_desc') list.sort((a, b) => b.viewCount - a.viewCount)
    else list.sort((a, b) => b.createTime.localeCompare(a.createTime))

    return ok(res, paginate(list.map(store.toServiceListItem), Number(pageNum), Number(pageSize)))
  })

  router.get('/service/myServices', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { status, pageNum = 1, pageSize = 10 } = req.query
    let list = state.services.filter((s) => s.sellerId === user.id)
    if (status !== undefined && status !== '') list = list.filter((s) => s.status === Number(status))
    return ok(res, paginate(list.map(store.toServiceListItem), Number(pageNum), Number(pageSize)))
  })

  router.post('/service/publish', requireAuth, (req, res) => {
    const user = reqUser(req)
    const body = req.body || {}
    if (!body.serviceTitle || !body.price) return fail(res, 40001, '请填写服务标题与价格')
    const id = store.genId()
    const svc: SeedService = {
      id,
      serviceTitle: String(body.serviceTitle),
      serviceDescription: String(body.serviceDescription || ''),
      sellerId: user.id,
      campusId: Number(body.campusId) || user.campusId || 1,
      tagId: Number(body.tagId) || 1,
      tradeType: Number(body.tradeType) || 2,
      tradeLocationId: body.tradeLocationId ? Number(body.tradeLocationId) : 1,
      price: Number(body.price),
      maxPurchases: Number(body.maxPurchases) || 20,
      currentPurchaseCount: 0,
      isPhysical: Number(body.isPhysical) || 0,
      requireSlot: Number(body.requireSlot) || 0,
      cancelFreeHours: Number(body.cancelFreeHours) || 6,
      requireBleVerify: Number(body.requireBleVerify) || 0,
      status: 1,
      reviewStatus: 1,
      viewCount: 1,
      collectionCount: 0,
      salesCount: 0,
      averageRating: 5.0,
      reviewCount: 0,
      images: Array.isArray(body.images) && body.images.length
        ? body.images
        : ['https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1000&q=80'],
      deliveryTime: '下单后按约定时间交付',
      requirements: '下单后请在站内私信沟通需求细节。',
      createTime: nowStr(),
      updateTime: nowStr()
    }
    state.services.unshift(svc)
    return ok(res, id)
  })

  router.get('/service/:id/slots', (req, res) => {
    const serviceId = Number(req.params.id)
    return ok(res, state.serviceSlots.filter((sl) => sl.serviceId === serviceId))
  })

  router.post('/service/:id/slots/batch', requireAuth, (req, res) => {
    const serviceId = Number(req.params.id)
    const slots = Array.isArray(req.body?.slots) ? req.body.slots : []
    for (const s of slots) {
      state.serviceSlots.push({
        id: store.genId(),
        serviceId,
        slotStart: s.slotStart,
        slotEnd: s.slotEnd,
        maxCapacity: Number(s.maxCapacity) || 2,
        bookedCount: 0,
        remainingCapacity: Number(s.maxCapacity) || 2,
        status: 1
      })
    }
    return ok(res, true)
  })

  router.get('/service/:id', (req, res) => {
    const svc = state.services.find((s) => s.id === Number(req.params.id))
    if (!svc) return fail(res, 40400, '服务不存在')
    svc.viewCount += 1
    const user = getAuthUser(req)
    return ok(res, store.toServiceDetail(svc, user?.id))
  })

  router.put('/service/:id', requireAuth, (req, res) => {
    const svc = state.services.find((s) => s.id === Number(req.params.id))
    if (!svc) return fail(res, 40400, '服务不存在')
    Object.assign(svc, req.body, { updateTime: nowStr() })
    return ok(res, null)
  })

  router.put('/service/:id/online', requireAuth, (req, res) => {
    const svc = state.services.find((s) => s.id === Number(req.params.id))
    if (!svc) return fail(res, 40400, '服务不存在')
    svc.status = 1
    svc.reviewStatus = 1
    return ok(res, null)
  })

  router.put('/service/:id/offline', requireAuth, (req, res) => {
    const svc = state.services.find((s) => s.id === Number(req.params.id))
    if (!svc) return fail(res, 40400, '服务不存在')
    svc.status = 2
    return ok(res, null)
  })

  router.delete('/service/:id', requireAuth, (req, res) => {
    const idx = state.services.findIndex((s) => s.id === Number(req.params.id))
    if (idx !== -1) state.services.splice(idx, 1)
    return ok(res, null)
  })

  // ==================== Demands (Bounties) ====================
  router.get('/demand/list', (req, res) => {
    const { pageNum = 1, pageSize = 12, campusId, tagId, tradeType, urgencyLevel, demandStatus, keyword } = req.query
    let list = [...state.demands]
    if (campusId) list = list.filter((d) => d.campusId === Number(campusId))
    if (tagId) list = list.filter((d) => d.tagId === Number(tagId))
    if (tradeType) list = list.filter((d) => d.tradeType === Number(tradeType))
    if (urgencyLevel) list = list.filter((d) => d.urgencyLevel === Number(urgencyLevel))
    if (demandStatus !== undefined && demandStatus !== '') list = list.filter((d) => d.demandStatus === Number(demandStatus))
    if (keyword) {
      const kw = String(keyword).trim().toLowerCase()
      list = list.filter((d) => d.title.toLowerCase().includes(kw) || d.description.toLowerCase().includes(kw))
    }
    return ok(res, paginate(list.map(store.toDemandVO), Number(pageNum), Number(pageSize)))
  })

  router.get('/demand/detail/:id', (req, res) => {
    const d = state.demands.find((item) => item.id === Number(req.params.id))
    return d ? ok(res, store.toDemandVO(d)) : fail(res, 40400, '悬赏需求不存在')
  })

  router.post('/demand/publish', requireAuth, (req, res) => {
    const user = reqUser(req)
    const body = req.body || {}
    if (!body.title) return fail(res, 40001, '请输入悬赏标题')
    const id = store.genId()
    const demand: SeedDemand = {
      id,
      demandNo: `DM${Date.now()}`,
      publisherId: user.id,
      campusId: Number(body.campusId) || user.campusId || 1,
      tagId: Number(body.tagId) || 1,
      title: String(body.title),
      description: String(body.description || ''),
      budgetMin: Number(body.budgetMin) || 50,
      budgetMax: Number(body.budgetMax) || 150,
      tradeType: Number(body.tradeType) || 2,
      urgencyLevel: Number(body.urgencyLevel) || 1,
      deadlineTime: body.deadlineTime || nowStr(),
      meetupLat: body.meetupLat,
      meetupLng: body.meetupLng,
      meetupPoiName: body.meetupPoiName || '主校区',
      meetupAddress: body.meetupAddress || '',
      meetupRadiusMeters: Number(body.meetupRadiusMeters) || 150,
      demandStatus: 0,
      createTime: nowStr(),
      bids: []
    }
    state.demands.unshift(demand)
    return ok(res, id)
  })

  router.post('/demand/:id/bid', requireAuth, (req, res) => {
    const user = reqUser(req)
    const demand = state.demands.find((d) => d.id === Number(req.params.id))
    if (!demand) return fail(res, 40400, '悬赏不存在')
    if (demand.publisherId === user.id) return fail(res, 40001, '不能竞标自己发布的悬赏')
    const bidId = store.genId()
    demand.bids.push({
      id: bidId,
      demandId: demand.id,
      bidderId: user.id,
      bidPrice: Number(req.body?.bidPrice) || demand.budgetMin,
      estimatedHours: Number(req.body?.estimatedHours) || 2,
      bidPlan: String(req.body?.bidPlan || '按要求高质量按期完成'),
      bidStatus: 0,
      createTime: nowStr()
    })
    return ok(res, bidId)
  })

  router.post('/demand/:id/accept/:bidId', requireAuth, (req, res) => {
    const user = reqUser(req)
    const demand = state.demands.find((d) => d.id === Number(req.params.id))
    if (!demand) return fail(res, 40400, '悬赏不存在')
    if (demand.publisherId !== user.id) return fail(res, 40003, '仅发布者可选标')
    const bid = demand.bids.find((b) => b.id === Number(req.params.bidId))
    if (!bid) return fail(res, 40400, '竞标方案不存在')
    demand.demandStatus = 1
    demand.acceptedBidId = bid.id
    const orderId = store.genId()
    demand.matchedOrderId = orderId
    for (const b of demand.bids) {
      b.bidStatus = b.id === bid.id ? 1 : 2
      if (b.id === bid.id) b.matchedOrderId = orderId
    }
    const order: SeedOrder = {
      id: orderId,
      orderNumber: `XYF${Date.now().toString().slice(-10)}`,
      serviceId: 1,
      serviceTitle: `[悬赏转单] ${demand.title}`,
      serviceDescription: demand.description,
      serviceImage: state.services[0].images[0],
      serviceTagName: store.getTagById(demand.tagId)?.tagName || '悬赏定制',
      unitPrice: bid.bidPrice,
      quantity: 1,
      totalAmount: bid.bidPrice,
      tradeType: demand.tradeType,
      campusId: demand.campusId,
      meetupPoiName: demand.meetupPoiName,
      meetupConfirmed: 0,
      requireBleVerify: 0,
      orderStatus: 1,
      paymentStatus: 1,
      paymentMethod: 1,
      frozenAmount: 0,
      platformFee: Number((bid.bidPrice * 0.05).toFixed(2)),
      sellerSettleAmount: Number((bid.bidPrice * 0.95).toFixed(2)),
      buyerId: user.id,
      sellerId: bid.bidderId,
      createTime: nowStr(),
      deliveryImages: [],
      verifyCode: String(Math.floor(100000 + Math.random() * 900000)),
      refundStatus: 0,
      logs: [{ id: store.genId(), orderId, operatorId: user.id, operatorName: user.nickname, action: 'CREATE', actionDesc: '悬赏选标自动生成担保订单', createTime: nowStr() }],
      meetupProposals: [],
      milestones: []
    }
    state.orders.unshift(order)
    return ok(res, orderId)
  })

  router.post('/demand/:id/cancel', requireAuth, (req, res) => {
    const demand = state.demands.find((d) => d.id === Number(req.params.id))
    if (!demand) return fail(res, 40400, '悬赏不存在')
    demand.demandStatus = 2
    demand.closeReason = String(req.body?.reason || '发布者主动取消')
    return ok(res, true)
  })

  router.get('/demand/my-published', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(res, state.demands.filter((d) => d.publisherId === user.id).map(store.toDemandVO))
  })

  router.get('/demand/my-bids', requireAuth, (req, res) => {
    const user = reqUser(req)
    const bids = state.demands.flatMap((d) => store.toDemandVO(d).bids.filter((b) => b.bidderId === user.id))
    return ok(res, bids)
  })

  // ==================== Group Sessions ====================
  router.get('/group-session/list', (req, res) => {
    const { pageNum = 1, pageSize = 12, campusId, tagId, tradeType, sessionStatus, keyword } = req.query
    let list = [...state.groupSessions]
    if (campusId) list = list.filter((g) => g.campusId === Number(campusId))
    if (tagId) list = list.filter((g) => g.tagId === Number(tagId))
    if (tradeType) list = list.filter((g) => g.tradeType === Number(tradeType))
    if (sessionStatus !== undefined && sessionStatus !== '') list = list.filter((g) => g.sessionStatus === Number(sessionStatus))
    if (keyword) {
      const kw = String(keyword).trim().toLowerCase()
      list = list.filter((g) => g.title.toLowerCase().includes(kw) || g.description.toLowerCase().includes(kw))
    }
    return ok(res, paginate(list.map(store.toGroupSessionVO), Number(pageNum), Number(pageSize)))
  })

  router.get('/group-session/detail/:id', (req, res) => {
    const g = state.groupSessions.find((s) => s.id === Number(req.params.id))
    return g ? ok(res, store.toGroupSessionVO(g)) : fail(res, 40400, '拼班团课不存在')
  })

  router.post('/group-session/create', requireAuth, (req, res) => {
    const user = reqUser(req)
    const b = req.body || {}
    const id = store.genId()
    const basePrice = Number(b.basePrice) || 99
    state.groupSessions.unshift({
      id,
      sessionNo: `GS${Date.now()}`,
      creatorId: user.id,
      campusId: Number(b.campusId) || user.campusId || 1,
      tagId: Number(b.tagId) || 1,
      title: String(b.title || '校园实战拼班团课'),
      description: String(b.description || ''),
      basePrice,
      tierTargetCount: Number(b.tierTargetCount) || 4,
      tierDiscountPrice: Number(b.tierDiscountPrice) || Math.max(10, basePrice - 20),
      currentUnitPrice: basePrice,
      minParticipants: Number(b.minParticipants) || 2,
      maxParticipants: Number(b.maxParticipants) || 8,
      tierRebateApplied: 0,
      tradeType: Number(b.tradeType) || 1,
      sessionStart: String(b.sessionStart || nowStr()),
      sessionEnd: String(b.sessionEnd || nowStr()),
      meetupPoiName: b.meetupPoiName || '主校区图书馆正门',
      meetupAddress: b.meetupAddress || '',
      meetupRadiusMeters: Number(b.meetupRadiusMeters) || 150,
      sessionStatus: 0,
      createTime: nowStr(),
      participants: []
    })
    return ok(res, id)
  })

  router.post('/group-session/:id/join', requireAuth, (req, res) => {
    const result = store.joinGroupSession(reqUser(req), Number(req.params.id))
    if ('error' in result) return fail(res, 40001, result.error)
    return ok(res, result.orderId)
  })

  router.post('/group-session/:id/cancel', requireAuth, (req, res) => {
    const g = state.groupSessions.find((s) => s.id === Number(req.params.id))
    if (!g) return fail(res, 40400, '团课不存在')
    g.sessionStatus = 4
    g.closeReason = String(req.body?.reason || '发起人取消')
    return ok(res, true)
  })

  router.get('/group-session/my-created', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(res, state.groupSessions.filter((g) => g.creatorId === user.id).map(store.toGroupSessionVO))
  })

  router.get('/group-session/my-joined', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(
      res,
      state.groupSessions.filter((g) => g.participants.some((p) => p.buyerId === user.id)).map(store.toGroupSessionVO)
    )
  })

  // ==================== Creator Leaderboard & Profile ====================
  router.get('/creator/leaderboard', (req, res) => {
    const { campusId, tagId, rankBy, limit } = req.query
    return ok(
      res,
      store.buildLeaderboard({
        campusId: campusId ? Number(campusId) : undefined,
        tagId: tagId ? Number(tagId) : undefined,
        rankBy: rankBy ? String(rankBy) : undefined,
        limit: limit ? Number(limit) : undefined
      })
    )
  })

  router.get('/creator/:userId/profile', (req, res) => {
    const creator = store.getUserById(Number(req.params.userId))
    if (!creator) return fail(res, 40400, '创作者不存在')
    const userServices = state.services.filter((s) => s.sellerId === creator.id && s.status === 1)
    const serviceIds = new Set(userServices.map((s) => s.id))
    const creatorReviews = state.reviews.filter((r) => serviceIds.has(r.serviceId))
    const avgRating =
      userServices.length > 0
        ? Number((userServices.reduce((acc, s) => acc + s.averageRating, 0) / userServices.length).toFixed(1))
        : 5.0
    return ok(res, {
      userId: creator.id,
      displayName: creator.nickname || creator.realName,
      avatar: creator.avatar,
      grade: creator.grade,
      professionalName: creator.professionalName,
      campusName: creator.campusName,
      bio: creator.bio,
      eduVerified: creator.eduVerified,
      creditScore: creator.creditScore,
      creditTier: creator.creditScore >= 780 ? '极好' : creator.creditScore >= 740 ? '优秀' : '良好',
      publishPermission: creator.publishPermission,
      sellerDeposit: creator.sellerDeposit,
      completedOrderCount: userServices.reduce((acc, s) => acc + s.salesCount, 0),
      activeServiceCount: userServices.length,
      reviewCount: userServices.reduce((acc, s) => acc + s.reviewCount, 0),
      averageRating: avgRating,
      services: userServices.map((s) => ({
        id: s.id,
        serviceTitle: s.serviceTitle,
        serviceDescription: s.serviceDescription,
        coverImage: s.images[0] || '',
        tagName: store.getTagById(s.tagId)?.tagName,
        tradeType: s.tradeType,
        price: s.price,
        currentPurchaseCount: s.currentPurchaseCount,
        averageRating: s.averageRating
      })),
      recentReviews: creatorReviews.map((r) => ({
        id: r.id,
        serviceId: r.serviceId,
        serviceTitle: state.services.find((s) => s.id === r.serviceId)?.serviceTitle || '校园技能服务',
        buyerDisplayName: r.anonymous ? '匿名同学' : r.reviewerName,
        rating: r.rating,
        content: r.content,
        createTime: r.createTime
      }))
    })
  })

  // ==================== Orders, Escrow, Meetup, Milestones, Certificates ====================
  router.post('/order/create', requireAuth, (req, res) => {
    const result = store.createOrder(reqUser(req), req.body || {})
    if ('error' in result) return fail(res, 40001, result.error)
    return ok(res, result.order.id)
  })

  router.get('/order/myOrders', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { status, pageNum = 1, pageSize = 10 } = req.query
    let list = state.orders.filter((o) => o.buyerId === user.id)
    if (status !== undefined && status !== '') list = list.filter((o) => o.orderStatus === Number(status))
    return ok(res, paginate(list.map(store.toOrderListItemVO), Number(pageNum), Number(pageSize)))
  })

  router.get('/order/mySellingOrders', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { status, pageNum = 1, pageSize = 10 } = req.query
    let list = state.orders.filter((o) => o.sellerId === user.id)
    if (status !== undefined && status !== '') list = list.filter((o) => o.orderStatus === Number(status))
    return ok(res, paginate(list.map(store.toOrderListItemVO), Number(pageNum), Number(pageSize)))
  })

  router.post('/payment/escrow/prepay', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.body?.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    return ok(res, {
      paymentNo: `ESC${Date.now()}`,
      orderId: order.id,
      payChannel: String(req.body?.payChannel || 'ALIPAY_SANDBOX'),
      amount: order.totalAmount,
      sdkPayParams: { channel: 'SANDBOX_ESCROW' },
      sandboxCallbackSign: 'sandbox_verified_sig'
    })
  })

  router.post('/payment/escrow/webhook/callback', requireAuth, (req, res) => {
    const user = reqUser(req)
    const unpaid = state.orders.find((o) => o.buyerId === user.id && o.orderStatus === 1)
    if (unpaid) store.payOrder(user, unpaid.id, 2)
    return ok(res, null)
  })

  router.post('/order/verify-qr', requireAuth, (req, res) => {
    const orderId = Number(req.body?.expectedOrderId)
    const order = state.orders.find((o) => o.id === orderId)
    if (!order) return fail(res, 40400, '订单不存在')
    const buyer = store.getUserById(order.buyerId)
    if (buyer) store.confirmOrder(buyer, order.id)
    return ok(res, order.id)
  })

  router.get('/order/certificate/:orderId', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    const seller = store.getUserById(order.sellerId)
    const buyer = store.getUserById(order.buyerId)
    const sha256Seal = crypto.createHash('sha256').update(`${order.orderNumber}:${order.totalAmount}:xyf`).digest('hex')
    return ok(res, {
      certificateNo: `CERT-${order.orderNumber}`,
      orderId: order.id,
      orderNumber: order.orderNumber,
      serviceId: order.serviceId,
      serviceTitle: order.serviceTitle,
      serviceTagName: order.serviceTagName,
      campusId: order.campusId,
      campusName: store.getCampusById(order.campusId)?.campusName || '主校区',
      sellerId: order.sellerId,
      sellerName: seller?.nickname || '创作者',
      sellerStudentIdMasked: `${(seller?.studentId || '20240000').slice(0, 4)}****`,
      sellerEduVerified: seller?.eduVerified ?? 1,
      buyerId: order.buyerId,
      buyerName: buyer?.nickname || '买家',
      buyerStudentIdMasked: `${(buyer?.studentId || '20240000').slice(0, 4)}****`,
      totalAmount: order.totalAmount,
      tradeType: order.tradeType,
      verifyChannel: order.verifyChannel || 'ONLINE_CONFIRM',
      meetupPoiName: order.meetupPoiName,
      practiceCreditHours: 2.5,
      completedTime: order.buyerConfirmTime || order.createTime,
      sha256Seal,
      verificationStatus: 'VERIFIED'
    })
  })

  router.get('/order/certificate/:orderId/verify', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    const expectedSeal = crypto.createHash('sha256').update(`${order.orderNumber}:${order.totalAmount}:xyf`).digest('hex')
    const authentic = String(req.query.sealHash || '') === expectedSeal
    return ok(res, {
      orderId: order.id,
      certificateNo: `CERT-${order.orderNumber}`,
      authentic,
      expectedSeal,
      message: authentic ? 'SHA-256 电子存证指纹校验通过' : '存证指纹不匹配'
    })
  })

  router.get('/order/noshow/:orderId', requireAuth, (req, res) => {
    const found = store.noshowClaims.find((c) => c.orderId === Number(req.params.orderId)) || null
    return ok(res, found)
  })

  router.post('/order/noshow/:orderId/claim', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    const claim = {
      id: store.genId(),
      claimNo: `NS${Date.now()}`,
      orderId: order.id,
      orderNumber: order.orderNumber,
      serviceId: order.serviceId,
      buyerId: order.buyerId,
      sellerId: order.sellerId,
      campusId: order.campusId,
      noshowType: 1,
      orderAmount: order.totalAmount,
      refundToBuyerAmount: order.totalAmount,
      compensationAmount: Number((order.totalAmount * 0.2).toFixed(2)),
      compensationSource: 'SELLER_DEPOSIT',
      creditDeducted: 15,
      claimStatus: 1,
      evidenceNote: String(req.body?.evidenceNote || '已提交爽约赔付申请'),
      createTime: nowStr()
    }
    store.noshowClaims.unshift(claim)
    return ok(res, claim)
  })

  router.get('/order/:orderId', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    return order ? ok(res, store.toOrderDetailVO(order)) : fail(res, 40400, '订单不存在')
  })

  router.post('/order/:orderId/pay', requireAuth, (req, res) => {
    const result = store.payOrder(reqUser(req), Number(req.params.orderId), Number(req.body?.paymentMethod) || 1)
    if ('error' in result) return fail(res, 40001, result.error)
    return ok(res, null)
  })

  router.post('/order/:orderId/cancel', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    order.orderStatus = 5
    return ok(res, null)
  })

  router.post('/order/:orderId/ship', requireAuth, (req, res) => {
    const user = reqUser(req)
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    order.orderStatus = 3
    order.sellerShipTime = nowStr()
    order.deliveryNote = String(req.body?.deliveryNote || '已完成服务交付，请验收确认。')
    if (Array.isArray(req.body?.deliveryImages)) order.deliveryImages = req.body.deliveryImages
    order.logs.push({
      id: store.genId(),
      orderId: order.id,
      operatorId: user.id,
      operatorName: user.nickname || user.realName,
      action: 'SHIP',
      actionDesc: order.deliveryNote,
      createTime: nowStr()
    })
    return ok(res, null)
  })

  router.post('/order/:orderId/confirm', requireAuth, (req, res) => {
    const result = store.confirmOrder(reqUser(req), Number(req.params.orderId))
    if ('error' in result) return fail(res, 40001, result.error)
    return ok(res, null)
  })

  router.get('/order/:orderId/verify-credential', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    return ok(res, {
      orderId: order.id,
      orderNumber: order.orderNumber,
      orderStatus: order.orderStatus,
      verifyCode: order.verifyCode,
      codePeriodSeconds: 120,
      codeRemainingSeconds: 95,
      qrToken: `QR:${order.id}:${order.verifyCode}`,
      qrPeriodSeconds: 60,
      qrRemainingSeconds: 45,
      serverTimestamp: Date.now()
    })
  })

  router.post('/order/:orderId/verify-code', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    if (String(req.body?.verifyCode || '').trim() !== order.verifyCode) {
      return fail(res, 40001, '核销码不正确')
    }
    const buyer = store.getUserById(order.buyerId)
    if (buyer) store.confirmOrder(buyer, order.id)
    return ok(res, null)
  })

  router.post('/order/:orderId/refund', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    order.refundStatus = 1
    order.refundReason = String(req.body?.reason || '协商退款')
    order.refundRequestTime = nowStr()
    return ok(res, null)
  })

  router.post('/order/:orderId/handleRefund', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    if (req.body?.approve) {
      order.refundStatus = 4
      order.orderStatus = 5
      order.paymentStatus = 3
      const buyer = store.getUserById(order.buyerId)
      if (buyer) {
        buyer.walletBalance = Number((buyer.walletBalance + order.frozenAmount).toFixed(2))
        buyer.frozenAmount = Math.max(0, Number((buyer.frozenAmount - order.frozenAmount).toFixed(2)))
      }
      order.frozenAmount = 0
    } else {
      order.refundStatus = 3
    }
    return ok(res, null)
  })

  router.get('/order/:orderId/meetup/proposals', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    return ok(res, order?.meetupProposals || [])
  })

  router.post('/order/:orderId/meetup/propose', requireAuth, (req, res) => {
    const user = reqUser(req)
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    const id = store.genId()
    order.meetupProposals.unshift({
      id,
      orderId: order.id,
      proposerId: user.id,
      proposerName: user.nickname || user.realName,
      proposerRole: order.buyerId === user.id ? 1 : 2,
      latitude: Number(req.body?.latitude) || 39.9991,
      longitude: Number(req.body?.longitude) || 116.3268,
      poiName: String(req.body?.poiName || '主校区图书馆正门'),
      detailAddress: String(req.body?.detailAddress || ''),
      radiusMeters: Number(req.body?.radiusMeters) || 150,
      proposedTime: String(req.body?.proposedTime || nowStr()),
      remark: String(req.body?.remark || ''),
      proposalStatus: 0,
      createTime: nowStr()
    })
    return ok(res, id)
  })

  router.post('/order/meetup/:proposalId/respond', requireAuth, (req, res) => {
    const pid = Number(req.params.proposalId)
    for (const order of state.orders) {
      const p = order.meetupProposals.find((item) => item.id === pid)
      if (p) {
        p.proposalStatus = req.body?.accept ? 1 : 2
        p.rejectReason = req.body?.rejectReason
        p.respondTime = nowStr()
        if (req.body?.accept) {
          order.meetupConfirmed = 1
          order.meetupPoiName = p.poiName
          order.meetupAddress = p.detailAddress
          order.meetupLat = p.latitude
          order.meetupLng = p.longitude
        }
        return ok(res, null)
      }
    }
    return fail(res, 40400, '协商提案不存在')
  })

  router.get('/order/:orderId/milestones', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    return ok(res, order?.milestones || [])
  })

  router.post('/order/:orderId/milestones/plan', requireAuth, (req, res) => {
    const order = state.orders.find((o) => o.id === Number(req.params.orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    const stages = Array.isArray(req.body?.stages) ? req.body.stages : []
    order.milestones = stages.map((st: { title: string; amount: number }, i: number) => ({
      id: store.genId(),
      orderId: order.id,
      stageIndex: i + 1,
      title: st.title,
      amount: Number(st.amount),
      milestoneStatus: 0,
      createTime: nowStr()
    }))
    return ok(res, order.milestones)
  })

  router.post('/order/milestone/:milestoneId/submit', requireAuth, (req, res) => {
    const mid = Number(req.params.milestoneId)
    for (const o of state.orders) {
      const m = o.milestones.find((item) => item.id === mid)
      if (m) {
        m.milestoneStatus = 1
        m.proofNote = String(req.body?.proofNote || '已提交阶段交付成果')
        m.submittedTime = nowStr()
        return ok(res, true)
      }
    }
    return fail(res, 40400, '里程碑不存在')
  })

  router.post('/order/milestone/:milestoneId/release', requireAuth, (req, res) => {
    const mid = Number(req.params.milestoneId)
    for (const o of state.orders) {
      const m = o.milestones.find((item) => item.id === mid)
      if (m) {
        m.milestoneStatus = 2
        m.releasedTime = nowStr()
        m.platformFee = Number((m.amount * 0.05).toFixed(2))
        m.sellerNetAmount = Number((m.amount * 0.95).toFixed(2))
        return ok(res, true)
      }
    }
    return fail(res, 40400, '里程碑不存在')
  })

  // ==================== Reviews & Favorites ====================
  router.get('/review/service/:serviceId', (req, res) => {
    const serviceId = Number(req.params.serviceId)
    const { pageNum = 1, pageSize = 10 } = req.query
    const list = state.reviews.filter((r) => r.serviceId === serviceId)
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.get('/review/order/:orderId/status', requireAuth, (req, res) => {
    const reviewed = state.reviews.some((r) => r.orderId === Number(req.params.orderId))
    return ok(res, reviewed)
  })

  router.post('/review/create', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { orderId, rating = 5, content = '', anonymous = false } = req.body || {}
    const order = state.orders.find((o) => o.id === Number(orderId))
    if (!order) return fail(res, 40400, '订单不存在')
    const id = store.genId()
    state.reviews.unshift({
      id,
      serviceId: order.serviceId,
      orderId: order.id,
      reviewerId: user.id,
      reviewerName: user.nickname || user.realName,
      reviewerAvatar: user.avatar,
      rating: Number(rating),
      content: String(content),
      anonymous: Boolean(anonymous),
      createTime: nowStr()
    })
    return ok(res, id)
  })

  router.post('/favorite/collect', requireAuth, (req, res) => {
    const user = reqUser(req)
    const serviceId = Number(req.body?.serviceId)
    if (!state.favorites.some((f) => f.userId === user.id && f.serviceId === serviceId)) {
      state.favorites.unshift({ favoriteId: store.genId(), userId: user.id, serviceId, createTime: nowStr() })
      const svc = state.services.find((s) => s.id === serviceId)
      if (svc) svc.collectionCount += 1
    }
    return ok(res, null)
  })

  router.delete('/favorite/collect/:serviceId', requireAuth, (req, res) => {
    const user = reqUser(req)
    const serviceId = Number(req.params.serviceId)
    const idx = state.favorites.findIndex((f) => f.userId === user.id && f.serviceId === serviceId)
    if (idx !== -1) {
      state.favorites.splice(idx, 1)
      const svc = state.services.find((s) => s.id === serviceId)
      if (svc) svc.collectionCount = Math.max(0, svc.collectionCount - 1)
    }
    return ok(res, null)
  })

  router.get('/favorite/myCollections', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { pageNum = 1, pageSize = 10 } = req.query
    const items = state.favorites
      .filter((f) => f.userId === user.id)
      .map((f) => {
        const svc = state.services.find((s) => s.id === f.serviceId)
        if (!svc) return null
        const vo = store.toServiceListItem(svc)
        return {
          favoriteId: f.favoriteId,
          service: vo,
          sellerName: vo.sellerName,
          createTime: f.createTime
        }
      })
      .filter(Boolean)
    return ok(res, paginate(items, Number(pageNum), Number(pageSize)))
  })

  // ==================== Wallet & Deposit ====================
  router.get('/wallet/balance', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(res, {
      walletBalance: user.walletBalance,
      frozenAmount: user.frozenAmount,
      totalAmount: Number((user.walletBalance + user.frozenAmount).toFixed(2))
    })
  })

  router.get('/wallet/transactions', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { pageNum = 1, pageSize = 10, transactionType } = req.query
    let list = state.walletTransactions.filter((t) => t.userId === user.id)
    if (transactionType) list = list.filter((t) => t.transactionType === Number(transactionType))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.post('/wallet/recharge', requireAuth, (req, res) => {
    const user = reqUser(req)
    const amount = Number(req.body?.amount)
    if (!amount || amount <= 0) return fail(res, 40001, '请输入有效充值金额')
    const before = user.walletBalance
    user.walletBalance = Number((user.walletBalance + amount).toFixed(2))
    state.walletTransactions.unshift({
      transactionId: store.genId(),
      userId: user.id,
      transactionType: 1,
      typeName: '充值',
      amount,
      balanceBefore: before,
      balanceAfter: user.walletBalance,
      frozenBefore: user.frozenAmount,
      frozenAfter: user.frozenAmount,
      transactionNo: `WT${Date.now()}`,
      remark: '钱包在线充值',
      createTime: nowStr()
    })
    return ok(res, {
      walletBalance: user.walletBalance,
      frozenAmount: user.frozenAmount,
      totalAmount: Number((user.walletBalance + user.frozenAmount).toFixed(2))
    })
  })

  router.post('/wallet/withdraw', requireAuth, (req, res) => {
    const user = reqUser(req)
    const amount = Number(req.body?.amount)
    if (!amount || amount <= 0 || amount > user.walletBalance) return fail(res, 40001, '提现金额无效或超出可用余额')
    const before = user.walletBalance
    user.walletBalance = Number((user.walletBalance - amount).toFixed(2))
    state.walletTransactions.unshift({
      transactionId: store.genId(),
      userId: user.id,
      transactionType: 2,
      typeName: '提现',
      amount,
      balanceBefore: before,
      balanceAfter: user.walletBalance,
      frozenBefore: user.frozenAmount,
      frozenAfter: user.frozenAmount,
      transactionNo: `WT${Date.now()}`,
      remark: '余额提现申请',
      createTime: nowStr()
    })
    return ok(res, {
      walletBalance: user.walletBalance,
      frozenAmount: user.frozenAmount,
      totalAmount: Number((user.walletBalance + user.frozenAmount).toFixed(2))
    })
  })

  router.get('/wallet/deposit/info', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(res, {
      sellerDeposit: user.sellerDeposit,
      walletBalance: user.walletBalance,
      logs: store.depositLogs.filter((l) => l.userId === user.id)
    })
  })

  router.post('/wallet/deposit/pay', requireAuth, (req, res) => {
    const user = reqUser(req)
    const amount = Number(req.body?.amount)
    if (!amount || amount <= 0 || amount > user.walletBalance) return fail(res, 40001, '可用余额不足')
    const depBefore = user.sellerDeposit
    user.walletBalance = Number((user.walletBalance - amount).toFixed(2))
    user.sellerDeposit = Number((user.sellerDeposit + amount).toFixed(2))
    store.depositLogs.unshift({
      id: store.genId(),
      userId: user.id,
      ledgerNo: `DEP${Date.now()}`,
      actionType: 1,
      amount,
      depositBefore: depBefore,
      depositAfter: user.sellerDeposit,
      remark: '追加诚信保证金',
      createTime: nowStr()
    })
    return ok(res, true)
  })

  router.post('/wallet/deposit/withdraw', requireAuth, (req, res) => {
    const user = reqUser(req)
    const amount = Number(req.body?.amount)
    if (!amount || amount <= 0 || amount > user.sellerDeposit) return fail(res, 40001, '可退保证金不足')
    const depBefore = user.sellerDeposit
    user.sellerDeposit = Number((user.sellerDeposit - amount).toFixed(2))
    user.walletBalance = Number((user.walletBalance + amount).toFixed(2))
    store.depositLogs.unshift({
      id: store.genId(),
      userId: user.id,
      ledgerNo: `DEP${Date.now()}`,
      actionType: 2,
      amount,
      depositBefore: depBefore,
      depositAfter: user.sellerDeposit,
      remark: '退还诚信保证金至钱包余额',
      createTime: nowStr()
    })
    return ok(res, true)
  })

  // ==================== Notifications & Chat ====================
  router.get('/notification/my-notifications', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { pageNum = 1, pageSize = 10, notificationType } = req.query
    let list = state.notifications.filter((n) => n.userId === user.id)
    if (notificationType) list = list.filter((n) => n.notificationType === Number(notificationType))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.get('/notification/unreadCount', requireAuth, (req, res) => {
    const user = reqUser(req)
    return ok(res, state.notifications.filter((n) => n.userId === user.id && n.isRead === 0).length)
  })

  router.post('/notification/:id/read', requireAuth, (req, res) => {
    const item = state.notifications.find((n) => n.id === Number(req.params.id))
    if (item) item.isRead = 1
    return ok(res, null)
  })

  router.post('/notification/readAll', requireAuth, (req, res) => {
    const user = reqUser(req)
    state.notifications.filter((n) => n.userId === user.id).forEach((n) => (n.isRead = 1))
    return ok(res, null)
  })

  router.get('/chat/conversations', requireAuth, (req, res) => {
    const user = reqUser(req)
    const peerIds = new Set<number>()
    for (const m of state.chatMessages) {
      if (m.senderId === user.id) peerIds.add(m.receiverId)
      if (m.receiverId === user.id) peerIds.add(m.senderId)
    }
    const conversations = Array.from(peerIds).map((peerId) => {
      const peer = store.getUserById(peerId)
      const msgs = state.chatMessages
        .filter((m) => (m.senderId === user.id && m.receiverId === peerId) || (m.senderId === peerId && m.receiverId === user.id))
        .sort((a, b) => b.createTime.localeCompare(a.createTime))
      const unreadCount = msgs.filter((m) => m.receiverId === user.id && m.isRead === 0).length
      return {
        userId: peerId,
        realName: peer?.nickname || peer?.realName || '校园同学',
        avatar: peer?.avatar || '',
        lastMessage: msgs[0]?.content || '',
        lastMessageTime: msgs[0]?.createTime || nowStr(),
        unreadCount
      }
    })
    return ok(res, conversations)
  })

  router.get('/chat/messages/:userId', requireAuth, (req, res) => {
    const user = reqUser(req)
    const peerId = Number(req.params.userId)
    const { pageNum = 1, pageSize = 50 } = req.query
    const msgs = state.chatMessages
      .filter((m) => (m.senderId === user.id && m.receiverId === peerId) || (m.senderId === peerId && m.receiverId === user.id))
      .map((m) => {
        if (m.receiverId === user.id) m.isRead = 1
        const s = store.getUserById(m.senderId)
        const r = store.getUserById(m.receiverId)
        return {
          messageId: m.messageId,
          content: m.content,
          messageType: m.messageType,
          isRead: m.isRead,
          relatedOrderId: m.relatedOrderId,
          relatedServiceId: m.relatedServiceId,
          createTime: m.createTime,
          sender: { userId: m.senderId, realName: s?.nickname || s?.realName || '用户', avatar: s?.avatar || '' },
          receiver: { userId: m.receiverId, realName: r?.nickname || r?.realName || '用户', avatar: r?.avatar || '' }
        }
      })
    return ok(res, paginate(msgs, Number(pageNum), Number(pageSize)))
  })

  router.post('/chat/send', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { receiverId, content, messageType = 1, relatedOrderId, relatedServiceId } = req.body || {}
    if (!receiverId || !content) return fail(res, 40001, '消息内容不能为空')
    state.chatMessages.push({
      messageId: store.genId(),
      senderId: user.id,
      receiverId: Number(receiverId),
      content: String(content),
      messageType: Number(messageType) as 1 | 2 | 3,
      isRead: 0,
      relatedOrderId,
      relatedServiceId,
      createTime: nowStr()
    })
    return ok(res, null)
  })

  // ==================== Permissions, Statistics & Disputes ====================
  router.get('/permission/status', requireAuth, (req, res) => {
    const user = reqUser(req)
    const statusMap: Record<number, { status: string | null; text: string }> = {
      0: { status: 'pending', text: '审核中' },
      1: { status: 'approved', text: '已批准' },
      2: { status: user.publishPermission === 1 ? 'approved' : null, text: user.publishPermission === 1 ? '已批准' : '未申请' }
    }
    const mapped = statusMap[user.permissionReviewStatus] || { status: null, text: '未申请' }
    return ok(res, {
      userId: user.id,
      studentId: user.studentId,
      hasPermission: user.publishPermission === 1,
      publishPermission: user.publishPermission,
      permissionReviewStatus: user.permissionReviewStatus,
      status: mapped.status,
      statusText: mapped.text
    })
  })

  router.post('/permission/apply', requireAuth, (req, res) => {
    const user = reqUser(req)
    if (user.publishPermission === 1) return ok(res, { status: 'approved', message: '您已具备服务发布权限' })
    user.permissionReviewStatus = 0
    user.applyReason = String(req.body?.reason || '申请成为学艺坊校园技能创作者')
    return ok(res, { status: 'pending', message: '申请已提交，等待管理员审核' })
  })

  router.get('/statistics/sales', requireAuth, (req, res) => {
    const user = reqUser(req)
    const myServices = state.services.filter((s) => s.sellerId === user.id)
    const mySalesOrders = state.orders.filter((o) => o.sellerId === user.id && o.orderStatus === 4)
    const totalSales = myServices.reduce((acc, s) => acc + s.salesCount, 0)
    const totalRevenue = myServices.reduce((acc, s) => acc + s.salesCount * s.price, 0)
    const best = [...myServices].sort((a, b) => b.salesCount * b.price - a.salesCount * a.price)[0]
    return ok(res, {
      totalSales,
      totalRevenue,
      averagePrice: totalSales > 0 ? Number((totalRevenue / totalSales).toFixed(2)) : 0,
      bestService: best
        ? { serviceId: best.id, serviceTitle: best.serviceTitle, sales: best.salesCount, revenue: best.salesCount * best.price }
        : null,
      recentOrders: mySalesOrders.slice(0, 5).map((o) => ({
        orderId: o.id,
        orderNumber: o.orderNumber,
        totalAmount: o.totalAmount,
        createTime: o.createTime
      }))
    })
  })

  const toDisputeVO = (d: (typeof state.disputes)[number]) => {
    const order = state.orders.find((o) => o.id === d.orderId)
    const buyer = order ? store.getUserById(order.buyerId) : undefined
    const seller = order ? store.getUserById(order.sellerId) : undefined
    return {
      ...d,
      orderNumber: order?.orderNumber || 'XYF0000',
      serviceTitle: order?.serviceTitle || '校园技能服务',
      totalAmount: order?.totalAmount || 0,
      frozenAmount: order?.frozenAmount || 0,
      orderStatus: order?.orderStatus || 3,
      tradeType: order?.tradeType || 2,
      buyerId: order?.buyerId || 3,
      buyerName: buyer?.nickname || '买家',
      buyerAvatar: buyer?.avatar || '',
      sellerId: order?.sellerId || 2,
      sellerName: seller?.nickname || '卖家',
      sellerAvatar: seller?.avatar || '',
      deliveryNote: order?.deliveryNote || '',
      deliveryImages: order?.deliveryImages || []
    }
  }

  router.post('/dispute/create', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { orderId, disputeType = 4, description = '', evidenceImages = [] } = req.body || {}
    const disputeId = store.genId()
    state.disputes.unshift({
      disputeId,
      orderId: Number(orderId),
      disputeInitiatorId: user.id,
      disputeType: Number(disputeType),
      description: String(description),
      evidenceImages: Array.isArray(evidenceImages) ? evidenceImages : [],
      disputeStatus: 1,
      createTime: nowStr()
    })
    return ok(res, disputeId)
  })

  router.get('/dispute/my', requireAuth, (req, res) => {
    const user = reqUser(req)
    const { pageNum = 1, pageSize = 10 } = req.query
    const list = state.disputes
      .map(toDisputeVO)
      .filter((d) => d.buyerId === user.id || d.sellerId === user.id)
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.get('/dispute/:disputeId', requireAuth, (req, res) => {
    const found = state.disputes.find((d) => d.disputeId === Number(req.params.disputeId))
    return found ? ok(res, toDisputeVO(found)) : fail(res, 40400, '纠纷不存在')
  })

  // ==================== Admin Endpoints ====================
  router.get('/admin/statistics', requireAdmin, (_req, res) => {
    const completed = state.orders.filter((o) => o.orderStatus === 4)
    return ok(res, {
      totalUsers: state.users.length,
      activeUsers: state.users.filter((u) => u.accountStatus === 1).length,
      totalServices: state.services.length,
      totalOrders: state.orders.length + 124,
      completedOrders: completed.length + 112,
      pendingDisputes: state.disputes.filter((d) => d.disputeStatus === 1).length,
      totalTransactionAmount: 18640.5 + state.orders.reduce((acc, o) => acc + o.totalAmount, 0),
      todayNewUsers: 3,
      todayNewServices: 2,
      todayNewOrders: 9
    })
  })

  router.get('/admin/statistics/trend', requireAdmin, (_req, res) => {
    const dates: string[] = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date(Date.now() - i * 86400000)
      dates.push(`${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
    }
    return ok(res, {
      dates,
      orderCounts: [12, 16, 14, 21, 19, 25, 28],
      serviceCounts: [2, 3, 1, 4, 2, 3, 5],
      transactionAmounts: [1280, 1840, 1520, 2460, 2190, 2980, 3420]
    })
  })

  router.get('/admin/users/pending', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10 } = req.query
    const pending = state.users
      .filter((u) => u.permissionReviewStatus === 0)
      .map((u) => ({
        userId: u.id,
        studentId: u.studentId,
        realName: u.realName,
        phone: u.phone,
        email: u.email,
        grade: u.grade,
        professionalName: u.professionalName,
        permissionReviewStatus: u.permissionReviewStatus,
        applyReason: u.applyReason || '申请开通校园创作者权限',
        createTime: u.createTime
      }))
    return ok(res, paginate(pending, Number(pageNum), Number(pageSize)))
  })

  router.put('/admin/permission/review', requireAdmin, (req, res) => {
    const { userId, approved } = req.body || {}
    const u = store.getUserById(Number(userId))
    if (!u) return fail(res, 40400, '用户不存在')
    u.publishPermission = approved ? 1 : 0
    u.permissionReviewStatus = approved ? 1 : 2
    return ok(res, null)
  })

  router.get('/admin/users/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, keyword, accountStatus, publishPermission } = req.query
    let list = [...state.users]
    if (keyword) {
      const kw = String(keyword).trim().toLowerCase()
      list = list.filter((u) => u.studentId.includes(kw) || u.realName.toLowerCase().includes(kw) || u.nickname.toLowerCase().includes(kw))
    }
    if (accountStatus !== undefined && accountStatus !== '') list = list.filter((u) => u.accountStatus === Number(accountStatus))
    if (publishPermission !== undefined && publishPermission !== '') list = list.filter((u) => u.publishPermission === Number(publishPermission))
    const mapped = list.map((u) => ({
      userId: u.id,
      studentId: u.studentId,
      realName: u.realName,
      nickname: u.nickname,
      phone: u.phone,
      email: u.email,
      dormitory: u.dormitory,
      grade: u.grade,
      professionalId: u.professionalId,
      professionalName: u.professionalName,
      role: u.role,
      publishPermission: u.publishPermission,
      permissionReviewStatus: u.permissionReviewStatus,
      applyReason: u.applyReason,
      walletBalance: u.walletBalance,
      frozenAmount: u.frozenAmount,
      accountStatus: u.accountStatus,
      createTime: u.createTime
    }))
    return ok(res, paginate(mapped, Number(pageNum), Number(pageSize)))
  })

  router.put('/admin/users/status', requireAdmin, (req, res) => {
    const { userId, accountStatus, publishPermission } = req.body || {}
    const u = store.getUserById(Number(userId))
    if (!u) return fail(res, 40400, '用户不存在')
    if (accountStatus !== undefined) u.accountStatus = Number(accountStatus)
    if (publishPermission !== undefined) u.publishPermission = Number(publishPermission)
    return ok(res, null)
  })

  router.put('/admin/users/:userId/reset-password', requireAdmin, (req, res) => {
    const u = store.getUserById(Number(req.params.userId))
    if (u) u.password = 'password123'
    return ok(res, null)
  })

  const toAdminServiceVO = (s: SeedService) => ({
    serviceId: s.id,
    serviceTitle: s.serviceTitle,
    coverImage: s.images[0] || null,
    description: s.serviceDescription,
    sellerName: store.getUserById(s.sellerId)?.nickname || '创作者',
    tagName: store.getTagById(s.tagId)?.tagName || '校园技能',
    price: s.price,
    status: s.status,
    reviewStatus: s.reviewStatus,
    reviewReason: s.reviewReason || null,
    createTime: s.createTime
  })

  router.get('/admin/services/pending', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10 } = req.query
    const list = state.services.filter((s) => s.status === 3 || s.reviewStatus === 2).map(toAdminServiceVO)
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.get('/admin/services/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, status, keyword } = req.query
    let list = [...state.services]
    if (status !== undefined && status !== '') list = list.filter((s) => s.status === Number(status))
    if (keyword) {
      const kw = String(keyword).trim().toLowerCase()
      list = list.filter((s) => s.serviceTitle.toLowerCase().includes(kw))
    }
    return ok(res, paginate(list.map(toAdminServiceVO), Number(pageNum), Number(pageSize)))
  })

  router.put('/admin/services/service/review', requireAdmin, (req, res) => {
    const { serviceId, approved, reason } = req.body || {}
    const svc = state.services.find((s) => s.id === Number(serviceId))
    if (!svc) return fail(res, 40400, '服务不存在')
    svc.status = approved ? 1 : 4
    svc.reviewStatus = approved ? 1 : 3
    svc.reviewReason = reason
    return ok(res, null)
  })

  router.put('/admin/services/:serviceId/offline', requireAdmin, (req, res) => {
    const svc = state.services.find((s) => s.id === Number(req.params.serviceId))
    if (svc) {
      svc.status = 2
      svc.reviewReason = String(req.body?.reason || '管理员巡检下架')
    }
    return ok(res, null)
  })

  router.get('/admin/demand/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, status, keyword } = req.query
    let list = [...state.demands]
    if (status !== undefined && status !== '') list = list.filter((d) => d.demandStatus === Number(status))
    if (keyword) {
      const kw = String(keyword).trim().toLowerCase()
      list = list.filter((d) => d.title.toLowerCase().includes(kw))
    }
    return ok(res, paginate(list.map(store.toDemandVO), Number(pageNum), Number(pageSize)))
  })

  router.post('/admin/demand/:id/close', requireAdmin, (req, res) => {
    const d = state.demands.find((item) => item.id === Number(req.params.id))
    if (d) {
      d.demandStatus = 3
      d.closeReason = String(req.body?.reason || '管理员关闭')
    }
    return ok(res, true)
  })

  router.get('/admin/dispute/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, disputeStatus, orderNumber } = req.query
    let list = state.disputes.map(toDisputeVO)
    if (disputeStatus !== undefined && disputeStatus !== '') list = list.filter((d) => d.disputeStatus === Number(disputeStatus))
    if (orderNumber) list = list.filter((d) => d.orderNumber.includes(String(orderNumber)))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.get('/admin/dispute/by-order/:orderId', requireAdmin, (req, res) => {
    const found = state.disputes.find((d) => d.orderId === Number(req.params.orderId))
    return ok(res, found ? toDisputeVO(found) : null)
  })

  router.get('/admin/dispute/:disputeId', requireAdmin, (req, res) => {
    const found = state.disputes.find((d) => d.disputeId === Number(req.params.disputeId))
    return found ? ok(res, toDisputeVO(found)) : fail(res, 40400, '纠纷不存在')
  })

  router.get('/admin/dispute/:disputeId/chat-logs', requireAdmin, (_req, res) => ok(res, []))

  router.post('/admin/dispute/:disputeId/handle', requireAdmin, (req, res) => {
    const d = state.disputes.find((item) => item.disputeId === Number(req.params.disputeId))
    if (!d) return fail(res, 40400, '纠纷不存在')
    d.disputeStatus = Number(req.body?.actionType) === 1 ? 3 : 4
    d.adminReply = String(req.body?.adminReply || '')
    d.resolution = String(req.body?.resolution || '')
    return ok(res, true)
  })

  // Admin Resources (Tags, Professionals, Campuses, Trade Locations, SysConfigs)
  router.get('/admin/service-tag/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, nameLike } = req.query
    let list = [...state.serviceTags]
    if (nameLike) list = list.filter((t) => t.tagName.includes(String(nameLike)))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })
  router.post('/admin/service-tag/add', requireAdmin, (req, res) => {
    const id = store.genId()
    state.serviceTags.push({
      tagId: id,
      id,
      tagName: String(req.body?.tagName || '新标签'),
      description: String(req.body?.description || ''),
      icon: 'CollectionTag',
      iconUrl: String(req.body?.iconUrl || ''),
      createTime: nowStr()
    })
    return ok(res, true)
  })
  router.put('/admin/service-tag/update', requireAdmin, (req, res) => {
    const t = state.serviceTags.find((item) => item.id === Number(req.body?.id))
    if (t) Object.assign(t, req.body)
    return ok(res, true)
  })
  router.delete('/admin/service-tag/:id', requireAdmin, (req, res) => {
    const idx = state.serviceTags.findIndex((item) => item.id === Number(req.params.id))
    if (idx !== -1) state.serviceTags.splice(idx, 1)
    return ok(res, true)
  })

  router.get('/admin/professional/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, nameLike } = req.query
    let list = [...state.professionals]
    if (nameLike) list = list.filter((p) => p.professionalName.includes(String(nameLike)))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })
  router.post('/admin/professional/add', requireAdmin, (req, res) => {
    state.professionals.push({ id: store.genId(), professionalName: String(req.body?.professionalName || '新专业'), description: String(req.body?.description || '') })
    return ok(res, true)
  })
  router.put('/admin/professional/update', requireAdmin, (req, res) => {
    const p = state.professionals.find((item) => item.id === Number(req.body?.id))
    if (p) Object.assign(p, req.body)
    return ok(res, true)
  })
  router.delete('/admin/professional/:id', requireAdmin, (req, res) => {
    const idx = state.professionals.findIndex((item) => item.id === Number(req.params.id))
    if (idx !== -1) state.professionals.splice(idx, 1)
    return ok(res, true)
  })

  router.get('/admin/campus/list', requireAdmin, (_req, res) => ok(res, state.campuses))
  router.post('/admin/campus/save', requireAdmin, (req, res) => {
    const body = req.body || {}
    if (body.id) {
      const existing = state.campuses.find((c) => c.id === Number(body.id))
      if (existing) Object.assign(existing, body)
      return ok(res, Number(body.id))
    }
    const id = store.genId()
    state.campuses.push({ ...state.campuses[0], ...body, id })
    return ok(res, id)
  })
  router.get('/admin/campus/revenue-summary', requireAdmin, (_req, res) =>
    ok(res, { totalCommissionRevenue: 932.4, settledOrderCount: 115 })
  )

  router.get('/admin/trade-location/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, nameLike, isAvailable } = req.query
    let list = [...state.tradeLocations]
    if (nameLike) list = list.filter((l) => l.locationName.includes(String(nameLike)))
    if (isAvailable !== undefined && isAvailable !== '') list = list.filter((l) => l.isAvailable === Number(isAvailable))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })
  router.post('/admin/trade-location/add', requireAdmin, (req, res) => {
    state.tradeLocations.push({ ...state.tradeLocations[0], ...req.body, id: store.genId() })
    return ok(res, true)
  })
  router.put('/admin/trade-location/update', requireAdmin, (req, res) => {
    const l = state.tradeLocations.find((item) => item.id === Number(req.body?.id))
    if (l) Object.assign(l, req.body)
    return ok(res, true)
  })
  router.delete('/admin/trade-location/:id', requireAdmin, (req, res) => {
    const idx = state.tradeLocations.findIndex((item) => item.id === Number(req.params.id))
    if (idx !== -1) state.tradeLocations.splice(idx, 1)
    return ok(res, true)
  })

  router.get('/admin/sys-config/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 20, keyLike } = req.query
    let list = [...state.sysConfigs]
    if (keyLike) list = list.filter((c) => c.configKey.toLowerCase().includes(String(keyLike).toLowerCase()))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })
  router.get('/admin/sys-config/key-values', requireAdmin, (_req, res) => {
    const kv: Record<string, string> = {}
    state.sysConfigs.forEach((c) => (kv[c.configKey] = c.configValue))
    return ok(res, kv)
  })
  router.get('/admin/sys-config/:id', requireAdmin, (req, res) => {
    const c = state.sysConfigs.find((item) => item.id === Number(req.params.id))
    return c ? ok(res, c) : fail(res, 40400, '配置不存在')
  })
  router.put('/admin/sys-config/update', requireAdmin, (req, res) => {
    const c = state.sysConfigs.find((item) => item.id === Number(req.body?.id) || item.configKey === req.body?.configKey)
    if (c) {
      c.configValue = String(req.body?.configValue ?? c.configValue)
      c.updateTime = nowStr()
    }
    return ok(res, true)
  })

  router.post('/admin/user-import/upload', requireAdmin, (_req, res) =>
    ok(res, { totalCount: 0, successCount: 0, failureCount: 0, errors: [] })
  )

  // Admin Risk Radar, Finance & System Health Audit
  router.get('/admin/risk-radar/overview', requireAdmin, (_req, res) =>
    ok(res, {
      totalAlerts: state.riskAlerts.length,
      pendingAlerts: state.riskAlerts.filter((a) => a.handleStatus === 0).length,
      highSeverityAlerts: state.riskAlerts.filter((a) => a.severity >= 3).length,
      confirmedPenalties: state.riskAlerts.filter((a) => a.handleStatus === 1).length,
      dismissedAlerts: state.riskAlerts.filter((a) => a.handleStatus === 2).length,
      scannedOrders7d: 68,
      bidirectionalWashCount: 0,
      highFreqPairCount: 0,
      ultraFastCompletionCount: state.riskAlerts.length
    })
  )

  router.get('/admin/risk-radar/list', requireAdmin, (req, res) => {
    const { pageNum = 1, pageSize = 10, handleStatus } = req.query
    let list = [...state.riskAlerts]
    if (handleStatus !== undefined && handleStatus !== '') list = list.filter((a) => a.handleStatus === Number(handleStatus))
    return ok(res, paginate(list, Number(pageNum), Number(pageSize)))
  })

  router.post('/admin/risk-radar/scan', requireAdmin, (_req, res) => ok(res, state.riskAlerts.length))

  router.post('/admin/risk-radar/:id/resolve', requireAdmin, (req, res) => {
    const alert = state.riskAlerts.find((a) => a.id === Number(req.params.id))
    if (!alert) return fail(res, 40400, '预警不存在')
    alert.handleStatus = Number(req.body?.handleStatus) || 2
    alert.handleRemark = String(req.body?.handleRemark || '')
    alert.handleTime = nowStr()
    return ok(res, alert)
  })

  router.get('/admin/finance/overview', requireAdmin, (_req, res) => {
    const frozenPool = state.orders.reduce((acc, o) => acc + o.frozenAmount, 0)
    const depositPool = state.users.reduce((acc, u) => acc + u.sellerDeposit, 0)
    return ok(res, {
      totalPlatformCommission: 932.4,
      totalSellerSettled: 17715.6,
      currentEscrowFrozenPool: frozenPool,
      currentSellerDepositPool: depositPool,
      totalNoshowClaims: store.noshowClaims.length,
      totalNoshowCompensationAmount: 0,
      totalReconcileReports: store.reconcileReports.length,
      discrepancyReportCount: 0,
      recentReports: store.reconcileReports,
      recentNoshowClaims: store.noshowClaims
    })
  })

  router.post('/admin/finance/reconcile', requireAdmin, (req, res) => {
    const reportDate = String(req.query.reportDate || new Date().toISOString().slice(0, 10))
    const report = {
      id: store.genId(),
      reportNo: `REC${Date.now()}`,
      reportDate,
      paidOrderCount: state.orders.filter((o) => o.paymentStatus === 2).length,
      completedOrderCount: state.orders.filter((o) => o.orderStatus === 4).length,
      refundOrderCount: 0,
      noshowCompensatedCount: 0,
      grossEscrowInflow: state.orders.reduce((acc, o) => acc + o.totalAmount, 0),
      sellerSettledAmount: state.orders.filter((o) => o.orderStatus === 4).reduce((acc, o) => acc + o.sellerSettleAmount, 0),
      platformCommissionAmount: state.orders.filter((o) => o.orderStatus === 4).reduce((acc, o) => acc + o.platformFee, 0),
      refundOutflowAmount: 0,
      noshowPenaltyAmount: 0,
      activeEscrowFrozenPool: state.orders.reduce((acc, o) => acc + o.frozenAmount, 0),
      discrepancyCount: 0,
      discrepancyAmount: 0,
      reconcileStatus: 1,
      auditNote: '实时日终对账完成，总分账一致',
      createTime: nowStr()
    }
    store.reconcileReports.unshift(report)
    return ok(res, report)
  })

  router.get('/admin/finance/export-csv', requireAdmin, (_req, res) =>
    ok(res, {
      fileName: `xueyifang-reconcile-${new Date().toISOString().slice(0, 10)}.csv`,
      csvContent: 'reportNo,reportDate,grossEscrowInflow,platformCommissionAmount,reconcileStatus\nREC20261127001,2026-11-27,1846,78.5,1\n'
    })
  )

  router.post('/admin/finance/noshow/scan', requireAdmin, (_req, res) => ok(res, 0))

  router.get('/admin/system-audit/inspect', requireAdmin, (_req, res) =>
    ok(res, {
      auditBatchNo: `AUD${Date.now()}`,
      inspectTime: nowStr(),
      overallHealthScore: 98,
      healthLevel: 'EXCELLENT',
      negativeWalletCount: 0,
      escrowMismatchCount: 0,
      unverifiedActiveSellerCount: 0,
      pendingHighRiskAlertCount: 0,
      overdueUncompensatedOrderCount: 0,
      totalUsers: state.users.length,
      totalActiveServices: state.services.filter((s) => s.status === 1).length,
      totalCompletedOrders: state.orders.filter((o) => o.orderStatus === 4).length + 112,
      totalGroupSessions: state.groupSessions.length,
      totalDemands: state.demands.length,
      compositeIndexOptimized: true,
      securityChecks: [
        { checkCode: 'WALLET_NON_NEGATIVE', checkTitle: '账户余额与冻结金非负校验', passed: true, severity: 'PASS', detail: '全部钱包余额与托管金额均 ≥ 0', recommendation: '保持事务原子性扣款' },
        { checkCode: 'ESCROW_BALANCE_MATCH', checkTitle: '担保资金池总分账一致', passed: true, severity: 'PASS', detail: '在途订单冻结金总额与托管总账完全一致', recommendation: '每日定时执行财务对账' },
        { checkCode: 'EDU_AUTH_COVERAGE', checkTitle: '活跃创作者高校邮箱核验率', passed: true, severity: 'PASS', detail: '100% 活跃创作者已完成实名核验', recommendation: '持续要求新申请者绑定 .edu.cn 邮箱' }
      ]
    })
  )

  return router
}
