import { describe, expect, it } from 'vitest'
import { toInstantValue } from './date.js'

describe('toInstantValue', () => {
  it('converts the entered local date and time to an exact UTC instant', () => {
    const expected = new Date(2026, 9, 7, 8, 30).toISOString()

    expect(toInstantValue('2026-10-07', '08:30')).toBe(expected)
    expect(toInstantValue('2026-10-07', '08:30')).toMatch(/Z$/)
  })

  it('returns an empty value until both parts are present', () => {
    expect(toInstantValue('', '08:30')).toBe('')
    expect(toInstantValue('2026-10-07', '')).toBe('')
  })
})
