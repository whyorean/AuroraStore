'use client'

import Link from 'next/link'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'
import { Home, Search, Download, Settings, RefreshCcw } from 'lucide-react'
import { useState, useEffect, Suspense } from 'react'
import { cn } from '@/lib/utils'

function BottomNavContent() {
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const router = useRouter()
  const [query, setQuery] = useState(searchParams.get('q') ?? '')
  const [searchOpen, setSearchOpen] = useState(false)

  // Keep search input in sync with URL
  useEffect(() => {
    setQuery(searchParams.get('q') ?? '')
  }, [searchParams])

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = query.trim()
    setSearchOpen(false)
    if (trimmed) {
      router.push(`/?q=${encodeURIComponent(trimmed)}`)
    } else {
      router.push('/')
    }
  }

  const navItems = [
    { label: 'Browse', href: '/', icon: Home },
    { label: 'Updates', href: '/updates', icon: RefreshCcw },
    { label: 'Downloads', href: '/downloads', icon: Download },
  ]

  return (
    <>
      {/* Inline search drawer */}
      {searchOpen && (
        <div className="fixed inset-x-0 bottom-16 z-40 border-t border-gray-100 bg-white px-4 pb-safe pt-3 shadow-lg md:hidden">
          <form onSubmit={handleSearchSubmit} role="search" className="flex gap-2">
            <label htmlFor="bottomnav-search" className="sr-only">
              Search apps
            </label>
            <input
              id="bottomnav-search"
              type="search"
              autoFocus
              placeholder="Search apps or package names..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="h-11 flex-1 rounded-lg border border-gray-200 bg-gray-50 px-4 text-sm text-gray-900 placeholder:text-gray-500 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <button
              type="submit"
              className="h-11 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white min-w-[60px]"
            >
              Go
            </button>
          </form>
        </div>
      )}

      {/* Bottom tab bar */}
      <nav
        aria-label="Bottom navigation"
        className="fixed inset-x-0 bottom-0 z-50 flex h-16 items-stretch border-t border-gray-100 bg-white/95 backdrop-blur-md pb-safe md:hidden"
      >
        {navItems.map(({ label, href, icon: Icon }) => {
          const isActive = pathname === href && !searchOpen
          return (
            <Link
              key={label}
              href={href}
              onClick={() => setSearchOpen(false)}
              aria-current={isActive ? 'page' : undefined}
              className={cn(
                "flex flex-1 flex-col items-center justify-center gap-1 text-[10px] font-medium transition-colors",
                isActive ? 'text-blue-600' : 'text-gray-500'
              )}
            >
              <Icon
                className={cn("h-5 w-5", isActive ? 'text-blue-600' : 'text-gray-400')}
                aria-hidden="true"
              />
              <span>{label}</span>
            </Link>
          )
        })}

        {/* Search tab */}
        <button
          onClick={() => setSearchOpen((o) => !o)}
          aria-pressed={searchOpen}
          aria-label="Search"
          className={cn(
            "flex flex-1 flex-col items-center justify-center gap-1 text-[10px] font-medium transition-colors",
            searchOpen ? 'text-blue-600' : 'text-gray-500'
          )}
        >
          <Search
            className={cn("h-5 w-5", searchOpen ? 'text-blue-600' : 'text-gray-400')}
            aria-hidden="true"
          />
          <span>Search</span>
        </button>
      </nav>
    </>
  )
}

export function BottomNav() {
  return (
    <Suspense fallback={null}>
      <BottomNavContent />
    </Suspense>
  )
}
