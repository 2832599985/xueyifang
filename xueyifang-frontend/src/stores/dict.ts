import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { listServiceTags, listProfessionals, listTradeLocations } from '@/api/dict'
import type { ServiceTag, Professional, TradeLocation } from '@/types/dict'

export const useDictStore = defineStore('dict', () => {
  const serviceTags = ref<ServiceTag[] | null>(null)
  const professionals = ref<Professional[] | null>(null)
  const tradeLocations = ref<TradeLocation[] | null>(null)

  const loadingTags = ref(false)
  const loadingProfessionals = ref(false)
  const loadingLocations = ref(false)

  let tagsPromise: Promise<ServiceTag[]> | null = null
  let professionalsPromise: Promise<Professional[]> | null = null
  let locationsPromise: Promise<TradeLocation[]> | null = null

  const availableLocations = computed(() => {
    if (!tradeLocations.value) return []
    return tradeLocations.value.filter(loc => loc.isAvailable === 1)
  })

  const hasServiceTags = computed(() => serviceTags.value !== null)
  const hasProfessionals = computed(() => professionals.value !== null)
  const hasTradeLocations = computed(() => tradeLocations.value !== null)

  const fetchServiceTags = async (force = false): Promise<ServiceTag[]> => {
    if (serviceTags.value && !force) return serviceTags.value
    if (tagsPromise && !force) return tagsPromise

    loadingTags.value = true
    tagsPromise = listServiceTags()
      .then((res) => {
        serviceTags.value = res
        return res
      })
      .finally(() => {
        loadingTags.value = false
        tagsPromise = null
      })
    return tagsPromise
  }

  const fetchProfessionals = async (force = false): Promise<Professional[]> => {
    if (professionals.value && !force) return professionals.value
    if (professionalsPromise && !force) return professionalsPromise

    loadingProfessionals.value = true
    professionalsPromise = listProfessionals()
      .then((res) => {
        professionals.value = res
        return res
      })
      .finally(() => {
        loadingProfessionals.value = false
        professionalsPromise = null
      })
    return professionalsPromise
  }

  const fetchTradeLocations = async (force = false): Promise<TradeLocation[]> => {
    if (tradeLocations.value && !force) return tradeLocations.value
    if (locationsPromise && !force) return locationsPromise

    loadingLocations.value = true
    locationsPromise = listTradeLocations()
      .then((res) => {
        tradeLocations.value = res
        return res
      })
      .finally(() => {
        loadingLocations.value = false
        locationsPromise = null
      })
    return locationsPromise
  }

  const fetchAll = async (force = false): Promise<void> => {
    await Promise.all([
      fetchServiceTags(force),
      fetchProfessionals(force),
      fetchTradeLocations(force)
    ])
  }

  const getTagName = (tagId: number): string => {
    return serviceTags.value?.find(t => t.tagId === tagId)?.tagName || '未分类'
  }

  const getProfessionalName = (id: number): string => {
    return professionals.value?.find(p => p.id === id)?.professionalName || '未知专业'
  }

  const getLocationById = (id?: number): TradeLocation | undefined => {
    if (!id || !tradeLocations.value) return undefined
    return tradeLocations.value.find(l => l.id === id)
  }

  const getLocationName = (id: number): string => {
    return getLocationById(id)?.locationName || '未知地点'
  }

  const clearCache = () => {
    serviceTags.value = null
    professionals.value = null
    tradeLocations.value = null
  }

  return {
    serviceTags,
    professionals,
    tradeLocations,
    loadingTags,
    loadingProfessionals,
    loadingLocations,
    availableLocations,
    hasServiceTags,
    hasProfessionals,
    hasTradeLocations,
    fetchServiceTags,
    fetchProfessionals,
    fetchTradeLocations,
    fetchAll,
    getTagName,
    getProfessionalName,
    getLocationById,
    getLocationName,
    clearCache
  }
})
