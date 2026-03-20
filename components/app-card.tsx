import Link from 'next/link'
import Image from 'next/image'
import { Star, GitBranch } from 'lucide-react'
import type { App } from '@/lib/types'
import { formatRatingCount } from '@/lib/format'
import { CategoryBadge } from './category-badge'

interface AppCardProps {
  app: App
  variant?: 'grid' | 'row'
}

export function AppCard({ app, variant = 'grid' }: AppCardProps) {
  if (variant === 'row') {
    return (
      <Link
        href={`/app/${app.packageName}`}
        className="group flex items-center gap-3 rounded-lg border border-gray-100 bg-white p-3 transition-colors hover:border-blue-400 hover:bg-gray-50"
      >
        <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-xl">
          <Image
            src={app.iconUrl}
            alt={`${app.name} icon`}
            fill
            className="object-cover"
            unoptimized
          />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-sm font-medium text-gray-900 group-hover:text-blue-600">
              {app.name}
            </p>
            {app.isOpenSource && (
              <GitBranch className="h-3 w-3 shrink-0 text-blue-600" aria-label="Open source" />
            )}
          </div>
          <p className="truncate text-xs text-gray-500">{app.developer}</p>
        </div>
        <div className="shrink-0 text-right">
          <div className="flex items-center gap-1 text-xs text-gray-500">
            <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" aria-hidden="true" />
            <span>{app.rating.average.toFixed(1)}</span>
          </div>
          <CategoryBadge category={app.category} size="xs" />
        </div>
      </Link>
    )
  }

  return (
    <Link
      href={`/app/${app.packageName}`}
      className="group flex flex-col rounded-xl border border-gray-100 bg-white transition-colors hover:border-blue-400 hover:bg-gray-50"
    >
      <div className="flex items-start gap-3 p-4">
        <div className="relative h-14 w-14 shrink-0 overflow-hidden rounded-2xl border border-gray-100">
          <Image
            src={app.iconUrl}
            alt={`${app.name} icon`}
            fill
            className="object-cover"
            unoptimized
          />
        </div>
        <div className="min-w-0 flex-1 pt-0.5">
          <div className="flex items-center gap-1.5">
            <h3 className="truncate text-sm font-semibold text-gray-900 group-hover:text-blue-600">
              {app.name}
            </h3>
            {app.isOpenSource && (
              <GitBranch
                className="h-3 w-3 shrink-0 text-blue-600"
                aria-label="Open source"
              />
            )}
          </div>
          <p className="truncate text-xs text-gray-500">{app.developer}</p>
          <CategoryBadge category={app.category} size="xs" className="mt-1" />
        </div>
      </div>

      <p className="line-clamp-2 flex-1 px-4 pb-3 text-xs leading-relaxed text-gray-600">
        {app.shortDescription}
      </p>

      <div className="flex items-center justify-between border-t border-gray-50 px-4 py-2.5">
        <div className="flex items-center gap-1 text-xs text-gray-500">
          <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" aria-hidden="true" />
          <span>{app.rating.average.toFixed(1)}</span>
          <span className="text-gray-400">
            ({formatRatingCount(app.rating.count)})
          </span>
        </div>
        <span className="text-xs text-gray-500">{app.downloads}</span>
      </div>
    </Link>
  )
}
