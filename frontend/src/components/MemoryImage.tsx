import { useEffect, useState } from 'react'
import { memoryApi } from '../services/memoryService'

export interface MemoryImageProps {
  memoryId: number
  alt?: string
  className?: string
}

type ImageState =
  | { status: 'loading'; objectUrl: null; error: null }
  | { status: 'loaded'; objectUrl: string; error: null }
  | { status: 'error'; objectUrl: null; error: string }

/**
 * Renders an authenticated photo for a given memory.
 * - Fetches photo binary blob using credentials.
 * - Allocates and revokes object URLs safely across React Strict Mode lifecycles and prop changes.
 * - Gracefully falls back on loading, 404/missing, and error states.
 */
export function MemoryImage({ memoryId, alt = 'Memory photo', className }: MemoryImageProps) {
  const [state, setState] = useState<ImageState>({
    status: 'loading',
    objectUrl: null,
    error: null
  })

  useEffect(() => {
    let isCancelled = false
    let createdUrl: string | null = null

    setState({
      status: 'loading',
      objectUrl: null,
      error: null
    })

    memoryApi
      .getPhotoBlob(memoryId)
      .then((blob) => {
        if (isCancelled) return

        createdUrl = URL.createObjectURL(blob)
        setState({
          status: 'loaded',
          objectUrl: createdUrl,
          error: null
        })
      })
      .catch((err) => {
        if (isCancelled) return

        const message = err instanceof Error ? err.message : 'Photo unavailable'
        setState({
          status: 'error',
          objectUrl: null,
          error: message
        })
      })

    return () => {
      isCancelled = true
      if (createdUrl) {
        URL.revokeObjectURL(createdUrl)
        createdUrl = null
      }
    }
  }, [memoryId])

  if (state.status === 'loading') {
    return (
      <div className={`memory-image-container memory-image-loading ${className || ''}`.trim()} aria-busy="true">
        <div className="memory-image-placeholder" aria-label="Loading memory photo">
          <span className="memory-image-spinner" aria-hidden="true" />
          <span className="sr-only">Loading photo...</span>
        </div>
      </div>
    )
  }

  if (state.status === 'error' || !state.objectUrl) {
    return (
      <div className={`memory-image-container memory-image-fallback ${className || ''}`.trim()} role="img" aria-label={alt}>
        <div className="memory-image-placeholder">
          <span className="memory-image-fallback-icon" aria-hidden="true">🖼️</span>
          <span className="memory-image-fallback-text">{state.error || 'Photo unavailable'}</span>
        </div>
      </div>
    )
  }

  return (
    <div className={`memory-image-container ${className || ''}`.trim()}>
      <img
        src={state.objectUrl}
        alt={alt}
        className="memory-image"
        loading="lazy"
      />
    </div>
  )
}

