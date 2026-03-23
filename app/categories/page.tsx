import { Navbar } from '@/components/navbar'
import { ALL_CATEGORIES } from '@/lib/mock-data'
import Link from 'next/link'
import { LayoutGrid, Shield, MessageCircle, Gamepad2, Wrench, PlayCircle, Lock, Wallet, HeartPulse } from 'lucide-react'

const CATEGORY_ICONS: Record<string, any> = {
  'Productivity': LayoutGrid,
  'Social': MessageCircle,
  'Games': Gamepad2,
  'Tools': Wrench,
  'Media': PlayCircle,
  'Security': Lock,
  'Finance': Wallet,
  'Health': HeartPulse,
}

export default function CategoriesPage() {
  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
        <header className="mb-10">
          <h1 className="text-3xl font-black tracking-tight text-gray-900">Categories</h1>
          <p className="mt-2 text-gray-500">Explore apps by their function and use case.</p>
        </header>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {ALL_CATEGORIES.map((cat) => {
            const Icon = CATEGORY_ICONS[cat] || LayoutGrid
            return (
              <Link
                key={cat}
                href={`/?category=${encodeURIComponent(cat)}`}
                className="group flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 transition-all hover:border-blue-200 hover:shadow-sm"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-600 transition-colors group-hover:bg-blue-600 group-hover:text-white">
                  <Icon className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="font-bold text-gray-900">{cat}</h2>
                  <p className="text-xs text-gray-500">Browse apps</p>
                </div>
              </Link>
            )
          })}
        </div>
      </main>
    </>
  )
}
