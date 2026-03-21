'use client'

import * as React from 'react'
import { useEffect } from 'react'
import { cn } from '@/lib/utils'
import { X } from 'lucide-react'

interface DrawerProps {
  isOpen: boolean
  onClose: () => void
  title?: string
  children: React.ReactNode
}

export function Drawer({ isOpen, onClose, title, children }: DrawerProps) {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isOpen])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-[100] flex items-end justify-center sm:items-center sm:p-4">
      {/* Overlay */}
      <div
        className="absolute inset-0 bg-gray-950/40 backdrop-blur-md animate-in fade-in duration-300"
        onClick={onClose}
      />

      {/* Content */}
      <div className="relative w-full max-w-lg overflow-hidden rounded-t-[40px] bg-white shadow-2xl animate-in slide-in-from-bottom-full duration-500 ease-out sm:rounded-[40px]">
        {/* Handle for mobile */}
        <div className="flex justify-center pt-4 sm:hidden">
           <div className="h-1.5 w-12 rounded-full bg-gray-100" />
        </div>

        <div className="px-8 pb-10 pt-6">
          <div className="mb-8 flex items-center justify-between">
            <div>
               <h2 className="text-2xl font-black tracking-tight text-gray-900">{title}</h2>
               <div className="mt-1 h-1 w-12 rounded-full bg-blue-600" />
            </div>
            <button
              onClick={onClose}
              className="rounded-2xl bg-gray-50 p-3 text-gray-500 hover:bg-gray-100 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="max-h-[70vh] overflow-y-auto no-scrollbar">
            {children}
          </div>
        </div>
      </div>
    </div>
  )
}
