import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getFullReport, downloadReport, getGroup } from '../api/client'
import Spinner from '../components/Spinner'

export default function ImportReport() {
  const { id: groupId, jobId } = useParams()
  const [group, setGroup] = useState(null)
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => { loadData() }, [groupId, jobId])

  async function loadData() {
    try {
      const [grRes, repRes] = await Promise.all([
        getGroup(groupId),
        getFullReport(groupId, jobId),
      ])
      setGroup(grRes.data)
      setReport(repRes.data)
    } catch {
      setError('Failed to load report')
    } finally {
      setLoading(false)
    }
  }

  function handleDownload() {
    const url = downloadReport(groupId, jobId)
    const token = localStorage.getItem('token')
    // Open in same tab with auth via fetched blob
    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then((r) => r.blob())
      .then((blob) => {
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = `import-report-${jobId}.json`
        a.click()
      })
  }

  if (loading) return <Spinner />
  if (error) return <p className="text-red-600 text-sm">{error}</p>

  const summary = report?.summary || {}
  const issueStats = report?.issueStats || {}
  const issues = report?.issuesFound || []
  const imported = report?.importedRecords || []
  const skipped = report?.skippedRecords || []

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <Link to={`/groups/${groupId}/imports/${jobId}/review`} className="text-xs text-gray-400 hover:underline">← Review</Link>
          <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Import Report</h1>
          <p className="text-sm text-gray-500">{summary.filename}</p>
        </div>
        <button
          onClick={handleDownload}
          className="text-sm border border-gray-300 rounded px-3 py-1.5 text-gray-600 hover:border-blue-400 hover:text-blue-600"
        >
          ↓ Download JSON
        </button>
      </div>

      {/* Summary */}
      <div className="bg-white border border-gray-200 rounded p-4">
        <h2 className="text-sm font-medium text-gray-700 mb-3">Summary</h2>
        <div className="grid grid-cols-4 gap-4 text-sm">
          <div><p className="text-xs text-gray-500">Total Rows</p><p className="font-medium">{summary.totalRows ?? '—'}</p></div>
          <div><p className="text-xs text-gray-500">Valid</p><p className="font-medium text-green-600">{summary.validRows ?? '—'}</p></div>
          <div><p className="text-xs text-gray-500">Invalid</p><p className="font-medium text-red-600">{summary.invalidRows ?? '—'}</p></div>
          <div><p className="text-xs text-gray-500">Imported</p><p className="font-medium text-purple-600">{summary.importedRows ?? '—'}</p></div>
        </div>
        <div className="mt-3 pt-3 border-t border-gray-100 grid grid-cols-3 gap-4 text-sm">
          <div><p className="text-xs text-gray-500">Job Status</p><p className="font-medium">{summary.jobStatus}</p></div>
          <div><p className="text-xs text-gray-500">Review Status</p><p className="font-medium">{summary.reviewStatus || '—'}</p></div>
          <div><p className="text-xs text-gray-500">Generated At</p><p className="font-medium">{report?.generatedAt ? new Date(report.generatedAt).toLocaleString() : '—'}</p></div>
        </div>
      </div>

      {/* Issue stats */}
      {issueStats.totalIssues > 0 && (
        <div className="bg-white border border-gray-200 rounded p-4">
          <h2 className="text-sm font-medium text-gray-700 mb-3">Issue Statistics</h2>
          <div className="grid grid-cols-4 gap-4 text-sm">
            <div><p className="text-xs text-gray-500">Total Issues</p><p className="font-medium">{issueStats.totalIssues}</p></div>
            <div><p className="text-xs text-gray-500">Errors</p><p className="font-medium text-red-600">{issueStats.errors}</p></div>
            <div><p className="text-xs text-gray-500">Warnings</p><p className="font-medium text-yellow-600">{issueStats.warnings}</p></div>
            <div><p className="text-xs text-gray-500">Rows with Errors</p><p className="font-medium text-red-500">{issueStats.rowsWithErrors}</p></div>
          </div>
        </div>
      )}

      {/* Issues found */}
      {issues.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Issues Found</h2>
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Row</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Type</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Severity</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Message</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {issues.map((issue, i) => (
                  <tr key={i} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-500">{issue.rowNumber}</td>
                    <td className="px-3 py-2 text-xs text-gray-600">{issue.issueType}</td>
                    <td className="px-3 py-2">
                      {issue.severity === 'ERROR'
                        ? <span className="text-xs bg-red-100 text-red-700 px-1.5 py-0.5 rounded">ERROR</span>
                        : <span className="text-xs bg-yellow-100 text-yellow-700 px-1.5 py-0.5 rounded">WARN</span>
                      }
                    </td>
                    <td className="px-3 py-2 text-gray-700">{issue.message}</td>
                    <td className="px-3 py-2 text-gray-500 text-xs">{issue.recommendedAction || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Imported records */}
      {imported.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">
            Imported Records ({imported.length})
          </h2>
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Row</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Description</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Date</th>
                  <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Amount</th>
                  <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Expense ID</th>
                </tr>
              </thead>
              <tbody>
                {imported.map((r, i) => (
                  <tr key={i} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-500">{r.rowNumber}</td>
                    <td className="px-3 py-2 text-gray-800">{r.description}</td>
                    <td className="px-3 py-2 text-gray-500">{r.date}</td>
                    <td className="px-3 py-2 text-right">{r.currency} {parseFloat(r.amount).toFixed(2)}</td>
                    <td className="px-3 py-2 text-right text-gray-400">#{r.expenseId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Skipped records */}
      {skipped.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">
            Skipped Records ({skipped.length})
          </h2>
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Row</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Description</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Status</th>
                  <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Issues</th>
                </tr>
              </thead>
              <tbody>
                {skipped.map((r, i) => (
                  <tr key={i} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-500">{r.rowNumber}</td>
                    <td className="px-3 py-2 text-gray-600">{r.description}</td>
                    <td className="px-3 py-2 text-xs text-gray-500">{r.status}</td>
                    <td className="px-3 py-2 text-right text-red-500">{r.issuesCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}
