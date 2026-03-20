import Link from 'next/link'
import Image from 'next/image'
import { ArrowRight, GitBranch } from 'lucide-react'
import type { App } from '@/lib/types'
import { CategoryBadge } from '@/components/category-badge'
import { StarRating } from '@/components/star-rating'

interface FeaturedBannerProps {
  app: App
}

export function FeaturedBanner({ app }: FeaturedBannerProps) {
  return (
    <Link
      href={`/app/${app.packageName}`}
      className="group relative flex min-h-[200px] overflow-hidden rounded-xl border border-gray-100 bg-white transition-colors hover:border-blue-400"
    >
      <div className="relative flex w-full flex-col justify-end p-5 sm:flex-row sm:items-center sm:justify-start sm:gap-5">
        <div className="mb-3 shrink-0 sm:mb-0">
          <div className="relative h-16 w-16 overflow-hidden rounded-2xl border border-gray-100 shadow-sm">
            <Image
              src={app.iconUrl}
              alt={`${app.name} icon`}
              fill
              className="object-cover"
              unoptimized
            />
          </div>
        </div>

        <div className="flex-1">
          <div className="mb-1 flex flex-wrap items-center gap-2">
            <span className="text-[10px] font-semibold uppercase tracking-widest text-blue-600">
              Featured
            </span>
            <CategoryBadge category={app.category} size="xs" />
          </div>
          <h2 className="text-lg font-bold text-gray-900 group-hover:text-blue-600">
            {app.name}
          </h2>
          <p className="mt-0.5 text-xs text-gray-500">{app.developer}</p>
          <p className="mt-1.5 line-clamp-2 text-sm leading-relaxed text-gray-600">
            {app.shortDescription}
          </p>
          <div className="mt-3 flex items-center gap-3">
            <StarRating value={app.rating.average} />
            <span className="text-xs text-gray-500">{app.rating.average.toFixed(1)}</span>
            <span className="ml-auto flex items-center gap-1 text-xs font-medium text-blue-600">
              View app
              <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
            </span>
          </div>
        </div>
      </div>
    </Link>
  )
}
