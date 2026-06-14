import { useState, useContext } from 'react'
import { NavLink, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  LayoutDashboard,
  Users,
  UserPlus,
  Info,
  Search,
  LogOut,
  X,
  Menu,
  Settings
} from 'lucide-react'

export default function Layout({ children }) {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [isMobileMenuOpen, setMobileMenuOpen] = useState(false)

  function handleSignOut() {
    signOut()
    navigate('/login')
  }

  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard, exact: true },
    { to: '/groups', label: 'Groups', icon: Users },
    { to: '/profile', label: 'Profile', icon: Settings },
  ]

  const initial = user?.displayName ? user.displayName.substring(0, 2) : (user?.email ? user.email.substring(0, 2) : 'U')

  return (
    <div className="flex h-screen bg-white font-sans text-gray-900 overflow-hidden">


      {isMobileMenuOpen && (
        <div
          className="fixed inset-0 bg-black/25 z-30 md:hidden transition-opacity"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}


      <aside className={`
        fixed md:static inset-y-0 left-0 z-40
        w-64 bg-gray-50 border-r border-gray-200
        transform transition-transform duration-300 ease-in-out
        flex flex-col h-screen overflow-y-auto shrink-0
        ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
      `}>

        <div className="p-6 flex items-center justify-between">
          <div
            onClick={() => { navigate('/'); setMobileMenuOpen(false); }}
            className="flex items-center gap-2 text-blue-600 font-bold text-xl cursor-pointer"
          >
            <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center border-2 border-blue-500">
              <span className="text-blue-600">S</span>
            </div>
            <span>SplitApp</span>
          </div>
          <button className="md:hidden text-gray-500" onClick={() => setMobileMenuOpen(false)}>
            <X size={20} />
          </button>
        </div>


        <div
          className="px-4 mb-6 cursor-pointer hover:opacity-90 transition"
          onClick={() => { navigate('/profile'); setMobileMenuOpen(false); }}
        >
          <div className="bg-[#e6f0fa] rounded-xl p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-yellow-400 border-2 border-white flex items-center justify-center text-sm font-bold text-white uppercase shrink-0">
              {initial}
            </div>
            <div className="overflow-hidden">
              <h3 className="font-semibold text-gray-900 text-sm truncate">
                {user?.displayName || 'User'}
              </h3>
              <p className="text-xs text-gray-500 truncate">{user?.email}</p>
            </div>
          </div>
        </div>


        <nav className="flex-1 px-2 space-y-1">
          {navItems.map((item) => {
            const isActive = item.exact
              ? location.pathname === item.to
              : location.pathname.startsWith(item.to)

            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.exact}
                onClick={() => setMobileMenuOpen(false)}
                className={`
                  w-full flex items-center gap-3 px-4 py-3 text-sm font-medium rounded-lg transition-colors
                  ${isActive
                    ? 'bg-[#f0f4f8] text-blue-700 border-l-4 border-blue-600 font-semibold'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900 border-l-4 border-transparent'}
                `}
              >
                <item.icon size={18} className={isActive ? 'text-blue-600' : 'text-gray-400'} />
                {item.label}
              </NavLink>
            );
          })}
        </nav>


        <div className="p-4 border-t border-gray-200">
          <button
            onClick={handleSignOut}
            className="w-full flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
          >
            <LogOut size={18} />
            Logout
          </button>
        </div>
      </aside>


      <div className="flex-1 flex flex-col h-screen overflow-hidden">


        <header className="h-16 bg-white border-b border-gray-100 flex items-center justify-between px-6 shrink-0">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="p-2 text-gray-600 hover:bg-gray-100 rounded-md md:hidden"
            >
              <Menu size={20} />
            </button>
            <div className="relative hidden sm:block">
              <Search size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search..."
                className="pl-10 pr-4 py-2 bg-gray-50 border-none rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 w-64"
              />
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div
              onClick={() => navigate('/profile')}
              className="w-8 h-8 rounded-full bg-yellow-400 border-2 border-white shadow-sm flex items-center justify-center font-bold text-xs text-white uppercase cursor-pointer hover:opacity-90 shrink-0"
            >
              {initial}
            </div>
          </div>
        </header>


        <main className="flex-1 overflow-y-auto p-6 lg:p-10 bg-white">
          <div className="max-w-6xl mx-auto">
            {children}
          </div>
        </main>
      </div>

    </div>
  )
}
