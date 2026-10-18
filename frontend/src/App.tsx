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

function App() {
  // Navigation tab state: 'Dashboard' | 'My Bucket List' | 'Memory Wall'
  const [activeNav, setActiveNav] = useState<'Dashboard' | 'My Bucket List' | 'Memory Wall'>('Dashboard')

  // Filter state for dream cards
  const [activeFilter, setActiveFilter] = useState<'All' | 'In Progress' | 'Completed' | 'Not Started'>('All')

  // Filter items based on activeFilter selection
  const filteredDreams = INITIAL_DREAMS.filter((dream) => {
    if (activeFilter === 'All') return true
    return dream.status === activeFilter
  })

  // Quick stats computed directly from our sample data
  const totalCount = INITIAL_DREAMS.length
  const completedCount = INITIAL_DREAMS.filter(d => d.status === 'Completed').length
  const inProgressCount = INITIAL_DREAMS.filter(d => d.status === 'In Progress').length

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
            <p className="page-subtitle">Make Life Count — Keep pursuing what sets your soul on fire.</p>
          </div>
          <button
            type="button"
            className="add-dream-btn"
            onClick={() => alert('Add Dream modal will be connected as we build interactive forms!')}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 12h14"/>
              <path d="M12 5v14"/>
            </svg>
            Add New Dream
          </button>
        </header>

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

        {/* Dreams / Bucket List Cards Section */}
        <section aria-label="Aspirations List">
          <div className="section-top">
            <h3 className="section-title">Current Aspirations</h3>
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
            {filteredDreams.map((dream) => {
              const statusClass = dream.status.toLowerCase().replace(/\s+/g, '-')

              return (
                <article key={dream.id} className="dream-card">
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
            })}
          </div>
        </section>
      </main>
    </div>
  )
}

export default App

