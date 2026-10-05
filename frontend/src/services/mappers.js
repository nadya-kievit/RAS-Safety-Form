export function mapUser(user) {
  if (!user) return null
  return {
    id: user.id,
    firstName: user.first_name,
    lastName: user.last_name,
    username: user.username,
    role: user.role,
    active: user.active,
    createdAt: user.created_at,
  }
}

export function mapSite(site) {
  if (!site) return null
  return {
    id: site.id,
    name: site.name,
    safetyChecklistId: site.safety_checklist_id,
    active: site.active,
    createdAt: site.created_at,
  }
}

export function mapSubmission(submission) {
  return {
    id: submission.id,
    userId: submission.user_id,
    siteId: submission.site_id,
    formDate: submission.form_date,
    notes: submission.notes,
    submittedAt: submission.submitted_at,
    updatedAt: submission.updated_at,
    user: mapUser(submission.user),
    site: mapSite(submission.site),
  }
}

export function mapChecklist(checklist) {
  return {
    id: checklist.id,
    name: checklist.name,
    items: (checklist.items || []).map((item) => ({
      id: item.id,
      safetyChecklistId: item.safety_checklist_id,
      item: item.item,
    })),
  }
}

export function mapPhoto(photo) {
  return {
    id: photo.id,
    safetyFormId: photo.safety_form_id,
    storagePath: photo.storage_path,
    filename: photo.filename,
    mimeType: photo.mime_type,
    fileSize: photo.file_size,
    createdAt: photo.created_at,
  }
}
