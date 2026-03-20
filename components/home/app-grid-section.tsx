'use client'

import { useId } from 'react'
import type { App } from '@/lib/types'
import { AppCard } from '@/components/app-card'
import { Empty } from '@/components/ui/empty'
import { Search } from 'lucide-react'

interface AppGridSectionProps {
  apps: App[]
  title: string
  description?: string
  variant?: 'grid' | 'row'
}

export function AppGridSection({ apps, title, description, variant = 'grid' }: AppGridSectionProps) {
  const headingId = useId()

  return (
    <section aria-labelledby={headingId} className="mb-8">
      <div className="mb-4 flex items-baseline gap-2">
        <h2
          id={headingId}
          className="text-base font-semibold text-gray-900"
        >
          {title}
        </h2>
        {apps.length > 0 && (
          <span className="text-xs text-gray-500">{apps.length} apps</span>
        )}
      </div>
      {description && (
        <p className="mb-4 text-sm text-gray-500">{description}</p>
      )}

      {apps.length === 0 ? (
        <Empty
          title="No apps found"
          description="Try a different search term or category."
          icon={<Search className="h-6 w-6 text-gray-400" />}
        />
      ) : variant === 'grid' ? (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {apps.map((app) => (
            <AppCard key={app.packageName} app={app} variant="grid" />
          ))}
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          {apps.map((app) => (
            <AppCard key={app.packageName} app={app} variant="row" />
          ))}
        </div>
      )}
    </section>
  )
}
