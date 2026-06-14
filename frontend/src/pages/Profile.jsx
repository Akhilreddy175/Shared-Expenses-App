import { useEffect, useState } from 'react'
import { getMe } from '../api/client'
import { useAuth } from '../context/AuthContext'
import Spinner from '../components/Spinner'

export default function Profile() {
  const { user, signOut } = useAuth()
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

  return (
    <div className="space-y-4 max-w-md">
      <h1 className="text-xl font-semibold text-gray-800">Profile</h1>

      <div className="bg-white border border-gray-200 rounded p-4 space-y-3">
        <div>
          <p className="text-xs text-gray-500">Name</p>
          <p className="text-sm font-medium text-gray-800">{displayUser?.displayName || '—'}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500">Email</p>
          <p className="text-sm text-gray-700">{displayUser?.email || '—'}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500">User ID</p>
          <p className="text-sm text-gray-400">{displayUser?.userId || displayUser?.id || '—'}</p>
        </div>
      </div>

      <div className="text-xs text-gray-400 bg-blue-50 border border-blue-100 rounded px-3 py-2">
        Share your User ID with other group members so they can add you to groups.
      </div>
    </div>
  )
}
