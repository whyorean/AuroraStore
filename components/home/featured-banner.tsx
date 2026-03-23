import Link from 'next/link'
import Image from 'next/image'
import { ArrowRight } from 'lucide-react'
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
      className="group relative flex flex-col overflow-hidden rounded-3xl border border-gray-100 bg-white transition-all hover:border-blue-400 hover:shadow-xl hover:shadow-blue-500/5 active:scale-[0.98]"
    >
      <div className="relative aspect-[16/9] w-full bg-blue-50 overflow-hidden">
        <Image
          src={app.headerImageUrl}
          alt={`${app.name} cover`}
          fill
          className="object-cover transition-transform duration-500 group-hover:scale-105"
          unoptimized
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent md:hidden" />
      </div>

      <div className="flex items-center gap-4 p-5">
        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-2xl border border-gray-100 shadow-md">
          <Image
            src={app.iconUrl}
            alt={`${app.name} icon`}
            fill
            className="object-cover"
            unoptimized
          />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 mb-0.5">
            <h3 className="truncate text-lg font-bold text-gray-900 group-hover:text-blue-600">
              {app.name}
            </h3>
          </div>
          <p className="truncate text-sm text-gray-500">{app.developer}</p>
        </div>

        <div className="hidden sm:block">
           <span className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-50 text-blue-600 transition-colors group-hover:bg-blue-600 group-hover:text-white">
            <ArrowRight size={20} />
          </span>
        </div>
      </div>
    </Link>
  )
}
