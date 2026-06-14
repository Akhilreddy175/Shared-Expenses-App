import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { recordSettlement, getMembers, getGroup, getBalances } from '../api/client'
import Spinner from '../components/Spinner'

export default function RecordSettlement() {
  const { id: groupId } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState(null)
  const [members, setMembers] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const [payerId, setPayerId] = useState('')
  const [receiverId, setReceiverId] = useState('')
  const [amount, setAmount] = useState('')
  const [settlementDate, setSettlementDate] = useState(new Date().toISOString().split('T')[0])
  const [note, setNote] = useState('')

  useEffect(() => { loadData() }, [groupId])

  async function loadData() {
    try {
      const [grRes, memRes, balRes] = await Promise.all([
        getGroup(groupId),
        getMembers(groupId),
        getBalances(groupId),
      ])
      setGroup(grRes.data)
      setMembers(memRes.data)
      setSuggestions(balRes.data?.suggestedSettlements || [])
      if (memRes.data.length > 0) {
        setPayerId(String(memRes.data[0].userId))
        setReceiverId(memRes.data.length > 1 ? String(memRes.data[1].userId) : String(memRes.data[0].userId))
      }
    } catch {
      setError('Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  function fillFromSuggestion(s) {
    setPayerId(String(s.from))
    setReceiverId(String(s.to))
    setAmount(String(parseFloat(s.amount).toFixed(2)))
  }

  function getMemberName(userId) {
    const m = members.find((m) => m.userId === userId || String(m.userId) === String(userId))
    return m?.displayName || `User #${userId}`
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (payerId === receiverId) { setError('Payer and receiver must be different'); return }
    setSubmitting(true)
    try {
      await recordSettlement(groupId, {
        payerId: parseInt(payerId),
        receiverId: parseInt(receiverId),
        amount: parseFloat(amount),
        settlementDate,
        note: note || null,
      })
      navigate(`/groups/${groupId}/settlements`)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to record settlement')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Spinner />

  return (
    <div className="space-y-4 max-w-lg">
      <div>
        <Link to={`/groups/${groupId}/settlements`} className="text-xs text-gray-400 hover:underline">← Settlements</Link>
        <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Record Settlement</h1>
      </div>

      {/* Suggestions */}
      {suggestions.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded p-3">
          <p className="text-xs font-medium text-blue-700 mb-2">Suggested settlements — click to pre-fill:</p>
          <div className="space-y-1">
            {suggestions.map((s, i) => (
              <button
                key={i}
                type="button"
                onClick={() => fillFromSuggestion(s)}
                className="block w-full text-left text-sm text-blue-800 hover:bg-blue-100 rounded px-2 py-1"
              >
                {getMemberName(s.from)} → {getMemberName(s.to)}: ₹{parseFloat(s.amount).toFixed(2)}
              </button>
            ))}
          </div>
        </div>
      )}

      {error && <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded">{error}</p>}

      <form onSubmit={handleSubmit} className="bg-white border border-gray-200 rounded p-4 space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm text-gray-600 mb-1">Payer *</label>
            <select
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={payerId}
              onChange={(e) => setPayerId(e.target.value)}
              required
            >
              {members.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.displayName || `User #${m.userId}`}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Receiver *</label>
            <select
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={receiverId}
              onChange={(e) => setReceiverId(e.target.value)}
              required
            >
              {members.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.displayName || `User #${m.userId}`}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Amount *</label>
            <input
              type="number" step="0.01" min="0.01"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Date</label>
            <input
              type="date"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={settlementDate}
              onChange={(e) => setSettlementDate(e.target.value)}
            />
          </div>
          <div className="col-span-2">
            <label className="block text-sm text-gray-600 mb-1">Note</label>
            <input
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              placeholder="Optional note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={submitting}
            className="bg-blue-600 text-white text-sm px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? 'Saving…' : 'Record Settlement'}
          </button>
          <Link to={`/groups/${groupId}/settlements`} className="text-sm text-gray-500 px-4 py-2 hover:text-gray-800">Cancel</Link>
        </div>
      </form>
    </div>
  )
}
