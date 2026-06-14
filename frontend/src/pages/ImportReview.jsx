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
    PENDING: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
  }[status] || 'bg-gray-100 text-gray-700'
  return <span className={`text-xs px-2 py-0.5 rounded font-medium ${cls}`}>{status}</span>
}

function severityBadge(severity) {
  return severity === 'ERROR'
    ? <span className="text-xs bg-red-100 text-red-700 px-1.5 py-0.5 rounded">ERROR</span>
    : <span className="text-xs bg-yellow-100 text-yellow-700 px-1.5 py-0.5 rounded">WARN</span>
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

      // Try loading review (may not exist yet)
      try {
        const revRes = await getReview(groupId, jobId)
        setReview(revRes.data)
      } catch {
        setReview(null)
      }
    } catch {
      setError('Failed to load review data')
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
  if (error && !job) return <p className="text-red-600 text-sm">{error}</p>

  const issues = report?.issuesFound || []
  const reviewStatus = review?.reviewStatus

  return (
    <div className="space-y-6">
      <div>
        <Link to={`/groups/${groupId}/imports`} className="text-xs text-gray-400 hover:underline">← CSV Import</Link>
        <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Import Review</h1>
        <p className="text-sm text-gray-500">{job?.filename}</p>
      </div>

      {error && <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded">{error}</p>}

      {/* Job summary */}
      <div className="bg-white border border-gray-200 rounded p-4">
        <h2 className="text-sm font-medium text-gray-700 mb-3">Import Summary</h2>
        <div className="grid grid-cols-4 gap-4 text-sm">
          <div>
            <p className="text-xs text-gray-500">Total Rows</p>
            <p className="font-medium">{job?.totalRows ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Valid</p>
            <p className="font-medium text-green-600">{job?.validRows ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Invalid</p>
            <p className="font-medium text-red-600">{job?.invalidRows ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Job Status</p>
            <p className="font-medium">{job?.status}</p>
          </div>
        </div>
        {review && (
          <div className="mt-3 pt-3 border-t border-gray-100 flex items-center gap-2">
            <span className="text-xs text-gray-500">Review Status:</span>
            {reviewBadge(reviewStatus)}
            {review.note && <span className="text-xs text-gray-500 ml-2">Note: {review.note}</span>}
          </div>
        )}
      </div>

      {/* Detected anomalies */}
      {issues.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">
            Detected Anomalies ({issues.length})
          </h2>
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Row</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Severity</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Type</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Message</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Recommended Action</th>
                </tr>
              </thead>
              <tbody>
                {issues.map((issue, i) => (
                  <tr key={i} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-500">{issue.rowNumber}</td>
                    <td className="px-3 py-2">{severityBadge(issue.severity)}</td>
                    <td className="px-3 py-2 text-gray-600 text-xs">{issue.issueType}</td>
                    <td className="px-3 py-2 text-gray-700">{issue.message}</td>
                    <td className="px-3 py-2 text-gray-500 text-xs">{issue.recommendedAction || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Review actions */}
      <section>
        <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-3">Actions</h2>
        <div className="bg-white border border-gray-200 rounded p-4 space-y-3">
          {!review && job?.status !== 'IMPORTED' && (
            <div>
              <p className="text-sm text-gray-600 mb-2">Submit this import for review.</p>
              <button
                onClick={() => doAction('submit')}
                disabled={!!actionLoading}
                className="bg-blue-600 text-white text-sm px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
              >
                {actionLoading === 'submit' ? 'Submitting…' : 'Submit for Review'}
              </button>
            </div>
          )}

          {reviewStatus === 'PENDING' && (
            <>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Note (optional)</label>
                <input
                  className="border border-gray-300 rounded px-3 py-2 text-sm w-full max-w-md focus:outline-none focus:border-blue-500"
                  placeholder="Add a note…"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => doAction('approve')}
                  disabled={!!actionLoading}
                  className="bg-green-600 text-white text-sm px-4 py-2 rounded hover:bg-green-700 disabled:opacity-50"
                >
                  {actionLoading === 'approve' ? 'Approving…' : 'Approve'}
                </button>
                <button
                  onClick={() => doAction('reject')}
                  disabled={!!actionLoading}
                  className="bg-red-600 text-white text-sm px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50"
                >
                  {actionLoading === 'reject' ? 'Rejecting…' : 'Reject'}
                </button>
              </div>
            </>
          )}

          {reviewStatus === 'APPROVED' && job?.status !== 'IMPORTED' && (
            <div>
              <p className="text-sm text-green-700 mb-2">✓ Review approved. Ready to import.</p>
              <button
                onClick={() => doAction('confirm')}
                disabled={!!actionLoading}
                className="bg-purple-600 text-white text-sm px-4 py-2 rounded hover:bg-purple-700 disabled:opacity-50"
              >
                {actionLoading === 'confirm' ? 'Importing…' : 'Confirm & Import'}
              </button>
            </div>
          )}

          {reviewStatus === 'REJECTED' && (
            <div>
              <p className="text-sm text-red-600 mb-2">✗ Review rejected. Fix issues and resubmit.</p>
              <button
                onClick={() => doAction('resubmit')}
                disabled={!!actionLoading}
                className="bg-yellow-600 text-white text-sm px-4 py-2 rounded hover:bg-yellow-700 disabled:opacity-50"
              >
                {actionLoading === 'resubmit' ? 'Resubmitting…' : 'Resubmit'}
              </button>
            </div>
          )}

          {job?.status === 'IMPORTED' && (
            <p className="text-sm text-purple-700 font-medium">✓ Import completed successfully.</p>
          )}

          <div className="pt-2 border-t border-gray-100">
            <Link
              to={`/groups/${groupId}/imports/${jobId}/report`}
              className="text-sm text-blue-600 hover:underline"
            >
              View Full Report →
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
