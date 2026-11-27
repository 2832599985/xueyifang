import request from './request'
import type { CreatorProfileData } from '@/types/demand'

export function getCreatorProfile(userId: number | string): Promise<CreatorProfileData> {
  return request<CreatorProfileData>({
    url: `/creator/${userId}/profile`,
    method: 'get'
  })
}
