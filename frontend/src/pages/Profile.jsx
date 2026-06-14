import { useEffect, useState } from 'react'
import { getMe } from '../api/client'
import { useAuth } from '../context/AuthContext'
import Spinner from '../components/Spinner'

export default function Profile() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMe()
      .then((res) => setProfile(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Spinner />

  const displayUser = profile || user
  const initial = displayUser?.displayName
    ? displayUser.displayName.substring(0, 2)
    : (displayUser?.email ? displayUser.email.substring(0, 2) : 'U')

  return (
    <div className="space-y-6 max-w-md">


      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">Profile</h1>
        <p className="text-xs text-slate-500 mt-1">Manage your account information and credentials</p>
      </div>


      <div className="bg-white border border-slate-200 rounded p-6 space-y-4">


        <div className="w-12 h-12 bg-slate-800 text-white font-bold text-sm flex items-center justify-center rounded uppercase">
          {initial}
        </div>

        <div className="w-full space-y-3 pt-2">
          <div className="p-3 bg-slate-50 border border-slate-200 rounded">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Full Name</span>
            <span className="text-xs font-bold text-slate-800 mt-0.5 block">{displayUser?.displayName || '—'}</span>
          </div>

          <div className="p-3 bg-slate-50 border border-slate-200 rounded">
            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">Email Address</span>
            <span className="text-xs font-semibold text-slate-700 mt-0.5 block truncate">{displayUser?.email || '—'}</span>
          </div>

          <div className="p-3 bg-slate-50 border border-slate-200 rounded flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider block">User ID</span>
              <span className="text-xs font-extrabold text-slate-900 mt-0.5 block">{displayUser?.userId || displayUser?.id || '—'}</span>
            </div>
            <button
              onClick={() => {
                navigator.clipboard.writeText(String(displayUser?.userId || displayUser?.id))
                alert('User ID copied to clipboard!')
              }}
              className="text-[10px] font-bold text-slate-700 bg-white border border-slate-250 rounded px-2.5 py-1 hover:bg-slate-50 cursor-pointer"
            >
              Copy ID
            </button>
          </div>
        </div>
      </div>


      <div className="text-xs text-slate-700 bg-slate-50 border border-slate-200 rounded p-3">
        Share your <strong>User ID</strong> with other members so they can add you to their expense groups.
      </div>

    </div>
  )
}
