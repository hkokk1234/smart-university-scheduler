import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { Upload, Trash2, Plus, CheckCircle } from 'lucide-react'
import { api } from '@/lib/api'
import type { ImportPreview, ImportReport, ImportRow, ImportSummary, ImportTable } from '@/lib/types'
import { usePlanner } from '@/features/planner/plannerStore'
import { useSession } from '@/features/auth/sessionStore'
import { PageHeading } from '@/components/shared/PageHeading'
import { SemesterSelect } from '@/components/shared/SemesterSelect'
import { ErrorState, EmptyState } from '@/components/shared/RequestStates'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input, Select } from '@/components/ui/input'
import { defaultValues, importFields, inferMapping, mapImportRows } from './importMapping'

export default function CatalogImportPage() {
  const semesterId =usePlanner(state => state.semesterId)
  const user =useSession(state => state.user)
  return <>
    <Link to="/admin" className="text-sm text-muted-foreground">← Administration</Link>
    <PageHeading eyebrow="From document to catalog" title="Import PDF / Excel" description="Extract tables, match columns and review every meeting before publishing to the selected semester." action={<SemesterSelect />} />
    {semesterId ? <ImportWorkspace key={user?.id + ':' + semesterId} semesterId={semesterId} /> : <EmptyState title="Choose a semester" description="Select an existing semester before importing courses." />}
  </>
}

function ImportWorkspace({ semesterId }: { semesterId: string }) {
  const [file, setFile] =useState<File | null>(null)
  const [preview, setPreview] =useState<ImportPreview | null>(null)
  const [tableIndex, setTableIndex] =useState(0)
  const [rows, setRows] =useState<ImportRow[] | null>(null)
  const [report, setReport] =useState<ImportReport | null>(null)
  const [summary, setSummary] =useState<ImportSummary | null>(null)
  const [error, setError] =useState<Error | null>(null)
  const [busy, setBusy] =useState<'upload' | 'validate' | 'commit' | null>(null)
  const [page, setPage] =useState(0)
  const active =useRef<AbortController | null>(null)
  const client =useQueryClient()
  const user =useSession(state => state.user)
  useEffect(() => () => active.current?.abort(), [])
  const selected =preview?.tables[tableIndex]
  const totalPages =Math.ceil((rows?.length ?? 0) / 25)
  const rowErrors =new Map(report?.errors.map(row => [row.rowIndex, row.fields]))

  async function run(action: 'upload' | 'validate' | 'commit') {
    active.current?.abort()
    const controller =new AbortController(); active.current =controller
    setBusy(action); setError(null)
    try {
      if (action ==='upload' && file) {
        const result =await api.previewImport(file, controller.signal)
        if (controller.signal.aborted) return
        setPreview(result); setTableIndex(0); setRows(null); setReport(null); setSummary(null)
      } else if (action ==='validate' && rows) {
        const result =await api.validateImport(semesterId, rows, controller.signal)
        if (controller.signal.aborted) return
        setReport(result)
        if (result.errors.length) setPage(Math.floor(result.errors[0].rowIndex / 25))
      } else if (action ==='commit' && rows && report?.valid) {
        const result =await api.commitImport(semesterId, rows, controller.signal)
        if (controller.signal.aborted) return
        setSummary(result)
        void client.invalidateQueries({ queryKey: ['courses'] })
        void client.invalidateQueries({ queryKey: ['course'] })
        void client.invalidateQueries({ queryKey: ['private', user?.id, 'digital-twin'] })
      }
    } catch (failure) {
      if (!controller.signal.aborted) {
        setError(failure instanceof Error ? failure : new Error('The import could not be completed.'))
        if (action ==='commit') setReport(null)
      }
    } finally { if (!controller.signal.aborted) setBusy(null) }
  }
  function edit(index: number, key: keyof ImportRow, value: string) {
    setRows(current => current?.map((row, rowIndex) => rowIndex ===index ? { ...row, [key]: value } : row) ?? null)
    setReport(null); setError(null)
  }
  function remove(index: number) {
    const next =rows?.filter((_, rowIndex) => rowIndex !==index) ?? []
    setRows(next)
    setPage(currentPage => Math.min(currentPage, Math.max(0, Math.ceil(next.length / 25) - 1)))
    setReport(null); setError(null)
  }
  return <>
    <p className="rounded-xl border bg-secondary/40 p-4 text-sm text-muted-foreground">One row represents one weekly meeting. Multiple meetings can share a section. Imports create new sections and preserve existing ones. Changing semester clears this preview.</p>
    {summary ? <Card className="space-y-4 p-6"><h2 className="flex items-center gap-2 text-lg font-semibold text-primary"><CheckCircle size={20} />Import completed</h2><p>{summary.coursesCreated} courses, {summary.groupsCreated} groups, {summary.sectionsCreated} sections and {summary.meetingsCreated} meetings created.</p><div className="flex flex-wrap gap-3"><Button asChild><Link to="/catalog">View catalog</Link></Button><Button variant="outline" onClick={() => { setSummary(null); setRows(null); setReport(null); setPreview(null); setFile(null) }}>Import another file</Button></div></Card> : <>
      {!rows && <Card className="space-y-4 p-5">
        <h2 className="font-semibold">1. Choose a document</h2>
        <p className="text-sm text-muted-foreground">PDF with selectable text, .xlsx or .xls. Up to 5 MB, 500 non-empty rows, 30 columns and 20 sheets or pages. Scanned PDFs require OCR before upload.</p>
        <label className="block space-y-2 text-sm"><span>Timetable file</span><Input type="file" accept=".pdf,.xlsx,.xls" disabled={!!busy} onChange={event => {
          const next =event.target.files?.[0] ?? null
          setFile(next); setPreview(null); setReport(null); setError(next && next.size > 5 * 1024 * 1024 ? new Error('Choose a file up to 5 MB.') : null)
        }} /></label>
        <Button disabled={!file || !!busy || file.size > 5 * 1024 * 1024} onClick={() => void run('upload')}><Upload size={16} />{busy ==='upload' ? 'Reading document…' : 'Extract preview'}</Button>
      </Card>}
      {preview?.warnings.map(warning => <p key={warning} role="status" className="rounded-xl border p-4 text-sm text-muted-foreground">{warning}</p>)}
      {!rows && preview && selected && busy !=='upload' && <>
        <label className="block space-y-2 text-sm font-medium"><span>Sheet / PDF page to import ({preview.tables.length} available)</span><Select value={tableIndex} onChange={event => setTableIndex(Number(event.target.value))}>{preview.tables.map((table, index) => <option key={index} value={index}>{table.name} · {table.rows.length} rows</option>)}</Select></label>
        <p className="text-xs text-muted-foreground">Each import uses the selected sheet or page. Other sheets and pages can be imported separately.</p>
        <ColumnMapping key={tableIndex} table={selected} onReview={mapped => { setRows(mapped); setPage(0); setReport(null); setError(null) }} />
      </>}
      {rows && <Card className="space-y-5 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-semibold">3. Review and correct {rows.length} meetings</h2><p className="mt-1 text-xs text-muted-foreground">Remove headings, check inferred cells and fill required values. Times use 24-hour format; weekdays accept English or Greek.</p></div><Button variant="outline" disabled={!!busy} onClick={() => { setRows(null); setReport(null); setError(null) }}>Back to columns</Button></div>
        <fieldset disabled={!!busy} className="min-w-0"><legend className="sr-only">Editable meeting rows</legend>
          <div className="overflow-x-auto"><table className="w-full text-left text-xs"><caption className="sr-only">Review imported meetings before validating</caption><thead><tr><th className="p-2">Row</th>{importFields.map(field => <th key={field.key} className="min-w-36 p-2">{field.label}</th>)}<th className="p-2">Remove</th></tr></thead><tbody>{rows.slice(page * 25, (page + 1) * 25).map((row, offset) => {
            const index =page * 25 + offset
            const errors =rowErrors.get(index)
            return <tr key={index} className={errors ? 'bg-destructive/5' : ''}><th className="p-2 font-medium">{index + 1}</th>{importFields.map(({ key, label }) => <td key={key} className="p-1.5 align-top"><Input aria-label={`Row ${index + 1}: ${label}`} aria-invalid={!!errors?.[key]} aria-describedby={errors?.[key] ? `import-error-${index}-${key}` : undefined} value={row[key]} maxLength={500} onChange={event => edit(index, key, event.target.value)} />{errors?.[key] && <p id={`import-error-${index}-${key}`} className="mt-1 max-w-52 text-destructive">{errors[key]}</p>}</td>)}<td className="p-2 align-top"><Button variant="ghost" size="sm" aria-label={`Remove row ${index + 1}`} onClick={() => remove(index)}><Trash2 size={15} /></Button></td></tr>
          })}</tbody></table></div>
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><Button variant="outline" size="sm" disabled={rows.length >=500} onClick={() => { setRows([...rows, { ...defaultValues }]); setPage(Math.floor(rows.length / 25)); setReport(null) }}><Plus size={14} />Add meeting</Button><div className="flex items-center gap-3"><Button variant="outline" size="sm" disabled={page ===0} onClick={() => setPage(page - 1)}>Previous</Button><span className="text-xs">Page {page + 1} of {Math.max(1, totalPages)}</span><Button variant="outline" size="sm" disabled={page + 1 >=totalPages} onClick={() => setPage(page + 1)}>Next</Button></div></div>
        </fieldset>
        {report && <p role="status" className={report.valid ? 'text-sm text-primary' : 'text-sm text-destructive'}>{report.valid ? `Ready: ${report.courseCount} courses, ${report.sectionCount} sections, ${report.meetingCount} meetings. Existing matching courses and groups will be reused.` : `${report.errors.length} rows need correction. Errors appear beside the relevant cells.`}</p>}
        <p className="text-xs text-muted-foreground">Publishing makes these sections available to students. All rows are checked again and saved together. Room and instructor assignments can be added in Administration afterwards.</p>
        <div className="flex flex-wrap justify-end gap-3"><Button variant="outline" disabled={!!busy || !rows.length} onClick={() => void run('validate')}>{busy ==='validate' ? 'Checking rows…' : 'Validate rows'}</Button><Button disabled={!!busy || !report?.valid} onClick={() => void run('commit')}>{busy ==='commit' ? 'Publishing…' : 'Publish to catalog'}</Button></div>
      </Card>}
      {error && <ErrorState error={error} />}
    </>}
  </>
}

function ColumnMapping({ table, onReview }: { table: ImportTable; onReview: (rows: ImportRow[]) => void }) {
  const [hasHeader, setHasHeader] =useState(true)
  const [mapping, setMapping] =useState(() => inferMapping(table, true))
  const [defaults, setDefaults] =useState({ ...defaultValues })
  const width =Math.max(...table.rows.map(row => row.length))
  const headers =Array.from({ length: width }, (_, index) => hasHeader ? table.rows[0]?.[index] || `Column ${index + 1}` : `Column ${index + 1}`)
  return <Card className="space-y-5 p-5">
    <h2 className="font-semibold">2. Match your columns</h2>
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={hasHeader} onChange={event => { setHasHeader(event.target.checked); setMapping(inferMapping(table, event.target.checked)) }} />First row contains column headings</label>
    <p className="text-xs text-muted-foreground">Choose a source column or enter an explicit default. Separate lectures and labs with different group codes; alternatives share a group code and have different section codes. A combined time column can be mapped to both start and end.</p>
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{importFields.map(({ key, label }) => <div key={key} className="space-y-2"><label htmlFor={`map-${key}`} className="text-xs font-medium">{label}</label><Select id={`map-${key}`} value={mapping[key]} onChange={event => setMapping(current => ({ ...current, [key]: event.target.value }))}><option value="">Use default value</option>{headers.map((header, index) => <option key={index} value={index}>{index + 1}. {header.slice(0, 80)}</option>)}</Select>{mapping[key] ==='' && <Input aria-label={`Default ${label}`} placeholder={`Enter ${label.toLowerCase()}`} value={defaults[key]} maxLength={500} onChange={event => setDefaults(current => ({ ...current, [key]: event.target.value }))} />}</div>)}</div>
    <details><summary className="cursor-pointer text-sm font-medium">Inspect extracted source (first 10 rows)</summary><div className="mt-3 overflow-x-auto"><table className="w-full text-left text-xs"><tbody>{table.rows.slice(0, 10).map((row, index) => <tr key={index}>{row.map((cell, column) => <td key={column} className="max-w-72 whitespace-pre-wrap border p-2">{cell}</td>)}</tr>)}</tbody></table></div></details>
    <div className="flex justify-end"><Button disabled={hasHeader && table.rows.length <=1} onClick={() => onReview(mapImportRows(table, hasHeader, mapping, defaults))}>Review editable rows</Button></div>
  </Card>
}
