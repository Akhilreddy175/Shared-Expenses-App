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
    if (!confirm('Are you sure you want to delete this expense?')) return
    try {
      await deleteExpense(groupId, expenseId)
      setExpenses((prev) => prev.filter((e) => e.id !== expenseId))
    } catch (err) {
      alert(err.response?.data?.error || 'Delete failed')
    }
  }

  const categories = [...new Set(expenses.map((e) => e.category).filter(Boolean))]

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
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">Expenses</h1>
        </div>
        <Link
          to={`/groups/${groupId}/expenses/add`}
          className="inline-flex items-center gap-1.5 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold px-4 py-2.5 rounded transition-colors"
        >
          Add Expense
        </Link>
      </div>


      <div className="bg-white border border-slate-200 rounded p-4 flex flex-wrap items-center gap-3">
        <div className="flex-1 min-w-[200px]">
          <input
            type="text"
            placeholder="Search description…"
            className="w-full border border-slate-200 rounded px-3 py-2 text-xs focus:outline-none focus:border-slate-800"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {categories.length > 0 && (
          <select
            className="border border-slate-200 rounded px-3 py-2 text-xs focus:outline-none focus:border-slate-800 bg-white cursor-pointer min-w-[150px]"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        )}

        {(search || categoryFilter) && (
          <button
            onClick={() => { setSearch(''); setCategoryFilter('') }}
            className="text-xs font-semibold text-slate-500 hover:text-slate-800 cursor-pointer"
          >
            Clear Filters
          </button>
        )}
      </div>


      {filtered.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded p-12 text-center">
          <p className="text-slate-500 text-xs">
            {expenses.length === 0
              ? 'No expenses recorded yet in this group.'
              : 'No expenses match your search query or filters.'}
          </p>
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-250 bg-slate-50">
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Description</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Category</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Date</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Split Type</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Amount</th>
                  <th className="px-4 py-3 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((e) => (
                  <tr key={e.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-4 py-3.5">
                      <div className="font-semibold text-slate-800">{e.description}</div>
                      <div className="text-[10px] text-slate-400 mt-0.5">Paid by {e.paidByName || `User #${e.paidBy}`}</div>
                    </td>
                    <td className="px-4 py-3.5">
                      {e.category ? (
                        <span className="inline-flex items-center text-[10px] font-bold bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200 uppercase tracking-wider">
                          {e.category}
                        </span>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5 text-slate-500 font-medium">{e.expenseDate}</td>
                    <td className="px-4 py-3.5">
                      <span className="inline-flex items-center text-[10px] font-bold bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200 uppercase tracking-wider">
                        {e.splitType}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right font-bold text-slate-900">
                      {e.currency} {parseFloat(e.amount).toFixed(2)}
                    </td>
                    <td className="px-4 py-3.5 text-right space-x-1 whitespace-nowrap">
                      <Link
                        to={`/groups/${groupId}/expenses/${e.id}/edit`}
                        className="text-xs font-semibold text-slate-600 hover:text-slate-950 hover:underline px-2 py-1"
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => handleDelete(e.id)}
                        className="text-xs font-semibold text-red-600 hover:text-red-800 hover:bg-red-50 px-2 py-1 rounded transition-colors cursor-pointer border border-transparent hover:border-red-100"
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
