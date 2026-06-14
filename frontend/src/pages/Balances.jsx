import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getBalances, getGroup } from '../api/client'
import Spinner from '../components/Spinner'

export default function Balances() {
  const { id: groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [balanceData, setBalanceData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => { loadData() }, [groupId])

  async function loadData() {
    try {
      const [grRes, balRes] = await Promise.all([
        getGroup(groupId),
        getBalances(groupId),
      ])
      setGroup(grRes.data)
      setBalanceData(balRes.data)
    } catch {
      setError('Failed to load balance details')
    } finally {
      setLoading(false)
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

  const memberBalances = balanceData?.memberBalances || []
  const suggestions = balanceData?.suggestedSettlements || []

  return (
    <div className="space-y-6">


      <div className="pb-4 border-b border-slate-200">
        <Link to={`/groups/${groupId}`} className="inline-flex items-center text-xs text-slate-400 hover:text-slate-800 hover:underline mb-1">
          ← {group?.name || 'Back to Group'}
        </Link>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">Balances</h1>
        <p className="text-xs text-slate-500 mt-1">Detailed summary of who owes how much within the group</p>
      </div>


      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">


        <div className="lg:col-span-2 space-y-4">
          <section className="bg-white border border-slate-200 rounded p-5 space-y-4">
            <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider pb-2 border-b border-slate-100">
              Individual Balances
            </h2>

            {memberBalances.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-8">No expenses recorded yet in this group.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50">
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider">Member</th>
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Paid</th>
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Owed</th>
                      <th className="px-4 py-2 text-[10px] text-slate-500 font-bold uppercase tracking-wider text-right">Net Balance</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {memberBalances.map((m) => {
                      const bal = parseFloat(m.balance || 0)
                      return (
                        <tr key={m.userId} className="hover:bg-slate-50/50 transition-colors">
                          <td className="px-4 py-3">
                            <span className="font-semibold text-slate-800 block">
                              {m.displayName || `User #${m.userId}`}
                            </span>
                            <span className="text-[10px] text-slate-400">User ID: {m.userId}</span>
                          </td>
                          <td className="px-4 py-3 text-right text-slate-600">
                            ₹{parseFloat(m.totalPaid || 0).toFixed(2)}
                          </td>
                          <td className="px-4 py-3 text-right text-slate-600">
                            ₹{parseFloat(m.totalOwed || 0).toFixed(2)}
                          </td>
                          <td className={`px-4 py-3 text-right font-bold ${bal > 0 ? 'text-green-600' : bal < 0 ? 'text-red-600' : 'text-slate-500'}`}>
                            {bal > 0 ? '+' : ''}₹{bal.toFixed(2)}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>


        <div className="space-y-4">
          <section className="bg-white border border-slate-200 rounded p-5 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                Suggested Settlements
              </h2>
              {suggestions.length > 0 && (
                <Link
                  to={`/groups/${groupId}/settlements/record`}
                  className="text-xs font-bold text-slate-800 hover:underline"
                >
                  Record
                </Link>
              )}
            </div>

            {suggestions.length === 0 ? (
              <div className="text-center py-6">
                <p className="text-xs text-green-700 font-semibold">Everyone is fully settled up!</p>
              </div>
            ) : (
              <div className="space-y-2">
                {suggestions.map((s, i) => (
                  <div key={i} className="bg-slate-50 border border-slate-200 rounded p-3 space-y-1.5">
                    <div className="flex items-center justify-between gap-1 text-xs text-slate-700 font-medium">
                      <span className="font-bold truncate max-w-[80px]">{s.fromName}</span>
                      <span className="text-slate-400">owes</span>
                      <span className="font-bold truncate max-w-[80px]">{s.toName}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">Amount</span>
                      <span className="text-xs font-bold text-slate-900">₹{parseFloat(s.amount).toFixed(2)}</span>
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
