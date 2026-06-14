import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getGroups, getExpenses, getBalances, getImportJobs } from '../api/client'
import { useAuth } from '../context/AuthContext'
import Spinner from '../components/Spinner'
import { PlusCircle, Receipt, ArrowRight, AlertTriangle } from 'lucide-react'

export default function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [groups, setGroups] = useState([])
  const [recentExpenses, setRecentExpenses] = useState([])
  const [pendingImports, setPendingImports] = useState([])
  const [allBalances, setAllBalances] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      try {
        const grRes = await getGroups()
        const groupList = grRes.data
        setGroups(groupList)


        const expensePromises = groupList.map((g) =>
          getExpenses(g.id).then((r) => r.data.map((e) => ({ ...e, groupName: g.name, groupId: g.id }))).catch(() => [])
        )
        const importPromises = groupList.map((g) =>
          getImportJobs(g.id).then((r) => r.data.map((j) => ({ ...j, groupName: g.name, groupId: g.id }))).catch(() => [])
        )
        const balancePromises = groupList.map((g) =>
          getBalances(g.id).then((r) => ({ groupId: g.id, groupName: g.name, data: r.data })).catch(() => null)
        )

        const [allExpenses, allImports, balanceResults] = await Promise.all([
          Promise.all(expensePromises),
          Promise.all(importPromises),
          Promise.all(balancePromises),
        ])

        const flatExpenses = allExpenses.flat().sort((a, b) => new Date(b.expenseDate) - new Date(a.expenseDate))
        setRecentExpenses(flatExpenses.slice(0, 10))

        const flatImports = allImports.flat().filter((j) => j.status === 'PENDING_REVIEW')
        setPendingImports(flatImports)

        setAllBalances(balanceResults.filter(Boolean))
      } catch (err) {
        setError('Failed to load dashboard data')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) return <Spinner />
  if (error) return <p className="text-red-600 text-sm p-4">{error}</p>


  let overallNetOwed = 0
  allBalances.forEach(b => {
    const bal = b.data?.userBalances?.[user?.userId]
    if (bal !== undefined) overallNetOwed += bal
  })

  return (
    <div className="animate-fadeIn space-y-8 text-gray-900">


      <div className="bg-[#e6f0fa] rounded-xl p-8 flex items-center justify-between relative overflow-hidden">
        <div className="relative z-10 max-w-md">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Hello {user?.displayName || 'user'}, Welcome back!
          </h1>
          <p className="text-gray-600 text-sm mb-6 leading-relaxed">
            Keep track of shared expenses and settle your corresponding balances in a convenient and personalized way.
          </p>
          <div className="flex gap-3">
            <button
              onClick={() => navigate('/groups')}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-md transition-colors shadow-sm cursor-pointer"
            >
              View Groups
            </button>
            <span className={`inline-flex items-center px-3 py-2 rounded-md text-xs font-bold bg-white/80 border border-slate-200/50 ${
              overallNetOwed > 0 ? 'text-green-700' : overallNetOwed < 0 ? 'text-rose-700' : 'text-slate-600'
            }`}>
              Net Balance: {overallNetOwed >= 0 ? `+₹${overallNetOwed.toFixed(2)}` : `-₹${Math.abs(overallNetOwed).toFixed(2)}`}
            </span>
          </div>
        </div>


        <div className="hidden md:flex absolute right-0 bottom-0 top-0 w-1/2 items-end justify-end pr-8 pb-4 opacity-90 select-none pointer-events-none">
           <div className="flex items-end space-x-2">
              <div className="w-12 h-24 bg-blue-200 rounded-t-sm border border-blue-300 flex items-center justify-center text-blue-500 font-bold">$</div>
              <div className="w-16 h-32 bg-yellow-300 rounded-t-sm border border-yellow-400"></div>
              <div className="w-10 h-16 bg-blue-300 rounded-t-sm border border-blue-400"></div>
           </div>
        </div>
      </div>


      <div className="flex flex-col items-center justify-center py-8 bg-gray-50 border border-gray-100 rounded-xl">
        <p className="text-gray-700 text-lg mb-2">Ready to split a bill?</p>
        <button
          onClick={() => navigate('/groups')}
          className="text-blue-700 font-medium hover:underline text-lg flex items-center gap-1.5 focus:outline-none cursor-pointer"
        >
          <PlusCircle size={20} /> Create or manage Groups
        </button>
      </div>


      {pendingImports.length > 0 && (
        <section className="bg-amber-50 border border-amber-200 rounded-xl p-5 space-y-3 shadow-sm">
          <div className="flex items-center gap-2 text-amber-800">
            <AlertTriangle size={18} className="text-amber-600 shrink-0" />
            <h2 className="text-sm font-semibold">Action Needed: Pending CSV Reviews ({pendingImports.length})</h2>
          </div>
          <div className="grid gap-2">
            {pendingImports.map((j) => (
              <div key={j.id} className="bg-white border border-amber-100 rounded-lg p-3 flex items-center justify-between shadow-xs">
                <div>
                  <p className="text-sm font-semibold text-gray-800">{j.filename}</p>
                  <p className="text-xs text-gray-500">Group: {j.groupName}</p>
                </div>
                <Link
                  to={`/groups/${j.groupId}/imports/${j.id}/review`}
                  className="inline-flex items-center px-3 py-1.5 text-xs font-semibold bg-amber-600 hover:bg-amber-700 text-white rounded-md transition-colors"
                >
                  Review
                </Link>
              </div>
            ))}
          </div>
        </section>
      )}


      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">


        <section className="bg-white border border-gray-200 rounded-xl p-5 shadow-xs flex flex-col">
          <div className="flex items-center justify-between mb-4 pb-2 border-b border-gray-100">
            <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider">Your Groups</h2>
            <Link to="/groups" className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-0.5">
              View All <ArrowRight size={12} />
            </Link>
          </div>
          {groups.length === 0 ? (
            <p className="text-sm text-gray-500 italic py-4">No groups yet.</p>
          ) : (
            <div className="space-y-2.5 flex-1">
              {groups.slice(0, 3).map((g) => (
                <Link
                  key={g.id}
                  to={`/groups/${g.id}`}
                  className="block p-3 border border-gray-150 rounded-lg bg-gray-50/50 hover:bg-white hover:border-blue-300 transition-colors shadow-2xs"
                >
                  <p className="text-sm font-bold text-gray-800">{g.name}</p>
                  {g.description && <p className="text-xs text-gray-500 mt-0.5 truncate">{g.description}</p>}
                </Link>
              ))}
            </div>
          )}
        </section>


        <section className="bg-white border border-gray-200 rounded-xl p-5 shadow-xs flex flex-col">
          <div className="flex items-center justify-between mb-4 pb-2 border-b border-gray-100">
            <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider">Group Balances</h2>
          </div>
          {allBalances.length === 0 ? (
            <p className="text-sm text-gray-500 italic py-4">All settled up.</p>
          ) : (
            <div className="space-y-2 flex-1">
              {allBalances.map((b) => {
                const balances = b.data?.userBalances || {}
                const myBalance = balances[user?.userId]
                if (myBalance === undefined) return null
                return (
                  <div key={b.groupId} className="flex items-center justify-between p-2.5 bg-gray-50/50 rounded-lg border border-gray-100 hover:border-gray-200 transition-all">
                    <Link to={`/groups/${b.groupId}/balances`} className="text-sm font-semibold text-blue-600 hover:underline">
                      {b.groupName}
                    </Link>
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold ${
                      myBalance > 0
                        ? 'bg-green-50 text-green-700'
                        : myBalance < 0
                          ? 'bg-rose-50 text-rose-700'
                          : 'bg-gray-100 text-gray-600'
                    }`}>
                      {myBalance > 0 ? `+₹${myBalance.toFixed(2)}` : myBalance < 0 ? `-₹${Math.abs(myBalance).toFixed(2)}` : '0.00'}
                    </span>
                  </div>
                )
              })}
            </div>
          )}
        </section>
      </div>


      <section className="bg-white border border-gray-200 rounded-xl p-5 shadow-xs">
        <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider mb-4 pb-2 border-b border-gray-100">Recent Expenses</h2>
        {recentExpenses.length === 0 ? (
          <p className="text-sm text-gray-500 italic py-4">No transactions logged yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50/80">
                  <th className="px-4 py-3 text-xs text-gray-500 font-bold uppercase tracking-wider">Description</th>
                  <th className="px-4 py-3 text-xs text-gray-500 font-bold uppercase tracking-wider">Group</th>
                  <th className="px-4 py-3 text-xs text-slate-500 font-bold uppercase tracking-wider">Date</th>
                  <th className="px-4 py-3 text-xs text-gray-500 font-bold uppercase tracking-wider text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {recentExpenses.map((e) => (
                  <tr key={`${e.groupId}-${e.id}`} className="hover:bg-slate-50/40 transition-colors">
                    <td className="px-4 py-3 font-semibold text-gray-800 flex items-center gap-2">
                      <Receipt size={14} className="text-gray-400" />
                      <span>{e.description}</span>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{e.groupName}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs font-semibold">{e.expenseDate}</td>
                    <td className="px-4 py-3 text-right font-bold text-gray-900">
                      {e.currency} {parseFloat(e.amount).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

    </div>
  )
}
