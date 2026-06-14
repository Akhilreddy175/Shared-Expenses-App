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

  if (error) {
    return (
      <div className="space-y-4">
        <Link to={`/groups/${groupId}/imports/${jobId}/review`} className="text-xs text-slate-500 hover:text-slate-900 hover:underline">← Back to Review</Link>
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-xs max-w-lg">
          {error}
        </div>
      </div>
    )
  }

  const summary = report?.summary || {}
  const issueStats = report?.issueStats || {}
  const issues = report?.issuesFound || []
  const imported = report?.importedRecords || []
  const skipped = report?.skippedRecords || []

  return (
    <div className="space-y-6">


      <div className="flex items-start justify-between pb-4 border-b border-slate-200">
        <div>
          <Link to={`/groups/${groupId}/imports/${jobId}/review`} className="inline-flex items-center text-xs text-slate-500 hover:text-slate-900 hover:underline mb-1">
            ← Review
          </Link>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">Import Report</h1>
          <p className="text-xs text-slate-500 mt-1">{summary.filename} • Group: {group?.name}</p>
        </div>
        <button
          onClick={handleDownload}
          className="text-xs font-bold border border-slate-200 hover:border-slate-800 hover:bg-slate-50 px-3.5 py-2 rounded transition-colors cursor-pointer"
        >
          Download JSON
        </button>
      </div>


      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">


        <div className="bg-white border border-slate-200 rounded p-5 space-y-4">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Summary Statistics</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Total Rows</span>
              <span className="text-sm font-bold text-slate-800 mt-0.5 block">{summary.totalRows ?? '—'}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Valid Rows</span>
              <span className="text-sm font-bold text-green-700 mt-0.5 block">{summary.validRows ?? '—'}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Invalid Rows</span>
              <span className="text-sm font-bold text-red-700 mt-0.5 block">{summary.invalidRows ?? '—'}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Imported Rows</span>
              <span className="text-sm font-bold text-purple-700 mt-0.5 block">{summary.importedRows ?? '—'}</span>
            </div>
          </div>
          <div className="pt-2 border-t border-slate-100 grid grid-cols-2 gap-3 text-xs text-slate-550">
            <div><span className="font-semibold block text-[10px] text-slate-400 uppercase tracking-wider">Workflow status</span>{summary.jobStatus}</div>
            <div><span className="font-semibold block text-[10px] text-slate-400 uppercase tracking-wider">Generated At</span>{report?.generatedAt ? new Date(report.generatedAt).toLocaleString() : '—'}</div>
          </div>
        </div>


        <div className="bg-white border border-slate-200 rounded p-5 space-y-4">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Issue Diagnostics</h2>
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Total Anomalies</span>
              <span className="text-sm font-bold text-slate-800 mt-0.5 block">{issueStats.totalIssues ?? 0}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Errors</span>
              <span className="text-sm font-bold text-red-700 mt-0.5 block">{issueStats.errors ?? 0}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Warnings</span>
              <span className="text-sm font-bold text-amber-600 mt-0.5 block">{issueStats.warnings ?? 0}</span>
            </div>
            <div className="p-3 bg-slate-50 rounded border border-slate-200">
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Impacted Rows</span>
              <span className="text-sm font-bold text-red-600 mt-0.5 block">{issueStats.rowsWithErrors ?? 0}</span>
            </div>
          </div>
          <div className="pt-2 border-t border-slate-100 text-xs text-slate-550">
            <div><span className="font-semibold block text-[10px] text-slate-400 uppercase tracking-wider">Approval Status</span>{summary.reviewStatus || '—'}</div>
          </div>
        </div>

      </div>


      {issues.length > 0 && (
        <section className="bg-white border border-slate-200 rounded p-5 space-y-3">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">
            Identified Issues ({issues.length})
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Row</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Anomaly Type</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Severity</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Message Description</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Correction Advised</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {issues.map((issue, i) => (
                  <tr key={i} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3 text-slate-500 font-semibold">{issue.rowNumber}</td>
                    <td className="px-4 py-3 text-slate-700 font-semibold whitespace-nowrap">{issue.issueType}</td>
                    <td className="px-4 py-3">
                      {issue.severity === 'ERROR'
                        ? <span className="inline-flex items-center text-[9px] font-bold bg-red-50 text-red-800 px-1.5 py-0.5 rounded border border-red-200">ERROR</span>
                        : <span className="inline-flex items-center text-[9px] font-bold bg-amber-50 text-amber-800 px-1.5 py-0.5 rounded border border-amber-200">WARN</span>
                      }
                    </td>
                    <td className="px-4 py-3 text-slate-800 leading-normal">{issue.message}</td>
                    <td className="px-4 py-3 text-slate-500 leading-normal font-medium">{issue.recommendedAction || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}


      {imported.length > 0 && (
        <section className="bg-white border border-slate-200 rounded p-5 space-y-3">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">
            Imported Records ({imported.length})
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Row</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Description</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Date</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Amount</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Created Expense ID</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {imported.map((r, i) => (
                  <tr key={i} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3 text-slate-500 font-semibold">{r.rowNumber}</td>
                    <td className="px-4 py-3 font-semibold text-slate-800">{r.description}</td>
                    <td className="px-4 py-3 text-slate-500 font-semibold">{r.date}</td>
                    <td className="px-4 py-3 text-right font-bold text-slate-900">{r.currency} {parseFloat(r.amount).toFixed(2)}</td>
                    <td className="px-4 py-3 text-right text-xs text-slate-400 font-semibold">#{r.expenseId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}


      {skipped.length > 0 && (
        <section className="bg-white border border-slate-200 rounded p-5 space-y-3">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">
            Skipped / Excluded Records ({skipped.length})
          </h2>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Row</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Description</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Reason status</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Errors Found</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {skipped.map((r, i) => (
                  <tr key={i} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3 text-slate-500 font-semibold">{r.rowNumber}</td>
                    <td className="px-4 py-3 text-slate-700 font-medium">{r.description}</td>
                    <td className="px-4 py-3 text-slate-400 font-medium">{r.status}</td>
                    <td className="px-4 py-3 text-right text-xs text-red-600 font-bold">{r.issuesCount} issues</td>
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
