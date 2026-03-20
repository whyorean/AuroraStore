'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Search } from 'lucide-react'
import { ALL_CATEGORIES } from '@/lib/mock-data'

interface SearchHeroProps {
  initialQuery?: string
}

export function SearchHero({ initialQuery = '' }: SearchHeroProps) {
  const [query, setQuery] = useState(initialQuery)
  const router = useRouter()

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const q = query.trim()
    router.push(q ? `/?q=${encodeURIComponent(q)}` : '/')
  }

  function handleCategory(cat: string) {
    router.push(`/?category=${encodeURIComponent(cat)}`)
  }

  return (
    <section className="border-b border-gray-100 bg-white px-4 py-8 sm:px-6">
      <div className="mx-auto max-w-2xl text-center">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl">
          Open-source Android apps,{' '}
          <span className="text-blue-600">without the tracking</span>
        </h1>
        <p className="mt-2 text-sm text-gray-500">
          Browse, search, and install apps privately. No account required.
        </p>

        <form onSubmit={handleSubmit} className="mt-5 flex gap-2" role="search">
          <label htmlFor="hero-search" className="sr-only">
            Search apps or package names
          </label>
          <div className="relative flex-1">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
              aria-hidden="true"
            />
            <input
              id="hero-search"
              type="search"
              placeholder="Search apps or package names..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="h-10 w-full rounded-lg border border-gray-200 bg-white pl-10 pr-4 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>
          <button
            type="submit"
            className="h-10 shrink-0 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white transition-opacity hover:opacity-90"
          >
            Search
          </button>
        </form>

        <div className="mt-4 flex flex-wrap justify-center gap-2" aria-label="Filter by category">
          {ALL_CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategory(cat)}
              className="rounded-full border border-gray-200 bg-white px-3 py-1 text-xs text-gray-500 transition-colors hover:border-blue-400 hover:text-blue-600"
            >
              {cat}
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}
