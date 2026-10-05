import { apiRequest } from './api.js'
import { mapChecklist, mapPhoto, mapSubmission } from './mappers.js'

export async function createSafetyForm({ userId, siteId, formDate, notes }) {
  const submission = await apiRequest('/safety-forms', {
    method: 'POST',
    body: JSON.stringify({
      user_id: userId,
      site_id: siteId,
      form_date: formDate,
      notes: notes || null,
    }),
  })
  return mapSubmission(submission)
}

export async function uploadSubmissionPhotos(submissionId, files) {
  const formData = new FormData()
  files.forEach((file) => formData.append('photos', file))
  return apiRequest(`/safety-forms/${submissionId}/photos/upload`, {
    method: 'POST',
    body: formData,
  })
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
