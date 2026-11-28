import test from 'node:test'
import assert from 'node:assert/strict'
import { formatSessionTimeRange } from './format'

test('formatSessionTimeRange formats same-day session cleanly without trailing colons', () => {
  const res = formatSessionTimeRange('2026-12-01T11:33:14', '2026-12-01T15:33:14')
  assert.equal(res, '12月1日 11:33–15:33')
})

test('formatSessionTimeRange formats cross-day session with both dates', () => {
  const res = formatSessionTimeRange('2026-12-01T21:00:00', '2026-12-02T09:30:00')
  assert.equal(res, '12月1日 21:00 – 12月2日 09:30')
})

test('formatSessionTimeRange handles missing end or invalid start gracefully', () => {
  assert.equal(formatSessionTimeRange('2026-12-04T14:05:00'), '12月4日 14:05')
  assert.equal(formatSessionTimeRange(undefined), '时间待定')
})
