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


  const [editMode, setEditMode] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')


  const [showAddMember, setShowAddMember] = useState(false)
  const [newUserId, setNewUserId] = useState('')
  const [newJoinedAt, setNewJoinedAt] = useState('')
  const [memberError, setMemberError] = useState('')


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
      setError('Failed to load group details')
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

  if (error) {
    return (
      <div className="space-y-4">
        <Link to="/groups" className="text-xs text-slate-500 hover:text-slate-900 hover:underline">← Back to Groups</Link>
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-xs max-w-lg">
          {error}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">


      <div className="bg-white border border-slate-200 rounded p-6">
        <div className="flex items-start justify-between">
          <div className="space-y-1 flex-1">
            <Link to="/groups" className="inline-flex items-center text-xs text-slate-400 hover:text-slate-800 hover:underline mb-1">
              ← Back to Groups
            </Link>

            {editMode ? (
              <form onSubmit={handleUpdateGroup} className="mt-2 space-y-3 max-w-md">
                <div>
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Group Name</label>
                  <input
                    className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Description</label>
                  <input
                    className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
                    placeholder="Add description"
                    value={editDesc}
                    onChange={(e) => setEditDesc(e.target.value)}
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    type="submit"
                    className="bg-slate-800 hover:bg-slate-950 text-white text-xs font-semibold px-4 py-2 rounded transition-colors cursor-pointer"
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditMode(false)}
                    className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold px-4 py-2 rounded transition-colors cursor-pointer"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <>
                <h1 className="text-xl font-bold text-slate-900 tracking-tight">{group.name}</h1>
                <p className="text-xs text-slate-500">
                  {group.description || 'No description provided.'}
                </p>
              </>
            )}
          </div>

          {!editMode && (
            <button
              onClick={() => setEditMode(true)}
              className="inline-flex items-center gap-1 text-xs font-semibold text-slate-650 hover:text-slate-900 bg-white hover:bg-slate-50 px-3 py-1.5 rounded border border-slate-200 transition-colors cursor-pointer"
            >
              Edit Group
            </button>
          )}
        </div>
      </div>


      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[
          { to: `/groups/${id}/expenses`, label: 'Expenses' },
          { to: `/groups/${id}/balances`, label: 'Balances' },
          { to: `/groups/${id}/settlements`, label: 'Settlements' },
          { to: `/groups/${id}/imports`, label: 'CSV Import' },
        ].map((link) => (
          <Link
            key={link.to}
            to={link.to}
            className="flex flex-col items-center justify-center p-4 bg-white border border-slate-200 rounded text-slate-800 hover:border-slate-800 hover:text-slate-900 transition-colors text-xs font-semibold"
          >
            {link.label}
          </Link>
        ))}
      </div>


      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">


        <div className="lg:col-span-2 space-y-6">
          <section className="bg-white border border-slate-200 rounded p-5 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Active Members ({members.length})</h2>
              <button
                onClick={() => { setShowAddMember(!showAddMember); setMemberError('') }}
                className={`inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded border transition-colors cursor-pointer ${
                  showAddMember
                    ? 'bg-slate-100 border-slate-300 text-slate-700'
                    : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
                }`}
              >
                {showAddMember ? 'Cancel' : 'Add Member'}
              </button>
            </div>


            {showAddMember && (
              <form onSubmit={handleAddMember} className="bg-slate-50 border border-slate-200 rounded p-4 space-y-3">
                <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wide">Add Member</h3>
                {memberError && (
                  <p className="text-xs text-red-600 bg-red-50 border border-red-200 px-3 py-1.5 rounded">{memberError}</p>
                )}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">User ID *</label>
                    <input
                      type="number"
                      className="w-full border border-slate-200 rounded px-3 py-1.5 text-xs bg-white focus:outline-none focus:border-slate-800"
                      placeholder="e.g. 5"
                      value={newUserId}
                      onChange={(e) => setNewUserId(e.target.value)}
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">Joined Date</label>
                    <input
                      type="date"
                      className="w-full border border-slate-200 rounded px-3 py-1.5 text-xs bg-white focus:outline-none focus:border-slate-800"
                      value={newJoinedAt}
                      onChange={(e) => setNewJoinedAt(e.target.value)}
                    />
                  </div>
                </div>
                <button type="submit" className="bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-4 py-2 rounded transition-colors cursor-pointer">
                  Add Member
                </button>
              </form>
            )}

            {members.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-6">No active members in this group.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-250 bg-slate-50">
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Member</th>
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Joined Date</th>
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {members.map((m) => (
                      <tr key={m.userId} className="hover:bg-slate-50/50 transition-colors">
                        <td className="px-4 py-3">
                          <span className="font-semibold text-slate-800 block">
                            {m.displayName || `User #${m.userId}`}
                          </span>
                          <span className="text-[10px] text-slate-400">User ID: {m.userId} • {m.email}</span>
                        </td>
                        <td className="px-4 py-3 text-slate-500 font-medium">{m.joinedAt || '—'}</td>
                        <td className="px-4 py-3 text-right">
                          {removingId === m.userId ? (
                            <span className="flex items-center justify-end gap-2">
                              <input
                                type="date"
                                className="border border-slate-200 rounded px-2 py-1 text-xs focus:outline-none"
                                value={removeLeftAt}
                                onChange={(e) => setRemoveLeftAt(e.target.value)}
                              />
                              <button
                                onClick={() => handleRemoveMember(m.userId)}
                                className="text-xs font-semibold bg-red-650 hover:bg-red-700 text-white px-2.5 py-1 rounded cursor-pointer"
                              >
                                Confirm
                              </button>
                              <button onClick={() => setRemovingId(null)} className="text-xs text-slate-400 hover:text-slate-650">Cancel</button>
                            </span>
                          ) : (
                            <button
                              onClick={() => { setRemovingId(m.userId); setRemoveLeftAt('') }}
                              className="text-xs font-semibold text-red-600 hover:text-red-800 hover:bg-red-50 px-2 py-1 rounded transition-colors cursor-pointer border border-transparent hover:border-red-100"
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
        </div>


        <div>
          <section className="bg-white border border-slate-200 rounded p-5 space-y-4">
            <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">Former Members</h2>

            {!history.some((m) => m.leftAt) ? (
              <p className="text-xs text-slate-500 text-center py-6">No former members recorded.</p>
            ) : (
              <div className="space-y-2 max-h-[300px] overflow-y-auto">
                {history.filter((m) => m.leftAt).map((m, idx) => (
                  <div key={`${m.userId}-${idx}`} className="p-3 bg-slate-50 border border-slate-200 rounded space-y-1">
                    <p className="text-xs font-bold text-slate-800">{m.displayName || `User #${m.userId}`}</p>
                    <div className="flex justify-between text-[10px] text-slate-500">
                      <span>Joined: {m.joinedAt || '—'}</span>
                      <span className="text-red-600 font-semibold">Left: {m.leftAt}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>


      </div>
    </div>
  )
}
