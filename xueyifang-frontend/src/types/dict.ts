export interface ServiceTag {
  tagId: number
  tagName: string
  description: string
  icon: string
}

export interface Professional {
  id: number
  professionalName: string
  description: string
}

export interface TradeLocation {
  id: number
  campusId?: number
  locationName: string
  locationDescription: string
  locationAddress: string
  latitude?: number
  longitude?: number
  validRadiusMeters?: number
  hasCctv?: number
  isAvailable: number
}

export interface CampusInfo {
  id: number
  campusCode: string
  campusName: string
  city: string
  centerLat: number
  centerLng: number
  geofencePolygon?: string
  safeRadiusMeters?: number
  eduEmailDomains?: string
  status: number
}

export function getTagNameById(tags: ServiceTag[], tagId: number): string {
  const tag = tags.find(t => t.tagId === tagId)
  return tag?.tagName || '未知分类'
}

export function getProfessionalNameById(professionals: Professional[], id: number): string {
  const pro = professionals.find(p => p.id === id)
  return pro?.professionalName || '未知专业'
}

export function getLocationNameById(locations: TradeLocation[], id: number): string {
  const loc = locations.find(l => l.id === id)
  return loc?.locationName || '未知地点'
}

export function filterAvailableLocations(locations: TradeLocation[]): TradeLocation[] {
  return locations.filter(loc => loc.isAvailable === 1)
}
