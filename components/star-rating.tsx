import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StarRatingProps {
  value: number
  max?: number
  className?: string
}

export function StarRating({ value, max = 5, className }: StarRatingProps) {
  return (
    <span className={cn('flex items-center gap-0.5', className)} aria-label={`${value} out of ${max} stars`}>
      {Array.from({ length: max }).map((_, i) => {
        const filled = i < Math.floor(value)
        const partial = !filled && i < value
        return (
          <span key={i} className="relative h-4 w-4">
            <Star className="absolute inset-0 h-4 w-4 text-gray-200" aria-hidden="true" />
            {(filled || partial) && (
              <span
                className="absolute inset-0 overflow-hidden"
                style={{ width: partial ? `${(value % 1) * 100}%` : '100%' }}
              >
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" aria-hidden="true" />
              </span>
            )}
          </span>
        )
      })}
    </span>
  )
}
