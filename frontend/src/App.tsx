import { useState, useEffect } from 'react'
import './App.css'
import { dreamApi, type DreamRequest, type DreamResponse } from './services/dreamService'

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
  // Navigation tab state: 'Dashboard' | 'My Bucket List' | 'Memory Wall'
  const [activeNav, setActiveNav] = useState<'Dashboard' | 'My Bucket List' | 'Memory Wall'>('Dashboard')

  // Main state holding the list of dreams (backend is the single source of truth)
  const [dreams, setDreams] = useState<DreamItem[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [fetchError, setFetchError] = useState<string | null>(null)

  // Filter state for dream cards on the My Bucket List page
  const [activeFilter, setActiveFilter] = useState<'All' | 'In Progress' | 'Completed' | 'Not Started'>('All')

  // Modal dialog open/closed state & mode ('add' | 'edit')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [modalMode, setModalMode] = useState<'add' | 'edit'>('add')
  const [editingDreamId, setEditingDreamId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete confirmation modal state
  const [deletingDream, setDeletingDream] = useState<DreamItem | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // Form input states
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('Travel & Nature')
  const [description, setDescription] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [status, setStatus] = useState<'Not Started' | 'In Progress' | 'Completed'>('Not Started')
  const [titleError, setTitleError] = useState('')

  // Load dreams from backend API on initial mount
  const loadDreams = async () => {
    setIsLoading(true)
    setFetchError(null)
    try {
      const data = await dreamApi.getAll()
      setDreams(data.map(toDreamItem))
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unable to connect to Bucketly API. Ensure backend is running on http://localhost:8080.'
      setFetchError(message)
      setDreams([]) // Never silently fall back to mock data
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadDreams()
  }, [])

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
    'Memory Wall': 'A visual journal celebrating every dream accomplished.'
  }

  // Open modal in "Add" mode
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

  // Open modal in "Edit" mode with prefilled values
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

  // Close form modal and clear validation errors
  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingDreamId(null)
    setTitleError('')
    setActionError(null)
  }

  // Open delete confirmation modal
  const handleOpenDeleteModal = (dream: DreamItem) => {
    setDeletingDream(dream)
    setDeleteError(null)
  }

  // Close delete confirmation modal
  const handleCloseDeleteModal = () => {
    setDeletingDream(null)
    setDeleteError(null)
  }

  // Confirm delete and remove the dream from state via DELETE /api/dreams/{id}
  const handleConfirmDelete = async () => {
    if (!deletingDream) return
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await dreamApi.delete(deletingDream.id)
      setDreams((prev) => prev.filter((d) => d.id !== deletingDream.id))
      setDeletingDream(null)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete dream. Please check backend connection.'
      setDeleteError(message)
    } finally {
      setIsDeleting(false)
    }
  }

  // Handle form submission for both Add (POST) and Edit (PUT) modes
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    // Validate required title
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
      const message = err instanceof Error ? err.message : 'Operation failed. Please check backend connection.'
      setActionError(message)
    } finally {
      setIsSubmitting(false)
    }
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
          <div className="profile-avatar" aria-hidden="true">B</div>
          <div>
            <p className="profile-name">Explorer Alex</p>
            <p className="profile-tier">Dreamer Tier • 2026</p>
          </div>
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
        </header>

        {/* ------------------------------------------------------------------ */}
        {/* ASYNC STATE 1: Loading State                                       */}
        {/* ------------------------------------------------------------------ */}
        {isLoading && (
          <div className="state-card loading-state" role="status" aria-live="polite">
            <div className="loading-spinner" aria-hidden="true" />
            <h3 className="state-title">Connecting to Bucketly API...</h3>
            <p className="state-text">Loading your life milestones and bucket list from the backend server.</p>
          </div>
        )}

        {/* ------------------------------------------------------------------ */}
        {/* ASYNC STATE 2: Error State (No Silent Mock Fallback)               */}
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
            {/* VIEW 3: Memory Wall (Placeholder View)                             */}
            {/* ---------------------------------------------------------------- */}
            {activeNav === 'Memory Wall' && (
              <section className="placeholder-view" aria-label="Memory Wall Placeholder">
                <div className="placeholder-card">
                  <div className="placeholder-icon-wrap" aria-hidden="true">
                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/>
                      <circle cx="9" cy="9" r="2"/>
                      <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>
                    </svg>
                  </div>

                  <h3 className="placeholder-title">Memory Wall Coming Soon</h3>
                  <p className="placeholder-text">
                    This space will celebrate your accomplished dreams with photo galleries, completion stories, milestone reflections, and commemorative badges.
                  </p>

                  <div className="placeholder-preview-box">
                    <p className="preview-label">✦ Upcoming Memory Wall Features</p>
                    <ul className="preview-list">
                      <li>Upload adventure photos and travel journals</li>
                      <li>Pin reflections on what each achieved goal felt like</li>
                      <li>Share an interactive digital memory book with friends</li>
                    </ul>
                  </div>

                  <button
                    type="button"
                    className="placeholder-action-btn"
                    onClick={() => setActiveNav('My Bucket List')}
                  >
                    ← Back to My Bucket List
                  </button>
                </div>
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
      {/* Modal: Delete Confirmation Dialog                                   */}
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
            {/* Delete Modal Header */}
            <div className="modal-header">
              <div className="modal-title-group">
                <div className="modal-badge-icon danger" aria-hidden="true">🗑</div>
                <div>
                  <h3 id="delete-dialog-title" className="modal-title">Delete Dream?</h3>
                  <p className="modal-subtitle">This will permanently remove the dream from MySQL database.</p>
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

              {/* Preview Box of the Dream to be deleted */}
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

              {/* Confirmation Actions */}
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
    </div>
  )
}

export default App
