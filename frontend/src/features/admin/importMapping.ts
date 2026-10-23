import type { ImportRow, ImportTable } from '@/lib/types'

export const importFields: { key: keyof ImportRow; label: string; aliases: string[] }[] =[
  { key: 'courseCode', label: 'Course code', aliases: ['coursecode', 'code', 'κωδικος', 'κωδικοςμαθηματος'] },
  { key: 'title', label: 'Course title', aliases: ['coursetitle', 'title', 'course', 'μαθημα', 'τιτλος', 'τιτλοςμαθηματος'] },
  { key: 'credits', label: 'Credits / ECTS', aliases: ['credits', 'ects', 'πιστωτικεςμοναδες'] },
  { key: 'groupCode', label: 'Group code', aliases: ['groupcode', 'group', 'ομαδα', 'κωδικοςομαδας'] },
  { key: 'groupType', label: 'Group type', aliases: ['grouptype', 'type', 'τυπος', 'ειδος'] },
  { key: 'sectionCode', label: 'Section code', aliases: ['sectioncode', 'section', 'τμημα'] },
  { key: 'capacity', label: 'Capacity', aliases: ['capacity', 'χωρητικοτητα', 'θεσεις'] },
  { key: 'weekday', label: 'Weekday', aliases: ['weekday', 'day', 'ημερα'] },
  { key: 'startTime', label: 'Start time', aliases: ['starttime', 'start', 'from', 'εναρξη', 'απο', 'time', 'ωρα', 'ωρες'] },
  { key: 'endTime', label: 'End time', aliases: ['endtime', 'end', 'to', 'ληξη', 'εως', 'μεχρι', 'time', 'ωρα', 'ωρες'] },
]
export type ImportMapping =Record<keyof ImportRow, string>
export const defaultValues: ImportRow ={
  courseCode: '', title: '', credits: '', groupCode: 'LECTURE', groupType: 'LECTURE', sectionCode: 'A',
  capacity: '30', weekday: '', startTime: '', endTime: '',
}
function normalize(value: string) {
  return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^\p{L}\p{N}]/gu, '')
}
export function inferMapping(table: ImportTable, hasHeader: boolean): ImportMapping {
  return Object.fromEntries(importFields.map(field => {
    const index =hasHeader ? table.rows[0]?.findIndex(cell => field.aliases.includes(normalize(cell))) ?? -1 : -1
    return [field.key, index < 0 ? '' : String(index)]
  })) as ImportMapping
}
export function mapImportRows(table: ImportTable, hasHeader: boolean, mapping: ImportMapping, defaults: ImportRow): ImportRow[] {
  return table.rows.slice(hasHeader ? 1 : 0).filter(row => row.some(cell => cell.trim())).map(cells => {
    const row =Object.fromEntries(importFields.map(({ key }) => [key, mapping[key] ==='' ? defaults[key] : cells[Number(mapping[key])]?.trim() ?? ''])) as unknown as ImportRow
    if (mapping.startTime !=='' && mapping.startTime ===mapping.endTime) {
      const range =row.startTime.split(/\s*[-–—]\s*/)
      if (range.length ===2) { row.startTime =range[0]; row.endTime =range[1] }
    }
    return row
  })
}
