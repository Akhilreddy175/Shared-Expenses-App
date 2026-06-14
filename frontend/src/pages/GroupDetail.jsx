import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import {
  getGroup,
  updateGroup,
  getMembers,
  getMemberHistory,
  addMember,
  removeMember,
} from '../api/client'
import Spinner from '../components/Spinner'

export default function GroupDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState(null)
  const [members, setMembers] = useState([])
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Edit group
  const [editMode, setEditMode] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')

  // Add member
  const [showAddMember, setShowAddMember] = useState(false)
  const [newUserId, setNewUserId] = useState('')
  const [newJoinedAt, setNewJoinedAt] = useState('')
  const [memberError, setMemberError] = useState('')

  // Remove member
  const [removingId, setRemovingId] = useState(null)
  const [removeLeftAt, setRemoveLeftAt] = useState('')

  useEffect(() => { loadAll() }, [id])

  async function loadAll() {
    try {
      const [grRes, memRes, histRes] = await Promise.all([
        getGroup(id),
        getMembers(id),
        getMemberHistory(id),
      ])
      setGroup(grRes.data)
      setEditName(grRes.data.name)
      setEditDesc(grRes.data.description || '')
      setMembers(memRes.data)
      setHistory(histRes.data)
    } catch {
      setError('Failed to load group')
    } finally {
      setLoading(false)
    }
  }

  async function handleUpdateGroup(e) {
    e.preventDefault()
    try {
      const res = await updateGroup(id, editName, editDesc)
      setGroup(res.data)
      setEditMode(false)
    } catch (err) {
      alert(err.response?.data?.error || 'Update failed')
    }
  }

  async function handleAddMember(e) {
    e.preventDefault()
    setMemberError('')
    try {
      await addMember(id, parseInt(newUserId), newJoinedAt || undefined)
      setNewUserId('')
      setNewJoinedAt('')
      setShowAddMember(false)
      loadAll()
    } catch (err) {
      setMemberError(err.response?.data?.error || 'Failed to add member')
    }
  }

  async function handleRemoveMember(userId) {
    try {
      await removeMember(id, userId, removeLeftAt || undefined)
      setRemovingId(null)
      setRemoveLeftAt('')
      loadAll()
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to remove member')
    }
  }

  if (loading) return <Spinner />
  if (error) return <p className="text-red-600 text-sm">{error}</p>

  return (
    <div className="space-y-6">
      {/* Group header */}
      <div className="flex items-start justify-between">
        <div>
          <Link to="/groups" className="text-xs text-gray-400 hover:underline">← Groups</Link>
          {editMode ? (
            <form onSubmit={handleUpdateGroup} className="mt-1 space-y-2">
              <input
                className="border border-gray-300 rounded px-2 py-1 text-sm font-medium"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                required
              />
              <input
                className="border border-gray-300 rounded px-2 py-1 text-sm block"
                placeholder="Description"
                value={editDesc}
                onChange={(e) => setEditDesc(e.target.value)}
              />
              <div className="space-x-2">
                <button type="submit" className="text-xs bg-blue-600 text-white px-3 py-1 rounded">Save</button>
                <button type="button" onClick={() => setEditMode(false)} className="text-xs text-gray-500">Cancel</button>
              </div>
            </form>
          ) : (
            <>
              <h1 className="text-xl font-semibold text-gray-800 mt-1">{group.name}</h1>
              {group.description && <p className="text-sm text-gray-500">{group.description}</p>}
            </>
          )}
        </div>
        {!editMode && (
          <button onClick={() => setEditMode(true)} className="text-xs text-gray-500 hover:text-blue-600">Edit</button>
        )}
      </div>

      {/* Quick links */}
      <div className="flex flex-wrap gap-2">
        {[
          { to: `/groups/${id}/expenses`, label: 'Expenses' },
          { to: `/groups/${id}/balances`, label: 'Balances' },
          { to: `/groups/${id}/settlements`, label: 'Settlements' },
          { to: `/groups/${id}/imports`, label: 'CSV Import' },
        ].map((link) => (
          <Link
            key={link.to}
            to={link.to}
            className="text-sm border border-gray-300 rounded px-3 py-1.5 text-gray-700 hover:border-blue-400 hover:text-blue-600"
          >
            {link.label}
          </Link>
        ))}
      </div>

      {/* Active Members */}
      <section>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-sm font-semibold text-gray-700">Active Members ({members.length})</h2>
          <button
            onClick={() => setShowAddMember(!showAddMember)}
            className="text-xs text-blue-600 hover:underline"
          >
            {showAddMember ? 'Cancel' : '+ Add Member'}
          </button>
        </div>

        {showAddMember && (
          <form onSubmit={handleAddMember} className="bg-white border border-gray-200 rounded p-3 mb-3 space-y-2">
            {memberError && <p className="text-red-600 text-xs">{memberError}</p>}
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="block text-xs text-gray-500 mb-1">User ID *</label>
                <input
                  type="number"
                  className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:border-blue-500"
                  value={newUserId}
                  onChange={(e) => setNewUserId(e.target.value)}
                  required
                />
              </div>
              <div className="flex-1">
                <label className="block text-xs text-gray-500 mb-1">Joined At</label>
                <input
                  type="date"
                  className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:border-blue-500"
                  value={newJoinedAt}
                  onChange={(e) => setNewJoinedAt(e.target.value)}
                />
              </div>
            </div>
            <button type="submit" className="text-xs bg-blue-600 text-white px-3 py-1.5 rounded">Add</button>
          </form>
        )}

        {members.length === 0 ? (
          <p className="text-sm text-gray-500">No active members.</p>
        ) : (
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Name</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Joined</th>
                  <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {members.map((m) => (
                  <tr key={m.userId} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-800">{m.displayName || `User #${m.userId}`}</td>
                    <td className="px-3 py-2 text-gray-500">{m.joinedAt || '—'}</td>
                    <td className="px-3 py-2 text-right">
                      {removingId === m.userId ? (
                        <span className="flex items-center justify-end gap-2">
                          <input
                            type="date"
                            className="border border-gray-300 rounded px-1.5 py-1 text-xs"
                            value={removeLeftAt}
                            onChange={(e) => setRemoveLeftAt(e.target.value)}
                          />
                          <button
                            onClick={() => handleRemoveMember(m.userId)}
                            className="text-xs text-red-600 hover:underline"
                          >
                            Confirm
                          </button>
                          <button onClick={() => setRemovingId(null)} className="text-xs text-gray-400">Cancel</button>
                        </span>
                      ) : (
                        <button
                          onClick={() => { setRemovingId(m.userId); setRemoveLeftAt('') }}
                          className="text-xs text-red-500 hover:text-red-700"
                        >
                          Remove
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Member history */}
      {history.some((m) => m.leftAt) && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-2">Former Members</h2>
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Name</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Joined</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Left</th>
                </tr>
              </thead>
              <tbody>
                {history.filter((m) => m.leftAt).map((m) => (
                  <tr key={`${m.userId}-${m.joinedAt}`} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-500">{m.displayName || `User #${m.userId}`}</td>
                    <td className="px-3 py-2 text-gray-400">{m.joinedAt}</td>
                    <td className="px-3 py-2 text-gray-400">{m.leftAt}</td>
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
