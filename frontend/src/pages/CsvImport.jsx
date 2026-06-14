import { useEffect, useRef, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  getImportJobs,
  uploadCsv,
  deleteImportJob,
  getFullReport,
  submitForReview,
  getGroup,
} from '../api/client'
import Spinner from '../components/Spinner'

function statusBadge(status) {
  const cls = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    PROCESSING: 'bg-blue-100 text-blue-800',
    PROCESSED: 'bg-green-100 text-green-800',
    FAILED: 'bg-red-100 text-red-800',
    IMPORTED: 'bg-purple-100 text-purple-800',
    PENDING_REVIEW: 'bg-orange-100 text-orange-800',
  }[status] || 'bg-gray-100 text-gray-700'
  return <span className={`text-xs px-2 py-0.5 rounded ${cls}`}>{status}</span>
}

export default function CsvImport() {
  const { id: groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [uploadResult, setUploadResult] = useState(null)
  const fileRef = useRef(null)

  useEffect(() => { loadData() }, [groupId])

  async function loadData() {
    try {
      const [grRes, jobsRes] = await Promise.all([getGroup(groupId), getImportJobs(groupId)])
      setGroup(grRes.data)
      setJobs(jobsRes.data)
    } catch {
      setError('Failed to load import jobs')
    } finally {
      setLoading(false)
    }
  }

  async function handleUpload(e) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) { setError('Select a CSV file'); return }
    setError('')
    setUploadResult(null)
    setUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res = await uploadCsv(groupId, formData)
      setUploadResult(res.data)
      fileRef.current.value = ''
      loadData()
    } catch (err) {
      setError(err.response?.data?.error || 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  async function handleDelete(jobId) {
    if (!confirm('Delete this import job?')) return
    try {
      await deleteImportJob(groupId, jobId)
      setJobs((prev) => prev.filter((j) => j.id !== jobId))
    } catch (err) {
      alert(err.response?.data?.error || 'Delete failed')
    }
  }

  async function handleSubmitReview(jobId) {
    try {
      await submitForReview(groupId, jobId)
      loadData()
    } catch (err) {
      alert(err.response?.data?.error || 'Submit failed')
    }
  }

  if (loading) return <Spinner />

  return (
    <div className="space-y-6">
      <div>
        <Link to={`/groups/${groupId}`} className="text-xs text-gray-400 hover:underline">← {group?.name}</Link>
        <h1 className="text-xl font-semibold text-gray-800 mt-0.5">CSV Import</h1>
      </div>

      {/* Upload form */}
      <div className="bg-white border border-gray-200 rounded p-4">
        <h2 className="text-sm font-medium text-gray-700 mb-3">Upload CSV</h2>
        <form onSubmit={handleUpload} className="flex items-center gap-3">
          <input
            ref={fileRef}
            type="file"
            accept=".csv"
            className="text-sm text-gray-600 file:mr-2 file:py-1.5 file:px-3 file:rounded file:border file:border-gray-300 file:text-sm file:text-gray-600 file:bg-white hover:file:bg-gray-50"
          />
          <button
            type="submit"
            disabled={uploading}
            className="bg-blue-600 text-white text-sm px-4 py-1.5 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {uploading ? 'Uploading…' : 'Upload'}
          </button>
        </form>
        {error && <p className="text-red-600 text-sm mt-2">{error}</p>}

        {/* Upload result summary */}
        {uploadResult && (
          <div className="mt-3 bg-green-50 border border-green-200 rounded p-3">
            <p className="text-sm font-medium text-green-800 mb-1">Upload complete: {uploadResult.filename}</p>
            <div className="grid grid-cols-4 gap-2 text-xs text-green-700">
              <span>Total: {uploadResult.totalRows}</span>
              <span>Valid: {uploadResult.validRows}</span>
              <span>Invalid: {uploadResult.invalidRows}</span>
              <span>Status: {uploadResult.status}</span>
            </div>
          </div>
        )}
      </div>

      {/* Import jobs list */}
      <section>
        <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Import History</h2>
        {jobs.length === 0 ? (
          <p className="text-sm text-gray-500">No imports yet.</p>
        ) : (
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">File</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Status</th>
                  <th className="text-center px-3 py-2 text-xs text-gray-500 font-medium">Rows</th>
                  <th className="text-center px-3 py-2 text-xs text-gray-500 font-medium">Valid</th>
                  <th className="text-center px-3 py-2 text-xs text-gray-500 font-medium">Invalid</th>
                  <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((j) => (
                  <tr key={j.id} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2.5 text-gray-800 font-medium">{j.filename}</td>
                    <td className="px-3 py-2.5">{statusBadge(j.status)}</td>
                    <td className="px-3 py-2.5 text-center text-gray-500">{j.totalRows ?? '—'}</td>
                    <td className="px-3 py-2.5 text-center text-green-600">{j.validRows ?? '—'}</td>
                    <td className="px-3 py-2.5 text-center text-red-500">{j.invalidRows ?? '—'}</td>
                    <td className="px-3 py-2.5 text-right space-x-2">
                      {j.status === 'PROCESSED' && (
                        <button
                          onClick={() => handleSubmitReview(j.id)}
                          className="text-xs text-blue-600 hover:underline"
                        >
                          Submit for Review
                        </button>
                      )}
                      <Link
                        to={`/groups/${groupId}/imports/${j.id}/review`}
                        className="text-xs text-gray-500 hover:text-blue-600"
                      >
                        Review
                      </Link>
                      <Link
                        to={`/groups/${groupId}/imports/${j.id}/report`}
                        className="text-xs text-gray-500 hover:text-blue-600"
                      >
                        Report
                      </Link>
                      <button
                        onClick={() => handleDelete(j.id)}
                        className="text-xs text-red-500 hover:text-red-700"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
