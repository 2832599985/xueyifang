import test from 'node:test'
import assert from 'node:assert/strict'
import { isOutOfChina, wgs84ToGcj02 } from './coordTransform'

test('wgs84ToGcj02 converts Beijing coordinates with expected GCJ-02 offset', () => {
  // Tiananmen Square WGS-84 (~39.908722, 116.397499) -> GCJ-02 (~39.910126, 116.403744)
  const res = wgs84ToGcj02(39.908722, 116.397499)
  assert.ok(res.lat > 39.909 && res.lat < 39.912, `unexpected lat: ${res.lat}`)
  assert.ok(res.lng > 116.402 && res.lng < 116.406, `unexpected lng: ${res.lng}`)
})

test('wgs84ToGcj02 converts Shanghai coordinates into GCJ-02 datum', () => {
  const res = wgs84ToGcj02(31.230416, 121.473701)
  // Shanghai GCJ-02 delta is roughly -0.0019 lat, +0.0046 lng
  assert.ok(Math.abs(res.lat - 31.2285) < 0.002, `unexpected lat: ${res.lat}`)
  assert.ok(Math.abs(res.lng - 121.4783) < 0.002, `unexpected lng: ${res.lng}`)
})

test('wgs84ToGcj02 leaves coordinates outside China untouched', () => {
  assert.equal(isOutOfChina(37.7749, -122.4194), true)
  const sf = wgs84ToGcj02(37.7749, -122.4194)
  assert.deepEqual(sf, { lat: 37.7749, lng: -122.4194 })
})

test('wgs84ToGcj02 preserves non-finite inputs safely', () => {
  const res = wgs84ToGcj02(Number.NaN, 116.4)
  assert.ok(Number.isNaN(res.lat))
  assert.equal(res.lng, 116.4)
})
