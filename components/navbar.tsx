'use client'

import Link from 'next/link'
import { useState, useEffect, Suspense } from 'react'
import { Search, Menu, X, Download, Shield } from 'lucide-react'
import { useRouter, useSearchParams } from 'next/navigation'

function NavbarContent() {
  const [menuOpen, setMenuOpen] = useState(false)
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
        <div className="flex h-14 items-center gap-4">
          <Link href="/" className="flex shrink-0 items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-blue-600">
              <Shield className="h-4 w-4 text-white" aria-hidden="true" />
            </div>
            <span className="text-sm font-semibold tracking-tight text-gray-900">
              Aurora<span className="text-blue-600">Next</span>
            </span>
          </Link>

          <form
            onSubmit={handleSearch}
            className="relative hidden flex-1 max-w-md md:flex"
            role="search"
          >
            <label htmlFor="nav-search" className="sr-only">
              Search apps
            </label>
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-gray-400"
              aria-hidden="true"
            />
            <input
              id="nav-search"
              type="search"
              placeholder="Search apps, packages..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="h-8 w-full rounded-md border border-gray-200 bg-gray-50 pl-9 pr-3 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </form>

          <nav className="ml-auto hidden items-center gap-1 md:flex" aria-label="Main navigation">
            <Link
              href="/"
              className="rounded px-3 py-1.5 text-sm text-gray-600 transition-colors hover:text-gray-900"
            >
              Browse
            </Link>
            <Link
              href="/updates"
              className="rounded px-3 py-1.5 text-sm text-gray-600 transition-colors hover:text-gray-900"
            >
              Updates
            </Link>
            <Link
              href="/downloads"
              className="flex items-center gap-1.5 rounded px-3 py-1.5 text-sm text-gray-600 transition-colors hover:text-gray-900"
            >
              <Download className="h-3.5 w-3.5" aria-hidden="true" />
              Downloads
            </Link>
          </nav>

          <button
            className="ml-auto rounded p-1.5 text-gray-400 hover:text-gray-900 md:hidden"
            onClick={() => setMenuOpen((o) => !o)}
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={menuOpen}
          >
            {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>

        {menuOpen && (
          <div className="border-t border-gray-100 pb-4 pt-3 md:hidden">
            <form onSubmit={handleSearch} className="relative mb-3" role="search">
              <label htmlFor="mobile-search" className="sr-only">
                Search apps
              </label>
              <Search
                className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-gray-400"
                aria-hidden="true"
              />
              <input
                id="mobile-search"
                type="search"
                placeholder="Search apps, packages..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="h-9 w-full rounded-md border border-gray-200 bg-gray-50 pl-9 pr-3 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none"
              />
            </form>
            <nav className="flex flex-col gap-1" aria-label="Mobile navigation">
              {['Browse', 'Updates', 'Downloads'].map((label) => (
                <Link
                  key={label}
                  href={label === 'Browse' ? '/' : `/${label.toLowerCase()}`}
                  onClick={() => setMenuOpen(false)}
                  className="rounded px-2 py-2 text-sm text-gray-600 hover:bg-gray-50 hover:text-gray-900"
                >
                  {label}
                </Link>
              ))}
            </nav>
          </div>
        )}
      </div>
    </header>
  )
}

export function Navbar() {
  return (
    <Suspense fallback={<div className="h-14 bg-white border-b border-gray-100" />}>
      <NavbarContent />
    </Suspense>
  )
}
