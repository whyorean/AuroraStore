import { notFound } from 'next/navigation'
import type { Metadata } from 'next'
import Image from 'next/image'
import Link from 'next/link'
import {
  ArrowLeft,
  Download,
  GitBranch,
  Shield,
  AlertTriangle,
  ExternalLink,
  Package,
  Cpu,
  Calendar,
  HardDrive,
} from 'lucide-react'
import { getAppByPackageName } from '@/lib/mock-data'
import { formatBytes, formatDate, formatRatingCount } from '@/lib/format'
import { CategoryBadge } from '@/components/category-badge'
import { StarRating } from '@/components/star-rating'
import { Navbar } from '@/components/navbar'

interface AppPageProps {
  params: Promise<{ packageName: string }>
}

export async function generateMetadata({ params }: AppPageProps): Promise<Metadata> {
  const { packageName } = await params
  const app = getAppByPackageName(packageName)
  if (!app) return { title: 'App not found' }
  return {
    title: `${app.name} — Aurora Next`,
    description: app.shortDescription,
  }
}

export default async function AppPage({ params }: AppPageProps) {
  const { packageName } = await params
  const app = getAppByPackageName(packageName)
  if (!app) notFound()

  const dangerousPermissions = app.permissions.filter((p) => p.dangerous)
  const safePermissions = app.permissions.filter((p) => !p.dangerous)

  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
        <Link
          href="/"
          className="mb-6 inline-flex items-center gap-1.5 text-sm text-gray-500 transition-colors hover:text-gray-900"
        >
          <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
          Back to Browse
        </Link>

        <section aria-label="App overview" className="mb-8">
          <div className="flex items-start gap-5">
            <div className="relative h-20 w-20 shrink-0 overflow-hidden rounded-2xl border border-gray-100 shadow-sm">
              <Image
                src={app.iconUrl}
                alt={`${app.name} icon`}
                fill
                className="object-cover"
                unoptimized
                priority
              />
            </div>

            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-bold text-gray-900">{app.name}</h1>
                {app.isOpenSource && (
                  <span className="flex items-center gap-1 rounded border border-blue-200 bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-600">
                    <GitBranch className="h-3 w-3" aria-hidden="true" />
                    Open Source
                  </span>
                )}
              </div>
              <p className="mt-0.5 text-sm text-gray-500">{app.developer}</p>
              <div className="mt-2 flex flex-wrap items-center gap-3">
                <CategoryBadge category={app.category} size="sm" />
                <div className="flex items-center gap-1.5 text-sm text-gray-500">
                  <StarRating value={app.rating.average} />
                  <span className="font-medium text-gray-900">{app.rating.average.toFixed(1)}</span>
                  <span className="text-xs">({formatRatingCount(app.rating.count)})</span>
                </div>
                <span className="text-xs text-gray-500">{app.downloads} downloads</span>
              </div>
            </div>
          </div>

          <div className="mt-5 flex flex-wrap items-center gap-3">
            <button
              type="button"
              className="flex h-9 items-center gap-2 rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
            >
              <Download className="h-4 w-4" aria-hidden="true" />
              Install · {formatBytes(app.version.sizeBytes)}
            </button>

            {app.isOpenSource && app.sourceUrl && (
              <a
                href={app.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-9 items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 text-sm text-gray-600 transition-colors hover:border-blue-400 hover:text-blue-600"
              >
                <GitBranch className="h-3.5 w-3.5" aria-hidden="true" />
                Source
                <ExternalLink className="h-3 w-3" aria-hidden="true" />
              </a>
            )}
          </div>
        </section>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="flex flex-col gap-6 lg:col-span-2">
            <section aria-labelledby="desc-heading" className="rounded-xl border border-gray-100 bg-white p-5">
              <h2 id="desc-heading" className="mb-3 text-sm font-semibold text-gray-900">
                About
              </h2>
              <p className="text-sm leading-relaxed text-gray-600">{app.longDescription}</p>
            </section>

            <section aria-labelledby="perms-heading" className="rounded-xl border border-gray-100 bg-white p-5">
              <h2 id="perms-heading" className="mb-3 text-sm font-semibold text-gray-900">
                Permissions
              </h2>
              {app.permissions.length === 0 ? (
                <p className="text-sm text-gray-500">No permissions declared.</p>
              ) : (
                <div className="flex flex-col gap-2">
                  {dangerousPermissions.length > 0 && (
                    <>
                      <p className="text-xs font-medium uppercase tracking-wider text-gray-400">
                        Sensitive
                      </p>
                      {dangerousPermissions.map((perm) => (
                        <div key={perm.name} className="flex items-start gap-2.5">
                          <AlertTriangle
                            className="mt-0.5 h-3.5 w-3.5 shrink-0 text-red-500"
                            aria-hidden="true"
                          />
                          <div>
                            <p className="text-xs font-medium text-gray-900">{perm.name}</p>
                            <p className="text-xs text-gray-500">{perm.description}</p>
                          </div>
                        </div>
                      ))}
                    </>
                  )}
                  {safePermissions.length > 0 && (
                    <>
                      <p className="mt-2 text-xs font-medium uppercase tracking-wider text-gray-400">
                        Standard
                      </p>
                      {safePermissions.map((perm) => (
                        <div key={perm.name} className="flex items-start gap-2.5">
                          <Shield
                            className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gray-400"
                            aria-hidden="true"
                          />
                          <div>
                            <p className="text-xs font-medium text-gray-900">{perm.name}</p>
                            <p className="text-xs text-gray-500">{perm.description}</p>
                          </div>
                        </div>
                      ))}
                    </>
                  )}
                </div>
              )}
            </section>

            <section aria-labelledby="trackers-heading" className="rounded-xl border border-gray-100 bg-white p-5">
              <h2 id="trackers-heading" className="mb-1 text-sm font-semibold text-gray-900">
                Trackers
              </h2>
              {app.trackers.length === 0 ? (
                <p className="flex items-center gap-2 text-sm text-gray-500">
                  <Shield className="h-3.5 w-3.5 text-blue-600" aria-hidden="true" />
                  No known trackers detected.
                </p>
              ) : (
                <div className="mt-3 flex flex-col gap-2">
                  {app.trackers.map((tracker) => (
                    <div key={tracker.name} className="flex items-center justify-between text-sm">
                      <span className="flex items-center gap-2 text-gray-900">
                        <AlertTriangle
                          className="h-3.5 w-3.5 text-red-500"
                          aria-hidden="true"
                        />
                        {tracker.name}
                      </span>
                      <a
                        href={tracker.website}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-1 text-xs text-gray-500 hover:text-blue-600"
                      >
                        Info
                        <ExternalLink className="h-3 w-3" aria-hidden="true" />
                      </a>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>

          <div className="flex flex-col gap-6">
            <section aria-labelledby="version-heading" className="rounded-xl border border-gray-100 bg-white p-5">
              <h2 id="version-heading" className="mb-4 text-sm font-semibold text-gray-900">
                Version info
              </h2>
              <dl className="flex flex-col gap-3 text-sm">
                <div className="flex items-center justify-between gap-2">
                  <dt className="flex items-center gap-1.5 text-gray-500">
                    <Package className="h-3.5 w-3.5" aria-hidden="true" />
                    Version
                  </dt>
                  <dd className="font-medium text-gray-900">{app.version.versionName}</dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="flex items-center gap-1.5 text-gray-500">
                    <HardDrive className="h-3.5 w-3.5" aria-hidden="true" />
                    Size
                  </dt>
                  <dd className="font-medium text-gray-900">{formatBytes(app.version.sizeBytes)}</dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="flex items-center gap-1.5 text-gray-500">
                    <Cpu className="h-3.5 w-3.5" aria-hidden="true" />
                    Min SDK
                  </dt>
                  <dd className="font-medium text-gray-900">Android {app.version.minSdk}+</dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="flex items-center gap-1.5 text-gray-500">
                    <Cpu className="h-3.5 w-3.5" aria-hidden="true" />
                    Target SDK
                  </dt>
                  <dd className="font-medium text-gray-900">{app.version.targetSdk}</dd>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <dt className="flex items-center gap-1.5 text-gray-500">
                    <Calendar className="h-3.5 w-3.5" aria-hidden="true" />
                    Updated
                  </dt>
                  <dd className="font-medium text-gray-900">{formatDate(app.version.uploadDate)}</dd>
                </div>
              </dl>
            </section>

            {app.version.changelog && (
              <section aria-labelledby="changelog-heading" className="rounded-xl border border-gray-100 bg-white p-5">
                <h2 id="changelog-heading" className="mb-2 text-sm font-semibold text-gray-900">
                  {"What's new"}
                </h2>
                <p className="text-xs leading-relaxed text-gray-600">
                  {app.version.changelog}
                </p>
              </section>
            )}

            <section aria-label="Package name" className="rounded-xl border border-gray-100 bg-white p-5">
              <p className="mb-1 text-xs font-medium uppercase tracking-wider text-gray-400">
                Package
              </p>
              <p className="break-all font-mono text-xs text-gray-900">{app.packageName}</p>
            </section>
          </div>
        </div>
      </main>

      <footer className="mt-16 border-t border-gray-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
          <p className="text-center text-xs text-gray-500">
            Aurora Next — Open-source Android app store. Not affiliated with Google.
          </p>
        </div>
      </footer>
    </>
  )
}
