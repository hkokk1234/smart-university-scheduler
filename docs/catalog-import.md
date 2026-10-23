# PDF / Excel catalog import

Open **Administration → Import PDF / Excel** with an administrator account. Choose the target semester, upload a document, select its sheet or page, map columns, and review editable meeting rows. No catalog writes occur during extraction or validation. **Publish to catalog** revalidates and saves the complete batch in one transaction.

## Supported documents

- `.xlsx` and `.xls` workbooks, including formatted numeric time cells. Formula and error cells are left blank, with a warning; formulas are not executed.
- PDFs with an extractable text layer. Rows and columns are inferred from text coordinates, so complex layouts may require manual correction. Scanned/image-only PDFs require OCR outside this feature.
- Up to 5 MB per file, 500 non-empty source rows across the document, 30 columns, and 20 worksheets or PDF pages. Oversized files and documents are rejected rather than silently truncated. A selected sheet or page is imported at a time; other tables remain separate.

The parser uses [Apache POI 5.5.1](https://poi.apache.org/download.cgi) for Excel and [Apache PDFBox 3.0.8](https://pdfbox.apache.org/3.0/getting-started.html) for PDF. Extraction runs on the application server, without an AI provider, external OCR service, or persisted source upload.

## Mapping and review

English and Greek headings are recognized when possible. The first-row heading checkbox is explicit. Unrecognized columns can be mapped manually, and each unmapped field has an editable default. The extracted source is available for inspection.

| Field | Rules |
| --- | --- |
| Course code | Required, up to 32 characters; preserved after trimming |
| Course title | Required, up to 200 characters |
| Credits | Greater than 0 and at most 60; one decimal place, decimal comma accepted |
| Group code | Required, up to 32 characters; separates required lecture/lab/tutorial groups |
| Group type | LECTURE, LABORATORY, TUTORIAL, SEMINAR; common English and Greek aliases accepted |
| Section code | Required, up to 32 characters; alternative sections share a group code |
| Capacity | Whole number from 1 to 100000 |
| Weekday | Full English/Greek weekday names, common abbreviations, or ISO 1–7 |
| Start / end | 24-hour `H:mm`, `HH:mm` or `HH:mm:ss`; a dot separator is also accepted; end must follow start on the same day |

A source column containing `09:00–11:00` can be mapped to both start and end; the preview splits the range. Defaults shown in the mapping form are group `LECTURE`, type `LECTURE`, section `A`, and capacity `30`. Course metadata and meeting times have no guessed defaults.

Example rows (one row per weekly meeting):

| Course code | Title | Credits | Group code | Group type | Section | Capacity | Day | Start | End |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| CS101 | Algorithms | 6 | LEC | LECTURE | A | 80 | Δευτέρα | 09:00 | 11:00 |
| CS101 | Algorithms | 6 | LEC | LECTURE | A | 80 | Τετάρτη | 09:00 | 10:00 |
| CS101 | Algorithms | 6 | LAB | LABORATORY | L1 | 25 | Πέμπτη | 12:00 | 14:00 |
| CS101 | Algorithms | 6 | LAB | LABORATORY | L2 | 25 | Παρασκευή | 12:00 | 14:00 |

The first two rows become a single lecture section with two meetings. L1 and L2 are laboratory alternatives. All rows for a course must agree on title and credits; group types and section capacities must also agree. Duplicate or overlapping meetings within the same section are rejected. Overlaps across different sections are allowed because those sections may be alternatives.

The review table supports editing, deleting headings or unwanted rows, adding meetings, and pagination. Validation returns errors next to individual cells and opens the first invalid page. Any edit invalidates the previous validation result and disables publication until the rows are checked again.

## Existing catalog and atomic publication

Matching existing courses and groups are reused. Existing course titles, credits, group types and section contents are preserved. An existing section code is rejected; use a new alternative code or edit the section in Administration. This also prevents accidental repeated imports from duplicating meetings.

All validation runs again at publication, including checks against current database contents. Courses, groups, sections and meetings are written in a single transaction. Database unique constraints handle competing imports; a failure rolls back the batch. No migration is needed. Rooms and instructors are initially unassigned and can be added through the existing section editor.

## API

All endpoints require ADMIN and return `Cache-Control: no-store`.

| Endpoint | Body | Result |
| --- | --- | --- |
| POST `/api/v1/admin/catalog-import/preview` | `multipart/form-data`, `file` part | `tables: [{name, rows: string[][]}]`, `warnings` |
| POST `/api/v1/admin/catalog-import/validate` | `{semesterId, rows}` | `valid`, per-row/per-field `errors`, course/section/meeting counts |
| POST `/api/v1/admin/catalog-import/commit` | Same reviewed rows | 201, counts of created courses/groups/sections/meetings |

Rows use string fields: `courseCode`, `title`, `credits`, `groupCode`, `groupType`, `sectionCode`, `capacity`, `weekday`, `startTime`, `endTime`. `rowIndex` in validation errors is zero-based. At most 500 rows can be submitted. Invalid files return a useful `INVALID_IMPORT_FILE` error; oversized multipart uploads return 413. A stale or invalid batch returns `INVALID_IMPORT` without publication.

## Verification

Parser tests create real XLS, XLSX and PDF documents, verify Unicode and formatted times, and cover formulas, sparse cells, column/row limits and unreadable inputs. Service tests cover normalization, grouped meetings, existing records, duplicates, overlaps and validation before writes. HTTP tests cover administrator authorization, multipart uploads and request validation. Frontend tests cover mapping, time ranges, missing cells and authenticated multipart boundaries.

`ApiIT.catalogImportPreviewsWithoutWritesAndPublishesACompleteValidatedBatch` checks real PostgreSQL persistence, repeated imports and invalid batches under Testcontainers. Run it with the repository's integration profile and a working Docker daemon.
