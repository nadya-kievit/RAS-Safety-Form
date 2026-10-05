import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ChecklistFieldset from '../components/forms/ChecklistFieldset.jsx'
import PhotoInput from '../components/forms/PhotoInput.jsx'
import { useAuth } from '../context/auth.js'
import { getActiveSites, getChecklistForSite } from '../services/siteService.js'
import {
  createSafetyForm,
  uploadSubmissionPhotos,
} from '../services/submissionService.js'
import { todayInputValue } from '../utils/date.js'

function NewSafetyFormPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [date, setDate] = useState(todayInputValue)
  const [siteId, setSiteId] = useState('')
  const [sites, setSites] = useState([])
  const [checklist, setChecklist] = useState(null)
  const [checkedIds, setCheckedIds] = useState(new Set())
  const [photos, setPhotos] = useState([])
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')
  const [isLoadingSites, setIsLoadingSites] = useState(true)
  const [isLoadingChecklist, setIsLoadingChecklist] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    let ignore = false
    getActiveSites()
      .then((result) => {
        if (!ignore) setSites(result)
      })
      .catch(() => {
        if (!ignore) setError('Could not load sites.')
      })
      .finally(() => {
        if (!ignore) setIsLoadingSites(false)
      })
    return () => { ignore = true }
  }, [])

  useEffect(() => {
    if (!siteId) return

    let ignore = false
    getChecklistForSite(siteId)
      .then((result) => {
        if (!ignore) {
          setChecklist(result)
          setCheckedIds(new Set())
        }
      })
      .catch(() => {
        if (!ignore) setError('Could not load the site checklist.')
      })
      .finally(() => {
        if (!ignore) setIsLoadingChecklist(false)
      })
    return () => { ignore = true }
  }, [siteId])

  function toggleChecklistItem(itemId) {
    setCheckedIds((current) => {
      const next = new Set(current)
      if (next.has(itemId)) next.delete(itemId)
      else next.add(itemId)
      return next
    })
  }

  function handleSiteChange(event) {
    const nextSiteId = event.target.value
    setSiteId(nextSiteId)
    setChecklist(null)
    setCheckedIds(new Set())
    setError('')
    setIsLoadingChecklist(Boolean(nextSiteId))
  }

  function handlePhotoChange(selectedFiles, validationError) {
    setPhotos(selectedFiles)
    setError(validationError)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (!siteId || !checklist) {
      setError('Select a site and load its checklist.')
      return
    }

    if (checklist.items.length === 0) {
      setError('This site does not have a checklist configured.')
      return
    }

    if (checkedIds.size !== checklist.items.length) {
      setError('Every checklist item must be confirmed before submission.')
      return
    }

    setIsSubmitting(true)
    try {
      const submission = await createSafetyForm({
        userId: user.id,
        siteId: Number(siteId),
        formDate: date,
        notes,
      })

      if (photos.length > 0) {
        try {
          await uploadSubmissionPhotos(submission.id, photos)
        } catch (uploadError) {
          throw new Error(
            `The safety form was saved, but its photos could not be uploaded. ${uploadError.message}`,
            { cause: uploadError },
          )
        }
      }

      navigate('/framer', {
        replace: true,
        state: { message: 'Safety form submitted successfully.' },
      })
    } catch (requestError) {
      setError(requestError.message || 'Could not submit the safety form.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="content-page form-page">
      <Link className="back-link" to="/framer">← Back</Link>
      <h1>New Safety Form</h1>
      {error && <p className="message error" role="alert">{error}</p>}

      <form className="panel form-stack" onSubmit={handleSubmit}>
        <div className="field-group">
          <label htmlFor="form-date">Date</label>
          <input
            id="form-date"
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
            required
          />
        </div>

        <div className="field-group">
          <label htmlFor="site">Site</label>
          <select
            id="site"
            value={siteId}
            onChange={handleSiteChange}
            disabled={isLoadingSites}
            required
          >
            <option value="">{isLoadingSites ? 'Loading sites...' : 'Select a site'}</option>
            {sites.map((site) => (
              <option key={site.id} value={site.id}>{site.name}</option>
            ))}
          </select>
        </div>

        {isLoadingChecklist && <p>Loading checklist...</p>}
        <ChecklistFieldset
          checklist={checklist}
          checkedIds={checkedIds}
          onToggle={toggleChecklistItem}
        />

        <PhotoInput files={photos} onChange={handlePhotoChange} />

        <div className="field-group">
          <label htmlFor="notes">Notes (optional)</label>
          <textarea
            id="notes"
            rows="5"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
          />
        </div>

        <button type="submit" disabled={isSubmitting || isLoadingChecklist}>
          {isSubmitting ? 'Submitting...' : 'Submit Safety Form'}
        </button>
      </form>
    </section>
  )
}

export default NewSafetyFormPage
