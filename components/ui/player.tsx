'use client'

import * as React from 'react'
import { useState, useEffect } from 'react'
import { Play, Pause, SkipForward, SkipBack, X, Maximize2, Volume2 } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PlayerProps {
  title: string
  subtitle?: string
  isOpen: boolean
  onClose: () => void
}

export function Player({ title, subtitle, isOpen, onClose }: PlayerProps) {
  const [isPlaying, setIsPlaying] = useState(false)
  const [progress, setProgress] = useState(35)

  if (!isOpen) return null

  return (
    <div className="fixed inset-x-0 bottom-0 z-[60] p-4 md:inset-x-auto md:right-8 md:bottom-8 md:w-96 animate-in slide-in-from-bottom-8 fade-in duration-500">
      <div className="group overflow-hidden rounded-[32px] bg-gray-900/90 text-white shadow-[0_32px_64px_-12px_rgba(0,0,0,0.5)] ring-1 ring-white/10 backdrop-blur-2xl">
        {/* Progress bar with glow */}
        <div className="h-1 bg-white/10 relative">
          <div
            className="h-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.8)] transition-all duration-700 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>

        <div className="flex flex-col p-6">
          <div className="mb-6 flex items-center justify-between">
            <div className="min-w-0 flex-1">
              <h3 className="truncate text-base font-black tracking-tight leading-none text-white/95">{title}</h3>
              {subtitle && (
                <div className="mt-2 flex items-center gap-2">
                   <span className="flex h-1.5 w-1.5 animate-pulse rounded-full bg-blue-500" />
                   <p className="truncate text-[10px] font-black text-blue-400 uppercase tracking-widest">{subtitle}</p>
                </div>
              )}
            </div>
            <div className="flex items-center gap-1 ml-4 opacity-0 group-hover:opacity-100 transition-opacity">
              <button className="rounded-xl p-2 hover:bg-white/10 text-gray-400 hover:text-white transition-colors">
                <Maximize2 className="h-4 w-4" />
              </button>
              <button onClick={onClose} className="rounded-xl p-2 hover:bg-white/10 text-gray-400 hover:text-white transition-colors">
                <X className="h-4 w-4" />
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-4">
               <button className="text-gray-400 hover:text-white transition-colors">
                  <SkipBack className="h-5 w-5 fill-current" />
               </button>
               <button
                  onClick={() => setIsPlaying(!isPlaying)}
                  className="flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-blue-400 to-blue-600 text-white shadow-lg shadow-blue-500/20 hover:scale-105 active:scale-95 transition-all"
               >
                  {isPlaying ? (
                    <Pause className="h-7 w-7 fill-current" />
                  ) : (
                    <Play className="h-7 w-7 fill-current ml-1" />
                  )}
               </button>
               <button className="text-gray-400 hover:text-white transition-colors">
                  <SkipForward className="h-5 w-5 fill-current" />
               </button>
            </div>

            <div className="flex items-center gap-2 rounded-2xl bg-white/5 p-2 px-3">
               <Volume2 className="h-4 w-4 text-gray-400" />
               <div className="w-16 h-1 bg-white/10 rounded-full overflow-hidden">
                  <div className="w-3/4 h-full bg-white/40" />
               </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
