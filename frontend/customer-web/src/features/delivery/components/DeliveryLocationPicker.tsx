import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import vietmapgl from '@vietmap/vietmap-gl-js/dist/vietmap-gl'
import type { Map } from '@vietmap/vietmap-gl-js/dist/vietmap-gl'
import '@vietmap/vietmap-gl-js/dist/vietmap-gl.css'
import { CrosshairIcon } from '../../../components/icons/CrosshairIcon'
import { deliveryErrorMessage, getLocationPlace, reverseGeocode, searchLocations } from '../api/deliveryApi'
import type { LocationSearchCandidate, ReverseGeocodeCandidate } from '../types/delivery'

const defaultCenter: [number, number] = [105.8542, 21.0285]
// The configured VietMap Street source currently publishes vector tiles through zoom 15.
// Keeping the picker at that maximum avoids a blank canvas caused by requesting z16 tiles.
const addressPickerZoom = 15
const tilemapKey = import.meta.env.VITE_VIETMAP_TILEMAP_KEY
const vietMapStyle = tilemapKey ? `https://maps.vietmap.vn/maps/styles/tm/style.json?apikey=${encodeURIComponent(tilemapKey)}` : null
const samePosition = (left: [number, number], right: [number, number]) => Math.abs(left[0] - right[0]) < 0.000001 && Math.abs(left[1] - right[1]) < 0.000001

interface Props { initialLocation?: ReverseGeocodeCandidate | null; onConfirm: (location: ReverseGeocodeCandidate) => void; onClose: () => void }
type ProgrammaticMoveSource = 'PLACE' | 'GPS'

const locationError = (error: GeolocationPositionError) => error.code === error.PERMISSION_DENIED ? 'Bạn chưa cho phép truy cập vị trí.' : error.code === error.POSITION_UNAVAILABLE ? 'Không thể xác định vị trí hiện tại.' : error.code === error.TIMEOUT ? 'Xác định vị trí mất quá nhiều thời gian.' : 'Không thể xác định vị trí hiện tại.'

export function DeliveryLocationPicker({ initialLocation, onConfirm, onClose }: Props) {
  const mapNode = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<Map | null>(null)
  const reverseTimer = useRef<number | null>(null)
  const reverseAbort = useRef<AbortController | null>(null)
  const searchAbort = useRef<AbortController | null>(null)
  const placeAbort = useRef<AbortController | null>(null)
  const reverseGeneration = useRef(0)
  const placeGeneration = useRef(0)
  const programmaticMove = useRef<ProgrammaticMoveSource | null>(null)
  const userDragging = useRef(false)
  const [candidate, setCandidate] = useState<ReverseGeocodeCandidate | null>(initialLocation ?? null)
  const [candidateStale, setCandidateStale] = useState(false)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<LocationSearchCandidate[]>([])
  const [searching, setSearching] = useState(false)
  const [reverseLoading, setReverseLoading] = useState(false)
  const [placeLoading, setPlaceLoading] = useState(false)
  const [gpsBusy, setGpsBusy] = useState(false)
  const [mapError, setMapError] = useState<string | null>(null)
  const [reverseError, setReverseError] = useState<string | null>(null)
  const [placeError, setPlaceError] = useState<string | null>(null)

  const cancelReverse = useCallback(() => {
    if (reverseTimer.current) {
      window.clearTimeout(reverseTimer.current)
      reverseTimer.current = null
    }
    reverseAbort.current?.abort()
    reverseAbort.current = null
    reverseGeneration.current += 1
    setReverseLoading(false)
  }, [])

  const scheduleReverse = useCallback((longitude: number, latitude: number) => {
    if (reverseTimer.current) window.clearTimeout(reverseTimer.current)
    reverseAbort.current?.abort()
    const generation = ++reverseGeneration.current
    setReverseLoading(true)
    setReverseError(null)
    reverseTimer.current = window.setTimeout(() => {
      reverseTimer.current = null
      const controller = new AbortController()
      reverseAbort.current = controller
      void reverseGeocode({ latitude, longitude }, controller.signal)
        .then((next) => {
          if (!controller.signal.aborted && generation === reverseGeneration.current) {
            setCandidate(next)
            setCandidateStale(false)
          }
        })
        .catch((reason) => {
          if (!controller.signal.aborted && generation === reverseGeneration.current) setReverseError(deliveryErrorMessage(reason))
        })
        .finally(() => {
          if (!controller.signal.aborted && generation === reverseGeneration.current) setReverseLoading(false)
        })
    }, 450)
  }, [])

  const flyTo = useCallback((source: ProgrammaticMoveSource, longitude: number, latitude: number) => {
    const map = mapRef.current
    if (!map) {
      if (source === 'GPS') scheduleReverse(longitude, latitude)
      return
    }
    const target: [number, number] = [longitude, latitude]
    const current: [number, number] = [map.getCenter().lng, map.getCenter().lat]
    if (samePosition(current, target)) {
      if (source === 'GPS') scheduleReverse(longitude, latitude)
      return
    }
    programmaticMove.current = source
    map.flyTo({ center: target, zoom: addressPickerZoom, essential: true })
  }, [scheduleReverse])

  useLayoutEffect(() => {
    const node = mapNode.current
    if (!node || !vietMapStyle) {
      if (!vietMapStyle) setMapError('Bản đồ chưa được cấu hình. Vui lòng thử lại sau.')
      return undefined
    }
    const center: [number, number] = initialLocation ? [initialLocation.longitude, initialLocation.latitude] : defaultCenter
    const map = new vietmapgl.Map({
      container: node,
      style: vietMapStyle,
      center,
      zoom: addressPickerZoom,
      maxZoom: addressPickerZoom,
    })
    mapRef.current = map
    let frame: number | null = null
    let styleReady = false
    const resize = () => {
      frame = null
      const bounds = node.getBoundingClientRect()
      if (bounds.width > 0 && bounds.height > 0) map.resize()
    }
    const requestResize = () => {
      if (frame === null) frame = requestAnimationFrame(resize)
    }
    const onDragStart = () => {
      userDragging.current = true
      programmaticMove.current = null
      cancelReverse()
      setCandidateStale(true)
      setReverseError(null)
    }
    const onMoveEnd = () => {
      const source = programmaticMove.current
      if (source) {
        programmaticMove.current = null
        if (source === 'GPS') {
          const current = map.getCenter()
          scheduleReverse(current.lng, current.lat)
        }
        return
      }
      if (!userDragging.current) return
      userDragging.current = false
      const current = map.getCenter()
      scheduleReverse(current.lng, current.lat)
    }
    const onLoad = () => {
      styleReady = true
      setMapError(null)
      requestResize()
    }
    const onError = (event: { error?: { message?: string } }) => {
      const message = event.error?.message ?? ''
      if (!styleReady && /style|stylesheet|401|403/i.test(message)) {
        setMapError('Không thể tải bản đồ lúc này. Vui lòng kiểm tra kết nối rồi thử lại.')
      }
    }
    const observer = new ResizeObserver(requestResize)
    observer.observe(node)
    map.on('dragstart', onDragStart)
    map.on('moveend', onMoveEnd)
    map.once('load', onLoad)
    map.on('error', onError)
    requestResize()
    return () => {
      if (frame !== null) cancelAnimationFrame(frame)
      observer.disconnect()
      cancelReverse()
      map.remove()
      mapRef.current = null
      programmaticMove.current = null
      userDragging.current = false
    }
  // The dialog owns exactly one map instance for its mounted lifetime.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (query.trim().length < 3) { setResults([]); setSearching(false); return undefined }
    const timer = window.setTimeout(() => {
      searchAbort.current?.abort()
      const controller = new AbortController()
      searchAbort.current = controller
      setSearching(true)
      const center = mapRef.current?.getCenter()
      void searchLocations(query.trim(), center ? { latitude: center.lat, longitude: center.lng } : undefined, controller.signal)
        .then((items) => { if (!controller.signal.aborted) setResults(items) })
        .catch(() => { if (!controller.signal.aborted) setResults([]) })
        .finally(() => { if (!controller.signal.aborted) setSearching(false) })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [query])

  useEffect(() => () => { searchAbort.current?.abort(); placeAbort.current?.abort() }, [])
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  const selectCandidate = (result: LocationSearchCandidate) => {
    placeAbort.current?.abort()
    cancelReverse()
    const controller = new AbortController()
    const generation = ++placeGeneration.current
    placeAbort.current = controller
    setResults([])
    setQuery('')
    setPlaceLoading(true)
    setPlaceError(null)
    setReverseError(null)
    setCandidateStale(true)
    void getLocationPlace(result.providerRefId, controller.signal)
      .then((next) => {
        if (!controller.signal.aborted && generation === placeGeneration.current) {
          setCandidate(next)
          setCandidateStale(false)
          setReverseError(null)
          flyTo('PLACE', next.longitude, next.latitude)
        }
      })
      .catch((reason) => {
        if (!controller.signal.aborted && generation === placeGeneration.current) {
          setPlaceError(deliveryErrorMessage(reason))
          setCandidateStale(false)
        }
      })
      .finally(() => {
        if (!controller.signal.aborted && generation === placeGeneration.current) setPlaceLoading(false)
      })
  }

  const useCurrentLocation = () => {
    if (!navigator.geolocation) { setPlaceError('Trình duyệt không hỗ trợ lấy vị trí.'); return }
    setGpsBusy(true)
    setPlaceError(null)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setGpsBusy(false)
        cancelReverse()
        setReverseError(null)
        setCandidateStale(true)
        flyTo('GPS', position.coords.longitude, position.coords.latitude)
      },
      (reason) => { setGpsBusy(false); setPlaceError(locationError(reason)) },
      { enableHighAccuracy: true, timeout: 12_000, maximumAge: 30_000 },
    )
  }

  const footerText = placeLoading ? 'Đang tải vị trí…' : reverseLoading ? 'Đang xác định địa chỉ…' : candidate && !candidateStale ? candidate.formattedAddress : 'Di chuyển bản đồ để chọn vị trí'
  const footerError = placeError ?? reverseError
  const confirmDisabled = !candidate || candidateStale || reverseLoading || placeLoading || gpsBusy || Boolean(mapError)

  return <div className="location-picker-backdrop" role="presentation"><section className="location-picker" role="dialog" aria-modal="true" aria-labelledby="location-picker-title">
    <header><div><p className="eyebrow">Vị trí giao hàng</p><h2 id="location-picker-title">Chọn vị trí trên bản đồ</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Đóng bản đồ">×</button></header>
    <div className="location-picker-map-wrap"><div ref={mapNode} className="location-picker-map" aria-label="Bản đồ chọn vị trí" />{mapError && <div className="location-picker-map-error" role="alert">{mapError}</div>}<div className="location-picker-pin" aria-hidden="true">●</div><div className="location-picker-search"><label className="sr-only" htmlFor="location-search">Tìm địa điểm</label><input id="location-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm đường, tòa nhà, địa điểm" autoComplete="off" />{(results.length > 0 || searching) && <div className="location-picker-results" role="listbox">{searching && <p>Đang tìm…</p>}{results.map((item) => <button key={item.providerRefId} type="button" role="option" onClick={() => selectCandidate(item)}><strong>{item.formattedAddress}</strong><small>{item.district ?? item.city ?? ''}</small></button>)}</div>}</div><button type="button" className="location-picker-current" aria-label="Dùng vị trí hiện tại" onClick={useCurrentLocation} disabled={gpsBusy}>{gpsBusy ? '…' : <CrosshairIcon />}</button></div>
    <footer><div><strong>{footerText}</strong>{candidate && !candidateStale && <small>{[candidate.ward, candidate.district, candidate.city].filter(Boolean).join(', ')}</small>}{footerError && <p role="alert">{footerError}</p>}</div><button type="button" className="button primary" disabled={confirmDisabled} onClick={() => candidate && onConfirm(candidate)}>Xác nhận vị trí</button></footer>
  </section></div>
}
