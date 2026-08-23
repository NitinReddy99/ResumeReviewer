import { useMemo, useState } from 'react'
import './App.css'

const sampleResume = `Rahul Sharma
B.Tech Computer Science

Skills:
Java, Spring Boot, React, JavaScript, SQL, MySQL, Git, AWS

Experience:
Built full-stack web applications and REST APIs.`

const sampleJob = `Looking for a Software Engineer with:

Java
Spring Boot
React
SQL
REST API
Git
AWS experience.`


// ==========================================
// BACKEND API URL
// ==========================================

const api =
  import.meta.env.VITE_API_URL ||
  'https://resume-reviewer-backend-lw7h.onrender.com'


export default function App() {

  const [resume, setResume] =
    useState(sampleResume)

  const [jobDescription, setJobDescription] =
    useState(sampleJob)

  const [result, setResult] =
    useState(null)

  const [batch, setBatch] =
    useState(null)

  const [files, setFiles] =
    useState([])

  const [loading, setLoading] =
    useState(false)

  const [error, setError] =
    useState('')


  // ==========================================
  // SINGLE CANDIDATE ANALYSIS
  // ==========================================

  async function analyze() {

    if (!resume.trim()) {

      setError(
        'Please enter candidate profile information.'
      )

      return
    }


    if (!jobDescription.trim()) {

      setError(
        'Please enter role requirements.'
      )

      return
    }


    setLoading(true)

    setError('')

    setBatch(null)

    setResult(null)


    try {

      const response =
        await fetch(
          api + '/api/analyze',
          {
            method: 'POST',

            headers: {
              'Content-Type':
                'application/json'
            },

            body:
              JSON.stringify({
                resume,
                jobDescription
              })
          }
        )


      let json


      try {

        json =
          await response.json()

      } catch {

        throw new Error(
          'Invalid response from analysis service'
        )
      }


      if (!response.ok) {

        throw new Error(
          json.message ||
          json.error ||
          'Analysis service unavailable'
        )
      }


      setResult(json)


    } catch (e) {

      console.error(
        'Analysis error:',
        e
      )


      setError(

        e.message === 'Failed to fetch'

          ? 'Unable to connect to the backend. Please wait a few seconds and try again.'

          : (
              e.message ||
              'Something went wrong'
            )
      )


    } finally {

      setLoading(false)
    }
  }


  // ==========================================
  // MULTIPLE CANDIDATE ANALYSIS
  // ==========================================

  async function analyzeBatch() {

    if (!files.length) {

      setError(
        'Choose at least one PDF or text resume.'
      )

      return
    }


    if (!jobDescription.trim()) {

      setError(
        'Please enter role requirements first.'
      )

      return
    }


    setLoading(true)

    setError('')

    setResult(null)

    setBatch(null)


    try {

      const data =
        new FormData()


      files.forEach(file =>

        data.append(
          'files',
          file
        )
      )


      data.append(
        'jobDescription',
        jobDescription
      )


      const response =
        await fetch(
          api + '/api/candidates/batch',
          {
            method: 'POST',
            body: data
          }
        )


      let json


      try {

        json =
          await response.json()

      } catch {

        throw new Error(
          'Invalid response from batch analysis service'
        )
      }


      if (!response.ok) {

        throw new Error(
          json.message ||
          json.error ||
          'Batch analysis failed'
        )
      }


      setBatch(json)


    } catch (e) {

      console.error(
        'Batch analysis error:',
        e
      )


      setError(

        e.message === 'Failed to fetch'

          ? 'Unable to connect to the backend. Please wait a few seconds and try again.'

          : (
              e.message ||
              'Batch analysis failed'
            )
      )


    } finally {

      setLoading(false)
    }
  }


  const ring =
    useMemo(
      () => ({
        '--score':
          result?.score || 0
      }),
      [result]
    )


  return (

    <main>

      {/* ================= HERO ================= */}

      <section className="hero">

        <div className="brand">

          <span className="mark">
            R
          </span>

          ResumeReviewer

          <small>
            AI SCREENING STUDIO
          </small>

        </div>


        <div className="hero-grid">

          <div>

            <p className="eyebrow">
              DECISION INTELLIGENCE FOR HIRING
            </p>


            <h1>

              Find the signal.

              <br />

              <em>
                Not just the keywords.
              </em>

            </h1>


            <p className="subtitle">

              A focused screening workspace
              that turns resumes and job
              descriptions into explainable
              hiring insights.

            </p>

          </div>


          <div className="stat-card">

            <span>
              SCREENING ENGINE
            </span>


            <strong>
              Ollama AI matching
            </strong>


            <p>
              Skills, experience and education
              signals summarized using AI.
            </p>


            <div className="pulse">

              <i />

              Ready to analyze

            </div>

          </div>

        </div>

      </section>


      {/* ================= INPUT SECTION ================= */}

      <section className="workspace">


        {/* CANDIDATE PROFILE */}

        <div className="input-card">

          <div className="card-head">

            <span>
              01
            </span>


            <h2>
              Candidate profile
            </h2>

          </div>


          <textarea

            value={resume}

            onChange={e =>
              setResume(
                e.target.value
              )
            }

          />


          <p>
            Paste resume text for a
            single-candidate review.
          </p>

        </div>


        {/* ROLE REQUIREMENTS */}

        <div className="input-card">

          <div className="card-head">

            <span>
              02
            </span>


            <h2>
              Role requirements
            </h2>

          </div>


          <textarea

            value={jobDescription}

            onChange={e =>
              setJobDescription(
                e.target.value
              )
            }

          />


          <p>
            Include responsibilities,
            required skills and
            preferred qualifications.
          </p>

        </div>


        {/* ANALYZE BUTTON */}

        <button

          className="analyze"

          onClick={analyze}

          disabled={loading}

        >

          {
            loading

              ? 'Analyzing signals...'

              : 'Analyze candidate'
          }


          <b>
            →
          </b>

        </button>


        {/* ERROR */}

        {
          error &&

          <div className="error">

            {error}

          </div>
        }


      </section>


      {/* ================= BATCH SECTION ================= */}

      <section className="batch-zone">


        <div>

          <p className="eyebrow">
            MULTI-CANDIDATE MODE
          </p>


          <h2>
            Rank an entire candidate pool.
          </h2>


          <p>
            Upload multiple PDF or text
            resumes and receive an
            ordered shortlist.
          </p>

        </div>


        {/* FILE UPLOAD */}

        <label className="upload">

          <input

            type="file"

            multiple

            accept=".pdf,.txt"

            onChange={e =>

              setFiles(
                [...e.target.files]
              )

            }

          />


          <strong>

            {
              files.length

                ? files.length +
                  ' resumes selected'

                : 'Choose resumes'
            }

          </strong>


          <span>
            PDF or TXT · multiple files supported
          </span>

        </label>


        {/* BATCH BUTTON */}

        <button

          className="batch-btn"

          onClick={analyzeBatch}

          disabled={loading}

        >

          {
            loading

              ? 'Ranking candidates...'

              : 'Build shortlist'
          }

        </button>

      </section>


      {/* ================= SINGLE RESULT ================= */}

      {
        result &&

        <section className="results">

          <div className="result-top">


            <div>

              <p className="eyebrow">
                SCREENING RESULT
              </p>


              <h2>
                {result.candidate}
              </h2>


              <p>
                {result.summary}
              </p>

            </div>


            <div
              className="score"
              style={ring}
            >

              <div>

                <strong>
                  {result.score}
                </strong>


                <span>
                  /100
                </span>

              </div>

            </div>

          </div>


          <div className="recommendation">

            <span>
              Recommendation
            </span>


            <strong>
              {result.recommendation}
            </strong>


            <p>

              Experience:
              {' '}

              {result.experience}

              {' · '}

              {result.educationDetected}

            </p>

          </div>


          <div className="signal-grid">


            <Signal

              title="Matched signals"

              items={result.matchedSkills}

              type="good"

            />


            <Signal

              title="Potential gaps"

              items={result.missingSkills}

              type="gap"

            />


            <Signal

              title="Resume inventory"

              items={result.extractedSkills}

              type="neutral"

            />


          </div>

        </section>
      }


      {/* ================= BATCH RESULT ================= */}

      {
        batch &&

        <section className="ranking">


          <div className="ranking-head">


            <div>

              <p className="eyebrow">
                SHORTLIST
              </p>


              <h2>

                {batch.shortlisted}

                {' of '}

                {batch.total}

                {' candidates recommended'}

              </h2>

            </div>


            <span>
              Sorted by compatibility score
            </span>

          </div>


          {
            batch.candidates?.map(
              (c, i) =>

                <article
                  className="candidate"
                  key={c.id || i}
                >


                  <div className="rank">

                    {
                      String(i + 1)
                        .padStart(2, '0')
                    }

                  </div>


                  <div className="candidate-name">

                    <strong>
                      {c.name}
                    </strong>


                    <small>

                      {c.experience}

                      {' · '}

                      {c.educationDetected}

                    </small>

                  </div>


                  <div className="mini-chips">

                    {
                      c.matchedSkills
                        ?.slice(0, 4)
                        .map(skill =>

                          <span
                            key={skill}
                          >

                            {skill}

                          </span>

                        )
                    }

                  </div>


                  <div className="candidate-score">

                    <strong>
                      {c.score}
                    </strong>


                    <span>
                      {c.recommendation}
                    </span>

                  </div>


                </article>
            )
          }

        </section>
      }


      {/* ================= FOOTER ================= */}

      <footer>

        ResumeReviewer · React +
        Java Spring Boot ·
        Ollama-powered candidate intelligence

      </footer>


    </main>
  )
}


// ==========================================
// SIGNAL COMPONENT
// ==========================================

function Signal({
  title,
  items = [],
  type
}) {

  return (

    <article
      className={
        'signal ' + type
      }
    >

      <h3>
        {title}
      </h3>


      <div className="chips">

        {
          items.length

            ? items.map(item =>

                <span
                  key={item}
                >

                  {item}

                </span>

              )

            : (
                <p>
                  No signals detected
                </p>
              )
        }

      </div>

    </article>
  )
}