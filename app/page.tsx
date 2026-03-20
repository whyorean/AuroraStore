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
  let sectionTitle = 'All Apps'
  let sectionDescription: string | undefined

  if (q) {
    displayApps = searchApps(q)
    sectionTitle = `Results for "${q}"`
    sectionDescription =
      displayApps.length === 0
        ? undefined
        : `Found ${displayApps.length} app${displayApps.length !== 1 ? 's' : ''}`
  } else if (category) {
    displayApps = getAppsByCategory(category)
    sectionTitle = category
    sectionDescription = `Apps in the ${category} category`
  }

  const isFiltered = Boolean(q || category)

  return (
    <>
      <Navbar />
      <main>
        <SearchHero initialQuery={q} activeCategory={category} />

        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
          {!isFiltered && primaryFeatured && (
            <section className="mb-8" aria-label="Featured app">
              <h2 className="mb-3 text-base font-semibold text-gray-900">Featured</h2>
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
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
            variant="grid"
          />

          {!isFiltered && (
            <section className="mt-10">
              <h2 className="mb-5 text-base font-semibold text-gray-900">Browse by category</h2>
              <div className="flex flex-col gap-6">
                {(['Tools', 'Security', 'Media'] as const).map((cat) => {
                  const apps = getAppsByCategory(cat)
                  if (apps.length === 0) return null
                  return (
                    <AppGridSection
                      key={cat}
                      apps={apps}
                      title={cat}
                      variant="row"
                    />
                  )
                })}
              </div>
            </section>
          )}
        </div>
      </main>

      <footer className="mt-16 border-t border-gray-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
          <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
            <p className="text-xs text-gray-500">
              Aurora Next — Open-source Android app store. Not affiliated with Google.
            </p>
            <div className="flex items-center gap-4 text-xs text-gray-500">
              <a
                href="https://github.com/aurora-oss"
                target="_blank"
                rel="noopener noreferrer"
                className="hover:text-gray-900"
              >
                GitHub
              </a>
              <a href="/privacy" className="hover:text-gray-900">
                Privacy
              </a>
              <a href="/faq" className="hover:text-gray-900">
                FAQ
              </a>
            </div>
          </div>
        </div>
      </footer>
    </>
  )
}
