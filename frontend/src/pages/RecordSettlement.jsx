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
      setError('Failed to load group members')
    } finally {
      setLoading(false)
    }
  }

  function fillFromSuggestion(s) {
    setPayerId(String(s.fromUserId))
    setReceiverId(String(s.toUserId))
    setAmount(String(parseFloat(s.amount).toFixed(2)))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (payerId === receiverId) {
      setError('Payer and receiver must be different members')
      return
    }
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
    <div className="space-y-6 max-w-lg">


      <div>
        <Link to={`/groups/${groupId}/settlements`} className="inline-flex items-center text-xs text-slate-500 hover:text-slate-900 hover:underline mb-1">
          ← Settlements
        </Link>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">Record Settlement</h1>
        <p className="text-xs text-slate-500 mt-1">Log a debt settlement or balance payment for {group?.name}</p>
      </div>


      {suggestions.length > 0 && (
        <div className="bg-slate-50 border border-slate-200 rounded p-4 space-y-2">
          <div className="text-[10px] font-bold text-slate-700 uppercase tracking-wider">
            Suggested Settlements (Click to auto-fill)
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {suggestions.map((s, i) => (
              <button
                key={i}
                type="button"
                onClick={() => fillFromSuggestion(s)}
                className="w-full text-left bg-white hover:bg-slate-100 border border-slate-200 rounded p-3 text-xs flex flex-col justify-between transition-colors cursor-pointer shadow-sm"
              >
                <div className="flex items-center gap-1.5 font-semibold text-slate-700">
                  <span className="truncate max-w-[80px]">{s.fromName}</span>
                  <span>→</span>
                  <span className="truncate max-w-[80px]">{s.toName}</span>
                </div>
                <div className="text-xs font-bold text-slate-900 mt-1">
                  ₹{parseFloat(s.amount).toFixed(2)}
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-xs">
          {error}
        </div>
      )}


      <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded p-6 space-y-4">

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Payer (Who Paid) *</label>
            <select
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-slate-800 cursor-pointer"
              value={payerId}
              onChange={(e) => setPayerId(e.target.value)}
              required
            >
              {members.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.displayName || `User #${m.userId}`} (ID: {m.userId})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Receiver (Who Received) *</label>
            <select
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-slate-800 cursor-pointer"
              value={receiverId}
              onChange={(e) => setReceiverId(e.target.value)}
              required
            >
              {members.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.displayName || `User #${m.userId}`} (ID: {m.userId})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Amount *</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Date</label>
            <input
              type="date"
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
              value={settlementDate}
              onChange={(e) => setSettlementDate(e.target.value)}
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-slate-655 mb-1">Note</label>
            <input
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
              placeholder="Optional notes (e.g. UPI transfer, Cash)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>

        </div>


        <div className="flex items-center gap-3 pt-4 border-t border-slate-200">
          <button
            type="submit"
            disabled={submitting}
            className="bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-5 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
          >
            {submitting ? 'Recording…' : 'Record Settlement'}
          </button>
          <Link
            to={`/groups/${groupId}/settlements`}
            className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold px-5 py-2 rounded transition-colors"
          >
            Cancel
          </Link>
        </div>

      </form>
    </div>
  )
}
