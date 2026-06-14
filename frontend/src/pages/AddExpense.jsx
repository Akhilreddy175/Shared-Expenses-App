import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { createExpense, getMembers, getGroup } from '../api/client'
import Spinner from '../components/Spinner'

const SPLIT_TYPES = ['EQUAL', 'EXACT', 'PERCENTAGE', 'SHARES']

export default function AddExpense() {
  const { id: groupId } = useParams()
  const navigate = useNavigate()
  const [group, setGroup] = useState(null)
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('INR')
  const [expenseDate, setExpenseDate] = useState(new Date().toISOString().split('T')[0])
  const [paidBy, setPaidBy] = useState('')
  const [splitType, setSplitType] = useState('EQUAL')
  const [category, setCategory] = useState('')
  const [participants, setParticipants] = useState([])

  useEffect(() => { loadData() }, [groupId])

  async function loadData() {
    try {
      const [grRes, memRes] = await Promise.all([getGroup(groupId), getMembers(groupId)])
      setGroup(grRes.data)
      setMembers(memRes.data)
      // Default: all members selected
      setParticipants(memRes.data.map((m) => ({
        userId: m.userId,
        name: m.displayName || `User #${m.userId}`,
        selected: true,
        shareAmount: '',
        percentage: '',
        shares: '',
      })))
      if (memRes.data.length > 0) setPaidBy(String(memRes.data[0].userId))
    } catch {
      setError('Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  function toggleParticipant(userId) {
    setParticipants((prev) =>
      prev.map((p) => (p.userId === userId ? { ...p, selected: !p.selected } : p))
    )
  }

  function updateParticipant(userId, field, value) {
    setParticipants((prev) =>
      prev.map((p) => (p.userId === userId ? { ...p, [field]: value } : p))
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const selected = participants.filter((p) => p.selected)
    if (selected.length === 0) {
      setError('Select at least one participant')
      return
    }

    const participantData = selected.map((p) => ({
      userId: p.userId,
      shareAmount: p.shareAmount ? parseFloat(p.shareAmount) : null,
      percentage: p.percentage ? parseFloat(p.percentage) : null,
      shares: p.shares ? parseFloat(p.shares) : null,
    }))

    setSubmitting(true)
    try {
      await createExpense(groupId, {
        description,
        amount: parseFloat(amount),
        currency,
        expenseDate,
        paidBy: parseInt(paidBy),
        splitType,
        category: category || null,
        participants: participantData,
      })
      navigate(`/groups/${groupId}/expenses`)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create expense')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <Spinner />

  return (
    <div className="space-y-4 max-w-xl">
      <div>
        <Link to={`/groups/${groupId}/expenses`} className="text-xs text-gray-400 hover:underline">← Expenses</Link>
        <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Add Expense</h1>
      </div>

      {error && <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded">{error}</p>}

      <form onSubmit={handleSubmit} className="bg-white border border-gray-200 rounded p-4 space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="col-span-2">
            <label className="block text-sm text-gray-600 mb-1">Description *</label>
            <input
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Amount *</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Currency *</label>
            <select
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
            >
              {['INR', 'USD', 'EUR', 'GBP'].map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Date *</label>
            <input
              type="date"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={expenseDate}
              onChange={(e) => setExpenseDate(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Category</label>
            <input
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              placeholder="e.g. food, travel"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Paid By *</label>
            <select
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={paidBy}
              onChange={(e) => setPaidBy(e.target.value)}
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
            <label className="block text-sm text-gray-600 mb-1">Split Type *</label>
            <select
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
              value={splitType}
              onChange={(e) => setSplitType(e.target.value)}
            >
              {SPLIT_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Participants */}
        <div>
          <label className="block text-sm text-gray-600 mb-2">Participants</label>
          <div className="space-y-2">
            {participants.map((p) => (
              <div key={p.userId} className="flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={p.selected}
                  onChange={() => toggleParticipant(p.userId)}
                  className="rounded"
                />
                <span className="text-sm text-gray-700 w-28 truncate">{p.name}</span>
                {p.selected && splitType === 'EXACT' && (
                  <input
                    type="number"
                    step="0.01"
                    placeholder="Amount"
                    className="border border-gray-300 rounded px-2 py-1 text-sm w-28 focus:outline-none focus:border-blue-500"
                    value={p.shareAmount}
                    onChange={(e) => updateParticipant(p.userId, 'shareAmount', e.target.value)}
                  />
                )}
                {p.selected && splitType === 'PERCENTAGE' && (
                  <input
                    type="number"
                    step="0.01"
                    placeholder="%"
                    className="border border-gray-300 rounded px-2 py-1 text-sm w-24 focus:outline-none focus:border-blue-500"
                    value={p.percentage}
                    onChange={(e) => updateParticipant(p.userId, 'percentage', e.target.value)}
                  />
                )}
                {p.selected && splitType === 'SHARES' && (
                  <input
                    type="number"
                    step="0.5"
                    placeholder="Shares"
                    className="border border-gray-300 rounded px-2 py-1 text-sm w-24 focus:outline-none focus:border-blue-500"
                    value={p.shares}
                    onChange={(e) => updateParticipant(p.userId, 'shares', e.target.value)}
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={submitting}
            className="bg-blue-600 text-white text-sm px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? 'Saving…' : 'Add Expense'}
          </button>
          <Link
            to={`/groups/${groupId}/expenses`}
            className="text-sm text-gray-500 px-4 py-2 hover:text-gray-800"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  )
}
