import test from 'node:test'
import assert from 'node:assert/strict'
import { createMockStore, paginate } from './mockApi.ts'

test('paginate slices items and computes page counts accurately', () => {
  const result = paginate([1, 2, 3, 4, 5], 2, 2)
  assert.deepEqual(result.records, [3, 4])
  assert.equal(result.total, 5)
  assert.equal(result.pages, 3)
  assert.equal(result.current, 2)
})

test('loginUser authenticates seeded student and admin accounts and rejects invalid credentials', () => {
  const store = createMockStore()
  const studentLogin = store.loginUser('20240003', 'password123')
  assert.ok('token' in studentLogin)
  assert.equal(studentLogin.userInfo.studentId, '20240003')
  assert.equal(studentLogin.userInfo.role, 1)

  const adminLogin = store.loginUser('20240001', 'password123')
  assert.ok('token' in adminLogin)
  assert.equal(adminLogin.userInfo.role, 2)

  const badLogin = store.loginUser('20240003', 'wrong-pass')
  assert.ok('error' in badLogin)
})

test('order lifecycle deducts buyer wallet into escrow and settles net amount to seller on confirm', () => {
  const store = createMockStore()
  const buyer = store.getUserById(3)!
  const seller = store.getUserById(2)!
  const buyerStartBalance = buyer.walletBalance
  const sellerStartBalance = seller.walletBalance

  const created = store.createOrder(buyer, { serviceId: 1, quantity: 1, tradeType: 2 })
  assert.ok('order' in created)
  assert.equal(created.order.orderStatus, 1)

  const paid = store.payOrder(buyer, created.order.id, 1)
  assert.ok('order' in paid)
  assert.equal(paid.order.orderStatus, 2)
  assert.equal(buyer.walletBalance, Number((buyerStartBalance - created.order.totalAmount).toFixed(2)))

  const confirmed = store.confirmOrder(buyer, created.order.id)
  assert.ok('order' in confirmed)
  assert.equal(confirmed.order.orderStatus, 4)
  assert.equal(
    seller.walletBalance,
    Number((sellerStartBalance + created.order.sellerSettleAmount).toFixed(2))
  )
})

test('group session triggers automatic tiered discount rebate when target count is reached', () => {
  const store = createMockStore()
  // Session 1 has 3 participants and tierTargetCount = 4 (basePrice 89 -> tierDiscountPrice 59)
  const newJoiner = store.getUserById(5)!
  const existingMember = store.getUserById(3)!
  const existingBalanceBefore = existingMember.walletBalance

  const joined = store.joinGroupSession(newJoiner, 1)
  assert.ok('session' in joined)
  assert.equal(joined.session.participants.length, 4)
  assert.equal(joined.session.tierRebateApplied, 1)
  assert.equal(joined.session.currentUnitPrice, 59)
  assert.equal(existingMember.walletBalance, Number((existingBalanceBefore + 30).toFixed(2)))
})
