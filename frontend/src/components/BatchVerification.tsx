import { Fragment, useState } from 'react'
import Papa from 'papaparse'
import { verifyLabel } from '../services/verificationApi'
import type {
    ApplicationData,
    VerificationResult,
} from '../types/verification'
import { CheckResult, StatusBadge } from './CheckResult'

interface CsvRow {
    filename: string
    brandName: string
    classType: string
    alcoholContent: string
    netContents: string
}

type RowState = 'PENDING' | 'RUNNING' | 'DONE' | 'ERROR'

interface BatchRow {
    fileName: string
    application: ApplicationData
    image: File | null
    state: RowState
    result?: VerificationResult
    error?: string
}

export function BatchVerification() {
    const [csvFile, setCsvFile] = useState<File | null>(null)
    const [images, setImages] = useState<File[]>([])
    const [rows, setRows] = useState<BatchRow[]>([])
    const [running, setRunning] = useState(false)
    const [expanded, setExpanded] = useState<number | null>(null)

    // Replace one row in the list without touching the others.
    function updateRow(index: number, patch: Partial<BatchRow>) {
        setRows(prev =>
            prev.map((row, i) => (i === index ? { ...row, ...patch } : row))
        )
    }

    async function buildRows(csv: File): Promise<BatchRow[]> {
        const text = await csv.text()
        const parsed = Papa.parse<CsvRow>(text, {
            header: true,
            skipEmptyLines: true,
            transformHeader: header => header.trim(),
        })

        const imagesByName = new Map(images.map(file => [file.name, file]))

        return parsed.data.map(row => ({
            fileName: row.filename,
            application: {
                brandName: row.brandName,
                classType: row.classType,
                alcoholContent: row.alcoholContent,
                netContents: row.netContents,
            },
            image: imagesByName.get(row.filename) ?? null,
            state: 'PENDING',
    }))
    }

    async function handleStart() {
        if (!csvFile || images.length === 0 || running) return

        setRunning(true)
        setExpanded(null)

        try {
            const newRows = await buildRows(csvFile)
            setRows(newRows)

            // One at a time: OCR is CPU-bound, so parallel requests
            // would not finish sooner and could exhaust server memory.
            for (let i = 0; i < newRows.length; i++) {
                const row = newRows[i]
                if(row.image === null) {
                    updateRow(i, {state: "ERROR", error: 'Image file not found'})
                    continue
                }
                updateRow(i, { state: "RUNNING"})
                try {
                    const  result = await verifyLabel(row.image, row.application)
                    updateRow(i, { state: 'DONE', result })
                } catch (err) {
                    updateRow(i, {
                        state: "ERROR",
                        error: err instanceof Error ? err.message : 'An unexpected error occurred.',
                    })
                }

            }
        } finally {
            setRunning(false)
        }
    }

    const finished = rows.filter(
        row => row.state === 'DONE' || row.state === 'ERROR'
    ).length

    const passCount = rows.filter(row => row.result?.status === 'PASS').length
    const failCount = rows.filter(row => row.result?.status === 'FAIL').length
    const needsReviewCount = rows.filter(row => row.result?.status === 'NEEDS_REVIEW').length
    const errorCount = rows.filter(row => row.state === 'ERROR').length

    return (
        <>
            <section className="panel">
                <h2>Batch Verification</h2>
                <p className="section-description">
                    Upload a CSV with one row per label, plus the label images.
                    The CSV "filename" column must match each image's file name.
                </p>

                <label htmlFor="batchCsv">Application CSV</label>
                <input
                    id="batchCsv"
                    type="file"
                    accept=".csv,text/csv"
                    onChange={e => setCsvFile(e.target.files?.[0] ?? null)}
                />

                <label htmlFor="batchImages">Label Images</label>
                <input
                    id="batchImages"
                    type="file"
                    accept="image/jpeg,image/png"
                    multiple
                    onChange={e => setImages(Array.from(e.target.files ?? []))}
                />

                <button
                    type="button"
                    className="verify-button"
                    disabled={running || !csvFile || images.length === 0}
                    onClick={handleStart}
                >
                    {running
                        ? `Verifying ${finished} of ${rows.length}...`
                        : 'Verify All'}
                </button>
            </section>

            {rows.length > 0 && (
                <section className="results-section">
                    <h2>Batch Results</h2>
                    <p className="batch-summary">
                        PASS {passCount} · FAIL {failCount} · NEEDS_REVIEW {needsReviewCount} · ERROR {errorCount}
                    </p>

                    <table className="batch-table">
                        <thead>
                        <tr>
                            <th>File</th>
                            <th>Brand</th>
                            <th>Result</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {rows.map((row, index) => (
                            <Fragment key={`${row.fileName}-${index}`}>
                                <tr>
                                    <td>{row.fileName}</td>
                                    <td>{row.application.brandName}</td>
                                    <td>
                                        {row.state === 'DONE' && row.result && (
                                            <StatusBadge status={row.result.status} />
                                        )}
                                        {row.state === 'ERROR' && row.error}
                                        {row.state === 'RUNNING' && 'Verifying...'}
                                        {row.state === 'PENDING' && 'Waiting'}
                                    </td>
                                    <td>
                                        {row.result && (
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    setExpanded(expanded === index ? null : index)
                                                }
                                            >
                                                {expanded === index ? 'Hide' : 'Details'}
                                            </button>
                                        )}
                                    </td>
                                </tr>

                                {expanded === index && row.result && (
                                    <tr>
                                        <td colSpan={4}>
                                            {row.result.checks.map((check, i) => (
                                                <CheckResult
                                                    key={`${check.field}-${i}`}
                                                    check={check}
                                                />
                                            ))}
                                        </td>
                                    </tr>
                                )}
                            </Fragment>
                        ))}
                        </tbody>
                    </table>
                </section>
            )}
        </>
    )
}