import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getSettlements, deleteSettlement, getMembers, getGroup } from '../api/client'
import Spinner from '../components/Spinner'

export default function Settlements() {
  const { id: groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [members, setMembers] = useState([])
  const [settlements, setSettlements] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => { loadData() }, [groupId])

  async function loadData() {
    try {
      const [grRes, memRes, setRes] = await Promise.all([
        getGroup(groupId),
        getMembers(groupId),
        getSettlements(groupId),
      ])
      setGroup(grRes.data)
      setMembers(memRes.data)
      setSettlements(setRes.data)
    } catch {
      setError('Failed to load settlements')
    } finally {
      setLoading(false)
    }
  }

  function getMemberName(userId) {
    const m = members.find((m) => m.userId === userId || String(m.userId) === String(userId))
    return m?.displayName || `User #${userId}`
  }

  async function handleDelete(settlementId) {
    if (!confirm('Are you sure you want to delete this recorded settlement?')) return
    try {
      await deleteSettlement(groupId, settlementId)
      setSettlements((prev) => prev.filter((s) => s.id !== settlementId))
    } catch (err) {
      alert(err.response?.data?.error || 'Delete failed')
    }
  }

  if (loading) return <Spinner />

  if (error) {
    return (
      <div className="space-y-4">
        <Link to={`/groups/${groupId}`} className="text-xs text-slate-500 hover:text-slate-900 hover:underline">← Back to Group</Link>
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-xs max-w-lg">
          {error}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">


      <div className="flex items-center justify-between pb-4 border-b border-slate-200">
        <div>
          <Link to={`/groups/${groupId}`} className="inline-flex items-center text-xs text-slate-400 hover:text-slate-800 hover:underline mb-1">
            ← {group?.name || 'Back to Group'}
          </Link>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">Settlements</h1>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to={`/groups/${groupId}/balances`}
            className="text-xs font-semibold border border-slate-200 hover:border-slate-800 hover:bg-slate-50 text-slate-600 px-3.5 py-2 rounded transition-colors"
          >
            View Balances
          </Link>
          <Link
            to={`/groups/${groupId}/settlements/record`}
            className="inline-flex items-center bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-3.5 py-2 rounded transition-colors"
          >
            Record Settlement
          </Link>
        </div>
      </div>


      {settlements.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded p-12 text-center">
          <p className="text-slate-500 text-xs">No settlements recorded yet in this group.</p>
          <Link
            to={`/groups/${groupId}/settlements/record`}
            className="mt-3 inline-flex bg-slate-105 hover:bg-slate-200 text-slate-700 text-xs font-semibold px-4 py-2 rounded transition-colors"
          >
            Record First Settlement
          </Link>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-250 bg-slate-50">
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">From (Payer)</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">To (Receiver)</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Date</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Amount</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {settlements.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3.5 font-semibold text-slate-800">
                      {getMemberName(s.payerId)}
                      {s.note && <span className="block text-[10px] text-slate-400 font-medium mt-0.5">Note: {s.note}</span>}
                    </td>
                    <td className="px-4 py-3.5 font-semibold text-slate-800">{getMemberName(s.receiverId)}</td>
                    <td className="px-4 py-3.5 text-slate-500 font-medium">{s.settlementDate || '—'}</td>
                    <td className="px-4 py-3.5 text-right font-bold text-slate-900">₹{parseFloat(s.amount).toFixed(2)}</td>
                    <td className="px-4 py-3.5 text-right">
                      <button
                        onClick={() => handleDelete(s.id)}
                        className="text-xs font-semibold text-red-650 hover:text-red-800 hover:bg-red-50 px-2 py-1 rounded transition-colors cursor-pointer border border-transparent hover:border-red-100"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
