function PhotoInput({ files, onChange }) {
  function handleChange(event) {
    const selected = Array.from(event.target.files || [])
    const invalidFile = selected.find((file) => !file.type.startsWith('image/'))

    if (invalidFile) {
      event.target.value = ''
      onChange([], `${invalidFile.name} is not an image file.`)
      return
    }

    onChange(selected, '')
  }

  return (
    <div className="field-group">
      <label htmlFor="photos">Photos</label>
      <input
        id="photos"
        type="file"
        accept="image/*"
        multiple
        onChange={handleChange}
      />
      {files.length > 0 && (
        <ul className="compact-list">
          {files.map((file) => (
            <li key={`${file.name}-${file.lastModified}`}>{file.name}</li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default PhotoInput
