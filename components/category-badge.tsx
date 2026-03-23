import { cn } from '@/lib/utils'
import type { AppCategory } from '@/lib/types'

const CATEGORY_STYLES: Record<AppCategory, string> = {
  Productivity: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  Social: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  Games: 'bg-orange-500/10 text-orange-400 border-orange-500/20',
  Tools: 'bg-slate-500/10 text-slate-400 border-slate-500/20',
  Media: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
  Security: 'bg-green-500/10 text-green-400 border-green-500/20',
  Finance: 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20',
  Health: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
}

interface CategoryBadgeProps {
  category: AppCategory
  size?: 'xs' | 'sm'
  className?: string
}

export function CategoryBadge({ category, size = 'sm', className }: CategoryBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded border font-medium',
        size === 'xs' ? 'px-1.5 py-0 text-[10px]' : 'px-2 py-0.5 text-xs',
        CATEGORY_STYLES[category] ?? 'bg-muted text-muted-foreground border-border',
        className,
      )}
    >
      {category}
    </span>
  )
}
