import { useMemo, useState } from 'react'
import './App.css'

const sampleResume = `Nitin Reddy
B.Tech Computer Science
Skills: Java, Spring Boot, React, JavaScript, SQL, MySQL, Git, AWS
Experience: Built full-stack web applications and REST APIs.`
const sampleJob = `Looking for a Software Engineer with Java, Spring Boot, React, SQL, REST API, Git and AWS experience.`

export default function App() {
  const [resume, setResume] = useState(sampleResume)
  const [jobDescription, setJobDescription] = useState(sampleJob)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function analyze() {
    setLoading(true); setError('')
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8080'}/api/analyze`, {
        method: 'POST', headers: {'Content-Type':'application/json'},
        body: JSON.stringify({resume, jobDescription})
      })
      if (!response.ok) throw new Error('Analysis service unavailable')
      setResult(await response.json())
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }

  const ring = useMemo(() => ({ '--score': result?.score || 0 }), [result])

  return <main>
    <section className="hero">
      <div className="brand"><span className="mark">R</span> ResumeReviewer <small>AI SCREENING STUDIO</small></div>
      <div className="hero-grid">
        <div><p className="eyebrow">DECISION INTELLIGENCE FOR HIRING</p><h1>Find the signal.<br/><em>Not just the keywords.</em></h1><p className="subtitle">An elegant screening workspace that turns resumes and job descriptions into explainable hiring insights.</p></div>
        <div className="stat-card"><span>SCREENING ENGINE</span><strong>Explainable matching</strong><p>Skills, experience and education signals—summarized in one focused review.</p><div className="pulse"><i/> Ready to analyze</div></div>
      </div>
    </section>

    <section className="workspace">
      <div className="input-card"><div className="card-head"><span>01</span><h2>Candidate profile</h2></div><textarea value={resume} onChange={e=>setResume(e.target.value)} placeholder="Paste resume text here..."/><p>Paste resume text. PDF upload can be connected to the same API endpoint.</p></div>
      <div className="input-card"><div className="card-head"><span>02</span><h2>Role requirements</h2></div><textarea value={jobDescription} onChange={e=>setJobDescription(e.target.value)} placeholder="Paste the job description..."/><p>Include responsibilities, required skills and preferred qualifications.</p></div>
      <button className="analyze" onClick={analyze} disabled={loading}>{loading ? 'Analyzing signals...' : 'Analyze candidate'} <b>→</b></button>
      {error && <div className="error">{error}</div>}
    </section>

    {result && <section className="results">
      <div className="result-top"><div><p className="eyebrow">SCREENING RESULT</p><h2>{result.candidate}</h2><p>{result.summary}</p></div><div className="score" style={ring}><div><strong>{result.score}</strong><span>/100</span></div></div></div>
      <div className="recommendation"><span>Recommendation</span><strong>{result.recommendation}</strong><p>Experience: {result.experience} · {result.educationDetected}</p></div>
      <div className="signal-grid">
        <Signal title="Matched signals" items={result.matchedSkills} empty="No direct skill matches detected" type="good"/>
        <Signal title="Potential gaps" items={result.missingSkills} empty="No critical gaps detected" type="gap"/>
        <Signal title="Resume inventory" items={result.extractedSkills} empty="No known skills detected" type="neutral"/>
      </div>
    </section>}

    <footer>ResumeReviewer · Built with React + Java Spring Boot · Explainable candidate intelligence</footer>
  </main>
}

function Signal({title, items=[], empty, type}) { return <article className={`signal ${type}`}><h3>{title}</h3><div className="chips">{items.length ? items.map(x=><span key={x}>{x}</span>) : <p>{empty}</p>}</div></article> }
