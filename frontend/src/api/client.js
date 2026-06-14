import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)


export const login = (email, password) =>
  api.post('/auth/login', { email, password })
export const register = (email, password, displayName) =>
  api.post('/auth/register', { email, password, displayName })
export const getMe = () => api.get('/auth/me')


export const getGroups = () => api.get('/groups')
export const getGroup = (id) => api.get(`/groups/${id}`)
export const createGroup = (name, description) =>
  api.post('/groups', { name, description })
export const updateGroup = (id, name, description) =>
  api.put(`/groups/${id}`, { name, description })
export const deleteGroup = (id) => api.delete(`/groups/${id}`)


export const getMembers = (groupId) => api.get(`/groups/${groupId}/members`)
export const getMemberHistory = (groupId) =>
  api.get(`/groups/${groupId}/members/history`)
export const addMember = (groupId, userId, joinedAt) =>
  api.post(`/groups/${groupId}/members`, { userId, joinedAt })
export const removeMember = (groupId, userId, leftAt) =>
  api.delete(`/groups/${groupId}/members/${userId}`, { data: { leftAt } })


export const getExpenses = (groupId) =>
  api.get(`/groups/${groupId}/expenses`)
export const getExpense = (groupId, expenseId) =>
  api.get(`/groups/${groupId}/expenses/${expenseId}`)
export const createExpense = (groupId, data) =>
  api.post(`/groups/${groupId}/expenses`, data)
export const updateExpense = (groupId, expenseId, data) =>
  api.put(`/groups/${groupId}/expenses/${expenseId}`, data)
export const deleteExpense = (groupId, expenseId) =>
  api.delete(`/groups/${groupId}/expenses/${expenseId}`)


export const getBalances = (groupId) =>
  api.get(`/groups/${groupId}/expenses/balances`)
export const getSuggestedSettlements = (groupId) =>
  api.get(`/groups/${groupId}/expenses/balances/settlements`)


export const getSettlements = (groupId) =>
  api.get(`/groups/${groupId}/settlements`)
export const recordSettlement = (groupId, data) =>
  api.post(`/groups/${groupId}/settlements`, data)
export const deleteSettlement = (groupId, settlementId) =>
  api.delete(`/groups/${groupId}/settlements/${settlementId}`)


export const uploadCsv = (groupId, formData) =>
  api.post(`/groups/${groupId}/imports`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
export const getImportJobs = (groupId) =>
  api.get(`/groups/${groupId}/imports`)
export const getImportJob = (groupId, jobId) =>
  api.get(`/groups/${groupId}/imports/${jobId}`)
export const deleteImportJob = (groupId, jobId) =>
  api.delete(`/groups/${groupId}/imports/${jobId}`)


export const submitForReview = (groupId, jobId) =>
  api.post(`/groups/${groupId}/imports/${jobId}/review/submit`)
export const getReview = (groupId, jobId) =>
  api.get(`/groups/${groupId}/imports/${jobId}/review`)
export const approveReview = (groupId, jobId, note) =>
  api.post(`/groups/${groupId}/imports/${jobId}/review/approve`, { note })
export const rejectReview = (groupId, jobId, note) =>
  api.post(`/groups/${groupId}/imports/${jobId}/review/reject`, { note })
export const resubmitReview = (groupId, jobId) =>
  api.post(`/groups/${groupId}/imports/${jobId}/review/resubmit`)
export const confirmImport = (groupId, jobId) =>
  api.post(`/groups/${groupId}/imports/${jobId}/confirm`)


export const getImportReport = (groupId, jobId) =>
  api.get(`/groups/${groupId}/imports/${jobId}/report`)
export const getFullReport = (groupId, jobId) =>
  api.get(`/groups/${groupId}/imports/${jobId}/full-report`)
export const downloadReport = (groupId, jobId) =>
  `${window.location.origin}/api/groups/${groupId}/imports/${jobId}/full-report/download`

export default api
