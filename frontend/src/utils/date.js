export function todayInputValue() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

export function currentTimeInputValue() {
  const now = new Date()
  return [now.getHours(), now.getMinutes()]
    .map((part) => String(part).padStart(2, '0'))
    .join(':')
}

export function isFutureLocalDateTime(date, time) {
  if (!date || !time) return false
  return new Date(`${date}T${time}`).getTime() > Date.now()
}

// The form's date and time are entered in the user's local time zone; send an exact instant.
export function toInstantValue(date, time) {
  if (!date || !time) return ''
  return new Date(`${date}T${time}:00`).toISOString()
}

export function formatDate(value) {
  if (!value) return 'Not available'
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(
    new Date(`${value}T00:00:00`),
  )
}

export function formatDateTime(value) {
  if (!value) return 'Not available'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
