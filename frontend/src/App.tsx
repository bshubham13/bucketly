import { useState } from 'react'
import './App.css'

// Simple, beginner-friendly TypeScript type representing one bucket list goal
interface DreamItem {
  id: number
  title: string
  category: string
  status: 'In Progress' | 'Completed' | 'Not Started'
  progress: number
  targetDate: string
  description: string
}

// Initial sample data for the personal bucket list
const INITIAL_DREAMS: DreamItem[] = [
  {
    id: 1,
    title: 'Witness the Northern Lights in Tromsø',
    category: 'Travel & Nature',
    status: 'In Progress',
    progress: 65,
    targetDate: 'Winter 2026',
    description: 'Rent a glass igloo in Norway, chase the Aurora Borealis, and photograph the midnight sky.'
  },
  {
    id: 2,
    title: 'Ship Bucketly Full-Stack Portfolio',
    category: 'Career & Tech',
    status: 'In Progress',
    progress: 40,
    targetDate: 'Q2 2026',
    description: 'Design and deploy a full-stack personal app with React, TypeScript, Spring Boot, and MySQL.'
  },
  {
    id: 3,
    title: 'Run the Tokyo Marathon',
    category: 'Fitness & Health',
    status: 'Not Started',
    progress: 10,
    targetDate: 'Spring 2027',
    description: 'Build up 42.2km endurance, qualify for the Abbott World Marathon Majors lottery.'
  },
  {
    id: 4,
    title: 'Stargazing Camp in the Sahara Desert',
    category: 'Adventure',
    status: 'Completed',
    progress: 100,
    targetDate: 'Achieved Oct 2025',
    description: 'Slept under the unfiltered Milky Way in Merzouga dunes with Berber guides.'
  }
]

// Reusable Dream Card component to keep the code clean and avoid duplication
function DreamCard({ dream }: { dream: DreamItem }) {
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
          <button
            type="button"
            className="mark-btn"
            onClick={() => alert(`Goal "${dream.title}" selected.`)}
          >
            Details
          </button>
        </div>
      </div>
    </article>
  )
}

function App() {
  // Navigation tab state: 'Dashboard' | 'My Bucket List' | 'Memory Wall'
  const [activeNav, setActiveNav] = useState<'Dashboard' | 'My Bucket List' | 'Memory Wall'>('Dashboard')

  // Main state holding the list of dreams (initialized with our sample data)
  const [dreams, setDreams] = useState<DreamItem[]>(INITIAL_DREAMS)

  // Filter state for dream cards on the My Bucket List page
  const [activeFilter, setActiveFilter] = useState<'All' | 'In Progress' | 'Completed' | 'Not Started'>('All')

  // Modal dialog open/closed state
  const [isModalOpen, setIsModalOpen] = useState(false)

  // Form input states
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('Travel & Nature')
  const [description, setDescription] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [status, setStatus] = useState<'Not Started' | 'In Progress'>('Not Started')
  const [titleError, setTitleError] = useState('')

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

  // Open modal and reset inputs
  const handleOpenModal = () => {
    setTitle('')
    setCategory('Travel & Nature')
    setDescription('')
    setTargetDate('')
    setStatus('Not Started')
    setTitleError('')
    setIsModalOpen(true)
  }

  // Close modal and clear validation errors
  const handleCloseModal = () => {
    setIsModalOpen(false)
    setTitleError('')
  }

  // Handle form submission with required field validation
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    // Validate required title
    if (!title.trim()) {
      setTitleError('Please enter a title for your dream.')
      return
    }

    const newDream: DreamItem = {
      id: Date.now(),
      title: title.trim(),
      category,
      status,
      // If user marks as In Progress, give an encouraging initial milestone progress
      progress: status === 'In Progress' ? 20 : 0,
      targetDate: targetDate.trim() || 'Flexible Target',
      description: description.trim() || 'No description provided yet.'
    }

    // Prepend the new dream so it appears first in the list
    setDreams((prev) => [newDream, ...prev])
    handleCloseModal()
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
                  <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>
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
                  <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/>
                  <circle cx="9" cy="9" r="2"/>
                  <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>
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
            onClick={handleOpenModal}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 12h14"/>
              <path d="M12 5v14"/>
            </svg>
            Add New Dream
          </button>
        </header>

        {/* ------------------------------------------------------------------ */}
        {/* VIEW 1: Dashboard                                                  */}
        {/* ------------------------------------------------------------------ */}
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
                  <span className="stat-label">Next Target Year</span>
                  <span className="stat-icon-pill" style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#fbbf24' }} aria-hidden="true">🎯</span>
                </div>
                <p className="stat-value">2026</p>
                <p className="stat-caption">Tromsø & Portfolio milestones</p>
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

              <div className="dreams-grid">
                {inProgressDreams.map((dream) => (
                  <DreamCard key={dream.id} dream={dream} />
                ))}
              </div>
            </section>
          </>
        )}

        {/* ------------------------------------------------------------------ */}
        {/* VIEW 2: My Bucket List (Dedicated Exploration & Filter View)      */}
        {/* ------------------------------------------------------------------ */}
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

            <div className="dreams-grid">
              {filteredDreams.length > 0 ? (
                filteredDreams.map((dream) => (
                  <DreamCard key={dream.id} dream={dream} />
                ))
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
            </div>
          </section>
        )}

        {/* ------------------------------------------------------------------ */}
        {/* VIEW 3: Memory Wall (Placeholder View)                             */}
        {/* ------------------------------------------------------------------ */}
        {activeNav === 'Memory Wall' && (
          <section className="placeholder-view" aria-label="Memory Wall Placeholder">
            <div className="placeholder-card">
              <div className="placeholder-icon-wrap" aria-hidden="true">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/>
                  <circle cx="12" cy="13" r="3"/>
                </svg>
              </div>

              <h3 className="placeholder-title">Memory Wall Coming Soon</h3>
              <p className="placeholder-text">
                This space is reserved for celebrating completed experiences. Once you mark a bucket list dream as finished, you will be able to attach photos, reflections, and dates here to preserve the milestone forever.
              </p>

              <div className="placeholder-preview-box">
                <p className="placeholder-preview-title">First Completed Memory Ready</p>
                <p className="placeholder-preview-item">
                  <span style={{ color: 'var(--accent-emerald)', fontWeight: 'bold' }}>✓</span>
                  Stargazing Camp in the Sahara Desert (Achieved Oct 2025)
                </p>
              </div>

              <button
                type="button"
                className="placeholder-action-btn"
                onClick={() => setActiveNav('My Bucket List')}
              >
                Explore My Bucket List
              </button>
            </div>
          </section>
        )}
      </main>

      {/* -------------------------------------------------------------------- */}
      {/* Modal: Add New Dream Form                                            */}
      {/* -------------------------------------------------------------------- */}
      {isModalOpen && (
        <div
          className="modal-overlay"
          onClick={(e) => {
            if (e.target === e.currentTarget) handleCloseModal()
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape') handleCloseModal()
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
                <div className="modal-badge-icon" aria-hidden="true">✦</div>
                <div>
                  <h3 id="modal-title" className="modal-title">Add New Dream</h3>
                  <p className="modal-subtitle">Make Life Count — What aspiration will you chase?</p>
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={handleCloseModal}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleSubmit} className="modal-form" noValidate>
              {/* Field 1: Title (Required) */}
              <div className="form-group">
                <label htmlFor="dream-title" className="form-label">
                  Dream Title
                  <span className="required-badge">* Required</span>
                </label>
                <input
                  id="dream-title"
                  type="text"
                  className={`form-input ${titleError ? 'input-error' : ''}`}
                  placeholder="e.g. Scuba dive the Great Barrier Reef"
                  value={title}
                  onChange={(e) => {
                    setTitle(e.target.value)
                    if (titleError) setTitleError('')
                  }}
                  autoFocus
                  aria-invalid={!!titleError}
                  aria-describedby={titleError ? 'title-error-msg' : undefined}
                />
                {titleError && (
                  <p id="title-error-msg" className="error-message" role="alert">
                    <span>⚠</span> {titleError}
                  </p>
                )}
              </div>

              {/* Field 2 & 3: Category & Initial Status (2 Columns) */}
              <div className="form-row">
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
                    <option value="Fitness & Health">Fitness & Health</option>
                    <option value="Career & Tech">Career & Tech</option>
                    <option value="Personal Growth">Personal Growth</option>
                    <option value="Adventure">Adventure</option>
                    <option value="Other">Other</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="dream-status" className="form-label">
                    Initial Status
                  </label>
                  <select
                    id="dream-status"
                    className="form-select"
                    value={status}
                    onChange={(e) => setStatus(e.target.value as 'Not Started' | 'In Progress')}
                  >
                    <option value="Not Started">Not Started</option>
                    <option value="In Progress">In Progress</option>
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
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                >
                  Add to Bucket List
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default App


