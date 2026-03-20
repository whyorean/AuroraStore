import { Navbar } from '@/components/navbar'
import { SearchHero } from '@/components/home/search-hero'
import { FeaturedBanner } from '@/components/home/featured-banner'
import { AppGridSection } from '@/components/home/app-grid-section'
import {
  MOCK_APPS,
  getFeaturedApps,
  getAppsByCategory,
  searchApps,
} from '@/lib/mock-data'

interface HomePageProps {
  searchParams: Promise<{ q?: string; category?: string }>
}

export default async function HomePage({ searchParams }: HomePageProps) {
  const { q, category } = await searchParams

  const featuredApps = getFeaturedApps()
  const primaryFeatured = featuredApps[0]

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

      <footer className="mt-16 border-t border-gray-100 bg-white mb-20 md:mb-0">
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
          <div className="flex flex-col items-center justify-between gap-6 sm:flex-row">
            <div className="text-center sm:text-left">
               <h3 className="font-bold text-lg text-gray-900 mb-1">Aurora Next</h3>
               <p className="text-sm text-gray-500 max-w-xs">
                Open-source Android app store. Privacy first, no tracking, no Google account needed.
              </p>
            </div>
            <div className="flex items-center gap-6 text-sm font-medium text-gray-500">
              <a
                href="https://github.com/aurora-oss"
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
            &copy; {new Date().getFullYear()} Aurora OSS. All rights reserved.
          </div>
        </div>
      </footer>
    </>
  )
}
