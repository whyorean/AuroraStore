import type { Metadata, Viewport } from 'next'
import { Suspense } from 'react'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Aurora Next — Open-source Android App Store',
  description:
    'Browse, search, and sideload Android apps privately. Aurora Next is an open-source Google Play alternative with no tracking.',
  keywords: ['android', 'app store', 'aurora store', 'open source', 'sideload', 'apk'],
}

export const viewport: Viewport = {
  themeColor: '#2563eb',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} antialiased bg-gray-50 text-gray-900`}>
        <Suspense fallback={<div className="h-14 bg-white border-b border-gray-100" />}>
          {children}
        </Suspense>
      </body>
    </html>
  )
}
