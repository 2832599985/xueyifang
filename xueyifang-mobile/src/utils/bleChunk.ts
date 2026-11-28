import { stringToArrayBuffer, arrayBufferToString } from './bleCrypto'

export const BLE_DEFAULT_MAX_CHUNK_BYTES = 20
export const BLE_FRAME_HEADER_BYTES = 2
export const BLE_MAX_CHUNKS = 128

/**
 * Splits a UTF-8 string payload into BLE-safe GATT frames.
 * Each frame has a 2-byte header: [seqIndex, totalChunks] followed by payload bytes.
 * Guarantees every frame fits within maxChunkBytes (default 20 bytes for BLE 4.0 ATT MTU).
 */
export function encodeBleFrames(
  payload: string,
  maxChunkBytes: number = BLE_DEFAULT_MAX_CHUNK_BYTES
): ArrayBuffer[] {
  const effectiveMax = Math.max(BLE_FRAME_HEADER_BYTES + 1, Math.floor(maxChunkBytes))
  const maxBodyBytes = effectiveMax - BLE_FRAME_HEADER_BYTES
  const raw = new Uint8Array(stringToArrayBuffer(payload))

  if (raw.length === 0) {
    const empty = new Uint8Array([0, 1])
    return [empty.buffer]
  }

  const totalChunks = Math.ceil(raw.length / maxBodyBytes)
  if (totalChunks > BLE_MAX_CHUNKS) {
    throw new Error(`BLE payload too large (${totalChunks} chunks > ${BLE_MAX_CHUNKS})`)
  }

  const frames: ArrayBuffer[] = []
  for (let seq = 0; seq < totalChunks; seq++) {
    const start = seq * maxBodyBytes
    const slice = raw.subarray(start, Math.min(raw.length, start + maxBodyBytes))
    const frame = new Uint8Array(BLE_FRAME_HEADER_BYTES + slice.length)
    frame[0] = seq
    frame[1] = totalChunks
    frame.set(slice, BLE_FRAME_HEADER_BYTES)
    frames.push(frame.buffer)
  }
  return frames
}

/**
 * Reassembles framed BLE GATT notifications back into the original UTF-8 string.
 * Also supports legacy single-packet unframed JSON (starting with '{') for backwards compatibility.
 */
export class BleFrameAssembler {
  private expectedTotal: number | null = null
  private chunks = new Map<number, Uint8Array>()

  reset(): void {
    this.expectedTotal = null
    this.chunks.clear()
  }

  push(buffer: ArrayBuffer): string | null {
    const bytes = new Uint8Array(buffer)
    if (bytes.length === 0) return null

    // Legacy single-packet unframed JSON fallback
    if (this.expectedTotal === null && bytes[0] === 0x7b /* '{' */) {
      return arrayBufferToString(buffer)
    }

    if (bytes.length < BLE_FRAME_HEADER_BYTES) {
      return null
    }

    const seq = bytes[0]
    const total = bytes[1]
    if (total < 1 || total > BLE_MAX_CHUNKS || seq >= total) {
      return null
    }

    if (this.expectedTotal !== null && this.expectedTotal !== total) {
      this.reset()
    }
    this.expectedTotal = total
    this.chunks.set(seq, bytes.slice(BLE_FRAME_HEADER_BYTES))

    if (this.chunks.size < total) {
      return null
    }

    let totalLen = 0
    for (let i = 0; i < total; i++) {
      const part = this.chunks.get(i)
      if (!part) return null
      totalLen += part.length
    }

    const merged = new Uint8Array(totalLen)
    let offset = 0
    for (let i = 0; i < total; i++) {
      const part = this.chunks.get(i)!
      merged.set(part, offset)
      offset += part.length
    }

    this.reset()
    return arrayBufferToString(merged.buffer)
  }
}
