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
      setError('Failed to load group members')
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
      setError('Please select at least one participant')
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
    <div className="space-y-6 max-w-xl">


      <div>
        <Link to={`/groups/${groupId}/expenses`} className="inline-flex items-center text-xs text-slate-500 hover:text-slate-900 hover:underline mb-1">
          ← Expenses
        </Link>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">Add Expense</h1>
        <p className="text-xs text-slate-500 mt-1">Record a new transaction for {group?.name}</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-xs">
          {error}
        </div>
      )}


      <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded p-6 space-y-6">


        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

          <div className="md:col-span-2">
            <label className="block text-xs font-semibold text-slate-650 mb-1">Description *</label>
            <input
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
              placeholder="e.g. Grocery shopping, Electricity bill"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
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
            <label className="block text-xs font-semibold text-slate-655 mb-1">Currency *</label>
            <select
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-slate-800 cursor-pointer"
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
            >
              {['INR', 'USD', 'EUR', 'GBP'].map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Date *</label>
            <input
              type="date"
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
              value={expenseDate}
              onChange={(e) => setExpenseDate(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Category</label>
            <input
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm focus:outline-none focus:border-slate-800"
              placeholder="e.g. Food, Utilities, Travel"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-655 mb-1">Paid By *</label>
            <select
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-slate-800 cursor-pointer"
              value={paidBy}
              onChange={(e) => setPaidBy(e.target.value)}
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
            <label className="block text-xs font-semibold text-slate-655 mb-1">Split Type *</label>
            <select
              className="w-full border border-slate-200 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-slate-800 cursor-pointer"
              value={splitType}
              onChange={(e) => setSplitType(e.target.value)}
            >
              {SPLIT_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>

        </div>


        <div className="pt-4 border-t border-slate-200">
          <label className="block text-xs font-bold text-slate-800 uppercase tracking-wider mb-2">
            Participants split details
          </label>

          <div className="bg-slate-50 border border-slate-200 rounded p-4 space-y-3">
            {participants.map((p) => (
              <div key={p.userId} className="flex items-center justify-between gap-4 p-2 bg-white rounded border border-slate-100">
                <label className="flex items-center gap-2 cursor-pointer select-none flex-1 min-w-0 text-xs font-semibold text-slate-800">
                  <input
                    type="checkbox"
                    checked={p.selected}
                    onChange={() => toggleParticipant(p.userId)}
                    className="w-4 h-4 text-slate-850 border-slate-300 rounded"
                  />
                  <span>{p.name}</span>
                </label>

                {p.selected && (
                  <div className="shrink-0">
                    {splitType === 'EXACT' && (
                      <div className="relative flex items-center">
                        <span className="text-xs text-slate-400 font-bold mr-1">₹</span>
                        <input
                          type="number"
                          step="0.01"
                          placeholder="Amount"
                          className="border border-slate-200 rounded px-2 py-1 text-xs w-24 bg-white focus:outline-none focus:border-slate-800"
                          value={p.shareAmount}
                          onChange={(e) => updateParticipant(p.userId, 'shareAmount', e.target.value)}
                        />
                      </div>
                    )}
                    {splitType === 'PERCENTAGE' && (
                      <div className="relative flex items-center">
                        <input
                          type="number"
                          step="0.01"
                          placeholder="Percent"
                          className="border border-slate-200 rounded px-2 py-1 text-xs w-20 bg-white focus:outline-none focus:border-slate-800 text-right mr-1"
                          value={p.percentage}
                          onChange={(e) => updateParticipant(p.userId, 'percentage', e.target.value)}
                        />
                        <span className="text-xs text-slate-400 font-bold">%</span>
                      </div>
                    )}
                    {splitType === 'SHARES' && (
                      <input
                        type="number"
                        step="0.5"
                        placeholder="Shares"
                        className="border border-slate-200 rounded px-2 py-1 text-xs w-20 bg-white focus:outline-none focus:border-slate-800 text-right"
                        value={p.shares}
                        onChange={(e) => updateParticipant(p.userId, 'shares', e.target.value)}
                      />
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>


        <div className="flex items-center gap-3 pt-4 border-t border-slate-200">
          <button
            type="submit"
            disabled={submitting}
            className="bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-5 py-2 rounded transition-colors disabled:opacity-50 flex items-center gap-2 cursor-pointer"
          >
            {submitting ? 'Saving Expense…' : 'Add Expense'}
          </button>

          <Link
            to={`/groups/${groupId}/expenses`}
            className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold px-5 py-2 rounded transition-colors"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  )
}
