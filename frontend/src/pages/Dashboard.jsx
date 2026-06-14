import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getGroups, getExpenses, getBalances, getImportJobs } from '../api/client'
import { useAuth } from '../context/AuthContext'
import Spinner from '../components/Spinner'

export default function Dashboard() {
  const { user } = useAuth()
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

        // Fetch expenses and imports from all groups in parallel
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
  if (error) return <p className="text-red-600 text-sm">{error}</p>

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-semibold text-gray-800">
        Hello, {user?.displayName || 'there'} 👋
      </h1>

      {/* Groups overview */}
      <section>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">Your Groups</h2>
          <Link to="/groups" className="text-sm text-blue-600 hover:underline">View all</Link>
        </div>
        {groups.length === 0 ? (
          <p className="text-sm text-gray-500">No groups yet. <Link to="/groups" className="text-blue-600 hover:underline">Create one</Link>.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {groups.map((g) => (
              <Link
                key={g.id}
                to={`/groups/${g.id}`}
                className="block border border-gray-200 rounded p-3 bg-white hover:border-blue-400"
              >
                <p className="text-sm font-medium text-gray-800">{g.name}</p>
                {g.description && <p className="text-xs text-gray-500 mt-0.5">{g.description}</p>}
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Balance summary */}
      {allBalances.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Group Balances</h2>
          <div className="space-y-2">
            {allBalances.map((b) => {
              const balances = b.data?.userBalances || {}
              const myBalance = balances[user?.userId]
              return (
                <div key={b.groupId} className="bg-white border border-gray-200 rounded p-3">
                  <div className="flex items-center justify-between">
                    <Link to={`/groups/${b.groupId}/balances`} className="text-sm font-medium text-blue-600 hover:underline">
                      {b.groupName}
                    </Link>
                    {myBalance !== undefined && (
                      <span className={`text-sm font-medium ${myBalance >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {myBalance >= 0 ? `+₹${myBalance.toFixed(2)}` : `-₹${Math.abs(myBalance).toFixed(2)}`}
                      </span>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </section>
      )}

      {/* Pending import reviews */}
      {pendingImports.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Pending Import Reviews</h2>
          <div className="space-y-2">
            {pendingImports.map((j) => (
              <div key={j.id} className="bg-yellow-50 border border-yellow-200 rounded p-3 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-800">{j.filename}</p>
                  <p className="text-xs text-gray-500">{j.groupName}</p>
                </div>
                <Link
                  to={`/groups/${j.groupId}/imports/${j.id}/review`}
                  className="text-sm text-yellow-700 font-medium hover:underline"
                >
                  Review →
                </Link>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Recent expenses */}
      <section>
        <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Recent Expenses</h2>
        {recentExpenses.length === 0 ? (
          <p className="text-sm text-gray-500">No expenses recorded yet.</p>
        ) : (
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Description</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Group</th>
                  <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Date</th>
                  <th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">Amount</th>
                </tr>
              </thead>
              <tbody>
                {recentExpenses.map((e) => (
                  <tr key={`${e.groupId}-${e.id}`} className="border-b border-gray-50 last:border-0">
                    <td className="px-3 py-2 text-gray-800">{e.description}</td>
                    <td className="px-3 py-2 text-gray-500">{e.groupName}</td>
                    <td className="px-3 py-2 text-gray-500">{e.expenseDate}</td>
                    <td className="px-3 py-2 text-right font-medium">
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
