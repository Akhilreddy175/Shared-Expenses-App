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
    if (!confirm('Delete this settlement?')) return
    try {
      await deleteSettlement(groupId, settlementId)
      setSettlements((prev) => prev.filter((s) => s.id !== settlementId))
    } catch (err) {
      alert(err.response?.data?.error || 'Delete failed')
    }
  }

  if (loading) return <Spinner />
  if (error) return <p className="text-red-600 text-sm">{error}</p>

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Link to={`/groups/${groupId}`} className="text-xs text-gray-400 hover:underline">← {group?.name}</Link>
          <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Settlements</h1>
        </div>
        <div className="flex gap-2">
          <Link
            to={`/groups/${groupId}/balances`}
            className="text-sm border border-gray-300 rounded px-3 py-1.5 text-gray-600 hover:border-blue-400"
          >
            View Balances
          </Link>
          <Link
            to={`/groups/${groupId}/settlements/record`}
            className="bg-blue-600 text-white text-sm px-3 py-1.5 rounded hover:bg-blue-700"
          >
            + Record
          </Link>
        </div>
      </div>

      {settlements.length === 0 ? (
        <p className="text-sm text-gray-500">No settlements recorded yet.</p>
      ) : (
        <div className="bg-white border border-gray-200 rounded overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Payer</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Receiver</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Date</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Amount</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {settlements.map((s) => (
                <tr key={s.id} className="border-b border-gray-50 last:border-0">
                  <td className="px-3 py-2.5 text-gray-800">{getMemberName(s.payerId)}</td>
                  <td className="px-3 py-2.5 text-gray-800">{getMemberName(s.receiverId)}</td>
                  <td className="px-3 py-2.5 text-gray-500">{s.settlementDate || '—'}</td>
                  <td className="px-3 py-2.5 text-right font-medium">₹{parseFloat(s.amount).toFixed(2)}</td>
                  <td className="px-3 py-2.5 text-right">
                    <button
                      onClick={() => handleDelete(s.id)}
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
    </div>
  )
}
