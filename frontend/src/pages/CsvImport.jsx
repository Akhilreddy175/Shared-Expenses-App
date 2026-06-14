import { useEffect, useRef, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  getImportJobs,
  uploadCsv,
  deleteImportJob,
  submitForReview,
  getGroup,
} from '../api/client'
import Spinner from '../components/Spinner'

function statusBadge(status) {
  const cls = {
    PENDING: 'bg-yellow-50 text-yellow-800 border-yellow-200',
    PROCESSING: 'bg-blue-50 text-blue-800 border-blue-200',
    PROCESSED: 'bg-green-50 text-green-800 border-green-200',
    FAILED: 'bg-red-50 text-red-800 border-red-200',
    IMPORTED: 'bg-purple-50 text-purple-800 border-purple-200',
    PENDING_REVIEW: 'bg-amber-50 text-amber-800 border-amber-200',
  }[status] || 'bg-slate-50 text-slate-800 border-slate-200'

  return (
    <span className={`inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded border uppercase tracking-wider ${cls}`}>
      {status.replace('_', ' ')}
    </span>
  )
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
    if (!file) {
      setError('Please select a valid CSV file')
      return
    }
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
      setError(err.response?.data?.error || 'CSV Upload failed. Ensure the format matches the guidelines.')
    } finally {
      setUploading(false)
    }
  }

  async function handleDelete(jobId) {
    if (!confirm('Are you sure you want to delete this import job? This deletes all associated review records.')) return
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
        <Link to={`/groups/${groupId}`} className="inline-flex items-center text-xs text-slate-500 hover:text-slate-900 hover:underline mb-1">
          ← {group?.name || 'Back to Group'}
        </Link>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">CSV Import</h1>
        <p className="text-xs text-slate-500 mt-1">Batch import expenses using formatted CSV spreadsheets</p>
      </div>


      <div className="bg-white border border-slate-200 rounded p-6 space-y-4">
        <div>
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Upload CSV Document</h2>
          <p className="text-[10px] text-slate-500 mt-0.5">CSV must contain headers: Description, Amount, Date, SplitType, Category, and split values.</p>
        </div>

        <form onSubmit={handleUpload} className="flex flex-col sm:flex-row sm:items-center gap-3">
          <input
            ref={fileRef}
            type="file"
            accept=".csv"
            className="text-xs text-slate-500 bg-slate-50 border border-slate-200 rounded px-3 py-2 flex-1 cursor-pointer focus:outline-none file:mr-3 file:py-1 file:px-2 file:border file:border-slate-300 file:rounded file:text-[10px] file:font-bold file:uppercase file:bg-white file:text-slate-700 hover:file:bg-slate-50"
          />
          <button
            type="submit"
            disabled={uploading}
            className="bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-4 py-2 rounded transition-colors disabled:opacity-50 shrink-0 cursor-pointer flex items-center justify-center gap-2"
          >
            {uploading ? 'Uploading…' : 'Upload Document'}
          </button>
        </form>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded text-xs">
            {error}
          </div>
        )}


        {uploadResult && (
          <div className="bg-green-50 border border-green-200 rounded p-4 space-y-2">
            <p className="text-xs font-bold text-green-800">
              Upload Complete: {uploadResult.filename}
            </p>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs text-green-700 pt-1">
              <div><span className="font-semibold block text-[10px] uppercase text-slate-400">Total Rows</span>{uploadResult.totalRows}</div>
              <div><span className="font-semibold block text-[10px] uppercase text-green-600">Valid Rows</span>{uploadResult.validRows}</div>
              <div><span className="font-semibold block text-[10px] uppercase text-red-600">Invalid Rows</span>{uploadResult.invalidRows}</div>
              <div><span className="font-semibold block text-[10px] uppercase text-slate-400">Parsed Status</span>{uploadResult.status}</div>
            </div>
          </div>
        )}
      </div>


      <section className="bg-white border border-slate-200 rounded p-5 space-y-3">
        <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">
          Import History
        </h2>

        {jobs.length === 0 ? (
          <p className="text-xs text-slate-500 text-center py-6">No import jobs recorded yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-2.5 text-[10px] text-slate-500 font-bold uppercase tracking-wider">File Name</th>
                  <th className="px-4 py-2.5 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Status</th>
                  <th className="px-4 py-2.5 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-center">Total Rows</th>
                  <th className="px-4 py-2.5 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-center">Valid</th>
                  <th className="px-4 py-2.5 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-center">Invalid</th>
                  <th className="px-4 py-2.5 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {jobs.map((j) => (
                  <tr key={j.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3 font-semibold text-slate-800">{j.filename}</td>
                    <td className="px-4 py-3">{statusBadge(j.status)}</td>
                    <td className="px-4 py-3 text-center text-slate-500 font-medium">{j.totalRows ?? '—'}</td>
                    <td className="px-4 py-3 text-center text-green-600 font-bold">{j.validRows ?? '—'}</td>
                    <td className="px-4 py-3 text-center text-red-500 font-bold">{j.invalidRows ?? '—'}</td>
                    <td className="px-4 py-3 text-right space-x-2 whitespace-nowrap">
                      {j.status === 'PROCESSED' && (
                        <button
                          onClick={() => handleSubmitReview(j.id)}
                          className="bg-slate-800 hover:bg-slate-900 text-white text-[10px] font-bold px-2 py-1 rounded cursor-pointer"
                        >
                          Submit
                        </button>
                      )}
                      <Link
                        to={`/groups/${groupId}/imports/${j.id}/review`}
                        className="text-xs font-semibold text-slate-650 hover:underline"
                      >
                        Review
                      </Link>
                      <Link
                        to={`/groups/${groupId}/imports/${j.id}/report`}
                        className="text-xs font-semibold text-slate-650 hover:underline"
                      >
                        Report
                      </Link>
                      <button
                        onClick={() => handleDelete(j.id)}
                        className="text-xs font-semibold text-red-600 hover:text-red-800 hover:underline cursor-pointer"
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
