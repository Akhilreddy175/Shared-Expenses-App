import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getBalances, getMembers, getGroup } from '../api/client'
import Spinner from '../components/Spinner'

export default function Balances() {
  const { id: groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [members, setMembers] = useState([])
  const [balanceData, setBalanceData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

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
      setBalanceData(balRes.data)
    } catch {
      setError('Failed to load balances')
    } finally {
      setLoading(false)
    }
  }

  function getMemberName(userId) {
    const m = members.find((m) => m.userId === userId || String(m.userId) === String(userId))
    return m?.displayName || `User #${userId}`
  }

  if (loading) return <Spinner />
  if (error) return <p className="text-red-600 text-sm">{error}</p>

  const userBalances = balanceData?.userBalances || {}
  const suggestions = balanceData?.suggestedSettlements || []

  return (
    <div className="space-y-6">
      <div>
        <Link to={`/groups/${groupId}`} className="text-xs text-gray-400 hover:underline">← {group?.name}</Link>
        <h1 className="text-xl font-semibold text-gray-800 mt-0.5">Balances</h1>
      </div>

      {/* Individual Balances */}
      <section>
        <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Individual Balances</h2>
        {Object.keys(userBalances).length === 0 ? (
          <p className="text-sm text-gray-500">No expenses recorded yet.</p>
        ) : (
          <div className="bg-white border border-gray-200 rounded overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="text-left px-4 py-2 text-xs text-gray-500 font-medium">Member</th>
                  <th className="text-right px-4 py-2 text-xs text-gray-500 font-medium">Balance</th>
                  <th className="text-right px-4 py-2 text-xs text-gray-500 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(userBalances).map(([userId, balance]) => {
                  const bal = parseFloat(balance)
                  return (
                    <tr key={userId} className="border-b border-gray-50 last:border-0">
                      <td className="px-4 py-2.5 text-gray-800">{getMemberName(userId)}</td>
                      <td className={`px-4 py-2.5 text-right font-medium ${bal >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                        {bal >= 0 ? '+' : ''}₹{bal.toFixed(2)}
                      </td>
                      <td className="px-4 py-2.5 text-right">
                        {bal > 0 ? (
                          <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">gets back</span>
                        ) : bal < 0 ? (
                          <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded">owes</span>
                        ) : (
                          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">settled</span>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Suggested Settlements */}
      <section>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">Suggested Settlements</h2>
          <Link
            to={`/groups/${groupId}/settlements`}
            className="text-sm text-blue-600 hover:underline"
          >
            Record settlement →
          </Link>
        </div>

        {suggestions.length === 0 ? (
          <p className="text-sm text-gray-500">Everyone is settled up!</p>
        ) : (
          <div className="space-y-2">
            {suggestions.map((s, i) => (
              <div key={i} className="bg-white border border-gray-200 rounded px-4 py-3 flex items-center justify-between">
                <span className="text-sm text-gray-700">
                  <span className="font-medium">{getMemberName(s.from)}</span>
                  {' pays '}
                  <span className="font-medium">{getMemberName(s.to)}</span>
                </span>
                <span className="text-sm font-semibold text-gray-800">₹{parseFloat(s.amount).toFixed(2)}</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
