import { useEffect, useRef, useState } from 'react'
import { X } from 'lucide-react'
import {
  MAX_PHOTOS,
  PHOTO_ACCEPT,
  photoKey,
  preparePhoto,
} from '../../utils/photos.js'

function PhotoPreview({ photo, onRemove }) {
  const [failed, setFailed] = useState(false)

  return (
    <figure>
      <div className="photo-preview-frame">
        {failed ? (
          <span className="photo-preview-fallback">Preview unavailable</span>
        ) : (
          <img
            src={photo.previewUrl}
            alt={`Preview of ${photo.file.name}`}
            onError={() => setFailed(true)}
          />
        )}
        <button
          className="photo-remove-button"
          type="button"
          aria-label={`Remove ${photo.file.name}`}
          onClick={() => onRemove(photo)}
        >
          <X aria-hidden="true" />
        </button>
      </div>
      <figcaption>{photo.file.name}</figcaption>
    </figure>
  )
}

/**
 * Collects up to five photos. Each pick adds to the list, so several photos can be chosen in one
 * go or across multiple picks, and each can be removed individually.
 * `photos` is a list of { key, file, previewUrl }; `onChange(photos, errorMessage)` reports edits.
 */
function PhotoInput({ photos, onChange, error = '' }) {
  const latestPhotos = useRef(photos)

  useEffect(() => {
    latestPhotos.current = photos
  }, [photos])

  useEffect(() => () => {
    latestPhotos.current.forEach((photo) => URL.revokeObjectURL(photo.previewUrl))
  }, [])

  async function handleChange(event) {
    const selected = Array.from(event.target.files || [])
    // Reset so choosing the same photo again (after removing it) still fires a change.
    event.target.value = ''
    if (selected.length === 0) return

    const prepared = await Promise.all(selected.map(preparePhoto))
    const failure = prepared.find((result) => result.error)
    if (failure) {
      onChange(photos, failure.error)
      return
    }

    const known = new Set(photos.map((photo) => photo.key))
    const additions = []
    prepared.forEach(({ file }) => {
      const key = photoKey(file)
      if (known.has(key)) return
      known.add(key)
      additions.push({ key, file, previewUrl: URL.createObjectURL(file) })
    })

    if (photos.length + additions.length > MAX_PHOTOS) {
      additions.forEach((photo) => URL.revokeObjectURL(photo.previewUrl))
      onChange(photos, `You can upload a maximum of ${MAX_PHOTOS} photos.`)
      return
    }

    onChange([...photos, ...additions], '')
  }

  function handleRemove(photo) {
    URL.revokeObjectURL(photo.previewUrl)
    onChange(photos.filter((item) => item.key !== photo.key), '')
  }

  return (
    <div className="field-group photo-upload-field">
      <label htmlFor="photos">Photos</label>
      <input
        id="photos"
        type="file"
        accept={PHOTO_ACCEPT}
        multiple
        onChange={handleChange}
      />
      <small>
        At least 1 and up to {MAX_PHOTOS} JPEG, PNG, WebP, or GIF photos, 10 MB each.
      </small>
      {error && <p className="message error" role="alert">{error}</p>}
      {photos.length > 0 && (
        <div className="photo-previews" aria-label="Selected photo previews">
          {photos.map((photo) => (
            <PhotoPreview key={photo.key} photo={photo} onRemove={handleRemove} />
          ))}
        </div>
      )}
    </div>
  )
}

export default PhotoInput
