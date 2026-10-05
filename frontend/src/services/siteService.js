import { apiRequest } from './api.js'
import { mapChecklist, mapSite } from './mappers.js'

export async function getActiveSites() {
  const sites = await apiRequest('/sites')
  return sites.map(mapSite)
}

export async function getChecklistForSite(siteId) {
  return mapChecklist(await apiRequest(`/sites/${siteId}/checklist`))
}
