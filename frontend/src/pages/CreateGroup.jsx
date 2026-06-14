import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { createGroup } from '../api/client'
import { ChevronDown, ArrowLeft } from 'lucide-react'

export default function CreateGroup() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await createGroup(name, description)
      navigate(`/groups/${res.data.id}`)
    } catch (err) {
      setError(err.response?.data?.error || "Failed to create group.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="animate-fadeIn max-w-4xl space-y-6 text-gray-900">
      <div>
        <Link to="/groups" className="inline-flex items-center text-xs text-gray-400 hover:text-blue-600 hover:underline mb-1">
          <ArrowLeft size={14} className="mr-1" /> Back to Groups
        </Link>
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Create New Group</h1>
        <p className="text-xs text-gray-500 mt-1">Start a new household or travel expense sheet</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 p-3.5 rounded-xl text-sm">
          {error}
        </div>
      )}

      <form className="space-y-6 max-w-2xl bg-white border border-gray-200 rounded-2xl p-6 shadow-xs" onSubmit={handleSubmit}>
        <div>
          <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Group Name *</label>
          <input
            type="text"
            placeholder="e.g. Roommates 202, Eurotrip"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-sm"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Group Description</label>
          <textarea
            placeholder="Add description of what this group splits"
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all resize-none text-sm"
          ></textarea>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <Link
            to="/groups"
            className="bg-gray-100 hover:bg-gray-250 text-gray-700 text-sm font-semibold px-6 py-2.5 rounded-lg transition-all"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg shadow-sm disabled:opacity-50 transition-colors cursor-pointer"
          >
            {loading ? 'Creating...' : 'Create Group'}
          </button>
        </div>
      </form>
    </div>
  )
}
