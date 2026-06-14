import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'

import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Groups from './pages/Groups'
import CreateGroup from './pages/CreateGroup'
import GroupDetail from './pages/GroupDetail'
import Expenses from './pages/Expenses'
import AddExpense from './pages/AddExpense'
import EditExpense from './pages/EditExpense'
import Balances from './pages/Balances'
import Settlements from './pages/Settlements'
import RecordSettlement from './pages/RecordSettlement'
import CsvImport from './pages/CsvImport'
import ImportReview from './pages/ImportReview'
import ImportReport from './pages/ImportReport'
import Profile from './pages/Profile'

function ProtectedLayout({ children }) {
  return (
    <ProtectedRoute>
      <Layout>{children}</Layout>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route path="/" element={<ProtectedLayout><Dashboard /></ProtectedLayout>} />
          <Route path="/profile" element={<ProtectedLayout><Profile /></ProtectedLayout>} />

          <Route path="/groups" element={<ProtectedLayout><Groups /></ProtectedLayout>} />
          <Route path="/groups/create" element={<ProtectedLayout><CreateGroup /></ProtectedLayout>} />
          <Route path="/groups/:id" element={<ProtectedLayout><GroupDetail /></ProtectedLayout>} />

          <Route path="/groups/:id/expenses" element={<ProtectedLayout><Expenses /></ProtectedLayout>} />
          <Route path="/groups/:id/expenses/add" element={<ProtectedLayout><AddExpense /></ProtectedLayout>} />
          <Route path="/groups/:id/expenses/:expId/edit" element={<ProtectedLayout><EditExpense /></ProtectedLayout>} />

          <Route path="/groups/:id/balances" element={<ProtectedLayout><Balances /></ProtectedLayout>} />

          <Route path="/groups/:id/settlements" element={<ProtectedLayout><Settlements /></ProtectedLayout>} />
          <Route path="/groups/:id/settlements/record" element={<ProtectedLayout><RecordSettlement /></ProtectedLayout>} />

          <Route path="/groups/:id/imports" element={<ProtectedLayout><CsvImport /></ProtectedLayout>} />
          <Route path="/groups/:id/imports/:jobId/review" element={<ProtectedLayout><ImportReview /></ProtectedLayout>} />
          <Route path="/groups/:id/imports/:jobId/report" element={<ProtectedLayout><ImportReport /></ProtectedLayout>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
