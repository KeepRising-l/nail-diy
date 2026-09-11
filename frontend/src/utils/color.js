/**
 * 色彩工具：HEX ⇄ HSL 转换 + 色相/饱和度/明度微调
 * 用于设计台底胶的"色相 · 饱和度 · 明暗"精细调整
 */

export function clamp(v, min, max) { return Math.min(max, Math.max(min, v)) }

/** #RRGGBB → { h: 0-360, s: 0-100, l: 0-100 } */
export function hexToHsl(hex) {
  let m = /^#?([0-9a-f]{6})$/i.exec((hex || '').trim())
  if (!m) return { h: 0, s: 0, l: 90 }
  const n = parseInt(m[1], 16)
  const r = ((n >> 16) & 255) / 255
  const g = ((n >> 8) & 255) / 255
  const b = (n & 255) / 255
  const max = Math.max(r, g, b), min = Math.min(r, g, b)
  const l = (max + min) / 2
  let h = 0, s = 0
  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) * 60
    else if (max === g) h = ((b - r) / d + 2) * 60
    else h = ((r - g) / d + 4) * 60
  }
  return { h, s: s * 100, l: l * 100 }
}

const h2 = (v) => {
  const c = Math.round(clamp(v, 0, 255)).toString(16)
  return c.length === 1 ? '0' + c : c
}

/** { h, s, l } → #RRGGBB */
export function hslToHex(h, s, l) {
  h = ((h % 360) + 360) % 360
  s = clamp(s, 0, 100) / 100
  l = clamp(l, 0, 100) / 100
  const c = (1 - Math.abs(2 * l - 1)) * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = l - c / 2
  let r = 0, g = 0, b = 0
  if (h < 60)       { r = c; g = x }
  else if (h < 120) { r = x; g = c }
  else if (h < 180) { g = c; b = x }
  else if (h < 240) { g = x; b = c }
  else if (h < 300) { r = x; b = c }
  else              { r = c; b = x }
  return '#' + h2((r + m) * 255) + h2((g + m) * 255) + h2((b + m) * 255)
}

/**
 * 在原色基础上微调：保留原素材的光泽与纹理观感（只做 HSL 平移）
 * @param {string} hex 原色
 * @param {{h?:number, s?:number, l?:number}} adj  h: -180~180, s: -100~100, l: -100~100
 */
export function adjustColor(hex, adj = {}) {
  const { h = 0, s = 0, l = 0 } = adj
  if (!h && !s && !l) return (hex || '#F5F0E6').toUpperCase()
  const c = hexToHsl(hex)
  return hslToHex(c.h + h, c.s + s, c.l + l).toUpperCase()
}

/** 确定性伪随机（mulberry32）：保证同样参数下波点/豹纹布局稳定不闪动 */
export function seededRandom(seed) {
  let t = seed >>> 0
  return function () {
    t = t + 0x6D2B79F5 | 0
    let x = Math.imul(t ^ t >>> 15, 1 | t)
    x = x + Math.imul(x ^ x >>> 7, 61 | x) ^ x
    return ((x ^ x >>> 14) >>> 0) / 4294967296
  }
}

/** 判断颜色偏暗（用于波点"智能配色"自动取反差色） */
export function isDarkColor(hex) {
  const c = hexToHsl(hex)
  return c.l < 55
}
