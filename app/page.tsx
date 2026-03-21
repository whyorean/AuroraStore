'use client'

import { useState, use } from 'react'
import { Navbar } from '@/components/navbar'
import { SearchHero } from '@/components/home/search-hero'
import { FeaturedBanner } from '@/components/home/featured-banner'
import { AppGridSection } from '@/components/home/app-grid-section'
import { Drawer } from '@/components/ui/drawer'
import { Player } from '@/components/ui/player'
import {
  MOCK_APPS,
  getFeaturedApps,
  getAppsByCategory,
  searchApps,
  getRecentlyAddedApps,
} from '@/lib/mock-data'

interface HomePageProps {
  searchParams: Promise<{ q?: string; category?: string }>
}

export default function HomePage({ searchParams }: HomePageProps) {
  const { q, category } = use(searchParams)
  const [isPlayerOpen, setIsPlayerOpen] = useState(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)

  const featuredApps = getFeaturedApps()
  const primaryFeatured = featuredApps[0]
  const recentlyAdded = getRecentlyAddedApps()

  let displayApps = MOCK_APPS
  let sectionTitle = 'Recommended for you'
  let sectionDescription: string | undefined

  if (q) {
    displayApps = searchApps(q)
    sectionTitle = `Results for "${q}"`
  } else if (category) {
    displayApps = getAppsByCategory(category)
    sectionTitle = category
  }

  const isFiltered = Boolean(q || category)

  return (
    <>
      <Navbar />
      <main className="pb-8">
        {!isFiltered && <SearchHero initialQuery={q} activeCategory={category} />}

        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
          {!isFiltered && primaryFeatured && (
            <section className="mb-10" aria-label="Featured app">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold text-gray-900">Featured</h2>
                <button
                   onClick={() => setIsPlayerOpen(true)}
                   className="text-xs font-bold text-blue-600 hover:text-blue-700"
                >
                  Listen to JMODS Radio
                </button>
              </div>
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {featuredApps.map((app) => (
                  <FeaturedBanner key={app.packageName} app={app} />
                ))}
              </div>
            </section>
          )}

          <AppGridSection
            apps={displayApps}
            title={sectionTitle}
            description={sectionDescription}
            variant={isFiltered ? "grid" : "carousel"}
          />

          {!isFiltered && recentlyAdded.length > 0 && (
            <section className="mt-12">
               <AppGridSection
                  apps={recentlyAdded}
                  title="Recently Added"
                  variant="carousel"
                />
            </section>
          )}

          <div className="mt-12 flex justify-center">
             <button
               onClick={() => setIsDrawerOpen(true)}
               className="rounded-full bg-gray-900 px-6 py-2.5 text-sm font-bold text-white hover:bg-black transition-colors"
             >
               Quick Settings
             </button>
          </div>

          {!isFiltered && (
            <section className="mt-12">
              <div className="flex flex-col gap-10">
                {(['Tools', 'Security', 'Media'] as const).map((cat) => {
                  const apps = getAppsByCategory(cat)
                  if (apps.length === 0) return null
                  return (
                    <AppGridSection
                      key={cat}
                      apps={apps}
                      title={cat}
                      variant="carousel"
                    />
                  )
                })}
              </div>
            </section>
          )}
        </div>
      </main>

      <Player
        isOpen={isPlayerOpen}
        onClose={() => setIsPlayerOpen(false)}
        title="JMODS Community Radio"
        subtitle="Live stream"
      />

      <Drawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        title="Quick Settings"
      >
        <div className="space-y-4 pb-8">
           <div className="flex items-center justify-between rounded-2xl bg-gray-50 p-4">
              <div>
                <p className="font-bold text-gray-900">Automatic Updates</p>
                <p className="text-xs text-gray-500">Keep your apps at the latest version</p>
              </div>
              <div className="h-6 w-10 rounded-full bg-blue-600 p-1">
                 <div className="h-4 w-4 translate-x-4 rounded-full bg-white shadow-sm" />
              </div>
           </div>
           <div className="flex items-center justify-between rounded-2xl bg-gray-50 p-4">
              <div>
                <p className="font-bold text-gray-900">Anonymous Mode</p>
                <p className="text-xs text-gray-500">Protect your identity while browsing</p>
              </div>
              <div className="h-6 w-10 rounded-full bg-gray-300 p-1">
                 <div className="h-4 w-4 rounded-full bg-white shadow-sm" />
              </div>
           </div>
           <button className="w-full rounded-2xl bg-blue-600 py-4 font-black text-white hover:bg-blue-700 transition-colors">
              Save Preferences
           </button>
        </div>
      </Drawer>

      <footer className="mt-16 border-t border-gray-100 bg-white mb-20 md:mb-0">
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
          <div className="flex flex-col items-center justify-between gap-6 sm:flex-row">
            <div className="text-center sm:text-left">
               <h3 className="font-bold text-lg text-gray-900 mb-1">JMODS</h3>
               <p className="text-sm text-gray-500 max-w-xs">
                Open-source Android app store. Privacy first, no tracking, no Google account needed.
              </p>
            </div>
            <div className="flex items-center gap-6 text-sm font-medium text-gray-500">
              <a
                href="https://github.com/j-mods"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-blue-600 transition-colors"
              >
                GitHub
              </a>
              <a href="/privacy" className="hover:text-blue-600 transition-colors">
                Privacy
              </a>
              <a href="/faq" className="hover:text-blue-600 transition-colors">
                FAQ
              </a>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-50 text-center text-xs text-gray-400">
            &copy; {new Date().getFullYear()} JMODS. All rights reserved.
          </div>
        </div>
      </footer>
    </>
  )
}
