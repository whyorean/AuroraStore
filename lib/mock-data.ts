import type { App } from './types'

export const MOCK_APPS: App[] = [
  {
    packageName: 'org.signal.messenger',
    name: 'Signal',
    developer: 'Signal Foundation',
    category: 'Social',
    shortDescription: 'Private messenger. No ads, no trackers, no compromise.',
    longDescription:
      'Signal is a cross-platform encrypted messaging service. All messages are end-to-end encrypted, and Signal collects minimal metadata. It is free, open-source, and supported by grants and donations.',
    iconUrl: 'https://play-lh.googleusercontent.com/a1msHo9W52_K36SWfDyfPkHNtpDU_j9WKH7yrfzVyEqQC5hf2wROWuGfcvXzs4OGfQ',
    headerImageUrl: 'https://play-lh.googleusercontent.com/a1msHo9W52_K36SWfDyfPkHNtpDU_j9WKH7yrfzVyEqQC5hf2wROWuGfcvXzs4OGfQ',
    screenshotUrls: [],
    rating: { average: 4.6, count: 2100000 },
    downloads: '100M+',
    version: {
      versionName: '7.5.1',
      versionCode: 7501,
      minSdk: 24,
      targetSdk: 34,
      sizeBytes: 41000000,
      uploadDate: '2025-11-12',
      changelog: 'Improved notification reliability, bug fixes.',
    },
    permissions: [
      { name: 'CAMERA', description: 'Take photos and videos', dangerous: true },
      { name: 'CONTACTS', description: 'Read your contacts', dangerous: true },
      { name: 'INTERNET', description: 'Have full network access', dangerous: false },
    ],
    trackers: [],
    isOpenSource: true,
    sourceUrl: 'https://github.com/signalapp/Signal-Android',
    isFeatured: true,
  },
  {
    packageName: 'org.mozilla.firefox',
    name: 'Firefox',
    developer: 'Mozilla',
    category: 'Tools',
    shortDescription: 'Fast, private and secure browser by Mozilla.',
    longDescription:
      'Firefox for Android is built on GeckoView. It supports uBlock Origin and hundreds of other extensions. Enhanced Tracking Protection blocks trackers by default.',
    iconUrl: 'https://play-lh.googleusercontent.com/jLchAkTnGtgOBKyliuVnH5-JqJjHhL1TEKEAHfhQjGrAI-bdCvEE9fzgCgdV4OevLYE',
    headerImageUrl: 'https://play-lh.googleusercontent.com/jLchAkTnGtgOBKyliuVnH5-JqJjHhL1TEKEAHfhQjGrAI-bdCvEE9fzgCgdV4OevLYE',
    screenshotUrls: [],
    rating: { average: 4.3, count: 1500000 },
    downloads: '500M+',
    version: {
      versionName: '133.0',
      versionCode: 2016133000,
      minSdk: 21,
      targetSdk: 34,
      sizeBytes: 92000000,
      uploadDate: '2026-01-10',
      changelog: 'Improved tab management. Updated security patches.',
    },
    permissions: [
      { name: 'INTERNET', description: 'Have full network access', dangerous: false },
      { name: 'CAMERA', description: 'Take photos for web forms', dangerous: true },
    ],
    trackers: [{ name: 'Adjust', website: 'https://www.adjust.com' }],
    isOpenSource: true,
    sourceUrl: 'https://github.com/mozilla-mobile/fenix',
    isFeatured: true,
  }
]

export function getAppByPackageName(packageName: string): App | undefined {
  return MOCK_APPS.find((app) => app.packageName === packageName)
}

export function getAppsByCategory(category: string): App[] {
  return MOCK_APPS.filter((app) => app.category === category)
}

export function getFeaturedApps(): App[] {
  return MOCK_APPS.filter((app) => app.isFeatured)
}

export function searchApps(query: string): App[] {
  const q = query.toLowerCase()
  return MOCK_APPS.filter(
    (app) =>
      app.name.toLowerCase().includes(q) ||
      app.developer.toLowerCase().includes(q) ||
      app.shortDescription.toLowerCase().includes(q) ||
      app.packageName.toLowerCase().includes(q),
  )
}

export const ALL_CATEGORIES = [
  'Productivity',
  'Social',
  'Games',
  'Tools',
  'Media',
  'Security',
  'Finance',
  'Health',
] as const
