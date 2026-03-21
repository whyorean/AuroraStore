import { Navbar } from '@/components/navbar'
import { getAppByPackageName } from '@/lib/mock-data'
import { notFound } from 'next/navigation'
import Image from 'next/image'
import {
  Download,
  Shield,
  ShieldCheck,
  AlertTriangle,
  Package,
  Calendar,
  HardDrive,
  Cpu,
  GitBranch,
  ExternalLink,
  ChevronLeft,
  Info
} from 'lucide-react'
import Link from 'next/link'
import { CategoryBadge } from '@/components/category-badge'
import { StarRating } from '@/components/star-rating'
import { formatBytes, formatDate, formatRatingCount } from '@/lib/format'

interface AppDetailsPageProps {
  params: Promise<{ packageName: string }>
}

export default async function AppDetailsPage({ params }: AppDetailsPageProps) {
  const { packageName } = await params
  const app = getAppByPackageName(packageName)

  if (!app) {
    notFound()
  }

  const dangerousPermissions = app.permissions.filter((p) => p.dangerous)
  const safePermissions = app.permissions.filter((p) => !p.dangerous)

  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        <Link
          href="/"
          className="mb-6 inline-flex items-center gap-1 text-sm font-medium text-gray-500 hover:text-blue-600 transition-colors"
        >
          <ChevronLeft className="h-4 w-4" />
          Back to Browse
        </Link>

        {/* Header Section */}
        <section className="mb-10 rounded-3xl border border-gray-100 bg-white p-6 shadow-sm md:p-8">
          <div className="flex flex-col gap-6 md:flex-row md:items-start">
            <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-2xl border border-gray-50 shadow-inner md:h-32 md:w-32">
              <Image
                src={app.iconUrl}
                alt=""
                fill
                className="object-cover"
                unoptimized
              />
            </div>

            <div className="flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-2xl font-black tracking-tight text-gray-900 md:text-3xl">
                  {app.name}
                </h1>
                {app.isOpenSource && (
                  <span className="rounded-full bg-green-50 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider text-green-600 border border-green-100">
                    Open Source
                  </span>
                )}
              </div>
              <p className="mt-1 text-base font-medium text-gray-500">{app.developer}</p>
              <div className="mt-4 flex flex-wrap items-center gap-4">
                <CategoryBadge category={app.category} size="sm" />
                <div className="flex items-center gap-2 text-sm text-gray-600">
                  <StarRating value={app.rating.average} />
                  <span className="font-bold text-gray-900">{app.rating.average.toFixed(1)}</span>
                  <span className="text-gray-400">({formatRatingCount(app.rating.count)} reviews)</span>
                </div>
                <div className="flex items-center gap-1.5 text-sm text-gray-600">
                   <Download className="h-4 w-4 text-gray-400" />
                   <span className="font-semibold text-gray-900">{app.downloads}</span>
                   <span className="text-gray-400">downloads</span>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-8 flex flex-wrap items-center gap-3 border-t border-gray-50 pt-8">
            <button
              type="button"
              className="flex h-12 items-center gap-2 rounded-xl bg-blue-600 px-8 text-sm font-bold text-white shadow-lg shadow-blue-200 transition-all hover:bg-blue-700 hover:shadow-blue-300 active:scale-95"
            >
              <Download className="h-5 w-5" aria-hidden="true" />
              Download APK · {formatBytes(app.version.sizeBytes)}
            </button>

            {app.isOpenSource && app.sourceUrl && (
              <a
                href={app.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-12 items-center gap-2 rounded-xl border border-gray-200 bg-white px-6 text-sm font-bold text-gray-700 transition-all hover:border-blue-300 hover:bg-blue-50 active:scale-95"
              >
                <GitBranch className="h-4 w-4" aria-hidden="true" />
                Source Code
                <ExternalLink className="h-3.5 w-3.5 text-gray-400" aria-hidden="true" />
              </a>
            )}
          </div>
        </section>

        <div className="grid gap-8 lg:grid-cols-3">
          <div className="flex flex-col gap-8 lg:col-span-2">
            <section aria-labelledby="desc-heading" className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
              <h2 id="desc-heading" className="mb-4 flex items-center gap-2 text-lg font-bold text-gray-900">
                <Info className="h-5 w-5 text-blue-600" />
                About this app
              </h2>
              <p className="text-sm leading-relaxed text-gray-600 whitespace-pre-wrap">{app.longDescription}</p>
            </section>

            {/* Enhanced Permissions & Trackers UI */}
            <div className="grid gap-6 sm:grid-cols-2">
                <section aria-labelledby="perms-heading" className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
                  <h2 id="perms-heading" className="mb-4 flex items-center gap-2 text-lg font-bold text-gray-900">
                    <Shield className="h-5 w-5 text-blue-600" />
                    Permissions
                  </h2>
                  {app.permissions.length === 0 ? (
                    <div className="flex items-center gap-2 rounded-xl bg-green-50 p-4 text-sm text-green-700">
                        <ShieldCheck className="h-5 w-5" />
                        No permissions required.
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {dangerousPermissions.length > 0 && (
                        <div>
                          <p className="mb-2 text-[10px] font-bold uppercase tracking-widest text-red-500">Sensitive Access</p>
                          <div className="space-y-2">
                            {dangerousPermissions.map((perm) => (
                                <div key={perm.name} className="rounded-lg bg-red-50/50 p-3">
                                    <div className="flex items-center gap-2">
                                        <AlertTriangle className="h-3.5 w-3.5 text-red-500" />
                                        <span className="text-xs font-bold text-red-900">{perm.name}</span>
                                    </div>
                                    <p className="mt-1 text-[11px] leading-tight text-red-700">{perm.description}</p>
                                </div>
                            ))}
                          </div>
                        </div>
                      )}
                      {safePermissions.length > 0 && (
                        <div>
                          <p className="mb-2 text-[10px] font-bold uppercase tracking-widest text-gray-400">Standard Access</p>
                          <div className="space-y-2">
                            {safePermissions.map((perm) => (
                                <div key={perm.name} className="flex items-start gap-2 rounded-lg border border-gray-50 p-2">
                                    <Shield className="mt-0.5 h-3 w-3 text-gray-400" />
                                    <div>
                                        <span className="text-[11px] font-semibold text-gray-700">{perm.name}</span>
                                        <p className="text-[10px] text-gray-500">{perm.description}</p>
                                    </div>
                                </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </section>

                <section aria-labelledby="trackers-heading" className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
                  <h2 id="trackers-heading" className="mb-4 flex items-center gap-2 text-lg font-bold text-gray-900">
                    <ShieldCheck className="h-5 w-5 text-blue-600" />
                    Privacy Audit
                  </h2>
                  {app.trackers.length === 0 ? (
                    <div className="flex flex-col items-center justify-center rounded-2xl bg-blue-50/50 py-8 text-center">
                      <div className="mb-3 rounded-full bg-blue-100 p-3">
                        <ShieldCheck className="h-8 w-8 text-blue-600" />
                      </div>
                      <h3 className="font-bold text-blue-900">Clean App</h3>
                      <p className="px-4 text-xs text-blue-700">No known trackers or ads detected in this version.</p>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      <div className="flex items-center gap-2 rounded-xl bg-orange-50 p-3 text-sm text-orange-800 border border-orange-100">
                        <AlertTriangle className="h-5 w-5 shrink-0" />
                        <p className="text-xs font-medium">{app.trackers.length} trackers detected</p>
                      </div>
                      <div className="space-y-2">
                        {app.trackers.map((tracker) => (
                          <div key={tracker.name} className="flex items-center justify-between rounded-xl border border-gray-100 p-3">
                            <span className="text-sm font-bold text-gray-900">{tracker.name}</span>
                            <a
                              href={tracker.website}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-[10px] font-bold text-blue-600 underline underline-offset-2"
                            >
                              Details
                            </a>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </section>
            </div>
          </div>

          <div className="flex flex-col gap-8">
            <section aria-labelledby="version-heading" className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
              <h2 id="version-heading" className="mb-6 text-sm font-bold uppercase tracking-wider text-gray-400">
                Technical Specs
              </h2>
              <dl className="space-y-4">
                <div className="flex items-center justify-between">
                  <dt className="flex items-center gap-2 text-sm text-gray-500">
                    <Package className="h-4 w-4" />
                    Version
                  </dt>
                  <dd className="text-sm font-bold text-gray-900">{app.version.versionName}</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="flex items-center gap-2 text-sm text-gray-500">
                    <HardDrive className="h-4 w-4" />
                    Size
                  </dt>
                  <dd className="text-sm font-bold text-gray-900">{formatBytes(app.version.sizeBytes)}</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="flex items-center gap-2 text-sm text-gray-500">
                    <Cpu className="h-4 w-4" />
                    Architecture
                  </dt>
                  <dd className="text-sm font-bold text-gray-900">universal</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="flex items-center gap-2 text-sm text-gray-500">
                    <Shield className="h-4 w-4" />
                    Requirement
                  </dt>
                  <dd className="text-sm font-bold text-gray-900">Android {app.version.minSdk}+</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="flex items-center gap-2 text-sm text-gray-500">
                    <Calendar className="h-4 w-4" />
                    Released
                  </dt>
                  <dd className="text-sm font-bold text-gray-900">{formatDate(app.version.uploadDate)}</dd>
                </div>
              </dl>
            </section>

            {app.version.changelog && (
              <section aria-labelledby="changelog-heading" className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
                <h2 id="changelog-heading" className="mb-4 text-sm font-bold uppercase tracking-wider text-gray-400">
                  {"What's New"}
                </h2>
                <div className="rounded-xl bg-gray-50 p-4 text-xs leading-relaxed text-gray-600">
                  {app.version.changelog}
                </div>
              </section>
            )}

            <section aria-label="Package name" className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
              <p className="mb-2 text-[10px] font-bold uppercase tracking-widest text-gray-400">
                Package ID
              </p>
              <p className="break-all font-mono text-[10px] text-gray-900">{app.packageName}</p>
            </section>
          </div>
        </div>
      </main>

      <footer className="mt-20 border-t border-gray-100 bg-white pb-24 md:pb-12">
        <div className="mx-auto max-w-7xl px-4 pt-12 sm:px-6 text-center">
            <h2 className="text-xl font-black text-gray-900">JMODS</h2>
            <p className="mt-2 text-sm text-gray-500">The high-performance, private app store for Android.</p>
            <div className="mt-8 flex justify-center gap-8 text-xs font-bold text-gray-400">
                <Link href="/" className="hover:text-blue-600">Browse</Link>
                <Link href="/categories" className="hover:text-blue-600">Categories</Link>
                <Link href="/privacy" className="hover:text-blue-600">Privacy</Link>
            </div>
        </div>
      </footer>
    </>
  )
}
