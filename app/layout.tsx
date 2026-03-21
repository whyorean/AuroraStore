import type { Metadata, Viewport } from 'next'
import { Suspense } from 'react'
import { Inter } from 'next/font/google'
import { BottomNav } from '@/components/bottom-nav'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'JMODS — Open-source Android App Store',
  description:
    'Browse, search, and sideload Android apps privately. JMODS is an open-source Google Play alternative with no tracking.',
  keywords: ['android', 'app store', 'jmods', 'open source', 'sideload', 'apk'],
}

export const viewport: Viewport = {
  themeColor: '#2563eb',
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} antialiased bg-gray-50 text-gray-900 pb-20 md:pb-0`}>
        <Suspense fallback={<div className="h-16 bg-white border-b border-gray-100" />}>
          {children}
        </Suspense>
        <BottomNav />
      </body>
    </html>
  )
}
