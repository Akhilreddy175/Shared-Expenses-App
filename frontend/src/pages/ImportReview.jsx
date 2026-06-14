import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  getImportJob,
  getReview,
  getFullReport,
  submitForReview,
  approveReview,
  rejectReview,
  resubmitReview,
  confirmImport,
  getGroup,
} from '../api/client'
import Spinner from '../components/Spinner'

function reviewBadge(status) {
  const cls = {
    PENDING: 'bg-yellow-50 text-yellow-800 border-yellow-250',
    APPROVED: 'bg-green-50 text-green-800 border-green-250',
    REJECTED: 'bg-red-50 text-red-800 border-red-250',
  }[status] || 'bg-slate-50 text-slate-800 border-slate-200'

  return (
    <span className={`inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded border uppercase tracking-wider ${cls}`}>
      {status}
    </span>
  )
}

function severityBadge(severity) {
  return severity === 'ERROR'
    ? <span className="inline-flex items-center text-[9px] font-bold bg-red-50 text-red-800 px-1.5 py-0.5 rounded border border-red-200">ERROR</span>
    : <span className="inline-flex items-center text-[9px] font-bold bg-amber-50 text-amber-850 px-1.5 py-0.5 rounded border border-amber-200">WARN</span>
}

export default function ImportReview() {
  const { id: groupId, jobId } = useParams()
  const [group, setGroup] = useState(null)
  const [job, setJob] = useState(null)
  const [review, setReview] = useState(null)
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [note, setNote] = useState('')
  const [actionLoading, setActionLoading] = useState('')

  useEffect(() => { loadData() }, [groupId, jobId])

  async function loadData() {
    setLoading(true)
    try {
      const [grRes, jobRes, reportRes] = await Promise.all([
        getGroup(groupId),
        getImportJob(groupId, jobId),
        getFullReport(groupId, jobId),
      ])
      setGroup(grRes.data)
      setJob(jobRes.data)
      setReport(reportRes.data)


      try {
        const revRes = await getReview(groupId, jobId)
        setReview(revRes.data)
      } catch {
        setReview(null)
      }
    } catch {
      setError('Failed to load import review data')
    } finally {
      setLoading(false)
    }
  }

  async function doAction(action) {
    setActionLoading(action)
    setError('')
    try {
      switch (action) {
        case 'submit': await submitForReview(groupId, jobId); break
        case 'approve': await approveReview(groupId, jobId, note); break
        case 'reject': await rejectReview(groupId, jobId, note); break
        case 'resubmit': await resubmitReview(groupId, jobId); break
        case 'confirm': await confirmImport(groupId, jobId); break
      }
      setNote('')
      loadData()
    } catch (err) {
      setError(err.response?.data?.error || `Action "${action}" failed`)
    } finally {
      setActionLoading('')
    }
  }

  if (loading) return <Spinner />

  const issues = report?.issuesFound || []
  const reviewStatus = review?.reviewStatus

  return (
    <div className="space-y-6">


      <div>
        <Link to={`/groups/${groupId}/imports`} className="inline-flex items-center text-xs text-slate-500 hover:text-slate-900 hover:underline mb-1">
          ← CSV Import
        </Link>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">Import Review</h1>
        <p className="text-xs text-slate-500 mt-1">{job?.filename} • Group: {group?.name}</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-xs">
          {error}
        </div>
      )}


      <div className="bg-white border border-slate-200 rounded p-5 space-y-4">
        <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">File Processing Summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-1">
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Total Rows</span>
            <span className="text-sm font-bold text-slate-800 mt-0.5 block">{job?.totalRows ?? '—'}</span>
          </div>
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Valid Rows</span>
            <span className="text-sm font-bold text-green-700 mt-0.5 block">{job?.validRows ?? '—'}</span>
          </div>
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Invalid Rows</span>
            <span className="text-sm font-bold text-red-700 mt-0.5 block">{job?.invalidRows ?? '—'}</span>
          </div>
          <div className="p-3 bg-slate-50 rounded border border-slate-200">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Workflow Status</span>
            <span className="text-xs font-bold text-slate-800 mt-1 block">{job?.status}</span>
          </div>
        </div>

        {review && (
          <div className="mt-3 pt-3 flex flex-wrap items-center gap-3 border-t border-slate-100">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Review Decision:</span>
            {reviewBadge(reviewStatus)}
            {review.note && (
              <span className="text-xs text-slate-600 bg-slate-50 rounded px-3 py-1 border border-slate-200 block max-w-full truncate">
                Note: {review.note}
              </span>
            )}
          </div>
        )}
      </div>


      {issues.length > 0 && (
        <section className="bg-white border border-slate-200 rounded p-5 space-y-3">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-105">
            Detected Anomalies & Warnings ({issues.length})
          </h2>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Row</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Severity</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Anomaly Type</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Description Message</th>
                  <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Recommended Correction</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {issues.map((issue, i) => (
                  <tr key={i} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3 text-slate-500 font-semibold">{issue.rowNumber}</td>
                    <td className="px-4 py-3">{severityBadge(issue.severity)}</td>
                    <td className="px-4 py-3 text-slate-700 font-semibold whitespace-nowrap">{issue.issueType}</td>
                    <td className="px-4 py-3 text-slate-800 leading-normal">{issue.message}</td>
                    <td className="px-4 py-3 text-slate-500 leading-normal font-medium">{issue.recommendedAction || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}


      <section className="bg-white border border-slate-200 rounded p-6 space-y-4">
        <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">
          Workflow Action Panel
        </h2>

        <div className="space-y-4">
          {!review && job?.status !== 'IMPORTED' && (
            <div className="space-y-2">
              <p className="text-xs text-slate-600">Submit this processed CSV import file for peer group approval review.</p>
              <button
                onClick={() => doAction('submit')}
                disabled={!!actionLoading}
                className="bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-4 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
              >
                {actionLoading === 'submit' ? 'Submitting…' : 'Submit for Review'}
              </button>
            </div>
          )}

          {reviewStatus === 'PENDING' && (
            <div className="space-y-3.5 max-w-md">
              <div>
                <label className="block text-xs font-semibold text-slate-655 mb-1.5">Approver's Notes (Optional)</label>
                <input
                  className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
                  placeholder="e.g. Verified, duplicate amounts look fine"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => doAction('approve')}
                  disabled={!!actionLoading}
                  className="bg-green-600 hover:bg-green-700 text-white text-xs font-bold px-4 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
                >
                  Approve Import
                </button>
                <button
                  onClick={() => doAction('reject')}
                  disabled={!!actionLoading}
                  className="bg-red-600 hover:bg-red-700 text-white text-xs font-bold px-4 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
                >
                  Reject Import
                </button>
              </div>
            </div>
          )}

          {reviewStatus === 'APPROVED' && job?.status !== 'IMPORTED' && (
            <div className="space-y-2">
              <p className="text-xs text-green-700 font-semibold">
                Import review approved. Click below to confirm and finalize importing expense records.
              </p>
              <button
                onClick={() => doAction('confirm')}
                disabled={!!actionLoading}
                className="bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold px-5 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
              >
                Confirm & Import Expenses
              </button>
            </div>
          )}

          {reviewStatus === 'REJECTED' && (
            <div className="space-y-2">
              <p className="text-xs text-red-700 font-semibold">
                Review rejected by peer. Correct anomalies and click below to resubmit for review.
              </p>
              <button
                onClick={() => doAction('resubmit')}
                disabled={!!actionLoading}
                className="bg-slate-850 hover:bg-slate-900 text-white text-xs font-bold px-5 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
              >
                Resubmit for Review
              </button>
            </div>
          )}

          {job?.status === 'IMPORTED' && (
            <p className="text-xs text-slate-800 font-semibold bg-slate-50 rounded p-3.5 border border-slate-200">
              This import job has been finalized and imported into the group expense sheet.
            </p>
          )}

          <div className="pt-4 border-t border-slate-100 flex items-center">
            <Link
              to={`/groups/${groupId}/imports/${jobId}/report`}
              className="text-xs font-bold text-slate-800 hover:underline"
            >
              View Full Audit Report →
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
