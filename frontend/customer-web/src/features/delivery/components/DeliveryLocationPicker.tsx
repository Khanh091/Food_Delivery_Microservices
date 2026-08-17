import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import * as maplibregl from 'maplibre-gl'
import type { Map } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { CrosshairIcon } from '../../../components/icons/CrosshairIcon'
import { deliveryErrorMessage, getLocationPlace, reverseGeocode, searchLocations } from '../api/deliveryApi'
import type { LocationSearchCandidate, ReverseGeocodeCandidate } from '../types/delivery'

const defaultCenter: [number, number] = [105.8542, 21.0285]
const tilemapKey = import.meta.env.VITE_VIETMAP_TILEMAP_KEY
const vietMapStyle = tilemapKey ? `https://maps.vietmap.vn/maps/styles/tm/style.json?apikey=${encodeURIComponent(tilemapKey)}` : null
const samePosition = (left: [number, number], right: [number, number]) => Math.abs(left[0] - right[0]) < 0.000001 && Math.abs(left[1] - right[1]) < 0.000001

interface Props { initialLocation?: ReverseGeocodeCandidate | null; onConfirm: (location: ReverseGeocodeCandidate) => void; onClose: () => void }
interface ProgrammaticMove { center: [number, number]; reverseAfterMove: boolean }

const locationError = (error: GeolocationPositionError) => error.code === error.PERMISSION_DENIED ? 'Bạn chưa cho phép truy cập vị trí.' : error.code === error.POSITION_UNAVAILABLE ? 'Không thể xác định vị trí hiện tại.' : error.code === error.TIMEOUT ? 'Xác định vị trí mất quá nhiều thời gian.' : 'Không thể xác định vị trí hiện tại.'

export function DeliveryLocationPicker({ initialLocation, onConfirm, onClose }: Props) {
  const mapNode = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<Map | null>(null)
  const reverseTimer = useRef<number | null>(null)
  const reverseAbort = useRef<AbortController | null>(null)
  const searchAbort = useRef<AbortController | null>(null)
  const placeAbort = useRef<AbortController | null>(null)
  const reverseGeneration = useRef(0)
  const mapReady = useRef(false)
  const programmaticMove = useRef<ProgrammaticMove | null>(null)
  const [candidate, setCandidate] = useState<ReverseGeocodeCandidate | null>(initialLocation ?? null)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<LocationSearchCandidate[]>([])
  const [searching, setSearching] = useState(false)
  const [resolving, setResolving] = useState(false)
  const [centerDirty, setCenterDirty] = useState(false)
  const [gpsBusy, setGpsBusy] = useState(false)
  const [mapError, setMapError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const resolveLocation = useCallback((longitude: number, latitude: number) => {
    reverseAbort.current?.abort()
    const controller = new AbortController()
    const generation = ++reverseGeneration.current
    reverseAbort.current = controller
    setResolving(true)
    setError(null)
    void reverseGeocode({ latitude, longitude }, controller.signal)
      .then((next) => {
        if (!controller.signal.aborted && generation === reverseGeneration.current) {
          setCandidate(next)
          setCenterDirty(false)
        }
      })
      .catch((reason) => {
        if (!controller.signal.aborted && generation === reverseGeneration.current) setError(deliveryErrorMessage(reason))
      })
      .finally(() => {
        if (!controller.signal.aborted && generation === reverseGeneration.current) setResolving(false)
      })
  }, [])

  const scheduleReverse = useCallback((longitude: number, latitude: number) => {
    if (reverseTimer.current) window.clearTimeout(reverseTimer.current)
    reverseTimer.current = window.setTimeout(() => resolveLocation(longitude, latitude), 450)
  }, [resolveLocation])

  const flyTo = useCallback((longitude: number, latitude: number, reverseAfterMove: boolean) => {
    const map = mapRef.current
    if (!map) return
    programmaticMove.current = { center: [longitude, latitude], reverseAfterMove }
    setCenterDirty(true)
    map.flyTo({ center: [longitude, latitude], zoom: 16, essential: true })
  }, [])

  useLayoutEffect(() => {
    const node = mapNode.current
    if (!node || !vietMapStyle) {
      if (!vietMapStyle) setMapError('Bản đồ chưa được cấu hình. Vui lòng thử lại sau.')
      return undefined
    }
    const center: [number, number] = initialLocation ? [initialLocation.longitude, initialLocation.latitude] : defaultCenter
    const map = new maplibregl.Map({ container: node, style: vietMapStyle, center, zoom: initialLocation ? 16 : 13 })
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
    const onMoveStart = () => setCenterDirty(true)
    const onMoveEnd = () => {
      if (!mapReady.current) return
      const current: [number, number] = [map.getCenter().lng, map.getCenter().lat]
      const pending = programmaticMove.current
      if (pending && samePosition(current, pending.center)) {
        programmaticMove.current = null
        if (pending.reverseAfterMove) scheduleReverse(current[0], current[1])
        else setCenterDirty(false)
        return
      }
      programmaticMove.current = null
      scheduleReverse(current[0], current[1])
    }
    const onStyleLoad = () => {
      styleReady = true
      setMapError(null)
      requestResize()
    }
    const onLoad = () => {
      mapReady.current = true
      requestResize()
      if (!initialLocation) scheduleReverse(center[0], center[1])
    }
    const onError = (event: maplibregl.ErrorEvent) => {
      // Tile, glyph and sprite failures are recoverable. Only a failed initial style should block confirmation.
      const message = event.error?.message ?? ''
      if (!styleReady && /style|stylesheet|401|403/i.test(message)) {
        setMapError('Không thể tải bản đồ lúc này. Vui lòng kiểm tra kết nối rồi thử lại.')
      }
    }
    const observer = new ResizeObserver(requestResize)
    observer.observe(node)
    map.on('movestart', onMoveStart)
    map.on('moveend', onMoveEnd)
    map.on('style.load', onStyleLoad)
    map.once('load', onLoad)
    map.on('error', onError)
    requestResize()
    return () => {
      if (frame !== null) cancelAnimationFrame(frame)
      if (reverseTimer.current) window.clearTimeout(reverseTimer.current)
      observer.disconnect()
      reverseAbort.current?.abort()
      map.remove()
      mapRef.current = null
      mapReady.current = false
      programmaticMove.current = null
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
      void searchLocations(query.trim(), controller.signal)
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
    const controller = new AbortController()
    placeAbort.current = controller
    setResults([])
    setQuery('')
    setResolving(true)
    setError(null)
    void getLocationPlace(result.providerRefId, controller.signal)
      .then((next) => {
        if (!controller.signal.aborted) {
          setCandidate(next)
          flyTo(next.longitude, next.latitude, false)
        }
      })
      .catch((reason) => { if (!controller.signal.aborted) setError(deliveryErrorMessage(reason)) })
      .finally(() => { if (!controller.signal.aborted) setResolving(false) })
  }

  const useCurrentLocation = () => {
    if (!navigator.geolocation) { setError('Trình duyệt không hỗ trợ lấy vị trí.'); return }
    setGpsBusy(true)
    setError(null)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setGpsBusy(false)
        flyTo(position.coords.longitude, position.coords.latitude, true)
      },
      (reason) => { setGpsBusy(false); setError(locationError(reason)) },
      { enableHighAccuracy: true, timeout: 12_000, maximumAge: 30_000 },
    )
  }

  return <div className="location-picker-backdrop" role="presentation"><section className="location-picker" role="dialog" aria-modal="true" aria-labelledby="location-picker-title">
    <header><div><p className="eyebrow">Vị trí giao hàng</p><h2 id="location-picker-title">Chọn vị trí trên bản đồ</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Đóng bản đồ">×</button></header>
    <div className="location-picker-map-wrap"><div ref={mapNode} className="location-picker-map" aria-label="Bản đồ chọn vị trí" />{mapError && <div className="location-picker-map-error" role="alert">{mapError}</div>}<div className="location-picker-pin" aria-hidden="true">●</div><div className="location-picker-search"><label className="sr-only" htmlFor="location-search">Tìm địa điểm</label><input id="location-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm đường, tòa nhà, địa điểm" autoComplete="off" />{(results.length > 0 || searching) && <div className="location-picker-results" role="listbox">{searching && <p>Đang tìm…</p>}{results.map((item) => <button key={item.providerRefId} type="button" role="option" onClick={() => selectCandidate(item)}><strong>{item.formattedAddress}</strong><small>{item.district ?? item.city ?? ''}</small></button>)}</div>}</div><button type="button" className="location-picker-current" aria-label="Dùng vị trí hiện tại" onClick={useCurrentLocation} disabled={gpsBusy}>{gpsBusy ? '…' : <CrosshairIcon />}</button></div>
    <footer><div><strong>{resolving || centerDirty ? 'Đang xác định địa chỉ…' : candidate?.formattedAddress ?? 'Di chuyển bản đồ để chọn vị trí'}</strong>{candidate && !centerDirty && <small>{[candidate.ward, candidate.district, candidate.city].filter(Boolean).join(', ')}</small>}{error && <p role="alert">{error}</p>}</div><button type="button" className="button primary" disabled={!candidate || resolving || centerDirty || Boolean(mapError)} onClick={() => candidate && onConfirm(candidate)}>Xác nhận vị trí</button></footer>
  </section></div>
}
