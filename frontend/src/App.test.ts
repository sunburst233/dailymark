import { describe, expect, it } from 'vitest'
import { dateKey } from './App'

describe('dateKey', () => {
  it('groups ISO timestamps by a stable calendar date', () => {
    expect(dateKey('2026-10-14T12:00:00Z')).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
