import type { PublicCatalogItem, PublicOptionGroup } from '../types/restaurant'

export const minimumFor = (group: PublicOptionGroup) => Math.max(group.minimumSelections, group.required ? 1 : 0)

export const hasSelectableOptions = (item: PublicCatalogItem) => item.optionGroups.some((group) => group.values.length > 0)

export const hasUnavailableRequiredOptions = (item: PublicCatalogItem) => item.optionGroups.some((group) => minimumFor(group) > 0 && group.values.length === 0)
