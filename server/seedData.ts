export interface SeedUser {
  id: number
  studentId: string
  password: string
  realName: string
  nickname: string
  phone: string
  email: string
  dormitory: string
  grade: string
  professionalId: number
  professionalName: string
  campusId: number
  campusName: string
  avatar: string
  bio: string
  role: number // 1=student, 2=admin
  publishPermission: number // 0=no, 1=yes
  permissionReviewStatus: number // 0=pending, 1=approved, 2=rejected
  applyReason?: string
  walletBalance: number
  frozenAmount: number
  accountStatus: number // 1=normal, 0=disabled
  eduVerified: number // 0/1
  verifiedEduEmail?: string
  creditScore: number
  sellerDeposit: number
  createTime: string
}

export interface SeedCampus {
  id: number
  campusCode: string
  campusName: string
  city: string
  centerLat: number
  centerLng: number
  geofencePolygon: string
  safeRadiusMeters: number
  eduEmailDomains: string
  status: number
}

export interface SeedServiceTag {
  tagId: number
  id: number
  tagName: string
  description: string
  icon: string
  iconUrl: string
  createTime: string
}

export interface SeedProfessional {
  id: number
  professionalName: string
  description: string
}

export interface SeedTradeLocation {
  id: number
  campusId: number
  locationName: string
  locationDescription: string
  locationAddress: string
  latitude: number
  longitude: number
  validRadiusMeters: number
  hasCctv: number
  isAvailable: number
}

export interface SeedServiceSlot {
  id: number
  serviceId: number
  slotStart: string
  slotEnd: string
  maxCapacity: number
  bookedCount: number
  remainingCapacity: number
  status: number
}

export interface SeedService {
  id: number
  serviceTitle: string
  serviceDescription: string
  sellerId: number
  campusId: number
  tagId: number
  tradeType: number // 1=offline, 2=online
  tradeLocationId?: number
  price: number
  maxPurchases: number
  currentPurchaseCount: number
  isPhysical: number
  requireSlot: number
  cancelFreeHours: number
  requireBleVerify: number
  status: number // 1=online, 2=offline, 3=pending, 4=rejected
  reviewStatus: number // 1=approved, 2=pending, 3=rejected
  reviewReason?: string
  viewCount: number
  collectionCount: number
  salesCount: number
  averageRating: number
  reviewCount: number
  images: string[]
  deliveryTime: string
  requirements: string
  createTime: string
  updateTime: string
}

export interface SeedReview {
  id: number
  serviceId: number
  orderId: number
  reviewerId: number
  reviewerName: string
  reviewerAvatar: string
  rating: number
  content: string
  anonymous: boolean
  createTime: string
}

export interface SeedDemandBid {
  id: number
  demandId: number
  bidderId: number
  bidPrice: number
  estimatedHours: number
  bidPlan: string
  bidStatus: number // 0=pending, 1=accepted, 2=rejected
  matchedOrderId?: number
  createTime: string
}

export interface SeedDemand {
  id: number
  demandNo: string
  publisherId: number
  campusId: number
  tagId: number
  title: string
  description: string
  budgetMin: number
  budgetMax: number
  tradeType: number
  urgencyLevel: number // 1=常规, 2=加急, 3=十万火急
  deadlineTime: string
  meetupLat?: number
  meetupLng?: number
  meetupPoiName?: string
  meetupAddress?: string
  meetupRadiusMeters?: number
  demandStatus: number // 0=open, 1=matched, 2=cancelled, 3=closed
  acceptedBidId?: number
  matchedServiceId?: number
  matchedOrderId?: number
  closeReason?: string
  createTime: string
  bids: SeedDemandBid[]
}

export interface SeedGroupParticipant {
  id: number
  buyerId: number
  orderId: number
  lockedPrice: number
  effectivePrice: number
  rebateAmount: number
  createTime: string
}

export interface SeedGroupSession {
  id: number
  sessionNo: string
  creatorId: number
  campusId: number
  tagId: number
  title: string
  description: string
  basePrice: number
  tierTargetCount: number
  tierDiscountPrice: number
  currentUnitPrice: number
  minParticipants: number
  maxParticipants: number
  tierRebateApplied: number
  tradeType: number
  sessionStart: string
  sessionEnd: string
  meetupLat?: number
  meetupLng?: number
  meetupPoiName?: string
  meetupAddress?: string
  meetupRadiusMeters?: number
  sessionStatus: number // 0=recruiting, 1=formed, 2=full, 3=completed, 4=cancelled
  closeReason?: string
  createTime: string
  participants: SeedGroupParticipant[]
}

export interface SeedOrderLog {
  id: number
  orderId: number
  operatorId: number
  operatorName: string
  action: string
  actionDesc: string
  createTime: string
}

export interface SeedMeetupProposal {
  id: number
  orderId: number
  proposerId: number
  proposerName: string
  proposerRole: number
  latitude: number
  longitude: number
  poiName: string
  detailAddress: string
  radiusMeters: number
  proposedTime: string
  remark: string
  proposalStatus: number
  rejectReason?: string
  respondTime?: string
  createTime: string
}

export interface SeedMilestone {
  id: number
  orderId: number
  stageIndex: number
  title: string
  amount: number
  milestoneStatus: number
  proofNote?: string
  submittedTime?: string
  releasedTime?: string
  platformFee?: number
  sellerNetAmount?: number
  createTime: string
}

export interface SeedOrder {
  id: number
  orderNumber: string
  serviceId: number
  serviceTitle: string
  serviceDescription: string
  serviceImage: string
  serviceTagName: string
  unitPrice: number
  quantity: number
  totalAmount: number
  tradeType: number
  campusId: number
  tradeLocationId?: number
  tradeLocationName?: string
  slotId?: number
  scheduledStart?: string
  scheduledEnd?: string
  meetupLat?: number
  meetupLng?: number
  meetupPoiName?: string
  meetupAddress?: string
  meetupRadiusMeters?: number
  meetupConfirmed: number
  requireBleVerify: number
  orderStatus: number // 1=unpaid, 2=pending_ship, 3=pending_receipt, 4=completed, 5=cancelled, 6=failed
  paymentStatus: number // 1=unpaid, 2=paid, 3=refunded
  paymentMethod: number // 1=wallet, 2=escrow
  frozenAmount: number
  platformFee: number
  sellerSettleAmount: number
  verifyChannel?: string
  buyerId: number
  sellerId: number
  paymentTime?: string
  sellerShipTime?: string
  buyerConfirmTime?: string
  createTime: string
  remark?: string
  deliveryNote?: string
  deliveryImages: string[]
  verifyCode: string
  refundStatus: number
  refundReason?: string
  refundRequestTime?: string
  logs: SeedOrderLog[]
  meetupProposals: SeedMeetupProposal[]
  milestones: SeedMilestone[]
}

export interface SeedWalletTransaction {
  transactionId: number
  userId: number
  transactionType: number
  typeName: string
  amount: number
  balanceBefore: number
  balanceAfter: number
  frozenBefore: number
  frozenAfter: number
  relatedOrderId?: number
  relatedOrderNumber?: string
  transactionNo: string
  remark: string
  createTime: string
}

export interface SeedNotification {
  id: number
  userId: number
  title: string
  content: string
  createTime: string
  isRead: 0 | 1
  notificationType: 1 | 2 | 3 | 4 | 5
  relatedId?: number
}

export interface SeedChatMessage {
  messageId: number
  senderId: number
  receiverId: number
  content: string
  messageType: 1 | 2 | 3
  isRead: 0 | 1
  relatedOrderId?: number
  relatedServiceId?: number
  createTime: string
}

export interface SeedDispute {
  disputeId: number
  orderId: number
  disputeInitiatorId: number
  disputeType: number
  description: string
  evidenceImages: string[]
  disputeStatus: number
  adminReply?: string | null
  resolution?: string | null
  createTime: string
}

export interface SeedSysConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  isEditable: number
  createTime: string
  updateTime: string
}

export interface SeedRiskAlert {
  id: number
  alertNo: string
  ruleCode: string
  ruleName: string
  severity: number
  riskScore: number
  orderId: number
  orderNumber: string
  serviceId: number
  serviceTitle: string
  buyerId: number
  buyerName: string
  buyerStudentId: string
  buyerCreditScore: number
  sellerId: number
  sellerName: string
  sellerStudentId: string
  sellerCreditScore: number
  campusId: number
  orderAmount: number
  pairOrderCount7d: number
  reverseOrderCount7d: number
  fulfillDurationSeconds: number
  evidenceSummary: string
  handleStatus: number
  penaltyAction?: string
  creditDeducted?: number
  handlerId?: number
  handleRemark?: string
  handleTime?: string
  createTime: string
}

export function createInitialSeedData() {
  const now = new Date()
  const isoOffset = (daysOffset: number, hoursOffset = 0) => {
    const d = new Date(now.getTime() + daysOffset * 86400000 + hoursOffset * 3600000)
    return d.toISOString().replace('T', ' ').slice(0, 19)
  }

  const campuses: SeedCampus[] = [
    {
      id: 1,
      campusCode: 'MAIN',
      campusName: '主校区（清河园）',
      city: '北京市',
      centerLat: 39.9985,
      centerLng: 116.3264,
      geofencePolygon: '[[39.992,116.318],[40.005,116.318],[40.005,116.335],[39.992,116.335]]',
      safeRadiusMeters: 1200,
      eduEmailDomains: 'edu.cn,tsinghua.edu.cn,pku.edu.cn',
      status: 1
    },
    {
      id: 2,
      campusCode: 'EAST',
      campusName: '东校区（文理苑）',
      city: '北京市',
      centerLat: 39.9892,
      centerLng: 116.3512,
      geofencePolygon: '[[39.983,116.344],[39.995,116.344],[39.995,116.359],[39.983,116.359]]',
      safeRadiusMeters: 1000,
      eduEmailDomains: 'edu.cn',
      status: 1
    },
    {
      id: 3,
      campusCode: 'TECH',
      campusName: '科技城校区（工学部）',
      city: '北京市',
      centerLat: 40.0145,
      centerLng: 116.3088,
      geofencePolygon: '[[40.008,116.301],[40.021,116.301],[40.021,116.316],[40.008,116.316]]',
      safeRadiusMeters: 1500,
      eduEmailDomains: 'edu.cn',
      status: 1
    }
  ]

  const professionals: SeedProfessional[] = [
    { id: 1, professionalName: '计算机科学与技术', description: '全栈开发、算法与人工智能方向' },
    { id: 2, professionalName: '软件工程', description: '工程架构与产品研发方向' },
    { id: 3, professionalName: '视觉传达设计', description: '品牌设计、插画与商业路演PPT' },
    { id: 4, professionalName: '电子信息工程', description: '嵌入式、硬件维修与3D打印方向' },
    { id: 5, professionalName: '摄影与影视制作', description: '胶片人像、纪实跟拍与调色' },
    { id: 6, professionalName: '英语与国际传播', description: '雅思托福口语写作与学术润色' },
    { id: 7, professionalName: '音乐表演与制作', description: '木吉他指弹、编曲与声乐指导' },
    { id: 8, professionalName: '应用数学', description: '高数、线代、概率论与数学建模' },
    { id: 9, professionalName: '体育教育', description: '羽毛球、网球与体能训练指导' }
  ]

  const serviceTags: SeedServiceTag[] = [
    { tagId: 1, id: 1, tagName: '学业辅导', description: '高数、编程、算法与毕设辅导', icon: 'Reading', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 2, id: 2, tagName: '摄影约拍', description: '校园胶片写真、毕业跟拍与活动记录', icon: 'Camera', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 3, id: 3, tagName: '视觉设计', description: '海报插画、头像定制与路演PPT重塑', icon: 'Brush', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 4, id: 4, tagName: '硬件维修', description: '笔记本清灰、装机升级与3D打印代打', icon: 'Cpu', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 5, id: 5, tagName: '艺术乐器', description: '吉他弹唱、钢琴陪练与声乐入门', icon: 'Headset', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 6, id: 6, tagName: '语言考试', description: '雅思托福模考、四六级与论文润色', icon: 'ChatLineRound', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 7, id: 7, tagName: '跑腿代办', description: '大件快递闪送、校园物资代取', icon: 'Bicycle', iconUrl: '', createTime: isoOffset(-30) },
    { tagId: 8, id: 8, tagName: '运动陪练', description: '羽毛球、网球与游泳1v1陪练', icon: 'Trophy', iconUrl: '', createTime: isoOffset(-30) }
  ]

  const tradeLocations: SeedTradeLocation[] = [
    {
      id: 1,
      campusId: 1,
      locationName: '主校区图书馆正门',
      locationDescription: '图书馆东侧台阶广场，全天候监控覆盖',
      locationAddress: '清河园主校区图书馆南广场',
      latitude: 39.9991,
      longitude: 116.3268,
      validRadiusMeters: 150,
      hasCctv: 1,
      isAvailable: 1
    },
    {
      id: 2,
      campusId: 1,
      locationName: '西区大礼堂银杏大道',
      locationDescription: '约拍与线下交接热门打卡地标',
      locationAddress: '清河园西区大礼堂前银杏大道',
      latitude: 39.9978,
      longitude: 116.3235,
      validRadiusMeters: 180,
      hasCctv: 1,
      isAvailable: 1
    },
    {
      id: 3,
      campusId: 1,
      locationName: '创客空间一楼咖啡区',
      locationDescription: '配有电源与桌椅，适合代码调试与硬件清灰',
      locationAddress: '创新创业中心 1F 公共区',
      latitude: 40.0004,
      longitude: 116.3292,
      validRadiusMeters: 120,
      hasCctv: 1,
      isAvailable: 1
    },
    {
      id: 4,
      campusId: 2,
      locationName: '文理苑第二教学楼大厅',
      locationDescription: '二教一楼中庭服务台旁',
      locationAddress: '东校区第二教学楼 1F 大厅',
      latitude: 39.9896,
      longitude: 116.3515,
      validRadiusMeters: 150,
      hasCctv: 1,
      isAvailable: 1
    },
    {
      id: 5,
      campusId: 1,
      locationName: '综合体育馆北门',
      locationDescription: '羽毛球与运动陪练集合点',
      locationAddress: '主校区综合体育馆北侧入口',
      latitude: 40.0018,
      longitude: 116.3249,
      validRadiusMeters: 150,
      hasCctv: 1,
      isAvailable: 1
    }
  ]

  const users: SeedUser[] = [
    {
      id: 1,
      studentId: '20240001',
      password: 'password123',
      realName: '平台管理员',
      nickname: 'Admin',
      phone: '13800000001',
      email: 'admin@xueyifang.edu.cn',
      dormitory: '行政楼 302',
      grade: '教职工',
      professionalId: 1,
      professionalName: '计算机科学与技术',
      campusId: 1,
      campusName: '主校区（清河园）',
      avatar: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=400&q=80',
      bio: '学艺坊校园担保交易平台运营与安全治理负责人',
      role: 2,
      publishPermission: 0,
      permissionReviewStatus: 2,
      walletBalance: 10000,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'admin@xueyifang.edu.cn',
      creditScore: 800,
      sellerDeposit: 0,
      createTime: isoOffset(-90)
    },
    {
      id: 2,
      studentId: '20240002',
      password: 'password123',
      realName: '林砚舟',
      nickname: '林砚舟 · 全栈架构',
      phone: '13800000002',
      email: 'linyz@campus.edu.cn',
      dormitory: '紫荆公寓 4 号楼 512',
      grade: '大四',
      professionalId: 1,
      professionalName: '计算机科学与技术',
      campusId: 1,
      campusName: '主校区（清河园）',
      avatar: 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=400&q=80',
      bio: '蓝桥杯国一 / 字节跳动实习经历，专注 SpringBoot + Vue 全栈架构辅导与毕设代码排障。',
      role: 1,
      publishPermission: 1,
      permissionReviewStatus: 1,
      walletBalance: 2680.5,
      frozenAmount: 199,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'linyz24@campus.edu.cn',
      creditScore: 786,
      sellerDeposit: 500,
      createTime: isoOffset(-60)
    },
    {
      id: 3,
      studentId: '20240003',
      password: 'password123',
      realName: '苏念安',
      nickname: '苏念安',
      phone: '13800000003',
      email: 'sunianan@campus.edu.cn',
      dormitory: '南苑 3 号楼 201',
      grade: '大三',
      professionalId: 2,
      professionalName: '软件工程',
      campusId: 1,
      campusName: '主校区（清河园）',
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
      bio: '热爱校园摄影与效率工具，擅长前端开发与课程笔记整理。',
      role: 1,
      publishPermission: 1,
      permissionReviewStatus: 1,
      walletBalance: 1520,
      frozenAmount: 129,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'suna24@campus.edu.cn',
      creditScore: 752,
      sellerDeposit: 200,
      createTime: isoOffset(-50)
    },
    {
      id: 4,
      studentId: '20240004',
      password: 'password123',
      realName: '沈知微',
      nickname: '知微胶片馆',
      phone: '13800000004',
      email: 'shenzw@campus.edu.cn',
      dormitory: '西苑 7 号楼 308',
      grade: '研一',
      professionalId: 5,
      professionalName: '摄影与影视制作',
      campusId: 1,
      campusName: '主校区（清河园）',
      avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80',
      bio: '富士 X-T5 + 哈苏中画幅玩家，校摄影协会前会长，累计完成 80+ 场校园写真与毕业跟拍。',
      role: 1,
      publishPermission: 1,
      permissionReviewStatus: 1,
      walletBalance: 3420,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'shenzw@campus.edu.cn',
      creditScore: 794,
      sellerDeposit: 500,
      createTime: isoOffset(-75)
    },
    {
      id: 5,
      studentId: '20240005',
      password: 'password123',
      realName: '顾明远',
      nickname: '硬核拆机顾工',
      phone: '13800000005',
      email: 'gumy@campus.edu.cn',
      dormitory: '科技城 2 号楼 405',
      grade: '大四',
      professionalId: 4,
      professionalName: '电子信息工程',
      campusId: 3,
      campusName: '科技城校区（工学部）',
      avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80',
      bio: '机器人队硬件组长，精修游戏本清灰换硅脂/相变片，自有拓竹 P1S 多色 3D 打印工作站。',
      role: 1,
      publishPermission: 1,
      permissionReviewStatus: 1,
      walletBalance: 1960,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'gumy@campus.edu.cn',
      creditScore: 778,
      sellerDeposit: 300,
      createTime: isoOffset(-65)
    },
    {
      id: 6,
      studentId: '20240006',
      password: 'password123',
      realName: '许清禾',
      nickname: '清禾视觉设计',
      phone: '13800000006',
      email: 'xuqh@campus.edu.cn',
      dormitory: '文理苑 5 号楼 602',
      grade: '大三',
      professionalId: 3,
      professionalName: '视觉传达设计',
      campusId: 2,
      campusName: '东校区（文理苑）',
      avatar: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80',
      bio: '互联网+国金团队主视觉设计师，擅长 Procreate 日系水彩、社团海报与高规格答辩路演 PPT。',
      role: 1,
      publishPermission: 1,
      permissionReviewStatus: 1,
      walletBalance: 2240,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'xuqh@campus.edu.cn',
      creditScore: 771,
      sellerDeposit: 300,
      createTime: isoOffset(-55)
    },
    {
      id: 7,
      studentId: '20240007',
      password: 'password123',
      realName: '周序白',
      nickname: '周序白 · 数学建模',
      phone: '13800000007',
      email: 'zhouxb@campus.edu.cn',
      dormitory: '紫荆公寓 1 号楼 210',
      grade: '研二',
      professionalId: 8,
      professionalName: '应用数学',
      campusId: 1,
      campusName: '主校区（清河园）',
      avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80',
      bio: '全国大学生数学竞赛一等奖，连续三年担任高数/线代助教，考前押题串讲口碑拉满。',
      role: 1,
      publishPermission: 1,
      permissionReviewStatus: 1,
      walletBalance: 1850,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'zhouxb@campus.edu.cn',
      creditScore: 789,
      sellerDeposit: 400,
      createTime: isoOffset(-70)
    },
    {
      id: 8,
      studentId: '20240008',
      password: 'password123',
      realName: '江晚宁',
      nickname: '晚宁 IELTS 8.0',
      phone: '13800000008',
      email: 'jiangwn@campus.edu.cn',
      dormitory: '文理苑 2 号楼 415',
      grade: '大四',
      professionalId: 6,
      professionalName: '英语与国际传播',
      campusId: 2,
      campusName: '东校区（文理苑）',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80',
      bio: '雅思首考 8.0（口语 7.5 / 写作 7.5），已获 LSE 与 UCL Offer，专注口语全真模考与文书精修。',
      role: 1,
      publishPermission: 0,
      permissionReviewStatus: 0,
      applyReason: '具备雅思 8.0 官方成绩单与两年校内口语陪练经验，申请开通创作者发布权限。',
      walletBalance: 980,
      frozenAmount: 0,
      accountStatus: 1,
      eduVerified: 1,
      verifiedEduEmail: 'jiangwn@campus.edu.cn',
      creditScore: 760,
      sellerDeposit: 200,
      createTime: isoOffset(-40)
    }
  ]

  const services: SeedService[] = [
    {
      id: 1,
      serviceTitle: 'SpringBoot + Vue 全栈架构 1v1 代码辅导与排障',
      serviceDescription: '面向课程设计、毕业设计与大创项目，提供从数据库 ER 设计、SpringBoot 后端接口调试、Vue3 + TypeScript 前端工程化到 Docker 部署的全流程 1v1 指导。\n\n包含内容：\n1. 90 分钟深度代码走查与架构梳理；\n2. 现场定位并解决最多 3 个阻塞性 Bug；\n3. 附赠答辩高频提问清单与性能优化建议。',
      sellerId: 2,
      campusId: 1,
      tagId: 1,
      tradeType: 2,
      tradeLocationId: 3,
      price: 129,
      maxPurchases: 30,
      currentPurchaseCount: 19,
      isPhysical: 0,
      requireSlot: 1,
      cancelFreeHours: 6,
      requireBleVerify: 0,
      status: 1,
      reviewStatus: 1,
      viewCount: 1420,
      collectionCount: 96,
      salesCount: 19,
      averageRating: 4.9,
      reviewCount: 14,
      images: [
        'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '预约时段内 90 分钟 1v1 交付',
      requirements: '请提前准备 Git 仓库或本地可运行环境，并简述卡点问题。',
      createTime: isoOffset(-24),
      updateTime: isoOffset(-2)
    },
    {
      id: 2,
      serviceTitle: '富士 X-T5 西区礼堂与银杏大道胶片写真约拍',
      serviceDescription: '使用富士 X-T5 + XF 35mm f/1.4 定焦镜头，保留经典 NC / CC 胶片模拟色彩。适合校园日常纪念、毕业季写真、情侣合影与社团形象照。\n\n交付标准：\n- 拍摄时长 90 分钟，不限服装造型；\n- 底片全送（120+ 张），精修 15 张（72 小时内云盘直发）；\n- 含光线机位引导与动作指导，零镜头经验也能出片。',
      sellerId: 4,
      campusId: 1,
      tagId: 2,
      tradeType: 1,
      tradeLocationId: 2,
      price: 168,
      maxPurchases: 25,
      currentPurchaseCount: 21,
      isPhysical: 0,
      requireSlot: 1,
      cancelFreeHours: 12,
      requireBleVerify: 1,
      status: 1,
      reviewStatus: 1,
      viewCount: 1860,
      collectionCount: 142,
      salesCount: 21,
      averageRating: 5.0,
      reviewCount: 18,
      images: [
        'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '拍摄后 72 小时内交付全套精修图',
      requirements: '建议挑选晴天下午 15:30-17:30 柔和逆光时段，雨天支持免费改期。',
      createTime: isoOffset(-22),
      updateTime: isoOffset(-1)
    },
    {
      id: 3,
      serviceTitle: '游戏本深度清灰与霍尼韦尔 7950 相变片更换',
      serviceDescription: '拯救者、ROG、暗影精灵等主流游戏本/轻薄本专业散热维护。全程佩戴防静电手环，使用原装霍尼韦尔 PTM7950 相变导热片与高导热显存凝胶。\n\n服务包含：\n- 拆机清灰、风扇叶片精洗、出风口疏通；\n- CPU/GPU 核心涂覆 7950 相变片；\n- 免费烤机温度对比测试（通常直降 10-18°C）。',
      sellerId: 5,
      campusId: 3,
      tagId: 4,
      tradeType: 1,
      tradeLocationId: 3,
      price: 79,
      maxPurchases: 40,
      currentPurchaseCount: 28,
      isPhysical: 1,
      requireSlot: 1,
      cancelFreeHours: 4,
      requireBleVerify: 1,
      status: 1,
      reviewStatus: 1,
      viewCount: 1185,
      collectionCount: 84,
      salesCount: 28,
      averageRating: 4.9,
      reviewCount: 22,
      images: [
        'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1588702547919-26089e690ecc?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '当面拆装约 45 分钟立等可取',
      requirements: '下单备注笔记本具体型号，如需加装固态硬盘/内存条可免费代装。',
      createTime: isoOffset(-20),
      updateTime: isoOffset(-3)
    },
    {
      id: 4,
      serviceTitle: '挑战杯 / 互联网+ 国奖级路演 PPT 视觉重塑',
      serviceDescription: '由国金主视觉设计师操刀，针对商业计划书、答辩路演与学术汇报进行逻辑提炼与版式重塑。\n\n服务亮点：\n- 统一高级配色与网格系统，拒绝模板感；\n- 定制数据可视化信息图与商业模式架构图；\n- 附赠 Keynote/PPTX 可编辑源文件与 2 轮免费细节精修。',
      sellerId: 6,
      campusId: 2,
      tagId: 3,
      tradeType: 2,
      tradeLocationId: 4,
      price: 199,
      maxPurchases: 15,
      currentPurchaseCount: 11,
      isPhysical: 0,
      requireSlot: 0,
      cancelFreeHours: 12,
      requireBleVerify: 0,
      status: 1,
      reviewStatus: 1,
      viewCount: 1310,
      collectionCount: 109,
      salesCount: 11,
      averageRating: 4.9,
      reviewCount: 9,
      images: [
        'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1558655146-9f40138edfeb?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '20 页以内 48 小时交付初稿',
      requirements: '请提供 Word 文稿或初版 PPT，以及目标赛事名称与答辩时长。',
      createTime: isoOffset(-18),
      updateTime: isoOffset(-1)
    },
    {
      id: 5,
      serviceTitle: '高数与线性代数历年期末真题 3 小时押题串讲',
      serviceDescription: '近 6 年本校高数上/下及线性代数真题高频考点全拆解，针对中值定理、多元微积分、特征值与二次型等拉分模块提供“秒杀模板”。\n\n含纸质讲义 + 错题精讲，支持图书馆或线上屏幕共享 1v1 辅导。',
      sellerId: 7,
      campusId: 1,
      tagId: 1,
      tradeType: 1,
      tradeLocationId: 1,
      price: 118,
      maxPurchases: 30,
      currentPurchaseCount: 24,
      isPhysical: 0,
      requireSlot: 1,
      cancelFreeHours: 6,
      requireBleVerify: 0,
      status: 1,
      reviewStatus: 1,
      viewCount: 1540,
      collectionCount: 126,
      salesCount: 24,
      averageRating: 5.0,
      reviewCount: 19,
      images: [
        'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '单次 3 小时面授或腾讯会议精讲',
      requirements: '提前发一下平时薄弱章节或模考试卷即可定制节奏。',
      createTime: isoOffset(-16),
      updateTime: isoOffset(-2)
    },
    {
      id: 6,
      serviceTitle: 'Procreate 日系水彩双人头像与社团海报定制',
      serviceDescription: '纯手绘日系清透水彩风格，适用于情侣/闺蜜头像、音乐节社团海报与文创明信片插画。出草图后确认构图再上色，支持高清 300dpi 印刷导出。',
      sellerId: 6,
      campusId: 2,
      tagId: 3,
      tradeType: 2,
      tradeLocationId: 4,
      price: 68,
      maxPurchases: 25,
      currentPurchaseCount: 15,
      isPhysical: 0,
      requireSlot: 0,
      cancelFreeHours: 6,
      requireBleVerify: 0,
      status: 1,
      reviewStatus: 1,
      viewCount: 920,
      collectionCount: 73,
      salesCount: 15,
      averageRating: 4.8,
      reviewCount: 11,
      images: [
        'https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1561070791-2526d30994b5?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '下单后 36 小时内交付高清成图',
      requirements: '请上传参考照片与期望色调/文案内容。',
      createTime: isoOffset(-15),
      updateTime: isoOffset(-4)
    },
    {
      id: 7,
      serviceTitle: '雅思 8.0 学姐口语全真模考与大作文逐句精批',
      serviceDescription: '严格按照雅思口语 Part1-3 评分标准进行 20 分钟全真模拟 + 25 分钟逐项复盘（流利度、词汇地道度、语法多样性与发音纠正），另附 1 篇 Task 2 大作文逐句批改与 7.5+ 范文改写。',
      sellerId: 8,
      campusId: 2,
      tagId: 6,
      tradeType: 2,
      tradeLocationId: 4,
      price: 98,
      maxPurchases: 20,
      currentPurchaseCount: 12,
      isPhysical: 0,
      requireSlot: 1,
      cancelFreeHours: 6,
      requireBleVerify: 0,
      status: 1,
      reviewStatus: 1,
      viewCount: 845,
      collectionCount: 61,
      salesCount: 12,
      averageRating: 4.9,
      reviewCount: 8,
      images: [
        'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '45 分钟线上模考复盘 + 24 小时作文精批',
      requirements: '请备注考试日期与目标分数，提前发送 1 篇近期作文。',
      createTime: isoOffset(-12),
      updateTime: isoOffset(-2)
    },
    {
      id: 8,
      serviceTitle: '拓竹 P1S 高精度 FDM 3D 打印代打与模型修补',
      serviceDescription: '工科课设外壳、机器人结构件、建筑模型与手办摆件快速代打。提供哑光 PLA / PETG / ABS 耗材，层高精细至 0.12mm，免费帮忙检查 STL 破面与壁厚。',
      sellerId: 5,
      campusId: 3,
      tagId: 4,
      tradeType: 1,
      tradeLocationId: 3,
      price: 39,
      maxPurchases: 50,
      currentPurchaseCount: 34,
      isPhysical: 1,
      requireSlot: 0,
      cancelFreeHours: 4,
      requireBleVerify: 1,
      status: 1,
      reviewStatus: 1,
      viewCount: 790,
      collectionCount: 52,
      salesCount: 34,
      averageRating: 4.9,
      reviewCount: 16,
      images: [
        'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '常规零件 24 小时内打印完毕校园自提',
      requirements: '请通过私信发送 STL / STEP 模型文件以确认尺寸和颜色。',
      createTime: isoOffset(-10),
      updateTime: isoOffset(-1)
    },
    {
      id: 9,
      serviceTitle: '民谣木吉他 1v1 零基础弹唱与指弹入门课',
      serviceDescription: '拒绝枯燥乐理，3 节课带你完整弹唱《晴天》《成都》等热门曲目。提供备用面单吉他体验，手把手纠正按弦手型、扫弦律动与节奏稳度。',
      sellerId: 3,
      campusId: 1,
      tagId: 5,
      tradeType: 1,
      tradeLocationId: 1,
      price: 85,
      maxPurchases: 20,
      currentPurchaseCount: 9,
      isPhysical: 0,
      requireSlot: 1,
      cancelFreeHours: 6,
      requireBleVerify: 1,
      status: 1,
      reviewStatus: 1,
      viewCount: 680,
      collectionCount: 49,
      salesCount: 9,
      averageRating: 4.9,
      reviewCount: 7,
      images: [
        'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '单节 60 分钟面对面指导',
      requirements: '零基础可免费借用备用吉他，有琴同学建议自带。',
      createTime: isoOffset(-9),
      updateTime: isoOffset(-2)
    },
    {
      id: 10,
      serviceTitle: '校队主力 1v1 羽毛球发力纠正与多球陪练',
      serviceDescription: '针对高远球发力不足、步法混乱与反手过渡难题，进行动作慢放拆解 + 200 颗多球定点喂球训练。含训练用球，场地费 AA。',
      sellerId: 2,
      campusId: 1,
      tagId: 8,
      tradeType: 1,
      tradeLocationId: 5,
      price: 88,
      maxPurchases: 20,
      currentPurchaseCount: 13,
      isPhysical: 0,
      requireSlot: 1,
      cancelFreeHours: 6,
      requireBleVerify: 1,
      status: 1,
      reviewStatus: 1,
      viewCount: 740,
      collectionCount: 57,
      salesCount: 13,
      averageRating: 5.0,
      reviewCount: 10,
      images: [
        'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=1000&q=80',
        'https://images.unsplash.com/photo-1554068865-24cecd4e34b8?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '单次 90 分钟综合体育馆球场陪练',
      requirements: '请穿浅色无痕运动鞋并自备球拍。',
      createTime: isoOffset(-8),
      updateTime: isoOffset(-1)
    },
    {
      id: 11,
      serviceTitle: '南苑 / 松园大件快递与行李箱电动车闪送',
      serviceDescription: '宿舍搬迁、网购大件行李与多件包裹电动车直达楼下，雨天备有防雨帆布。校内 20 分钟极速响应，全程当面核销验收。',
      sellerId: 5,
      campusId: 1,
      tagId: 7,
      tradeType: 1,
      tradeLocationId: 1,
      price: 18,
      maxPurchases: 100,
      currentPurchaseCount: 46,
      isPhysical: 1,
      requireSlot: 0,
      cancelFreeHours: 1,
      requireBleVerify: 1,
      status: 3, // pending review item for admin moderation queue
      reviewStatus: 2,
      viewCount: 310,
      collectionCount: 22,
      salesCount: 46,
      averageRating: 4.8,
      reviewCount: 12,
      images: [
        'https://images.unsplash.com/photo-1526367790999-0150786686a2?auto=format&fit=crop&w=1000&q=80'
      ],
      deliveryTime: '接单后 25 分钟内送达宿舍楼下',
      requirements: '请备注菜鸟驿站取件码与宿舍楼栋号。',
      createTime: isoOffset(-1),
      updateTime: isoOffset(-1)
    }
  ]

  const serviceSlots: SeedServiceSlot[] = [
    { id: 101, serviceId: 1, slotStart: isoOffset(1, 2), slotEnd: isoOffset(1, 4), maxCapacity: 2, bookedCount: 1, remainingCapacity: 1, status: 1 },
    { id: 102, serviceId: 1, slotStart: isoOffset(2, 5), slotEnd: isoOffset(2, 7), maxCapacity: 2, bookedCount: 0, remainingCapacity: 2, status: 1 },
    { id: 103, serviceId: 2, slotStart: isoOffset(1, 6), slotEnd: isoOffset(1, 8), maxCapacity: 1, bookedCount: 0, remainingCapacity: 1, status: 1 },
    { id: 104, serviceId: 2, slotStart: isoOffset(3, 6), slotEnd: isoOffset(3, 8), maxCapacity: 1, bookedCount: 0, remainingCapacity: 1, status: 1 },
    { id: 105, serviceId: 3, slotStart: isoOffset(1, 3), slotEnd: isoOffset(1, 4), maxCapacity: 3, bookedCount: 1, remainingCapacity: 2, status: 1 },
    { id: 106, serviceId: 5, slotStart: isoOffset(2, 2), slotEnd: isoOffset(2, 5), maxCapacity: 2, bookedCount: 0, remainingCapacity: 2, status: 1 }
  ]

  const reviews: SeedReview[] = [
    {
      id: 1,
      serviceId: 1,
      orderId: 1001,
      reviewerId: 3,
      reviewerName: '苏念安',
      reviewerAvatar: users[2].avatar,
      rating: 5,
      content: '林学长排查 SpringBoot 事务失效和 Redis 缓存击穿太稳了，90 分钟直接把我卡了一周的毕设跑通了，还教了答辩怎么讲架构亮点！',
      anonymous: false,
      createTime: isoOffset(-5)
    },
    {
      id: 2,
      serviceId: 2,
      orderId: 1002,
      reviewerId: 3,
      reviewerName: '苏念安',
      reviewerAvatar: users[2].avatar,
      rating: 5,
      content: '西区银杏大道的逆光胶片质感绝了！知微学姐很会引导情绪和动作，第二天就收到精修图，朋友圈点赞破百。',
      anonymous: false,
      createTime: isoOffset(-3)
    },
    {
      id: 3,
      serviceId: 3,
      orderId: 1003,
      reviewerId: 7,
      reviewerName: '周序白',
      reviewerAvatar: users[6].avatar,
      rating: 5,
      content: '拆机特别细致，换完霍尼韦尔 7950 之后跑深度学习模型 CPU 温度直接降了 15 度，风扇再也不起飞了。',
      anonymous: false,
      createTime: isoOffset(-4)
    },
    {
      id: 4,
      serviceId: 4,
      orderId: 1004,
      reviewerId: 2,
      reviewerName: '林砚舟',
      reviewerAvatar: users[1].avatar,
      rating: 5,
      content: '帮我们团队重塑了大创路演 18 页 PPT，信息图清晰高级，评委老师专门夸了展示效果，强烈推荐！',
      anonymous: false,
      createTime: isoOffset(-6)
    }
  ]

  const demands: SeedDemand[] = [
    {
      id: 1,
      demandNo: 'DM202611280001',
      publisherId: 3,
      campusId: 1,
      tagId: 4,
      title: '急求 STM32 + OLED 传感器硬件联调与串口通信排障',
      description: '嵌入式课设下周二验收，目前 I2C 驱动温湿度传感器数据偶发丢包，希望有经验的工科同学在创客空间现场一起用逻辑分析仪排查并跑通演示。',
      budgetMin: 120,
      budgetMax: 220,
      tradeType: 1,
      urgencyLevel: 3,
      deadlineTime: isoOffset(2),
      meetupLat: 40.0004,
      meetupLng: 116.3292,
      meetupPoiName: '创客空间一楼咖啡区',
      meetupAddress: '创新创业中心 1F 公共区',
      meetupRadiusMeters: 150,
      demandStatus: 0,
      createTime: isoOffset(-1, -4),
      bids: [
        {
          id: 11,
          demandId: 1,
          bidderId: 5,
          bidPrice: 160,
          estimatedHours: 2,
          bidPlan: '自带 Saleae 逻辑分析仪和示波器探头，先测 I2C 上拉电阻波形，再检查 DMA 中断优先级，保证 2 小时内稳定出数。',
          bidStatus: 0,
          createTime: isoOffset(-1, -2)
        },
        {
          id: 12,
          demandId: 1,
          bidderId: 2,
          bidPrice: 180,
          estimatedHours: 2.5,
          bidPlan: '可协助梳理状态机与上位机串口协议帧校验，附赠可视化调试脚本。',
          bidStatus: 0,
          createTime: isoOffset(0, -5)
        }
      ]
    },
    {
      id: 2,
      demandNo: 'DM202611280002',
      publisherId: 7,
      campusId: 1,
      tagId: 2,
      title: '周五傍晚校园十佳歌手复赛现场纪实跟拍（2小时）',
      description: '需要双机位或大光圈变焦镜头捕捉舞台特写与观众氛围，活动在西区礼堂，要求次日上午交付 30 张精选调色图用于公众号推文。',
      budgetMin: 200,
      budgetMax: 320,
      tradeType: 1,
      urgencyLevel: 2,
      deadlineTime: isoOffset(3),
      meetupLat: 39.9978,
      meetupLng: 116.3235,
      meetupPoiName: '西区大礼堂银杏大道',
      meetupAddress: '清河园西区大礼堂正门',
      meetupRadiusMeters: 150,
      demandStatus: 0,
      createTime: isoOffset(-2),
      bids: [
        {
          id: 13,
          demandId: 2,
          bidderId: 4,
          bidPrice: 260,
          estimatedHours: 3,
          bidPlan: '带 70-200mm f/2.8 舞台长焦 + 35mm 定焦，高感降噪与舞台肤色还原经验丰富，次日 10 点前交付 40 张精修。',
          bidStatus: 0,
          createTime: isoOffset(-1, -6)
        }
      ]
    },
    {
      id: 3,
      demandNo: 'DM202611280003',
      publisherId: 3,
      campusId: 2,
      tagId: 3,
      title: '学院辩论赛决赛主视觉海报 + 微信推送封面套图设计',
      description: '主题为“算法时代的人文边界”，期望极简克制的新中式/瑞士排版风格，需提供竖版易拉宝、朋友圈长图与公众号首图三种尺寸。',
      budgetMin: 150,
      budgetMax: 260,
      tradeType: 2,
      urgencyLevel: 2,
      deadlineTime: isoOffset(4),
      meetupPoiName: '线上交付',
      demandStatus: 0,
      createTime: isoOffset(-2, -3),
      bids: [
        {
          id: 14,
          demandId: 3,
          bidderId: 6,
          bidPrice: 190,
          estimatedHours: 4,
          bidPlan: '先提供两套排版方向草图，选定后 24 小时完成三端尺寸延展与印刷出血位导出。',
          bidStatus: 0,
          createTime: isoOffset(-1, -1)
        }
      ]
    },
    {
      id: 4,
      demandNo: 'DM202611280004',
      publisherId: 5,
      campusId: 1,
      tagId: 6,
      title: 'IEEE 会议英文论文 6 页语言地道化润色与 LaTeX 排版检查',
      description: '计算机视觉方向投稿论文，已写完初稿但摘要与实验分析部分表达偏中式，寻找英语专业或有顶会发表经验的同学逐句润色。',
      budgetMin: 180,
      budgetMax: 300,
      tradeType: 2,
      urgencyLevel: 1,
      deadlineTime: isoOffset(5),
      meetupPoiName: '线上交付',
      demandStatus: 0,
      createTime: isoOffset(-3),
      bids: []
    }
  ]

  const groupSessions: SeedGroupSession[] = [
    {
      id: 1,
      sessionNo: 'GS202611280001',
      creatorId: 7,
      campusId: 1,
      tagId: 1,
      title: '线性代数期末核心题型 4 小时高分冲刺小班',
      description: '聚焦矩阵秩、向量组线性相关性、正交化与二次型标准化四大必考板块，配套近 5 年真题精编讲义，满 4 人自动触发阶梯返现，每人立省 30 元！',
      basePrice: 89,
      tierTargetCount: 4,
      tierDiscountPrice: 59,
      currentUnitPrice: 89,
      minParticipants: 2,
      maxParticipants: 8,
      tierRebateApplied: 0,
      tradeType: 1,
      sessionStart: isoOffset(3, 2),
      sessionEnd: isoOffset(3, 6),
      meetupLat: 39.9991,
      meetupLng: 116.3268,
      meetupPoiName: '主校区图书馆正门',
      meetupAddress: '主校区图书馆三层研讨室 306',
      meetupRadiusMeters: 150,
      sessionStatus: 0,
      createTime: isoOffset(-4),
      participants: [
        { id: 1, buyerId: 3, orderId: 1005, lockedPrice: 89, effectivePrice: 89, rebateAmount: 0, createTime: isoOffset(-2) },
        { id: 2, buyerId: 6, orderId: 1006, lockedPrice: 89, effectivePrice: 89, rebateAmount: 0, createTime: isoOffset(-1) },
        { id: 3, buyerId: 8, orderId: 1007, lockedPrice: 89, effectivePrice: 89, rebateAmount: 0, createTime: isoOffset(0, -6) }
      ]
    },
    {
      id: 2,
      sessionNo: 'GS202611280002',
      creatorId: 2,
      campusId: 1,
      tagId: 1,
      title: '蓝桥杯 / LeetCode 高频动态规划与图论实战集训',
      description: '结合真实竞赛真题手把手推导状态转移方程，现场 AC 12 道核心例题并提供 Python/Java/C++ 三语言模板。满 5 人全员自动退差价至 ¥69！',
      basePrice: 99,
      tierTargetCount: 5,
      tierDiscountPrice: 69,
      currentUnitPrice: 99,
      minParticipants: 3,
      maxParticipants: 10,
      tierRebateApplied: 0,
      tradeType: 2,
      sessionStart: isoOffset(5, 4),
      sessionEnd: isoOffset(5, 7),
      meetupPoiName: '创客空间一楼咖啡区 / 线上同步',
      meetupAddress: '主校区创客空间 1F 路演厅',
      meetupRadiusMeters: 150,
      sessionStatus: 0,
      createTime: isoOffset(-3),
      participants: [
        { id: 4, buyerId: 3, orderId: 1008, lockedPrice: 99, effectivePrice: 99, rebateAmount: 0, createTime: isoOffset(-2) },
        { id: 5, buyerId: 5, orderId: 1009, lockedPrice: 99, effectivePrice: 99, rebateAmount: 0, createTime: isoOffset(-1) }
      ]
    },
    {
      id: 3,
      sessionNo: 'GS202611280003',
      creatorId: 4,
      campusId: 1,
      tagId: 2,
      title: '周末校园黄金时刻人像用光与 Lightroom 电影感调色工作坊',
      description: '西区银杏大道实地外拍 1.5 小时 + 咖啡厅现场 Lightroom 曲线与 HSL 调色实战 1.5 小时，满 3 人即享团课优惠价 ¥78。',
      basePrice: 108,
      tierTargetCount: 3,
      tierDiscountPrice: 78,
      currentUnitPrice: 78,
      minParticipants: 2,
      maxParticipants: 6,
      tierRebateApplied: 1,
      tradeType: 1,
      sessionStart: isoOffset(6, 5),
      sessionEnd: isoOffset(6, 8),
      meetupLat: 39.9978,
      meetupLng: 116.3235,
      meetupPoiName: '西区大礼堂银杏大道',
      meetupAddress: '清河园西区大礼堂前银杏大道',
      meetupRadiusMeters: 180,
      sessionStatus: 1,
      createTime: isoOffset(-5),
      participants: [
        { id: 6, buyerId: 3, orderId: 1010, lockedPrice: 108, effectivePrice: 78, rebateAmount: 30, createTime: isoOffset(-4) },
        { id: 7, buyerId: 6, orderId: 1011, lockedPrice: 108, effectivePrice: 78, rebateAmount: 30, createTime: isoOffset(-3) },
        { id: 8, buyerId: 8, orderId: 1012, lockedPrice: 108, effectivePrice: 78, rebateAmount: 30, createTime: isoOffset(-2) }
      ]
    }
  ]

  const orders: SeedOrder[] = [
    {
      id: 1001,
      orderNumber: 'XYF202611220001',
      serviceId: 1,
      serviceTitle: services[0].serviceTitle,
      serviceDescription: services[0].serviceDescription,
      serviceImage: services[0].images[0],
      serviceTagName: '学业辅导',
      unitPrice: 129,
      quantity: 1,
      totalAmount: 129,
      tradeType: 2,
      campusId: 1,
      tradeLocationId: 3,
      tradeLocationName: '创客空间一楼咖啡区',
      meetupConfirmed: 1,
      requireBleVerify: 0,
      orderStatus: 4, // completed
      paymentStatus: 2,
      paymentMethod: 1,
      frozenAmount: 0,
      platformFee: 6.45,
      sellerSettleAmount: 122.55,
      verifyChannel: 'ONLINE_CONFIRM',
      buyerId: 3,
      sellerId: 2,
      paymentTime: isoOffset(-7, 1),
      sellerShipTime: isoOffset(-6, 2),
      buyerConfirmTime: isoOffset(-5, 3),
      createTime: isoOffset(-7),
      remark: '重点排查 SpringBoot 声明式事务与 Redis 分布式锁问题',
      deliveryNote: '已完成 90 分钟代码走查，已提交修复 PR 与答辩梳理文档。',
      deliveryImages: [services[0].images[1]],
      verifyCode: '842915',
      refundStatus: 0,
      logs: [
        { id: 1, orderId: 1001, operatorId: 3, operatorName: '苏念安', action: 'CREATE', actionDesc: '创建担保订单', createTime: isoOffset(-7) },
        { id: 2, orderId: 1001, operatorId: 3, operatorName: '苏念安', action: 'PAY', actionDesc: '余额担保支付 ¥129.00，资金已冻结托管', createTime: isoOffset(-7, 1) },
        { id: 3, orderId: 1001, operatorId: 2, operatorName: '林砚舟', action: 'SHIP', actionDesc: '卖家已提交服务交付凭证', createTime: isoOffset(-6, 2) },
        { id: 4, orderId: 1001, operatorId: 3, operatorName: '苏念安', action: 'CONFIRM', actionDesc: '买家确认验收，担保资金结算至卖家钱包', createTime: isoOffset(-5, 3) }
      ],
      meetupProposals: [],
      milestones: []
    },
    {
      id: 1002,
      orderNumber: 'XYF202611260002',
      serviceId: 2,
      serviceTitle: services[1].serviceTitle,
      serviceDescription: services[1].serviceDescription,
      serviceImage: services[1].images[0],
      serviceTagName: '摄影约拍',
      unitPrice: 168,
      quantity: 1,
      totalAmount: 168,
      tradeType: 1,
      campusId: 1,
      tradeLocationId: 2,
      tradeLocationName: '西区大礼堂银杏大道',
      meetupLat: 39.9978,
      meetupLng: 116.3235,
      meetupPoiName: '西区大礼堂银杏大道',
      meetupAddress: '清河园西区大礼堂前银杏大道',
      meetupRadiusMeters: 180,
      meetupConfirmed: 1,
      requireBleVerify: 1,
      orderStatus: 3, // pending_receipt
      paymentStatus: 2,
      paymentMethod: 1,
      frozenAmount: 168,
      platformFee: 8.4,
      sellerSettleAmount: 159.6,
      verifyChannel: 'OFFLINE_CODE',
      buyerId: 3,
      sellerId: 4,
      paymentTime: isoOffset(-2, 1),
      sellerShipTime: isoOffset(-1, 2),
      createTime: isoOffset(-2),
      remark: '希望偏日系清透色调，带一件米色风衣拍摄',
      deliveryNote: '外拍已完成，15 张精修与全部底片已上传云盘链接，请验收确认。',
      deliveryImages: [services[1].images[1]],
      verifyCode: '619304',
      refundStatus: 0,
      logs: [
        { id: 5, orderId: 1002, operatorId: 3, operatorName: '苏念安', action: 'CREATE', actionDesc: '创建线下约拍担保单', createTime: isoOffset(-2) },
        { id: 6, orderId: 1002, operatorId: 3, operatorName: '苏念安', action: 'PAY', actionDesc: '担保托管支付 ¥168.00', createTime: isoOffset(-2, 1) },
        { id: 7, orderId: 1002, operatorId: 4, operatorName: '沈知微', action: 'SHIP', actionDesc: '已完成拍摄并上传精修成片', createTime: isoOffset(-1, 2) }
      ],
      meetupProposals: [
        {
          id: 21,
          orderId: 1002,
          proposerId: 3,
          proposerName: '苏念安',
          proposerRole: 1,
          latitude: 39.9978,
          longitude: 116.3235,
          poiName: '西区大礼堂银杏大道',
          detailAddress: '礼堂正门西侧第二排银杏树下',
          radiusMeters: 150,
          proposedTime: isoOffset(-1, 0),
          remark: '下午四点阳光正好',
          proposalStatus: 1,
          respondTime: isoOffset(-2, 4),
          createTime: isoOffset(-2, 2)
        }
      ],
      milestones: []
    },
    {
      id: 1003,
      orderNumber: 'XYF202611270003',
      serviceId: 4,
      serviceTitle: services[3].serviceTitle,
      serviceDescription: services[3].serviceDescription,
      serviceImage: services[3].images[0],
      serviceTagName: '视觉设计',
      unitPrice: 199,
      quantity: 1,
      totalAmount: 199,
      tradeType: 2,
      campusId: 2,
      tradeLocationId: 4,
      tradeLocationName: '文理苑第二教学楼大厅',
      meetupConfirmed: 0,
      requireBleVerify: 0,
      orderStatus: 2, // pending_ship
      paymentStatus: 2,
      paymentMethod: 1,
      frozenAmount: 199,
      platformFee: 9.95,
      sellerSettleAmount: 189.05,
      buyerId: 2,
      sellerId: 6,
      paymentTime: isoOffset(-1, 1),
      createTime: isoOffset(-1),
      remark: '互联网+省赛路演 16 页，需要科技极简风',
      deliveryImages: [],
      verifyCode: '305742',
      refundStatus: 0,
      logs: [
        { id: 8, orderId: 1003, operatorId: 2, operatorName: '林砚舟', action: 'CREATE', actionDesc: '创建线上担保订单', createTime: isoOffset(-1) },
        { id: 9, orderId: 1003, operatorId: 2, operatorName: '林砚舟', action: 'PAY', actionDesc: '担保托管支付 ¥199.00', createTime: isoOffset(-1, 1) }
      ],
      meetupProposals: [],
      milestones: [
        { id: 31, orderId: 1003, stageIndex: 1, title: '逻辑框架与视觉风格提案', amount: 79, milestoneStatus: 1, proofNote: '已完成 4 页核心版式风格稿', submittedTime: isoOffset(0, -3), createTime: isoOffset(-1) },
        { id: 32, orderId: 1003, stageIndex: 2, title: '全套 16 页终稿与源文件交付', amount: 120, milestoneStatus: 0, createTime: isoOffset(-1) }
      ]
    }
  ]

  const walletTransactions: SeedWalletTransaction[] = [
    {
      transactionId: 1,
      userId: 3,
      transactionType: 1,
      typeName: '充值',
      amount: 1000,
      balanceBefore: 817,
      balanceAfter: 1817,
      frozenBefore: 0,
      frozenAfter: 0,
      transactionNo: 'WT202611200001',
      remark: '校园一卡通快捷充值',
      createTime: isoOffset(-8)
    },
    {
      transactionId: 2,
      userId: 3,
      transactionType: 3,
      typeName: '支付',
      amount: 129,
      balanceBefore: 1817,
      balanceAfter: 1688,
      frozenBefore: 0,
      frozenAfter: 129,
      relatedOrderId: 1001,
      relatedOrderNumber: 'XYF202611220001',
      transactionNo: 'WT202611220002',
      remark: '订单担保托管支付',
      createTime: isoOffset(-7, 1)
    },
    {
      transactionId: 3,
      userId: 3,
      transactionType: 6,
      typeName: '冻结',
      amount: 168,
      balanceBefore: 1688,
      balanceAfter: 1520,
      frozenBefore: 0,
      frozenAfter: 168,
      relatedOrderId: 1002,
      relatedOrderNumber: 'XYF202611260002',
      transactionNo: 'WT202611260003',
      remark: '约拍订单担保资金冻结',
      createTime: isoOffset(-2, 1)
    },
    {
      transactionId: 4,
      userId: 2,
      transactionType: 5,
      typeName: '收入',
      amount: 122.55,
      balanceBefore: 2557.95,
      balanceAfter: 2680.5,
      frozenBefore: 0,
      frozenAfter: 0,
      relatedOrderId: 1001,
      relatedOrderNumber: 'XYF202611220001',
      transactionNo: 'WT202611230004',
      remark: '订单验收结算收入（已扣 5% 平台服务费）',
      createTime: isoOffset(-5, 3)
    }
  ]

  const notifications: SeedNotification[] = [
    {
      id: 1,
      userId: 3,
      title: '订单 XYF202611260002 已交付待验收',
      content: '创作者「知微胶片馆」已上传精修写真成片，请及时核对并确认验收放款。',
      createTime: isoOffset(-1, 2),
      isRead: 0,
      notificationType: 3,
      relatedId: 1002
    },
    {
      id: 2,
      userId: 3,
      title: '悬赏需求收到新的行家报价',
      content: '硬核拆机顾工对您的悬赏「急求 STM32 + OLED 传感器硬件联调」提交了 ¥160 竞标方案。',
      createTime: isoOffset(-1, -2),
      isRead: 0,
      notificationType: 3
    },
    {
      id: 3,
      userId: 2,
      title: '您的服务交付已验收结算',
      content: '订单 XYF202611220001 买家已确认验收，净结算款 ¥122.55 已入账至可用余额。',
      createTime: isoOffset(-5, 3),
      isRead: 1,
      notificationType: 3,
      relatedId: 1001
    }
  ]

  const chatMessages: SeedChatMessage[] = [
    {
      messageId: 1,
      senderId: 3,
      receiverId: 2,
      content: '林学长你好，我周四晚上有空，想预约一下 SpringBoot + Vue 毕设排障辅导~',
      messageType: 1,
      isRead: 1,
      relatedServiceId: 1,
      createTime: isoOffset(-7, -2)
    },
    {
      messageId: 2,
      senderId: 2,
      receiverId: 3,
      content: '没问题！提前把报错日志和 Git 仓库地址发我，我先帮你看一遍依赖和事务配置。',
      messageType: 1,
      isRead: 1,
      relatedServiceId: 1,
      createTime: isoOffset(-7, -1)
    },
    {
      messageId: 3,
      senderId: 4,
      receiverId: 3,
      content: '念安同学，银杏大道的 15 张精修图和底片已经发到交付凭证里啦，有想微调的色调随时告诉我！',
      messageType: 1,
      isRead: 0,
      relatedOrderId: 1002,
      createTime: isoOffset(-1, 2)
    }
  ]

  const disputes: SeedDispute[] = [
    {
      disputeId: 1,
      orderId: 1002,
      disputeInitiatorId: 3,
      disputeType: 3,
      description: '示例仲裁记录：咨询精修交付张数与底片格式确认流程（已沟通一致）。',
      evidenceImages: [],
      disputeStatus: 3,
      adminReply: '经核查聊天留痕与交付凭证，双方已协商补充交付 RAW 原片，仲裁结案。',
      resolution: '双方达成一致，维持正常履约',
      createTime: isoOffset(-1)
    }
  ]

  const sysConfigs: SeedSysConfig[] = [
    { id: 1, configKey: 'REVIEW_MODE', configValue: '1', description: '服务发布审核模式（1=需管理员审核后上架, 2=免审直通上架）', isEditable: 1, createTime: isoOffset(-60), updateTime: isoOffset(-5) },
    { id: 2, configKey: 'REGISTER_ENABLED', configValue: '1', description: '自助注册开关（1=开启注册, 0=关闭注册）', isEditable: 1, createTime: isoOffset(-60), updateTime: isoOffset(-5) },
    { id: 3, configKey: 'ORDER_UNPAID_TIMEOUT_HOURS', configValue: '24', description: '未支付订单超时自动取消时间（小时）', isEditable: 1, createTime: isoOffset(-60), updateTime: isoOffset(-5) },
    { id: 4, configKey: 'AUTO_CONFIRM_RECEIPT_DAYS', configValue: '7', description: '卖家交付后买家超时自动确认收货时间（天）', isEditable: 1, createTime: isoOffset(-60), updateTime: isoOffset(-5) },
    { id: 5, configKey: 'PLATFORM_COMMISSION_RATE', configValue: '0.05', description: '平台担保交易技术服务费率（5%）', isEditable: 1, createTime: isoOffset(-60), updateTime: isoOffset(-5) },
    { id: 6, configKey: 'WALLET_SINGLE_RECHARGE_LIMIT', configValue: '5000', description: '钱包单笔充值风控上限（元）', isEditable: 1, createTime: isoOffset(-60), updateTime: isoOffset(-5) }
  ]

  const riskAlerts: SeedRiskAlert[] = [
    {
      id: 1,
      alertNo: 'RA202611270001',
      ruleCode: 'ULTRA_FAST_COMPLETION',
      ruleName: '极速完单异常预警',
      severity: 2,
      riskScore: 68,
      orderId: 1001,
      orderNumber: 'XYF202611220001',
      serviceId: 1,
      serviceTitle: services[0].serviceTitle,
      buyerId: 3,
      buyerName: '苏念安',
      buyerStudentId: '20240003',
      buyerCreditScore: 752,
      sellerId: 2,
      sellerName: '林砚舟',
      sellerStudentId: '20240002',
      sellerCreditScore: 786,
      campusId: 1,
      orderAmount: 129,
      pairOrderCount7d: 1,
      reverseOrderCount7d: 0,
      fulfillDurationSeconds: 185,
      evidenceSummary: '线下提前沟通完成后集中走单，下单至验收耗时偏短，建议人工核验聊天与交付记录。',
      handleStatus: 0,
      createTime: isoOffset(-1)
    }
  ]

  const favorites = [
    { favoriteId: 1, userId: 3, serviceId: 1, createTime: isoOffset(-6) },
    { favoriteId: 2, userId: 3, serviceId: 4, createTime: isoOffset(-3) }
  ]

  return {
    campuses,
    professionals,
    serviceTags,
    tradeLocations,
    users,
    services,
    serviceSlots,
    reviews,
    demands,
    groupSessions,
    orders,
    walletTransactions,
    notifications,
    chatMessages,
    disputes,
    sysConfigs,
    riskAlerts,
    favorites
  }
}
