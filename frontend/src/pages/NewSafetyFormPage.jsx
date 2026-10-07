import { useEffect, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import ChecklistFieldset from '../components/forms/ChecklistFieldset.jsx'
import PhotoInput from '../components/forms/PhotoInput.jsx'
import { useAuth } from '../context/auth.js'
import { getActiveSites, getChecklistForSite } from '../services/siteService.js'
import { submitSafetyForm } from '../services/submissionService.js'
import {
  currentTimeInputValue,
  isFutureLocalDateTime,
  toInstantValue,
  todayInputValue,
} from '../utils/date.js'

function NewSafetyFormPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [date, setDate] = useState(todayInputValue)
  const [time, setTime] = useState(currentTimeInputValue)
  const [siteId, setSiteId] = useState('')
  const [sites, setSites] = useState([])
  const [checklist, setChecklist] = useState(null)
  const [checkedIds, setCheckedIds] = useState(new Set())
  const [photos, setPhotos] = useState([])
  const [photoError, setPhotoError] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')
  const [isLoadingSites, setIsLoadingSites] = useState(true)
  const [isLoadingChecklist, setIsLoadingChecklist] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const checklistRef = useRef(null)
  const invalidScrollPending = useRef(false)

  function moveTo(element) {
    window.requestAnimationFrame(() => {
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      element?.focus?.({ preventScroll: true })
    })
  }

  function showValidationError(message, element) {
    setError(message)
    moveTo(element)
  }

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

  function handlePhotoChange(selectedPhotos, validationError) {
    setPhotos(selectedPhotos)
    setPhotoError(validationError)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (!siteId || !checklist) {
      showValidationError('Select a site and load its checklist.', document.getElementById('site'))
      return
    }

    if (isFutureLocalDateTime(date, time)) {
      showValidationError(
        'The safety form date and time cannot be in the future.',
        document.getElementById(date === todayInputValue() ? 'form-time' : 'form-date'),
      )
      return
    }

    if (photos.length === 0) {
      showValidationError(
        'Add at least one photo before submitting.',
        document.getElementById('photos'),
      )
      return
    }

    if (checklist.items.length === 0) {
      showValidationError('This site does not have a checklist configured.', checklistRef.current)
      return
    }

    if (checkedIds.size !== checklist.items.length) {
      showValidationError(
        'Every checklist item must be confirmed before submission.',
        checklistRef.current?.querySelector('input:not(:checked)'),
      )
      return
    }

    setIsSubmitting(true)
    try {
      await submitSafetyForm({
        userId: user.id,
        siteId: Number(siteId),
        formDate: toInstantValue(date, time),
        notes,
        checkedItemIds: Array.from(checkedIds),
        photos: photos.map((photo) => photo.file),
      })

      navigate('/framer', {
        replace: true,
        state: { message: 'Safety form submitted successfully.' },
      })
    } catch (requestError) {
      showValidationError(
        requestError.message || 'Could not submit the safety form.',
        document.querySelector('.form-page'),
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="content-page form-page">
      <h1 className="page-title">Safety Form</h1>
      {error && <p className="message error" role="alert">{error}</p>}

      <form
        className="panel form-stack"
        onSubmit={handleSubmit}
        onInvalidCapture={(event) => {
          if (invalidScrollPending.current) return
          invalidScrollPending.current = true
          moveTo(event.target)
          window.setTimeout(() => { invalidScrollPending.current = false }, 300)
        }}
      >
        <div className="form-date-time-fields">
          <div className="field-group">
            <label htmlFor="form-date">Date</label>
            <input
              id="form-date"
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              max={todayInputValue()}
              required
            />
          </div>
          <div className="field-group">
            <label htmlFor="form-time">Time</label>
            <input
              id="form-time"
              type="time"
              value={time}
              onChange={(event) => setTime(event.target.value)}
              max={date === todayInputValue() ? currentTimeInputValue() : undefined}
              required
            />
          </div>
        </div>

        <div className="field-group">
          <label htmlFor="site">Site</label>
          <div className="select-control">
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
            <ChevronDown aria-hidden="true" />
          </div>
        </div>

        {isLoadingChecklist && <p>Loading checklist...</p>}
        <div ref={checklistRef}>
          <ChecklistFieldset
            checklist={checklist}
            checkedIds={checkedIds}
            onToggle={toggleChecklistItem}
          />
        </div>

        <PhotoInput photos={photos} onChange={handlePhotoChange} error={photoError} />

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
          {isSubmitting ? 'Submitting...' : 'Submit'}
        </button>
      </form>
    </section>
  )
}

export default NewSafetyFormPage
