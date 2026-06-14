import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getGroups, createGroup, deleteGroup } from '../api/client'
import Spinner from '../components/Spinner'

export default function Groups() {
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  useEffect(() => { loadGroups() }, [])

  async function loadGroups() {
    try {
      const res = await getGroups()
      setGroups(res.data)
    } catch {
      setError('Failed to load groups')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    setFormError('')
    setSubmitting(true)
    try {
      await createGroup(name, description)
      setName('')
      setDescription('')
      setShowForm(false)
      loadGroups()
    } catch (err) {
      setFormError(err.response?.data?.error || 'Failed to create group')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(id) {
    if (!confirm('Delete this group? This cannot be undone.')) return
    try {
      await deleteGroup(id)
      setGroups((prev) => prev.filter((g) => g.id !== id))
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to delete group')
    }
  }

  if (loading) return <Spinner />

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-800">Groups</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 text-white text-sm px-3 py-1.5 rounded hover:bg-blue-700"
        >
          {showForm ? 'Cancel' : '+ New Group'}
        </button>
      </div>

      {error && <p className="text-red-600 text-sm">{error}</p>}

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white border border-gray-200 rounded p-4 space-y-3">
          <h2 className="text-sm font-medium text-gray-700">Create New Group</h2>
          {formError && <p className="text-red-600 text-sm">{formError}</p>}
          <div>
            <label className="block text-sm text-gray-600 mb-1">Name *</label>
            <input
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Description</label>
            <input
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="bg-blue-600 text-white text-sm px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? 'Creating…' : 'Create Group'}
          </button>
        </form>
      )}

      {groups.length === 0 ? (
        <p className="text-sm text-gray-500">No groups yet. Create one to get started.</p>
      ) : (
        <div className="bg-white border border-gray-200 rounded overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left px-4 py-2 text-xs text-gray-500 font-medium">Name</th>
                <th className="text-left px-4 py-2 text-xs text-gray-500 font-medium">Description</th>
                <th className="text-right px-4 py-2 text-xs text-gray-500 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {groups.map((g) => (
                <tr key={g.id} className="border-b border-gray-50 last:border-0">
                  <td className="px-4 py-2.5">
                    <Link to={`/groups/${g.id}`} className="font-medium text-blue-600 hover:underline">
                      {g.name}
                    </Link>
                  </td>
                  <td className="px-4 py-2.5 text-gray-500">{g.description || '—'}</td>
                  <td className="px-4 py-2.5 text-right space-x-3">
                    <Link to={`/groups/${g.id}`} className="text-gray-600 hover:text-blue-600 text-xs">
                      View
                    </Link>
                    <button
                      onClick={() => handleDelete(g.id)}
                      className="text-red-500 hover:text-red-700 text-xs"
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
    </div>
  )
}
