import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { verifyLabel } from './services/verificationApi'
import { CheckResult, StatusBadge } from './components/CheckResult'
import type {
  ApplicationData,
  VerificationResult,
} from './types/verification'
import './App.css'
import {BatchVerification} from "./components/BatchVerification.tsx";

function App() {
  const [application, setApplication] =
    useState<ApplicationData>({
      brandName: '',
      classType: '',
      alcoholContent: '',
      netContents: '',
    })
  const [mode, setMode] = useState<'single' | 'batch'>('single')
  const [image, setImage] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [result, setResult] =
    useState<VerificationResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!image) {
      setPreviewUrl(null)
      return
    }

    const url = URL.createObjectURL(image)
    setPreviewUrl(url)

    return () => URL.revokeObjectURL(url)
  }, [image])

  function updateField(field: keyof ApplicationData, value: string) {
    setApplication(prev => ({
      ...prev,
      [field]: value,
    }))
    setResult(null)
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] || null

    setError(null)
    setResult(null)
    setImage(null)

    if (!file) return

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setError('Please select a JPG or PNG image.')
      event.target.value = ''
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      setError('Image must be smaller than 10 MB.')
      event.target.value = ''
      return
    }

    setImage(file)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!image || loading) return

    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const verification = await verifyLabel(image, application)
      setResult(verification)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'An unexpected error occurred.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="page-header">
        <div className="eyebrow">AI ENGINEERING PROTOTYPE</div>
        <h1>Alcohol Label Verification</h1>
        <p>
          Review alcohol beverage labels using OCR and
          rule-based verification.
        </p>
      </header>

      <main>
        <section className="mode-switch">
          <button
              type="button"
              className={mode === 'single' ? 'active' : ''}
              onClick={() => setMode('single')}
          >
            Single Label
          </button>
          <button
              type="button"
              className={mode === 'batch' ? 'active' : ''}
              onClick={() => setMode('batch')}
          >
            Batch Upload
          </button>
        </section>
        {mode === 'single' ? (
            <>
              <form onSubmit={handleSubmit} className="form-grid">
                <section className="panel">
                  <h2>Application Information</h2>
                  <p className="section-description">
                    Enter the information submitted for label review.
                  </p>

                  <label htmlFor="brandName">Brand Name</label>
                  <input
                      id="brandName"
                      required
                      placeholder="e.g. Old Tom Distillery"
                      value={application.brandName}
                      onChange={e => updateField('brandName', e.target.value)}
                  />

                  <label htmlFor="classType">Class / Type</label>
                  <input
                      id="classType"
                      required
                      placeholder="e.g. Bourbon Whiskey"
                      value={application.classType}
                      onChange={e => updateField('classType', e.target.value)}
                  />

                  <label htmlFor="alcoholContent">
                    Alcohol Content
                  </label>
                  <input
                      id="alcoholContent"
                      required
                      placeholder="e.g. 45% ABV"
                      value={application.alcoholContent}
                      onChange={e =>
                          updateField('alcoholContent', e.target.value)
                      }
                  />

                  <label htmlFor="netContents">Net Contents</label>
                  <input
                      id="netContents"
                      required
                      placeholder="e.g. 750 mL"
                      value={application.netContents}
                      onChange={e => updateField('netContents', e.target.value)}
                  />
                </section>

                <section className="panel">
                  <h2>Label Image</h2>
                  <p className="section-description">
                    Upload a JPG or PNG image of the label.
                  </p>

                  <label htmlFor="labelImage" className="upload-zone">
                    {previewUrl ? (
                        <img
                            src={previewUrl}
                            alt="Uploaded alcohol label preview"
                            className="image-preview"
                        />
                    ) : (
                        <div className="upload-placeholder">
                          <span className="upload-icon">↑</span>
                          <strong>Choose label image</strong>
                          <span>JPG or PNG, maximum 10 MB</span>
                        </div>
                    )}
                  </label>

                  <input
                      id="labelImage"
                      type="file"
                      accept="image/jpeg,image/png"
                      onChange={handleFileChange}
                      className="file-input"
                  />

                  {image && <p className="filename">{image.name}</p>}
                </section>

                <div className="form-actions">
                  <button
                      type="submit"
                      disabled={loading || !image}
                      className="verify-button"
                  >
                    {loading ? 'Analyzing Label...' : 'Verify Label'}
                  </button>
                </div>
              </form>

              {error && (
                  <div className="error-message" role="alert">
                    {error}
                  </div>
              )}

              {result && (
                  <section className="results-section">
                    <div className="results-header">
                      <div>
                        <h2>Verification Report</h2>
                        <p>Automated analysis of submitted label evidence</p>
                      </div>
                      <StatusBadge status={result.status} />
                    </div>

                    <div className="notice">
                      This is a decision-support prototype, not a final
                      regulatory approval. Uncertain findings require
                      human review.
                    </div>

                    <div className="results-list">
                      {result.checks.map((check, index) => (
                          <CheckResult
                              key={`${check.field}-${index}`}
                              check={check}
                          />
                      ))}
                    </div>
                  </section>
              )}
            </>
        ) : (
            <BatchVerification />
        )}
      </main>

      <footer>
        Treasury AI Engineering Take-Home Prototype
        <span> · </span>
        Tess4J OCR + Spring Boot
      </footer>
    </div>
  )
}

export default App