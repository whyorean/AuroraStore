export type AppCategory =
  | 'Productivity'
  | 'Social'
  | 'Games'
  | 'Tools'
  | 'Media'
  | 'Security'
  | 'Finance'
  | 'Health'

export type InstallMethod = 'session' | 'root' | 'shizuku'

export interface AppRating {
  average: number
  count: number
}

export interface AppPermission {
  name: string
  description: string
  dangerous: boolean
}

export interface AppTracker {
  name: string
  website: string
}

export interface AppVersion {
  versionName: string
  versionCode: number
  minSdk: number
  targetSdk: number
  sizeBytes: number
  uploadDate: string
  changelog: string
}

export interface App {
  packageName: string
  name: string
  developer: string
  category: AppCategory
  shortDescription: string
  longDescription: string
  iconUrl: string
  headerImageUrl: string
  screenshotUrls: string[]
  rating: AppRating
  downloads: string
  version: AppVersion
  permissions: AppPermission[]
  trackers: AppTracker[]
  isOpenSource: boolean
  sourceUrl?: string
  isFeatured?: boolean
}
