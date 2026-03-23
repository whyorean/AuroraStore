export function formatBytes(bytes: number): string {
  if (bytes < 1000000) return `${(bytes / 1000).toFixed(0)} KB`
  return `${(bytes / 1000000).toFixed(1)} MB`
}

export function formatRatingCount(count: number): string {
  if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`
  if (count >= 1000) return `${(count / 1000).toFixed(0)}K`
  return String(count)
}

export function formatDate(dateStr: string): string {
  return new Date(`${dateStr}T00:00:00`).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
