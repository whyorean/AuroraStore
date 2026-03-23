'use client'

import type { App } from '@/lib/types'
import { AppCard } from './app-card'

interface AppCarouselProps {
  apps: App[]
}

export function AppCarousel({ apps }: AppCarouselProps) {
  return (
    <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide -mx-4 px-4 md:mx-0 md:px-0">
      {apps.map((app) => (
        <div key={app.packageName} className="w-[280px] shrink-0">
          <AppCard app={app} variant="grid" />
        </div>
      ))}
    </div>
  )
}
