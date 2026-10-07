import { useEffect, useState } from 'react'

function PhotoPreview({ file }) {
  const [url] = useState(() => URL.createObjectURL(file))

  useEffect(() => () => URL.revokeObjectURL(url), [url])

  return (
    <figure>
      <img src={url} alt={`Preview of ${file.name}`} />
      <figcaption>{file.name}</figcaption>
    </figure>
  )
}

function PhotoInput({ files, onChange }) {
  const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif'])
  const maximumFileSize = 10 * 1024 * 1024

  function handleChange(event) {
    const selected = Array.from(event.target.files || [])
    const invalidFile = selected.find((file) => !allowedTypes.has(file.type))

    if (invalidFile) {
      event.target.value = ''
      onChange([], `${invalidFile.name} is not a supported JPEG, PNG, WebP, or GIF image.`)
      return
    }

    const oversizedFile = selected.find((file) => file.size > maximumFileSize)
    if (oversizedFile) {
      event.target.value = ''
      onChange([], `${oversizedFile.name} is larger than 10 MB.`)
      return
    }

    if (selected.length > 5) {
      event.target.value = ''
      onChange([], 'You can upload a maximum of 5 photos.')
      return
    }

    onChange(selected, '')
  }

  return (
    <div className="field-group photo-upload-field">
      <label htmlFor="photos">Photos</label>
      <input
        id="photos"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        multiple
        onChange={handleChange}
      />
      <small>Up to 5 JPEG, PNG, WebP, or GIF photos, 10 MB each.</small>
      {files.length > 0 && (
        <div className="photo-previews" aria-label="Selected photo previews">
          {files.map((file) => (
            <PhotoPreview
              key={`${file.name}-${file.lastModified}`}
              file={file}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export default PhotoInput
