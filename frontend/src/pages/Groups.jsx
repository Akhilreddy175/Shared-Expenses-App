import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getGroups } from '../api/client'
import Spinner from '../components/Spinner'
import { Users, PlusCircle } from 'lucide-react'

export default function Groups() {
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        const res = await getGroups()
        setGroups(res.data)
      } catch (error) {
        console.error("Error fetching groups:", error)
      } finally {
        setLoading(false)
      }
    }
    fetchGroups()
  }, [])

  if (loading) return <Spinner />

  return (
    <div className="animate-fadeIn space-y-6 text-gray-900">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Your Groups</h1>
        <p className="text-xs text-gray-500 mt-1">Manage and view your shared household accounts</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

        <div
          onClick={() => navigate('/groups/create')}
          className="bg-gradient-to-br from-[#8fa5cc] to-[#6a7fa8] rounded-xl h-64 flex flex-col items-center justify-center text-white cursor-pointer hover:shadow-lg transition-shadow duration-200"
        >
          <div className="mb-3 relative">
            <Users size={48} className="opacity-90" />
            <div className="bg-white rounded-full p-1 absolute -right-2 -bottom-2 text-[#6a7fa8]">
              <PlusCircle size={16} />
            </div>
          </div>
          <h3 className="text-xl font-semibold">Create new group!</h3>
        </div>


        {groups.map((group) => (
          <div
            key={group.id}
            onClick={() => navigate(`/groups/${group.id}`)}
            className="bg-white border border-gray-200 rounded-xl h-64 p-6 flex flex-col justify-between cursor-pointer hover:shadow-md transition-shadow duration-200"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-xl uppercase">
                  {group.name.charAt(0)}
                </div>
                <span className="text-xs font-semibold px-2 py-1 bg-gray-100 rounded-full text-gray-600 uppercase">
                  ID: {group.id}
                </span>
              </div>
              <h3 className="text-lg font-bold text-gray-900 mb-1">{group.name}</h3>
              <p className="text-sm text-gray-500 line-clamp-2">{group.description || 'No description provided.'}</p>
            </div>

            <div className="border-t border-gray-100 pt-4 mt-auto">
              <span className="text-xs font-semibold text-blue-600 hover:underline flex items-center gap-1">
                View Details &rarr;
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
