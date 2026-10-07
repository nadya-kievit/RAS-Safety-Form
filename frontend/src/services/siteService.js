import { apiRequest } from './api.js'
import { mapChecklist, mapSite } from './mappers.js'

export async function getActiveSites() {
  const sites = await apiRequest('/sites')
  return sites.map(mapSite)
}

export async function getAllSites() {
  const sites = await apiRequest('/sites?include_inactive=true')
  return sites.map(mapSite)
}

export async function getChecklistForSite(siteId) {
  return mapChecklist(await apiRequest(`/sites/${siteId}/checklist`))
}

export async function createSite({ name, checklistItems }) {
  const site = await apiRequest('/sites', {
    method: 'POST',
    body: JSON.stringify({ name, checklist_items: checklistItems }),
  })
  return mapSite(site)
}

export async function renameSite(siteId, name) {
  const site = await apiRequest(`/sites/${siteId}`, {
    method: 'PATCH',
    body: JSON.stringify({ name }),
  })
  return mapSite(site)
}

export async function setSiteActive(siteId, active) {
  const site = await apiRequest(`/sites/${siteId}/active`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  })
  return mapSite(site)
}

export async function updateChecklist(siteId, { name, items }) {
  const checklist = await apiRequest(`/sites/${siteId}/checklist`, {
    method: 'PUT',
    body: JSON.stringify({
      name,
      items: items.map((item) => ({ id: item.id ?? null, item: item.item })),
    }),
  })
  return mapChecklist(checklist)
}
