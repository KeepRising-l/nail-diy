/**
 * 参数化款式引擎（对齐 nailartmaker 的图案体系）
 * 每种款式 = 元信息 + 纯函数生成器：输入 60x84 甲片坐标系参数，输出 SVG 元素描述数组
 * 生成器必须是纯函数（用 seededRandom 保证同一设计每次渲染一致），由 NailArt 组件统一消费
 * 与后端 material 表 PATTERN_STYLE 类型的 code 一一对应，attrs.paramsSchema 驱动参数面板
 */
import { seededRandom, isDarkColor } from '../utils/color'

/** 甲片坐标系常量：与 NailArt viewBox 对齐 */
export const NAIL_W = 60
export const NAIL_H = 84

/** 智能反差色：深底取浅纹，浅底取深纹 */
function contrastColor(baseColor, dark = 'rgba(60,45,35,0.7)', light = 'rgba(255,255,255,0.85)') {
  return isDarkColor(baseColor) ? light : dark
}

/** 五角星 path（中心 0,0，外径 r） */
function starPath(cx, cy, r) {
  const pts = []
  for (let i = 0; i < 10; i++) {
    const ang = -Math.PI / 2 + (i * Math.PI) / 5
    const rr = i % 2 === 0 ? r : r * 0.43
    pts.push(`${(cx + Math.cos(ang) * rr).toFixed(1)},${(cy + Math.sin(ang) * rr).toFixed(1)}`)
  }
  return 'M' + pts.join(' L') + ' Z'
}

export const PATTERN_STYLES = {
  /** 法式微笑线（现有 NailArt 已有，登记入册便于统一面板） */
  FRENCH: {
    name: '法式微笑线',
    gen({ color = '#FFFFFF', height = 26, curvature = 72 }) {
      const topY = 78 - Math.max(8, Math.min(40, height))
      const lift = (curvature / 100) * 16
      return [{ tag: 'path', d: `M -2 ${topY} Q 30 ${topY - lift} 62 ${topY} L 62 86 L -2 86 Z`, fill: color }]
    }
  },

  /** 波点 */
  DOTS: {
    name: '波点',
    gen({ density = 7, size = 2.5, color = '', baseColor = '#F5F0E6', seed = 1 }) {
      const rnd = seededRandom(seed * 97 + Math.round(density) * 13 + 1)
      const fill = color || contrastColor(baseColor)
      return Array.from({ length: Math.round(density) }, () => ({
        tag: 'ellipse',
        cx: 6 + rnd() * 48, cy: 10 + rnd() * 62,
        rx: size * (0.85 + rnd() * 0.4), ry: size * (0.85 + rnd() * 0.4) * 0.92,
        fill, opacity: 0.9
      }))
    }
  },

  /** 豹纹：不规则环 + 内点 */
  LEOPARD: {
    name: '豹纹',
    gen({ density = 7, size = 2.5, color = '', baseColor = '#F5F0E6', seed = 1 }) {
      const rnd = seededRandom(seed * 97 + Math.round(density) * 13 + 5)
      const fill = color || contrastColor(baseColor, 'rgba(70,45,20,0.85)', 'rgba(120,75,30,0.8)')
      const out = []
      for (let i = 0; i < Math.round(density); i++) {
        const x = 6 + rnd() * 48, y = 10 + rnd() * 62, r = size * (0.9 + rnd() * 0.7)
        out.push({ tag: 'circle', cx: x, cy: y, r, fill: 'none', stroke: fill, 'stroke-width': r * 0.62, opacity: 0.85 })
        out.push({ tag: 'circle', cx: x + r * 0.3, cy: y - r * 0.25, r: r * 0.3, fill })
      }
      return out
    }
  },

  /** 大理石纹：几条随机贝塞尔纹理 + 细分叉 */
  MARBLE: {
    name: '大理石纹',
    gen({ veinColor = '#FFFFFF', density = 4, thickness = 1.6, seed = 1 }) {
      const rnd = seededRandom(seed * 131 + Math.round(density) * 7)
      const out = []
      for (let i = 0; i < Math.round(density); i++) {
        const x1 = -4 + rnd() * 20, y1 = rnd() * 30
        const x2 = 20 + rnd() * 20, y2 = 30 + rnd() * 30
        const x3 = 40 + rnd() * 24, y3 = 60 + rnd() * 28
        out.push({
          tag: 'path',
          d: `M ${x1} ${y1} C ${x1 + 15 + rnd() * 10} ${y1 + 10 + rnd() * 20}, ${x2 - 10 + rnd() * 10} ${y2 - 10 + rnd() * 10}, ${x3} ${y3}`,
          fill: 'none', stroke: veinColor, 'stroke-width': thickness, 'stroke-linecap': 'round', opacity: 0.75
        })
        // 细分叉
        if (rnd() > 0.4) {
          const bx = (x1 + x2) / 2, by = (y1 + y2) / 2
          out.push({
            tag: 'path',
            d: `M ${bx} ${by} Q ${bx + 8} ${by + 6 + rnd() * 8}, ${bx + 10 + rnd() * 8} ${by + 14 + rnd() * 10}`,
            fill: 'none', stroke: veinColor, 'stroke-width': thickness * 0.6, 'stroke-linecap': 'round', opacity: 0.5
          })
        }
      }
      return out
    }
  },

  /** 玳瑁琥珀：半透明深色斑块叠加 */
  TORTOISE: {
    name: '玳瑁琥珀',
    gen({ spotColor = '#4A2C17', density = 6, seed = 1 }) {
      const rnd = seededRandom(seed * 61 + Math.round(density) * 17)
      const out = []
      for (let i = 0; i < Math.round(density); i++) {
        const cx = 8 + rnd() * 44, cy = 8 + rnd() * 68
        const r = 5 + rnd() * 9, rot = rnd() * 180
        out.push({
          tag: 'ellipse', cx, cy, rx: r, ry: r * (0.55 + rnd() * 0.4),
          fill: spotColor, opacity: 0.28 + rnd() * 0.3,
          transform: `rotate(${rot.toFixed(0)} ${cx.toFixed(1)} ${cy.toFixed(1)})`
        })
      }
      return out
    }
  },

  /** 极光晕染：多层同心半透明圆模拟径向光晕（避免额外 defs，便于统一渲染） */
  AURA: {
    name: '极光晕染',
    gen({ centerColor = '#F8C8DC', radius = 45, cy = 45 }) {
      const cx = 30
      const rings = []
      const steps = 4
      for (let i = steps; i >= 1; i--) {
        const r = radius * (i / steps)
        const opacity = 0.10 + (steps - i) * 0.09
        rings.push({ tag: 'circle', cx, cy, r, fill: centerColor, opacity: Number(opacity.toFixed(2)) })
      }
      return rings
    }
  },

  /** 复古曲线：几条流畅 S 形手绘曲线 */
  SWIRLS: {
    name: '复古曲线',
    gen({ lineColor = '#FFFFFF', count = 3, width = 1.4, seed = 1 }) {
      const rnd = seededRandom(seed * 53 + Math.round(count) * 11)
      const out = []
      for (let i = 0; i < Math.round(count); i++) {
        const y = 12 + (i / Math.max(1, count - 1)) * 58 + rnd() * 8
        const amp = 6 + rnd() * 8
        out.push({
          tag: 'path',
          d: `M -4 ${y} C 12 ${y - amp}, 22 ${y + amp}, 32 ${y} S 52 ${y - amp}, 64 ${y + amp * 0.6}`,
          fill: 'none', stroke: lineColor, 'stroke-width': width, 'stroke-linecap': 'round', opacity: 0.85
        })
      }
      return out
    }
  },

  /** 几何拼色：half 上下 / diagonal 对角 / tip 指尖三角 */
  GEO: {
    name: '几何拼色',
    gen({ color2 = '#FFFFFF', mode = 'diagonal' }) {
      if (mode === 'half') {
        return [{ tag: 'path', d: 'M -2 44 L 62 44 L 62 86 L -2 86 Z', fill: color2, opacity: 0.92 }]
      }
      if (mode === 'tip') {
        return [{ tag: 'path', d: 'M 30 24 L 58 78 L 2 78 Z', fill: color2, opacity: 0.92 }]
      }
      return [{ tag: 'path', d: 'M -2 30 L 62 66 L 62 86 L -2 86 Z', fill: color2, opacity: 0.92 }]
    }
  },

  /** 星星点缀：随机散布五角星 */
  STARS: {
    name: '星星点缀',
    gen({ color = '', count = 4, size = 2.4, baseColor = '#F5F0E6', seed = 1 }) {
      const rnd = seededRandom(seed * 83 + Math.round(count) * 19)
      const fill = color || contrastColor(baseColor)
      return Array.from({ length: Math.round(count) }, () => {
        const cx = 10 + rnd() * 40, cy = 10 + rnd() * 62, r = size * (0.8 + rnd() * 0.5)
        return { tag: 'path', d: starPath(cx, cy, r), fill, opacity: 0.95 }
      })
    }
  }
}

/**
 * 按款式 code + 参数生成 SVG 元素描述（未知款式返回空数组，保证渲染安全）
 * @param {string} code 款式 code（对应后端 PATTERN_STYLE 素材 code）
 * @param {object} params 款式参数（含 baseColor 底色、seed 随机种子）
 */
export function genPattern(code, params = {}) {
  const style = PATTERN_STYLES[code]
  return style ? style.gen(params) : []
}
