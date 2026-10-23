import { describe, expect, it } from 'vitest'
import { defaultValues, inferMapping, mapImportRows } from './importMapping'

describe('timetable column mapping', () => {
  it('recognizes Greek headings and splits a mapped time range without losing course titles', () => {
    const table ={ name: 'Πρόγραμμα', rows: [
      ['Κωδικός μαθήματος', 'Μάθημα', 'Ημέρα', 'Ώρα', 'ECTS'],
      ['CS101', 'Λειτουργικά Συστήματα', 'Δευτέρα', '09:00 – 11:00', '6'],
    ] }
    const mapping =inferMapping(table, true)
    expect(mapImportRows(table, true, mapping, defaultValues)).toEqual([{
      ...defaultValues, courseCode: 'CS101', title: 'Λειτουργικά Συστήματα', weekday: 'Δευτέρα',
      startTime: '09:00', endTime: '11:00', credits: '6',
    }])
  })
  it('retains the first data row when there is no header and applies explicit defaults', () => {
    const table ={ name: 'Page 1', rows: [['CS101', 'Algorithms'], ['', '']] }
    const mapping ={ ...inferMapping(table, false), courseCode: '0', title: '1' }
    expect(mapImportRows(table, false, mapping, { ...defaultValues, credits: '5', weekday: 'MONDAY' }))
      .toEqual([{ ...defaultValues, credits: '5', weekday: 'MONDAY', courseCode: 'CS101', title: 'Algorithms' }])
  })
  it('leaves missing mapped cells blank for validation rather than silently substituting defaults', () => {
    const table ={ name: 'Sheet', rows: [['Code', 'Title', 'Credits'], ['CS101', 'Algorithms']] }
    const mapping =inferMapping(table, true)
    expect(mapImportRows(table, true, mapping, { ...defaultValues, credits: '6' })[0].credits).toBe('')
  })
})
