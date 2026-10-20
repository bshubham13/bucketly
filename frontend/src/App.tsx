import { useState, useEffect } from 'react'
import './App.css'
import { dreamApi, type DreamRequest, type DreamResponse } from './services/dreamService'
import { memoryApi, type MemoryCreateRequest, type MemoryUpdateRequest, type MemoryResponse } from './services/memoryService'
import { authService, type AuthResponse } from './services/authService'
import { ApiError } from './services/apiClient'

// Simple, beginner-friendly TypeScript type representing one bucket list goal
export interface DreamItem {
  id: number
  title: string
  category: string
  status: 'In Progress' | 'Completed' | 'Not Started'
  progress: number
  targetDate: string
  description: string
}

// Convert backend DreamResponse DTO to UI DreamItem with calculated progress
function toDreamItem(dto: DreamResponse): DreamItem {
  let progress = 0
  const normalizedStatus = (['In Progress', 'Completed', 'Not Started'].includes(dto.status)
    ? dto.status
    : 'Not Started') as 'In Progress' | 'Completed' | 'Not Started'

  if (normalizedStatus === 'Completed') {
    progress = 100
  } else if (normalizedStatus === 'In Progress') {
    progress = 40
  } else {
    progress = 0
  }

  return {
    id: dto.id,
    title: dto.title,
    category: dto.category,
    status: normalizedStatus,
    progress,
    targetDate: dto.targetDate || 'Flexible Target',
    description: dto.description || 'No description provided yet.'
  }
}

// Reusable Dream Card component to keep the code clean and avoid duplication
function DreamCard({
  dream,
  onEdit,
  onDelete
}: {
  dream: DreamItem
  onEdit: (dream: DreamItem) => void
  onDelete: (dream: DreamItem) => void
}) {
  const statusClass = dream.status.toLowerCase().replace(/\s+/g, '-')

  return (
    <article className="dream-card">
      <div>
        <div className="card-top">
          <span className="category-tag">{dream.category}</span>
          <span className={`status-badge ${statusClass}`}>{dream.status}</span>
        </div>

        <h4 className="dream-title">{dream.title}</h4>
        <p className="dream-desc">{dream.description}</p>
      </div>

      <div>
        {/* Progress Bar */}
        <div className="progress-container">
          <div className="progress-labels">
            <span>Milestone Progress</span>
            <span>{dream.progress}%</span>
          </div>
          <div className="progress-track" role="progressbar" aria-valuenow={dream.progress} aria-valuemin={0} aria-valuemax={100}>
            <div className="progress-fill" style={{ width: `${dream.progress}%` }} />
          </div>
        </div>

        {/* Card Footer */}
        <div className="card-footer">
          <span className="target-badge">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            {dream.targetDate}
          </span>

          <div className="card-action-group">
            <button
              type="button"
              className="card-btn edit-btn"
              onClick={() => onEdit(dream)}
              aria-label={`Edit "${dream.title}"`}
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
                <path d="m15 5 4 4"/>
              </svg>
              Edit
            </button>
            <button
              type="button"
              className="card-btn delete-btn"
              onClick={() => onDelete(dream)}
              aria-label={`Delete "${dream.title}"`}
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M3 6h18"/>
                <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
              </svg>
              Delete
            </button>
          </div>
        </div>
      </div>
    </article>
  )
}

function App() {
  // Authentication & session state
  const [currentUser, setCurrentUser] = useState<AuthResponse | null>(null)
  const [isAuthChecking, setIsAuthChecking] = useState<boolean>(true)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [authGlobalMessage, setAuthGlobalMessage] = useState<string | null>(null)

  // Login form state
  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [loginError, setLoginError] = useState<string | null>(null)
  const [isLoggingIn, setIsLoggingIn] = useState(false)

  // Register form state
  const [regFullName, setRegFullName] = useState('')
  const [regEmail, setRegEmail] = useState('')
  const [regPassword, setRegPassword] = useState('')
  const [regError, setRegError] = useState<string | null>(null)
  const [regValidationErrors, setRegValidationErrors] = useState<Record<string, string>>({})
  const [isRegistering, setIsRegistering] = useState(false)

  // Navigation tab state: 'Dashboard' | 'My Bucket List' | 'Memory Wall'
  const [activeNav, setActiveNav] = useState<'Dashboard' | 'My Bucket List' | 'Memory Wall'>('Dashboard')

  // Main state holding dreams (backend is the single source of truth)
  const [dreams, setDreams] = useState<DreamItem[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [fetchError, setFetchError] = useState<string | null>(null)

  // Main state holding memories for Memory Wall
  const [memories, setMemories] = useState<MemoryResponse[]>([])
  const [isMemoriesLoading, setIsMemoriesLoading] = useState<boolean>(false)
  const [memoriesError, setMemoriesError] = useState<string | null>(null)

  // Filter state for dream cards on the My Bucket List page
  const [activeFilter, setActiveFilter] = useState<'All' | 'In Progress' | 'Completed' | 'Not Started'>('All')

  // Modal dialog state for Dreams ('add' | 'edit')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState<'add' | 'edit'>('add')
  const [editingDreamId, setEditingDreamId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete confirmation modal state for Dreams
  const [deletingDream, setDeletingDream] = useState<DreamItem | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Form input states for Dreams
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('Travel & Nature')
  const [description, setDescription] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [status, setStatus] = useState<'Not Started' | 'In Progress' | 'Completed'>('Not Started')
  const [titleError, setTitleError] = useState('')

  // Modal dialog state for Memories ('add' | 'edit')
  const [isMemoryModalOpen, setIsMemoryModalOpen] = useState(false)
  const [memoryModalMode, setMemoryModalMode] = useState<'add' | 'edit'>('add')
  const [editingMemoryId, setEditingMemoryId] = useState<number | null>(null)
  const [memoryFormError, setMemoryFormError] = useState<string | null>(null)
  const [isSubmittingMemory, setIsSubmittingMemory] = useState(false)

  // Delete confirmation modal state for Memories
  const [deletingMemory, setDeletingMemory] = useState<MemoryResponse | null>(null)
  const [deleteMemoryError, setDeleteMemoryError] = useState<string | null>(null)
  const [isDeletingMemory, setIsDeletingMemory] = useState(false)

  // Form input states for Memories
  const [selectedDreamId, setSelectedDreamId] = useState<number | ''>('')
  const [completedDate, setCompletedDate] = useState('')
  const [story, setStory] = useState('')

  // Load dreams from backend API
  const loadDreams = async () => {
    setIsLoading(true)
    setFetchError(null)
    try {
      const data = await dreamApi.getAll()
      setDreams(data.map(toDreamItem))
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setCurrentUser(null)
        setDreams([])
        setAuthGlobalMessage('Your session has expired. Please sign in again.')
        return
      }
      const message = err instanceof Error ? err.message : 'Unable to connect to Bucketly API. Ensure backend is running on http://localhost:8080.'
      setFetchError(message)
      setDreams([])
    } finally {
      setIsLoading(false)
    }
  }

  // Load memories from backend API
  const loadMemories = async () => {
    setIsMemoriesLoading(true)
    setMemoriesError(null)
    try {
      const data = await memoryApi.getAll()
      setMemories(data)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setCurrentUser(null)
        setMemories([])
        return
      }
      const message = err instanceof Error ? err.message : 'Unable to load Memory Wall reflections from backend.'
      setMemoriesError(message)
      setMemories([])
    } finally {
      setIsMemoriesLoading(false)
    }
  }

  // Restore session on initial load and handle StrictMode double-mount gracefully
  useEffect(() => {
    let isMounted = true

    const initializeAuth = async () => {
      try {
        const user = await authService.getMe()
        if (!isMounted) return
        if (user) {
          setCurrentUser(user)
          await Promise.all([loadDreams(), loadMemories()])
        } else {
          setCurrentUser(null)
          // Prime CSRF token for future login/register
          await authService.initCsrf()
        }
      } catch {
        if (!isMounted) return
        setCurrentUser(null)
        await authService.initCsrf()
      } finally {
        if (isMounted) {
          setIsAuthChecking(false)
        }
      }
    }

    initializeAuth()

    return () => {
      isMounted = false
    }
  }, [])

  // Handle Login submission
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!loginEmail.trim() || !loginPassword) {
      setLoginError('Please enter both email and password.')
      return
    }
    setIsLoggingIn(true)
    setLoginError(null)
    setAuthGlobalMessage(null)
    try {
      const user = await authService.login({
        email: loginEmail.trim(),
        password: loginPassword
      })
      setCurrentUser(user)
      setLoginPassword('')
      await Promise.all([loadDreams(), loadMemories()])
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Invalid email or password.'
      setLoginError(message)
    } finally {
      setIsLoggingIn(false)
    }
  }

  // Handle Register submission
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setRegError(null)
    setRegValidationErrors({})
    setAuthGlobalMessage(null)

    if (!regFullName.trim()) {
      setRegError('Please enter your full name.')
      return
    }
    if (!regEmail.trim()) {
      setRegError('Please enter an email address.')
      return
    }
    if (regPassword.length < 8) {
      setRegError('Password must be at least 8 characters long.')
      return
    }

    setIsRegistering(true)
    try {
      await authService.register({
        fullName: regFullName.trim(),
        email: regEmail.trim(),
        password: regPassword
      })
      // Immediately establish session via login
      const user = await authService.login({
        email: regEmail.trim(),
        password: regPassword
      })
      setCurrentUser(user)
      setRegFullName('')
      setRegEmail('')
      setRegPassword('')
      await Promise.all([loadDreams(), loadMemories()])
    } catch (err: unknown) {
      if (err instanceof ApiError && err.validationErrors) {
        setRegValidationErrors(err.validationErrors)
        setRegError(err.message)
      } else {
        const message = err instanceof Error ? err.message : 'Registration failed. Please try again.'
        setRegError(message)
      }
    } finally {
      setIsRegistering(false)
    }
  }

  // Handle Logout action
  const handleLogout = async () => {
    try {
      await authService.logout()
    } catch {
      // Clear frontend auth state even if server logout request encounters issues
    } finally {
      setCurrentUser(null)
      setDreams([])
      setMemories([])
      setAuthGlobalMessage('You have been signed out successfully.')
      await authService.initCsrf()
    }
  }

  // Eligible dreams for memory creation: dreams that do NOT already have a memory
  const eligibleDreams = dreams.filter(
    (d) => !memories.some((m) => m.dreamId === d.id)
  )

  // Filter items based on activeFilter selection
  const filteredDreams = dreams.filter((dream) => {
    if (activeFilter === 'All') return true
    return dream.status === activeFilter
  })

  // Quick stats computed reactively from the stateful dreams array
  const totalCount = dreams.length
  const completedCount = dreams.filter(d => d.status === 'Completed').length
  const inProgressCount = dreams.filter(d => d.status === 'In Progress').length
  const inProgressDreams = dreams.filter(d => d.status === 'In Progress')

  // Subtitles customized for each page
  const pageSubtitles = {
    'Dashboard': 'Make Life Count — Overview of your life milestones and current focus.',
    'My Bucket List': 'Browse, filter, and track all your lifelong dreams and aspirations.',
    'Memory Wall': 'A visual journal celebrating every accomplished dream and reflection.'
  }

  // Today's date in YYYY-MM-DD format for date picker max attribute
  const todayStr = new Date().toISOString().split('T')[0]

  // --------------------------------------------------------------------------
  // Dream Modal Handlers
  // --------------------------------------------------------------------------
  const handleOpenAddModal = () => {
    setModalMode('add')
    setEditingDreamId(null)
    setTitle('')
    setCategory('Travel & Nature')
    setDescription('')
    setTargetDate('')
    setStatus('Not Started')
    setTitleError('')
    setActionError(null)
    setIsModalOpen(true)
  }

  const handleOpenEditModal = (dream: DreamItem) => {
    setModalMode('edit')
    setEditingDreamId(dream.id)
    setTitle(dream.title)
    setCategory(dream.category)
    setDescription(dream.description === 'No description provided yet.' ? '' : dream.description)
    setTargetDate(dream.targetDate === 'Flexible Target' ? '' : dream.targetDate)
    setStatus(dream.status)
    setTitleError('')
    setActionError(null)
    setIsModalOpen(true)
  }

  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingDreamId(null)
    setTitleError('')
    setActionError(null)
  }

  const handleOpenDeleteModal = (dream: DreamItem) => {
    setDeletingDream(dream)
    setDeleteError(null)
  }

  const handleCloseDeleteModal = () => {
    setDeletingDream(null)
    setDeleteError(null)
  }

  const handleConfirmDelete = async () => {
    if (!deletingDream) return
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await dreamApi.delete(deletingDream.id)
      setDreams((prev) => prev.filter((d) => d.id !== deletingDream.id))
      // If the deleted dream had a memory, remove it from memory state as well
      setMemories((prev) => prev.filter((m) => m.dreamId !== deletingDream.id))
      setDeletingDream(null)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setCurrentUser(null)
        setDreams([])
        setAuthGlobalMessage('Your session has expired. Please sign in again.')
        return
      }
      const message = err instanceof Error ? err.message : 'Failed to delete dream. Please check backend connection.'
      setDeleteError(message)
    } finally {
      setIsDeleting(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!title.trim()) {
      setTitleError('Please enter a title for your dream.')
      return
    }

    setIsSubmitting(true)
    setActionError(null)

    const payload: DreamRequest = {
      title: title.trim(),
      category,
      status,
      targetDate: targetDate.trim() || undefined,
      description: description.trim() || undefined
    }

    try {
      if (modalMode === 'add') {
        const created = await dreamApi.create(payload)
        const newDream = toDreamItem(created)
        setDreams((prev) => [newDream, ...prev])
        handleCloseModal()
      } else if (modalMode === 'edit' && editingDreamId !== null) {
        const updated = await dreamApi.update(editingDreamId, payload)
        const updatedDream = toDreamItem(updated)
        setDreams((prev) =>
          prev.map((d) => (d.id === editingDreamId ? updatedDream : d))
        )
        handleCloseModal()
      }
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setCurrentUser(null)
        setDreams([])
        setAuthGlobalMessage('Your session has expired. Please sign in again.')
        return
      }
      const message = err instanceof Error ? err.message : 'Operation failed. Please check backend connection.'
      setActionError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  // --------------------------------------------------------------------------
  // Memory Modal Handlers
  // --------------------------------------------------------------------------
  const handleOpenAddMemoryModal = (preselectedDreamId?: number) => {
    setMemoryModalMode('add')
    setEditingMemoryId(null)

    if (preselectedDreamId && eligibleDreams.some((d) => d.id === preselectedDreamId)) {
      setSelectedDreamId(preselectedDreamId)
    } else if (eligibleDreams.length > 0) {
      setSelectedDreamId(eligibleDreams[0].id)
    } else {
      setSelectedDreamId('')
    }

    setCompletedDate(todayStr)
    setStory('')
    setMemoryFormError(null)
    setIsMemoryModalOpen(true)
  }

  const handleOpenEditMemoryModal = (memory: MemoryResponse) => {
    setMemoryModalMode('edit')
    setEditingMemoryId(memory.id)
    setSelectedDreamId(memory.dreamId)
    setCompletedDate(memory.completedDate)
    setStory(memory.story || '')
    setMemoryFormError(null)
    setIsMemoryModalOpen(true)
  }

  const handleCloseMemoryModal = () => {
    setIsMemoryModalOpen(false)
    setEditingMemoryId(null)
    setSelectedDreamId('')
    setStory('')
    setMemoryFormError(null)
  }

  const handleOpenDeleteMemoryModal = (memory: MemoryResponse) => {
    setDeletingMemory(memory)
    setDeleteMemoryError(null)
  }

  const handleCloseDeleteMemoryModal = () => {
    setDeletingMemory(null)
    setDeleteMemoryError(null)
  }

  const handleConfirmDeleteMemory = async () => {
    if (!deletingMemory) return
    setIsDeletingMemory(true)
    setDeleteMemoryError(null)
    try {
      await memoryApi.delete(deletingMemory.id)
      setMemories((prev) => prev.filter((m) => m.id !== deletingMemory.id))
      setDeletingMemory(null)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setCurrentUser(null)
        setMemories([])
        setAuthGlobalMessage('Your session has expired. Please sign in again.')
        return
      }
      const message = err instanceof Error ? err.message : 'Failed to delete memory reflection.'
      setDeleteMemoryError(message)
    } finally {
      setIsDeletingMemory(false)
    }
  }

  const handleSubmitMemory = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!completedDate) {
      setMemoryFormError('Please select a completion date.')
      return
    }

    if (completedDate > todayStr) {
      setMemoryFormError('Completion date cannot be in the future.')
      return
    }

    setIsSubmittingMemory(true)
    setMemoryFormError(null)

    try {
      if (memoryModalMode === 'add') {
        if (!selectedDreamId) {
          setMemoryFormError('Please select an eligible dream to commemorate.')
          setIsSubmittingMemory(false)
          return
        }

        const payload: MemoryCreateRequest = {
          dreamId: Number(selectedDreamId),
          completedDate,
          story: story.trim() || undefined
        }

        const created = await memoryApi.create(payload)
        setMemories((prev) => [created, ...prev])
        // Reload dreams to synchronize the dream status change to Completed in the dashboard
        await loadDreams()
        handleCloseMemoryModal()
      } else if (memoryModalMode === 'edit' && editingMemoryId !== null) {
        const payload: MemoryUpdateRequest = {
          completedDate,
          story: story.trim() || undefined
        }

        const updated = await memoryApi.update(editingMemoryId, payload)
        setMemories((prev) =>
          prev.map((m) => (m.id === editingMemoryId ? updated : m))
        )
        handleCloseMemoryModal()
      }
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        setCurrentUser(null)
        setMemories([])
        setAuthGlobalMessage('Your session has expired. Please sign in again.')
        return
      }
      const message = err instanceof Error ? err.message : 'Failed to save memory reflection.'
      setMemoryFormError(message)
    } finally {
      setIsSubmittingMemory(false)
    }
  }

  // Initial session restoration loading screen
  if (isAuthChecking) {
    return (
      <div className="auth-loading-screen" role="status" aria-live="polite">
        <div className="loading-spinner" aria-hidden="true" />
        <h3 className="state-title">Restoring Session...</h3>
        <p className="state-text">Connecting to Bucketly.</p>
      </div>
    )
  }

  // Unauthenticated view: Login & Registration screen
  if (!currentUser) {
    return (
      <div className="auth-page-container">
        <div className="auth-card">
          {/* Brand Logo & Header */}
          <div className="auth-brand-section">
            <div className="brand-logo-badge" aria-hidden="true">✦</div>
            <div>
              <h1 className="brand-title">Bucketly</h1>
              <p className="brand-tagline">Make Life Count</p>
            </div>
          </div>

          {/* Flash / Status Message */}
          {authGlobalMessage && (
            <div className="auth-info-banner" role="status">
              <span>{authGlobalMessage}</span>
            </div>
          )}

          {/* Mode Switcher Tabs */}
          <div className="auth-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={authMode === 'login'}
              className={`auth-tab ${authMode === 'login' ? 'active' : ''}`}
              onClick={() => {
                setAuthMode('login')
                setLoginError(null)
                setRegError(null)
                setRegValidationErrors({})
                setAuthGlobalMessage(null)
              }}
            >
              Sign In
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={authMode === 'register'}
              className={`auth-tab ${authMode === 'register' ? 'active' : ''}`}
              onClick={() => {
                setAuthMode('register')
                setLoginError(null)
                setRegError(null)
                setRegValidationErrors({})
                setAuthGlobalMessage(null)
              }}
            >
              Create Account
            </button>
          </div>

          {authMode === 'login' ? (
            <form onSubmit={handleLogin} className="auth-form" noValidate>
              {loginError && (
                <div className="form-action-error" role="alert">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>{loginError}</span>
                </div>
              )}

              <div className="form-group">
                <label htmlFor="login-email" className="form-label">
                  Email Address <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="login-email"
                  type="email"
                  className="form-input"
                  placeholder="name@example.com"
                  value={loginEmail}
                  onChange={(e) => setLoginEmail(e.target.value)}
                  autoComplete="email"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="login-password" className="form-label">
                  Password <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="login-password"
                  type="password"
                  className="form-input"
                  placeholder="••••••••"
                  value={loginPassword}
                  onChange={(e) => setLoginPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </div>

              <button
                type="submit"
                className="btn-primary auth-submit-btn"
                disabled={isLoggingIn}
              >
                {isLoggingIn ? 'Signing In...' : 'Sign In to Bucketly'}
              </button>

              <p className="auth-switch-prompt">
                Don't have an account?{' '}
                <button
                  type="button"
                  className="auth-inline-link"
                  onClick={() => {
                    setAuthMode('register')
                    setLoginError(null)
                    setAuthGlobalMessage(null)
                  }}
                >
                  Create one now →
                </button>
              </p>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="auth-form" noValidate>
              {regError && (
                <div className="form-action-error" role="alert">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>{regError}</span>
                </div>
              )}

              <div className="form-group">
                <label htmlFor="reg-fullname" className="form-label">
                  Full Name <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="reg-fullname"
                  type="text"
                  className={`form-input ${regValidationErrors.fullName ? 'has-error' : ''}`}
                  placeholder="Jane Doe"
                  value={regFullName}
                  onChange={(e) => setRegFullName(e.target.value)}
                  autoComplete="name"
                  required
                />
                {regValidationErrors.fullName && (
                  <p className="form-error-msg" role="alert">{regValidationErrors.fullName}</p>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="reg-email" className="form-label">
                  Email Address <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="reg-email"
                  type="email"
                  className={`form-input ${regValidationErrors.email ? 'has-error' : ''}`}
                  placeholder="name@example.com"
                  value={regEmail}
                  onChange={(e) => setRegEmail(e.target.value)}
                  autoComplete="email"
                  required
                />
                {regValidationErrors.email && (
                  <p className="form-error-msg" role="alert">{regValidationErrors.email}</p>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="reg-password" className="form-label">
                  Password <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="reg-password"
                  type="password"
                  className={`form-input ${regValidationErrors.password ? 'has-error' : ''}`}
                  placeholder="At least 8 characters"
                  value={regPassword}
                  onChange={(e) => setRegPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                />
                {regValidationErrors.password && (
                  <p className="form-error-msg" role="alert">{regValidationErrors.password}</p>
                )}
              </div>

              <button
                type="submit"
                className="btn-primary auth-submit-btn"
                disabled={isRegistering}
              >
                {isRegistering ? 'Creating Account...' : 'Create Account'}
              </button>

              <p className="auth-switch-prompt">
                Already have an account?{' '}
                <button
                  type="button"
                  className="auth-inline-link"
                  onClick={() => {
                    setAuthMode('login')
                    setRegError(null)
                    setRegValidationErrors({})
                    setAuthGlobalMessage(null)
                  }}
                >
                  Sign in here →
                </button>
              </p>
            </form>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="dashboard-layout">
      {/* -------------------------------------------------------------------- */}
      {/* Sidebar Navigation                                                  */}
      {/* -------------------------------------------------------------------- */}
      <aside className="sidebar" aria-label="Sidebar Navigation">
        <div>
          {/* Brand Logo & Tagline */}
          <div className="brand-section">
            <div className="brand-logo-badge" aria-hidden="true">✦</div>
            <div>
              <h1 className="brand-title">Bucketly</h1>
              <p className="brand-tagline">Make Life Count</p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="sidebar-nav">
            <button
              type="button"
              className={`nav-item-btn ${activeNav === 'Dashboard' ? 'active' : ''}`}
              onClick={() => setActiveNav('Dashboard')}
            >
              <span className="nav-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect width="7" height="9" x="3" y="3" rx="1"/>
                  <rect width="7" height="5" x="14" y="3" rx="1"/>
                  <rect width="7" height="9" x="14" y="12" rx="1"/>
                  <rect width="7" height="5" x="3" y="16" rx="1"/>
                </svg>
              </span>
              Dashboard
            </button>

            <button
              type="button"
              className={`nav-item-btn ${activeNav === 'My Bucket List' ? 'active' : ''}`}
              onClick={() => setActiveNav('My Bucket List')}
            >
              <span className="nav-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="m9 11 3 3L22 4"/>
                  <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
                </svg>
              </span>
              My Bucket List
            </button>

            <button
              type="button"
              className={`nav-item-btn ${activeNav === 'Memory Wall' ? 'active' : ''}`}
              onClick={() => setActiveNav('Memory Wall')}
            >
              <span className="nav-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
              </span>
              Memory Wall
            </button>
          </nav>
        </div>

        {/* User Profile Snippet */}
        <div className="sidebar-profile">
          <div className="profile-info-group">
            <div className="profile-avatar" aria-hidden="true">
              {currentUser?.fullName ? currentUser.fullName.charAt(0).toUpperCase() : 'B'}
            </div>
            <div className="profile-text-group">
              <p className="profile-name" title={currentUser?.fullName || 'Explorer'}>
                {currentUser?.fullName || 'Explorer'}
              </p>
              <p className="profile-tier" title={currentUser?.email || 'Dreamer'}>
                {currentUser?.email || 'Dreamer Tier'}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="sign-out-btn"
            onClick={handleLogout}
            aria-label="Sign out of Bucketly"
            title="Sign Out"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            Sign Out
          </button>
        </div>
      </aside>

      {/* -------------------------------------------------------------------- */}
      {/* Main Content Area                                                   */}
      {/* -------------------------------------------------------------------- */}
      <main className="main-content">
        {/* Top Header */}
        <header className="top-bar">
          <div>
            <h2 className="page-title">{activeNav}</h2>
            <p className="page-subtitle">{pageSubtitles[activeNav]}</p>
          </div>
          {activeNav === 'Memory Wall' ? (
            <button
              type="button"
              className="add-dream-btn"
              onClick={() => handleOpenAddMemoryModal()}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M12 5v14"/>
                <path d="M5 12h14"/>
              </svg>
              Record Memory
            </button>
          ) : (
            <button
              type="button"
              className="add-dream-btn"
              onClick={handleOpenAddModal}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M5 12h14"/>
                <path d="M12 5v14"/>
              </svg>
              Add New Dream
            </button>
          )}
        </header>

        {/* ------------------------------------------------------------------ */}
        {/* GLOBAL ASYNC STATE 1: Dreams Loading State                         */}
        {/* ------------------------------------------------------------------ */}
        {isLoading && (
          <div className="state-card loading-state" role="status" aria-live="polite">
            <div className="loading-spinner" aria-hidden="true" />
            <h3 className="state-title">Connecting to Bucketly API...</h3>
            <p className="state-text">Loading your life milestones and bucket list from the backend server.</p>
          </div>
        )}

        {/* ------------------------------------------------------------------ */}
        {/* GLOBAL ASYNC STATE 2: Dreams Connection Error State                */}
        {/* ------------------------------------------------------------------ */}
        {!isLoading && fetchError && (
          <div className="state-card error-state" role="alert">
            <div className="state-icon-badge error" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </div>
            <h3 className="state-title">Backend Connection Failed</h3>
            <p className="state-text">{fetchError}</p>
            <button type="button" className="btn-primary" onClick={loadDreams} style={{ marginTop: '1.25rem' }}>
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ marginRight: '0.4rem', verticalAlign: 'middle' }}>
                <path d="M21 2v6h-6"/>
                <path d="M3 12a9 9 0 0 1 15-6.7L21 8"/>
                <path d="M3 22v-6h6"/>
                <path d="M21 12a9 9 0 0 1-15 6.7L3 16"/>
              </svg>
              Retry Connection
            </button>
          </div>
        )}

        {/* ------------------------------------------------------------------ */}
        {/* NORMAL VIEWS (Rendered when not loading and no fetchError)          */}
        {/* ------------------------------------------------------------------ */}
        {!isLoading && !fetchError && (
          <>
            {/* ---------------------------------------------------------------- */}
            {/* VIEW 1: Dashboard                                                */}
            {/* ---------------------------------------------------------------- */}
            {activeNav === 'Dashboard' && (
              <>
                {/* Hero Inspiration Banner */}
                <section className="hero-banner" aria-label="Inspiration of the day">
                  <span className="hero-tag">🌟 Life Philosophy</span>
                  <blockquote className="hero-quote">
                    “The purpose of life is to live it, to taste experience to the utmost, to reach out eagerly and without fear for newer and richer experience.”
                  </blockquote>
                  <p className="hero-author">— Eleanor Roosevelt</p>
                </section>

                {/* Overview Statistics Grid */}
                <section className="stats-grid" aria-label="Overview Statistics">
                  <div className="stat-card">
                    <div className="stat-header">
                      <span className="stat-label">Total Dreams</span>
                      <span className="stat-icon-pill" style={{ background: 'rgba(168, 85, 247, 0.15)', color: '#c084fc' }} aria-hidden="true">✦</span>
                    </div>
                    <p className="stat-value">{totalCount}</p>
                    <p className="stat-caption">Lifelong aspirations registered</p>
                  </div>

                  <div className="stat-card">
                    <div className="stat-header">
                      <span className="stat-label">In Progress</span>
                      <span className="stat-icon-pill" style={{ background: 'rgba(56, 189, 248, 0.15)', color: '#38bdf8' }} aria-hidden="true">⚡</span>
                    </div>
                    <p className="stat-value">{inProgressCount}</p>
                    <p className="stat-caption">Actively being pursued right now</p>
                  </div>

                  <div className="stat-card">
                    <div className="stat-header">
                      <span className="stat-label">Accomplished</span>
                      <span className="stat-icon-pill" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34d399' }} aria-hidden="true">✓</span>
                    </div>
                    <p className="stat-value">{completedCount}</p>
                    <p className="stat-caption">Memories etched in stone</p>
                  </div>

                  <div className="stat-card">
                    <div className="stat-header">
                      <span className="stat-label">Completion Rate</span>
                      <span className="stat-icon-pill" style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24' }} aria-hidden="true">🎯</span>
                    </div>
                    <p className="stat-value">{totalCount > 0 ? `${Math.round((completedCount / totalCount) * 100)}%` : '0%'}</p>
                    <p className="stat-caption">Overall journey progression</p>
                  </div>
                </section>

                {/* In-Progress Milestones Spotlight */}
                <section aria-label="Active Focus Milestones">
                  <div className="section-top">
                    <div>
                      <h3 className="section-title">Active Milestones</h3>
                      <p className="list-meta-text">Goals currently being pursued right now ({inProgressDreams.length})</p>
                    </div>
                    <button
                      type="button"
                      className="text-link-btn"
                      onClick={() => setActiveNav('My Bucket List')}
                    >
                      View Full Bucket List ({totalCount}) →
                    </button>
                  </div>

                  {dreams.length === 0 ? (
                    <div className="state-card empty-state">
                      <div className="state-icon-badge" aria-hidden="true">✦</div>
                      <h3 className="state-title">Your Bucket List is Empty</h3>
                      <p className="state-text">No aspirations recorded yet. Start by adding your first dream to track your life journey!</p>
                      <button type="button" className="btn-primary" onClick={handleOpenAddModal} style={{ marginTop: '1.25rem' }}>
                        + Add Your First Dream
                      </button>
                    </div>
                  ) : inProgressDreams.length === 0 ? (
                    <div className="state-card empty-state-compact">
                      <h4 className="state-title" style={{ fontSize: '1.05rem' }}>No Active Milestones In Progress</h4>
                      <p className="state-text">You have no dreams currently marked as "In Progress". Pick an existing aspiration or add a new one.</p>
                      <button type="button" className="text-link-btn" onClick={() => setActiveNav('My Bucket List')} style={{ marginTop: '0.75rem' }}>
                        Browse all dreams to get started →
                      </button>
                    </div>
                  ) : (
                    <div className="dreams-grid">
                      {inProgressDreams.map((dream) => (
                        <DreamCard
                          key={dream.id}
                          dream={dream}
                          onEdit={handleOpenEditModal}
                          onDelete={handleOpenDeleteModal}
                        />
                      ))}
                    </div>
                  )}
                </section>
              </>
            )}

            {/* ---------------------------------------------------------------- */}
            {/* VIEW 2: My Bucket List (Dedicated Exploration & Filter View)      */}
            {/* ---------------------------------------------------------------- */}
            {activeNav === 'My Bucket List' && (
              <section aria-label="My Complete Bucket List">
                <div className="section-top">
                  <div>
                    <h3 className="section-title">All Aspirations</h3>
                    <p className="list-meta-text">Showing {filteredDreams.length} of {totalCount} dreams</p>
                  </div>

                  <div className="filters-wrapper" role="group" aria-label="Filter aspirations by status">
                    {(['All', 'In Progress', 'Completed', 'Not Started'] as const).map((filterName) => (
                      <button
                        key={filterName}
                        type="button"
                        className={`filter-chip ${activeFilter === filterName ? 'active' : ''}`}
                        onClick={() => setActiveFilter(filterName)}
                      >
                        {filterName}
                      </button>
                    ))}
                  </div>
                </div>

                {dreams.length === 0 ? (
                  <div className="state-card empty-state">
                    <div className="state-icon-badge" aria-hidden="true">✦</div>
                    <h3 className="state-title">No Dreams in Your Bucket List</h3>
                    <p className="state-text">Your list is currently empty. Click the button below to add your first goal!</p>
                    <button type="button" className="btn-primary" onClick={handleOpenAddModal} style={{ marginTop: '1.25rem' }}>
                      + Add Your First Dream
                    </button>
                  </div>
                ) : filteredDreams.length > 0 ? (
                  <div className="dreams-grid">
                    {filteredDreams.map((dream) => (
                      <DreamCard
                        key={dream.id}
                        dream={dream}
                        onEdit={handleOpenEditModal}
                        onDelete={handleOpenDeleteModal}
                      />
                    ))}
                  </div>
                ) : (
                  <div className="empty-filter-state">
                    <p className="empty-state-title">No dreams found</p>
                    <p className="empty-state-text">No aspirations currently match the "{activeFilter}" filter.</p>
                    <button
                      type="button"
                      className="placeholder-action-btn"
                      style={{ marginTop: '1rem' }}
                      onClick={() => setActiveFilter('All')}
                    >
                      Reset Filter to All
                    </button>
                  </div>
                )}
              </section>
            )}

            {/* ---------------------------------------------------------------- */}
            {/* VIEW 3: Memory Wall (Milestone Memories & Reflections)             */}
            {/* ---------------------------------------------------------------- */}
            {activeNav === 'Memory Wall' && (
              <section aria-label="Memory Wall">
                <div className="section-top">
                  <div>
                    <h3 className="section-title">Milestone Reflections</h3>
                    <p className="list-meta-text">
                      Celebrating {memories.length} accomplished {memories.length === 1 ? 'experience' : 'experiences'}
                    </p>
                  </div>
                  {eligibleDreams.length > 0 && (
                    <button
                      type="button"
                      className="text-link-btn"
                      onClick={() => handleOpenAddMemoryModal()}
                    >
                      + Record Memory for Dream ({eligibleDreams.length} eligible) →
                    </button>
                  )}
                </div>

                {isMemoriesLoading ? (
                  <div className="state-card loading-state" role="status" aria-live="polite">
                    <div className="loading-spinner" aria-hidden="true" />
                    <h3 className="state-title">Loading Memory Wall...</h3>
                    <p className="state-text">Fetching your milestone memories and reflections from the backend.</p>
                  </div>
                ) : memoriesError ? (
                  <div className="state-card error-state" role="alert">
                    <div className="state-icon-badge error" aria-hidden="true">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                      </svg>
                    </div>
                    <h3 className="state-title">Unable to Load Memories</h3>
                    <p className="state-text">{memoriesError}</p>
                    <button type="button" className="btn-primary" onClick={loadMemories} style={{ marginTop: '1.25rem' }}>
                      Retry Loading Memories
                    </button>
                  </div>
                ) : memories.length === 0 ? (
                  <div className="state-card empty-state">
                    <div className="state-icon-badge" aria-hidden="true">✦</div>
                    <h3 className="state-title">Your Memory Wall is Waiting</h3>
                    <p className="state-text">
                      No memories recorded yet. When you achieve a dream, commemorate it with the completion date and your personal reflections to celebrate making life count.
                    </p>
                    {eligibleDreams.length > 0 ? (
                      <button
                        type="button"
                        className="btn-primary"
                        onClick={() => handleOpenAddMemoryModal()}
                        style={{ marginTop: '1.25rem' }}
                      >
                        + Record Your First Memory
                      </button>
                    ) : (
                      <p className="list-meta-text" style={{ marginTop: '1.25rem' }}>
                        Create your dreams in your bucket list first to record memories for them!
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="memories-grid">
                    {memories.map((mem) => (
                      <article key={mem.id} className="memory-card">
                        <div>
                          <div className="memory-card-header">
                            <span className="category-tag">{mem.dreamCategory}</span>
                            <span className="memory-date-badge">
                              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"/>
                                <polyline points="12 6 12 12 16 14"/>
                              </svg>
                              Achieved {mem.completedDate}
                            </span>
                          </div>

                          <h4 className="memory-title">{mem.dreamTitle}</h4>

                          {mem.story ? (
                            <div className="memory-story-box">
                              “{mem.story}”
                            </div>
                          ) : (
                            <p className="memory-story-empty">
                              No written reflection recorded yet.
                            </p>
                          )}
                        </div>

                        <div className="memory-card-footer">
                          <span className="memory-badge-label">
                            <span>✓</span> Memory #{mem.id}
                          </span>
                          <div className="card-action-group">
                            <button
                              type="button"
                              className="card-btn edit-btn"
                              onClick={() => handleOpenEditMemoryModal(mem)}
                              aria-label={`Edit memory for "${mem.dreamTitle}"`}
                            >
                              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
                                <path d="m15 5 4 4"/>
                              </svg>
                              Edit
                            </button>
                            <button
                              type="button"
                              className="card-btn delete-btn"
                              onClick={() => handleOpenDeleteMemoryModal(mem)}
                              aria-label={`Delete memory for "${mem.dreamTitle}"`}
                            >
                              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                <path d="M3 6h18"/>
                                <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                                <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                              </svg>
                              Delete
                            </button>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            )}
          </>
        )}
      </main>

      {/* -------------------------------------------------------------------- */}
      {/* Modal Dialog: Add / Edit Dream Form                                  */}
      {/* -------------------------------------------------------------------- */}
      {isModalOpen && (
        <div
          className="modal-overlay"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isSubmitting) handleCloseModal()
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape' && !isSubmitting) handleCloseModal()
          }}
          role="presentation"
        >
          <div
            className="modal-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
          >
            {/* Modal Header */}
            <div className="modal-header">
              <div className="modal-title-group">
                <div className="modal-badge-icon" aria-hidden="true">
                  {modalMode === 'edit' ? '✏️' : '✦'}
                </div>
                <div>
                  <h3 id="modal-title" className="modal-title">
                    {modalMode === 'edit' ? 'Edit Dream' : 'Add New Dream'}
                  </h3>
                  <p className="modal-subtitle">
                    {modalMode === 'edit'
                      ? 'Update your aspiration details and synchronize with the backend.'
                      : 'Capture a lifelong goal and start making life count.'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseModal}
                disabled={isSubmitting}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleSubmit} className="modal-form" noValidate>
              {actionError && (
                <div className="form-action-error" role="alert">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>{actionError}</span>
                </div>
              )}

              {/* Field 1: Title (Required) */}
              <div className="form-group">
                <label htmlFor="dream-title" className="form-label">
                  Dream Title
                  <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="dream-title"
                  type="text"
                  className={`form-input ${titleError ? 'has-error' : ''}`}
                  placeholder="e.g. Scuba dive the Great Barrier Reef"
                  value={title}
                  onChange={(e) => {
                    setTitle(e.target.value)
                    if (titleError) setTitleError('')
                  }}
                  aria-invalid={titleError ? 'true' : 'false'}
                  aria-describedby={titleError ? 'title-error-msg' : undefined}
                  autoFocus
                />
                {titleError && (
                  <p id="title-error-msg" className="form-error-msg" role="alert">
                    {titleError}
                  </p>
                )}
              </div>

              {/* Form Row: Category & Status */}
              <div className="form-row">
                {/* Field 2: Category (Select) */}
                <div className="form-group">
                  <label htmlFor="dream-category" className="form-label">
                    Category
                  </label>
                  <select
                    id="dream-category"
                    className="form-select"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                  >
                    <option value="Travel & Nature">Travel & Nature</option>
                    <option value="Career & Tech">Career & Tech</option>
                    <option value="Fitness & Health">Fitness & Health</option>
                    <option value="Adventure">Adventure</option>
                    <option value="Personal Growth">Personal Growth</option>
                    <option value="Creativity & Arts">Creativity & Arts</option>
                    <option value="Other">Other</option>
                  </select>
                </div>

                {/* Field 3: Status (Select) */}
                <div className="form-group">
                  <label htmlFor="dream-status" className="form-label">
                    Status
                  </label>
                  <select
                    id="dream-status"
                    className="form-select"
                    value={status}
                    onChange={(e) => setStatus(e.target.value as 'Not Started' | 'In Progress' | 'Completed')}
                  >
                    <option value="Not Started">Not Started</option>
                    <option value="In Progress">In Progress</option>
                    <option value="Completed">Completed</option>
                  </select>
                </div>
              </div>

              {/* Field 4: Target Date (Optional) */}
              <div className="form-group">
                <label htmlFor="dream-target" className="form-label">
                  Target Date
                  <span className="optional-badge">(Optional)</span>
                </label>
                <input
                  id="dream-target"
                  type="text"
                  className="form-input"
                  placeholder="e.g. Summer 2026 or Dec 2027"
                  value={targetDate}
                  onChange={(e) => setTargetDate(e.target.value)}
                />
              </div>

              {/* Field 5: Description (Optional) */}
              <div className="form-group">
                <label htmlFor="dream-desc" className="form-label">
                  Description
                  <span className="optional-badge">(Optional)</span>
                </label>
                <textarea
                  id="dream-desc"
                  className="form-textarea"
                  placeholder="Why is this meaningful? Add any specific details or vision..."
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>

              {/* Form Action Buttons */}
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleCloseModal}
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting
                    ? modalMode === 'edit' ? 'Saving Changes...' : 'Adding Dream...'
                    : modalMode === 'edit' ? 'Save Changes' : 'Add to Bucket List'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* -------------------------------------------------------------------- */}
      {/* Modal: Delete Confirmation Dialog for Dream                          */}
      {/* -------------------------------------------------------------------- */}
      {deletingDream && (
        <div
          className="modal-overlay"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isDeleting) handleCloseDeleteModal()
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape' && !isDeleting) handleCloseDeleteModal()
          }}
          role="presentation"
        >
          <div
            className="modal-dialog"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-dialog-title"
            aria-describedby="delete-dialog-desc"
            style={{ maxWidth: '460px' }}
          >
            <div className="modal-header">
              <div className="modal-title-group">
                <div className="modal-badge-icon danger" aria-hidden="true">🗑</div>
                <div>
                  <h3 id="delete-dialog-title" className="modal-title">Delete Dream?</h3>
                  <p className="modal-subtitle">This will permanently remove the dream and any associated memories.</p>
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseDeleteModal}
                disabled={isDeleting}
                aria-label="Close delete confirmation"
              >
                ✕
              </button>
            </div>

            <div style={{ padding: '1.5rem 1.75rem' }}>
              {deleteError && (
                <div className="form-action-error" role="alert">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>{deleteError}</span>
                </div>
              )}

              <p id="delete-dialog-desc" style={{ margin: 0, fontSize: '0.92rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Are you sure you want to remove this aspiration from your bucket list? The change will be immediately reflected in your backend database and dashboard statistics.
              </p>

              <div className="delete-preview-box">
                <p className="delete-preview-title">{deletingDream.title}</p>
                <div className="delete-preview-meta">
                  <span>{deletingDream.category}</span>
                  <span>•</span>
                  <span>{deletingDream.status}</span>
                  <span>•</span>
                  <span>{deletingDream.targetDate}</span>
                </div>
              </div>

              <div className="modal-actions" style={{ marginTop: 0, paddingTop: '1rem' }}>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleCloseDeleteModal}
                  disabled={isDeleting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn-danger"
                  onClick={handleConfirmDelete}
                  disabled={isDeleting}
                  autoFocus
                >
                  {isDeleting ? 'Deleting...' : 'Delete Dream'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* -------------------------------------------------------------------- */}
      {/* Modal Dialog: Add / Edit Memory Form                                 */}
      {/* -------------------------------------------------------------------- */}
      {isMemoryModalOpen && (
        <div
          className="modal-overlay"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isSubmittingMemory) handleCloseMemoryModal()
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape' && !isSubmittingMemory) handleCloseMemoryModal()
          }}
          role="presentation"
        >
          <div
            className="modal-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="memory-modal-title"
          >
            {/* Modal Header */}
            <div className="modal-header">
              <div className="modal-title-group">
                <div className="modal-badge-icon" aria-hidden="true">
                  {memoryModalMode === 'edit' ? '✏️' : '🌟'}
                </div>
                <div>
                  <h3 id="memory-modal-title" className="modal-title">
                    {memoryModalMode === 'edit' ? 'Edit Milestone Reflection' : 'Record Milestone Memory'}
                  </h3>
                  <p className="modal-subtitle">
                    {memoryModalMode === 'edit'
                      ? 'Update your completion date and personal story on the Memory Wall.'
                      : 'Commemorate your achieved goal. Creating a memory marks the dream as Completed.'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseMemoryModal}
                disabled={isSubmittingMemory}
                aria-label="Close modal"
              >
                ✕
              </button>
            </div>

            {/* Memory Form */}
            <form onSubmit={handleSubmitMemory} className="modal-form" noValidate>
              {memoryFormError && (
                <div className="form-action-error" role="alert">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>{memoryFormError}</span>
                </div>
              )}

              {/* Field 1: Linked Dream */}
              {memoryModalMode === 'add' ? (
                <div className="form-group">
                  <label htmlFor="memory-dream-select" className="form-label">
                    Accomplished Dream
                    <span className="required-star" aria-hidden="true">*</span>
                  </label>
                  {eligibleDreams.length > 0 ? (
                    <select
                      id="memory-dream-select"
                      className="form-select"
                      value={selectedDreamId}
                      onChange={(e) => setSelectedDreamId(Number(e.target.value))}
                      autoFocus
                    >
                      {eligibleDreams.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.title} ({d.category}) — {d.status}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <p className="list-meta-text" style={{ padding: '0.5rem 0', color: '#fca5a5' }}>
                      All existing dreams already have recorded memories! Create a new dream first.
                    </p>
                  )}
                </div>
              ) : (
                <div className="form-group">
                  <label className="form-label">
                    Linked Dream
                  </label>
                  <input
                    type="text"
                    className="form-input"
                    value={memories.find((m) => m.id === editingMemoryId)?.dreamTitle || ''}
                    disabled
                    style={{ opacity: 0.8 }}
                  />
                  <p className="list-meta-text">The linked dream is fixed to preserve the one-to-one milestone relationship.</p>
                </div>
              )}

              {/* Field 2: Completion Date (Required, Past or Present) */}
              <div className="form-group">
                <label htmlFor="memory-date" className="form-label">
                  Completion Date
                  <span className="required-star" aria-hidden="true">*</span>
                </label>
                <input
                  id="memory-date"
                  type="date"
                  className="form-input"
                  max={todayStr}
                  value={completedDate}
                  onChange={(e) => setCompletedDate(e.target.value)}
                  required
                />
                <p className="list-meta-text">Must be today or in the past.</p>
              </div>

              {/* Field 3: Story Reflections (Optional) */}
              <div className="form-group">
                <label htmlFor="memory-story" className="form-label">
                  Personal Reflections & Story
                  <span className="optional-badge">(Optional)</span>
                </label>
                <textarea
                  id="memory-story"
                  className="form-textarea"
                  placeholder="What was the experience like? What emotions, hurdles, or memories stand out most?"
                  rows={4}
                  maxLength={4000}
                  value={story}
                  onChange={(e) => setStory(e.target.value)}
                />
              </div>

              {/* Form Action Buttons */}
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleCloseMemoryModal}
                  disabled={isSubmittingMemory}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={isSubmittingMemory || (memoryModalMode === 'add' && eligibleDreams.length === 0)}
                >
                  {isSubmittingMemory
                    ? memoryModalMode === 'edit' ? 'Saving Changes...' : 'Recording Memory...'
                    : memoryModalMode === 'edit' ? 'Save Reflections' : 'Record Memory'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* -------------------------------------------------------------------- */}
      {/* Modal: Delete Confirmation Dialog for Memory                         */}
      {/* -------------------------------------------------------------------- */}
      {deletingMemory && (
        <div
          className="modal-overlay"
          onClick={(e) => {
            if (e.target === e.currentTarget && !isDeletingMemory) handleCloseDeleteMemoryModal()
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape' && !isDeletingMemory) handleCloseDeleteMemoryModal()
          }}
          role="presentation"
        >
          <div
            className="modal-dialog"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-memory-dialog-title"
            aria-describedby="delete-memory-dialog-desc"
            style={{ maxWidth: '460px' }}
          >
            <div className="modal-header">
              <div className="modal-title-group">
                <div className="modal-badge-icon danger" aria-hidden="true">🗑</div>
                <div>
                  <h3 id="delete-memory-dialog-title" className="modal-title">Delete Memory Reflection?</h3>
                  <p className="modal-subtitle">Remove this reflection from your Memory Wall.</p>
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseDeleteMemoryModal}
                disabled={isDeletingMemory}
                aria-label="Close delete confirmation"
              >
                ✕
              </button>
            </div>

            <div style={{ padding: '1.5rem 1.75rem' }}>
              {deleteMemoryError && (
                <div className="form-action-error" role="alert">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                  <span>{deleteMemoryError}</span>
                </div>
              )}

              <p id="delete-memory-dialog-desc" style={{ margin: 0, fontSize: '0.92rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Are you sure you want to remove this reflection? Your underlying Dream (<strong>{deletingMemory.dreamTitle}</strong>) will remain marked as <strong>Completed</strong> in your bucket list.
              </p>

              <div className="delete-preview-box">
                <p className="delete-preview-title">{deletingMemory.dreamTitle}</p>
                <div className="delete-preview-meta">
                  <span>{deletingMemory.dreamCategory}</span>
                  <span>•</span>
                  <span>Achieved {deletingMemory.completedDate}</span>
                </div>
              </div>

              <div className="modal-actions" style={{ marginTop: 0, paddingTop: '1rem' }}>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={handleCloseDeleteMemoryModal}
                  disabled={isDeletingMemory}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn-danger"
                  onClick={handleConfirmDeleteMemory}
                  disabled={isDeletingMemory}
                  autoFocus
                >
                  {isDeletingMemory ? 'Deleting...' : 'Delete Memory'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default App
