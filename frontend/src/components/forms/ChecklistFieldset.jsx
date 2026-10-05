function ChecklistFieldset({ checklist, checkedIds, onToggle, disabled = false }) {
  if (!checklist) return null

  return (
    <fieldset>
      <legend>{checklist.name}</legend>
      {checklist.items.length === 0 ? (
        <p>No checklist items are configured for this site.</p>
      ) : (
        <div className="checkbox-list">
          {checklist.items.map((item) => (
            <label key={item.id}>
              <input
                type="checkbox"
                checked={checkedIds.has(item.id)}
                onChange={() => onToggle(item.id)}
                disabled={disabled}
              />
              {item.item}
            </label>
          ))}
        </div>
      )}
    </fieldset>
  )
}

export default ChecklistFieldset
