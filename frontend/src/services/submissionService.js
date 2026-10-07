import { apiRequest } from './api.js'
import { mapChecklist, mapPhoto, mapSubmission } from './mappers.js'

export async function submitSafetyForm({ userId, siteId, formDate, notes, photos }) {
  const formData = new FormData()
  formData.append('submission', new Blob([JSON.stringify({
    user_id: userId,
    site_id: siteId,
    form_date: formDate,
    notes: notes || null,
  })], { type: 'application/json' }))
  photos.forEach((file) => formData.append('photos', file))

  const submission = await apiRequest('/safety-forms/submit', {
    method: 'POST',
    body: formData,
  })
  return mapSubmission(submission)
}

export async function getUserSubmissions(userId) {
  const submissions = await apiRequest(`/users/${userId}/safety-forms`)
  return submissions.map(mapSubmission)
}

export async function getAllSubmissions(filters = {}) {
  const params = new URLSearchParams()
  if (filters.siteId) params.set('site_id', filters.siteId)
  if (filters.userId) params.set('user_id', filters.userId)
  if (filters.startDate) params.set('start_date', filters.startDate)
  if (filters.endDate) params.set('end_date', filters.endDate)
  const query = params.size ? `?${params.toString()}` : ''
  const submissions = await apiRequest(`/safety-forms${query}`)
  return submissions.map(mapSubmission)
}

export async function getSubmissionDetail(submissionId) {
  const submission = mapSubmission(
    await apiRequest(`/safety-forms/${submissionId}`),
  )

  const [checklist, photos] = await Promise.all([
    apiRequest(`/sites/${submission.siteId}/checklist`).then(mapChecklist),
    apiRequest(`/safety-forms/${submissionId}/photos`).then((items) =>
      items.map(mapPhoto),
    ),
  ])

  return { ...submission, checklist, photos }
}
