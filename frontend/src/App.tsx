import './styles/index.css'

/**
 * MediSphere Cognitive Twin — Root Application Component
 *
 * Phase 1: Minimal scaffold. Routing and feature pages
 * will be added in subsequent phases.
 */
function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>MediSphere Cognitive Twin</h1>
        <p className="app-subtitle">AI-based Healthcare Management Platform</p>
      </header>
      <main className="app-main">
        <div className="status-card">
          <h2>System Status</h2>
          <p>Milestone 1 — Phase 1 scaffolding complete.</p>
          <p className="status-hint">
            Backend: <code>http://localhost:8080</code>
          </p>
        </div>
      </main>
    </div>
  )
}

export default App
