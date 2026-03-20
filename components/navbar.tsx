'use client'

import Link from 'next/link'
import { useState, useEffect, Suspense } from 'react'
import { Search, Shield } from 'lucide-react'
import { useRouter, useSearchParams } from 'next/navigation'

function NavbarContent() {
  const searchParams = useSearchParams()
  const [query, setQuery] = useState(searchParams.get('q') ?? '')
  const router = useRouter()

  useEffect(() => {
    setQuery(searchParams.get('q') ?? '')
  }, [searchParams])

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (query.trim()) {
      router.push(`/?q=${encodeURIComponent(query.trim())}`)
    }
  }

  return (
    <header className="sticky top-0 z-50 border-b border-gray-100 bg-white/90 backdrop-blur-md">
      <div className="mx-auto max-w-7xl px-4 sm:px-6">
        <div className="flex h-16 items-center gap-4">
          <Link href="/" className="flex shrink-0 items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600">
              <Shield className="h-5 w-5 text-white" aria-hidden="true" />
            </div>
            <span className="text-base font-bold tracking-tight text-gray-900">
              Aurora<span className="text-blue-600">Next</span>
            </span>
          </Link>

          <form
            onSubmit={handleSearch}
            className="relative flex-1 max-w-md ml-auto"
            role="search"
          >
            <label htmlFor="nav-search" className="sr-only">
              Search apps
            </label>
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
              aria-hidden="true"
            />
            <input
              id="nav-search"
              type="search"
              placeholder="Search apps..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="h-10 w-full rounded-xl border border-gray-200 bg-gray-50 pl-10 pr-3 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </form>

          <nav className="hidden items-center gap-1 md:flex" aria-label="Main navigation">
            <Link
              href="/updates"
              className="rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
            >
              Updates
            </Link>
            <Link
              href="/downloads"
              className="rounded-lg px-3 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
            >
              Downloads
            </Link>
          </nav>
        </div>
      </div>
    </header>
  )
}

export function Navbar() {
  return (
    <Suspense fallback={<div className="h-16 bg-white border-b border-gray-100" />}>
      <NavbarContent />
    </Suspense>
  )
}
