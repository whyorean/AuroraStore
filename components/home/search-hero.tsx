'use client'

import { useRouter } from 'next/navigation'
import { ALL_CATEGORIES } from '@/lib/mock-data'

interface SearchHeroProps {
  initialQuery?: string
  activeCategory?: string
}

export function SearchHero({ initialQuery, activeCategory }: SearchHeroProps) {
  const router = useRouter()
  const isFiltering = Boolean(initialQuery || activeCategory)

  function handleCategory(cat: string) {
    router.push(`/?category=${encodeURIComponent(cat)}`)
  }

  return (
    <section className="border-b border-gray-100 bg-white px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-2xl text-center">
        {isFiltering ? (
          <p className="text-sm text-gray-500 mb-4">
            Showing results for{' '}
            <span className="font-medium text-gray-900">
              {initialQuery ?? activeCategory}
            </span>
            {' · '}
            <button
              onClick={() => router.push('/')}
              className="text-blue-600 hover:underline"
            >
              Clear
            </button>
          </p>
        ) : (
          <>
            <h1 className="text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl">
              Open-source Android apps,{' '}
              <span className="text-blue-600">without the tracking</span>
            </h1>
            <p className="mt-2 text-sm text-gray-500 mb-6">
              Browse, search, and install apps privately. No account required.
            </p>
          </>
        )}

        <div className="flex flex-wrap justify-center gap-2" aria-label="Filter by category">
          {ALL_CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategory(cat)}
              className={[
                'rounded-full border px-3 py-1 text-xs transition-colors',
                activeCategory === cat
                  ? 'border-blue-600 bg-blue-50 text-blue-600'
                  : 'border-gray-200 bg-white text-gray-500 hover:border-blue-400 hover:text-blue-600',
              ].join(' ')}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}
