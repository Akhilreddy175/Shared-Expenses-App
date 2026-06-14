import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getExpenses, deleteExpense, getGroup } from '../api/client'
import Spinner from '../components/Spinner'

export default function Expenses() {
  const { id: groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [filtered, setFiltered] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')

  useEffect(() => { loadAll() }, [groupId])

  useEffect(() => {
    let result = expenses
    if (search) result = result.filter((e) => e.description.toLowerCase().includes(search.toLowerCase()))
    if (categoryFilter) result = result.filter((e) => e.category === categoryFilter)
    setFiltered(result)
  }, [search, categoryFilter, expenses])

  async function loadAll() {
    try {
      const [grRes, expRes] = await Promise.all([getGroup(groupId), getExpenses(groupId)])
      setGroup(grRes.data)
      setExpenses(expRes.data)
      setFiltered(expRes.data)
    } catch {
      setError('Failed to load expenses')
    } finally {
      setLoading(false)
    }
  }

  async function handleDelete(expenseId) {
    if (!confirm('Delete this expense?')) return
    try {
      await deleteExpense(groupId, expenseId)
      setExpenses((prev) => prev.filter((e) => e.id !== expenseId))
    } catch (err) {
      alert(err.response?.data?.error || 'Delete failed')
    }
  }

  const categories = [...new Set(expenses.map((e) => e.category).filter(Boolean))]

  if (loading) return <Spinner />
  if (error) return <p className="text-red-600 text-sm">{error}</p>

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Link to={`/groups/${groupId}`} className="text-xs text-gray-400 hover:underline">← {group?.name}</Link>
          <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Expenses</h1>
        </div>
        <Link
          to={`/groups/${groupId}/expenses/add`}
          className="bg-blue-600 text-white text-sm px-3 py-1.5 rounded hover:bg-blue-700"
        >
          + Add Expense
        </Link>
      </div>

      {/* Filters */}
      <div className="flex gap-3">
        <input
          type="text"
          placeholder="Search description…"
          className="border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-blue-500"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {categories.length > 0 && (
          <select
            className="border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-blue-500"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <option value="">All categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        )}
        {(search || categoryFilter) && (
          <button
            onClick={() => { setSearch(''); setCategoryFilter('') }}
            className="text-sm text-gray-400 hover:text-gray-700"
          >
            Clear
          </button>
        )}
      </div>

      {filtered.length === 0 ? (
        <p className="text-sm text-gray-500">
          {expenses.length === 0
            ? 'No expenses yet.'
            : 'No expenses match your filters.'}
        </p>
      ) : (
        <div className="bg-white border border-gray-200 rounded overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Description</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Category</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Date</th>
                <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Split</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Amount</th>
                <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((e) => (
                <tr key={e.id} className="border-b border-gray-50 last:border-0">
                  <td className="px-3 py-2.5 text-gray-800 font-medium">{e.description}</td>
                  <td className="px-3 py-2.5">
                    {e.category ? (
                      <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{e.category}</span>
                    ) : '—'}
                  </td>
                  <td className="px-3 py-2.5 text-gray-500">{e.expenseDate}</td>
                  <td className="px-3 py-2.5 text-gray-400 text-xs">{e.splitType}</td>
                  <td className="px-3 py-2.5 text-right font-medium">
                    {e.currency} {parseFloat(e.amount).toFixed(2)}
                  </td>
                  <td className="px-3 py-2.5 text-right space-x-3">
                    <Link
                      to={`/groups/${groupId}/expenses/${e.id}/edit`}
                      className="text-xs text-gray-500 hover:text-blue-600"
                    >
                      Edit
                    </Link>
                    <button
                      onClick={() => handleDelete(e.id)}
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
