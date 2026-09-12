/**
 * 指甲油效果覆盖层引擎（OVERLAY 素材的过渡期参数化实现）
 *
 * 素材约定：material.type = 'OVERLAY'，category = attrs 中的子类：
 *   GLOSS 光泽 / PEARL 珠光 / SHIMMER 闪耀 / EFFECT 炫光 / SPECIAL 特殊
 * 每个 OVERLAY 素材：
 *   - image_url 为空 → 按 code 走本引擎参数化渲染（过渡期）
 *   - image_url 有值 → NailArt 直接叠加透明 PNG（最终形态，替换时业务结构不变）
 *
 * 输出坐标：60x84 标准甲面（与 patternStyles 同约定），由 NailArt 统一裁剪进轮廓
 * 并随长度系数缩放。全部为半透明层，叠加在任意底色上。
 */
import { seededRandom } from '../utils/color'

/** 60x84 甲面轮廓近似 path（覆盖层大面积元素的铺底形状，留 4px 边距避免顶到裁剪边缘） */
const VEIL = 'M 11 5 C 6 22 7 55 14 70 Q 30 82 46 70 C 53 55 54 22 49 5 Q 30 -1 11 5 Z'

const f1 = v => Number(v.toFixed(2))

/** 细颗粒（珠光/闪粉通用）：n 颗，尺寸/透明度/颜色由参数控制 */
function grains(rnd, { n, rMin, rMax, colors, opMin, opMax }) {
  const out = []
  for (let i = 0; i < n; i++) {
    out.push({
      tag: 'circle',
      cx: f1(7 + rnd() * 46),
      cy: f1(6 + rnd() * 72),
      r: f1(rMin + rnd() * (rMax - rMin)),
      fill: colors[Math.floor(rnd() * colors.length)],
      opacity: f1(opMin + rnd() * (opMax - opMin))
    })
  }
  return out
}

/** 柔和色晕（同心圆组模拟径向渐变，避免额外 defs） */
function glow(cx, cy, r, color, baseOpacity, steps = 4) {
  const out = []
  for (let i = steps; i >= 1; i--) {
    out.push({
      tag: 'circle', cx, cy, r: f1(r * (i / steps)),
      fill: color, opacity: f1(baseOpacity * (steps - i + 1) / steps)
    })
  }
  return out
}

export const POLISH_OVERLAYS = {
  // ───────── GLOSS 光泽 ─────────
  'gloss-soft': {
    name: '柔光',
    gen: () => [
      { tag: 'path', d: VEIL, fill: '#FFFFFF', opacity: 0.14 },
      { tag: 'path', d: 'M 22 10 C 19 24 18.5 38 22 50', stroke: '#FFFFFF', 'stroke-width': 4.5, fill: 'none', 'stroke-linecap': 'round', opacity: 0.4 }
    ]
  },
  'gloss-glass': {
    name: '玻璃高光',
    gen: () => [
      { tag: 'path', d: VEIL, fill: '#FFFFFF', opacity: 0.10 },
      { tag: 'path', d: 'M 19 8 C 15 24 14.5 40 18.5 54', stroke: '#FFFFFF', 'stroke-width': 6, fill: 'none', 'stroke-linecap': 'round', opacity: 0.5 },
      { tag: 'path', d: 'M 37 10 C 35 24 34.5 34 37 46', stroke: '#FFFFFF', 'stroke-width': 2.4, fill: 'none', 'stroke-linecap': 'round', opacity: 0.32 },
      { tag: 'path', d: 'M 45 12 C 51 26 51 44 45 56', stroke: '#FFFFFF', 'stroke-width': 1.6, fill: 'none', 'stroke-linecap': 'round', opacity: 0.22 }
    ]
  },
  'gloss-wet': {
    name: '湿润高光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 41 + 7)
      const out = [
        { tag: 'path', d: VEIL, fill: '#FFFFFF', opacity: 0.08 },
        { tag: 'path', d: 'M 17 9 C 13 26 13 42 18 55', stroke: '#FFFFFF', 'stroke-width': 7, fill: 'none', 'stroke-linecap': 'round', opacity: 0.55 }
      ]
      for (let i = 0; i < 4; i++) {
        const x = 26 + rnd() * 22, y = 14 + rnd() * 44
        out.push({ tag: 'circle', cx: f1(x), cy: f1(y), r: f1(1.6 + rnd() * 2.6), fill: '#FFFFFF', opacity: f1(0.35 + rnd() * 0.3) })
      }
      return out
    }
  },

  // ───────── PEARL 珠光 ─────────
  'pearl-white': {
    name: '白色珠光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 61 + 1)
      return [
        { tag: 'path', d: VEIL, fill: '#FFFFFF', opacity: 0.16 },
        { tag: 'path', d: 'M 14 12 Q 30 4 46 12 L 44 30 Q 30 22 16 30 Z', fill: '#FFF7FA', opacity: 0.25 },
        ...grains(rnd, { n: 26, rMin: 0.4, rMax: 0.9, colors: ['#FFFFFF', '#F3F0FA'], opMin: 0.3, opMax: 0.7 })
      ]
    }
  },
  'pearl-pink': {
    name: '粉色珠光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 61 + 2)
      return [
        { tag: 'path', d: VEIL, fill: '#F6C9DA', opacity: 0.15 },
        ...glow(30, 30, 24, '#FBDCE8', 0.1),
        ...grains(rnd, { n: 26, rMin: 0.4, rMax: 0.9, colors: ['#FBDCE8', '#FFFFFF'], opMin: 0.3, opMax: 0.65 })
      ]
    }
  },
  'pearl-purple': {
    name: '紫色珠光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 61 + 3)
      return [
        { tag: 'path', d: VEIL, fill: '#D9C6EE', opacity: 0.14 },
        ...glow(30, 44, 24, '#E4D4F5', 0.1),
        ...grains(rnd, { n: 26, rMin: 0.4, rMax: 0.9, colors: ['#E4D4F5', '#FFFFFF'], opMin: 0.3, opMax: 0.6 })
      ]
    }
  },
  'pearl-gold': {
    name: '金色珠光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 61 + 4)
      return [
        { tag: 'path', d: VEIL, fill: '#F0DCAE', opacity: 0.15 },
        ...glow(30, 26, 22, '#F8ECC8', 0.12),
        ...grains(rnd, { n: 28, rMin: 0.4, rMax: 0.9, colors: ['#F8ECC8', '#FFFFFF'], opMin: 0.3, opMax: 0.65 })
      ]
    }
  },
  'pearl-rainbow': {
    name: '彩虹珠光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 61 + 5)
      const hues = ['#F6C9DA', '#D9C6EE', '#BFD9F2', '#CDEBD8', '#F0DCAE']
      const out = [{ tag: 'path', d: VEIL, fill: '#FFFFFF', opacity: 0.06 }]
      hues.forEach((c, i) => out.push(...glow(18 + i * 6.5, 16 + i * 12, 17, c, 0.09, 3)))
      out.push(...grains(rnd, { n: 30, rMin: 0.35, rMax: 0.8, colors: hues, opMin: 0.28, opMax: 0.6 }))
      return out
    }
  },

  // ───────── SHIMMER 闪耀 ─────────
  'shimmer-silver': {
    name: '银色细闪',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 77 + 11)
      return grains(rnd, { n: 44, rMin: 0.3, rMax: 0.95, colors: ['#EDF1F7', '#FFFFFF', '#D8E0EC'], opMin: 0.3, opMax: 0.75 })
    }
  },
  'shimmer-gold': {
    name: '金色细闪',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 77 + 12)
      return grains(rnd, { n: 44, rMin: 0.3, rMax: 0.95, colors: ['#F6E8C0', '#FFF9E8', '#E8CE96'], opMin: 0.28, opMax: 0.7 })
    }
  },
  'shimmer-pink': {
    name: '粉色细闪',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 77 + 13)
      return grains(rnd, { n: 42, rMin: 0.3, rMax: 0.95, colors: ['#F9D7E4', '#FFFFFF', '#F0B9CE'], opMin: 0.3, opMax: 0.7 })
    }
  },
  'shimmer-multi': {
    name: '彩色细闪',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 77 + 14)
      return grains(rnd, { n: 46, rMin: 0.3, rMax: 0.95, colors: ['#F9D7E4', '#D9C6EE', '#BFD9F2', '#F3E3B2', '#CDEBD8'], opMin: 0.28, opMax: 0.7 })
    }
  },
  'shimmer-fine': {
    name: '超细微闪',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 77 + 15)
      return grains(rnd, { n: 95, rMin: 0.25, rMax: 0.55, colors: ['#FFFFFF', '#F2F5FA'], opMin: 0.3, opMax: 0.7 })
    }
  },

  // ───────── EFFECT 炫光 ─────────
  'effect-aurora': {
    name: '极光炫光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 91 + 21)
      const hues = ['#C9B7E8', '#A8D8EA', '#F5C3D6', '#BFE8D2']
      const out = []
      const n = 3 + Math.floor(rnd() * 2)
      for (let i = 0; i < n; i++) {
        out.push(...glow(f1(16 + rnd() * 28), f1(16 + rnd() * 52), f1(13 + rnd() * 9), hues[i % hues.length], 0.14, 3))
      }
      return out
    }
  },
  'effect-holo': {
    name: '全息炫光',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 91 + 22)
      const hues = ['#F6C9DA', '#C9B7E8', '#A8D8EA', '#CDEBD8', '#F0DCAE']
      const out = []
      for (let i = 0; i < 5; i++) {
        const x = -8 + i * 15 + rnd() * 6
        out.push({
          tag: 'path',
          d: `M ${f1(x)} 88 L ${f1(x + 26)} -4 L ${f1(x + 38)} -4 L ${f1(x + 12)} 88 Z`,
          fill: hues[i % hues.length], opacity: 0.12
        })
      }
      return out
    }
  },
  'effect-prism': {
    name: '棱镜折射',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 91 + 23)
      const hues = ['#F6C9DA', '#A8D8EA', '#CDEBD8', '#F0DCAE', '#D9C6EE']
      const out = []
      for (let i = 0; i < 5; i++) {
        const x1 = f1(6 + rnd() * 44), y1 = f1(4 + rnd() * 30)
        out.push({
          tag: 'path',
          d: `M ${x1} ${y1} L ${f1(x1 + 10 + rnd() * 12)} ${f1(y1 + 14 + rnd() * 18)} L ${f1(x1 - 12 + rnd() * 8)} ${f1(y1 + 18 + rnd() * 14)} Z`,
          fill: hues[i % hues.length], opacity: 0.12
        })
      }
      return out
    }
  },

  // ───────── SPECIAL 特殊 ─────────
  'special-chrome': {
    name: 'Chrome 镜面',
    gen: () => {
      const out = [{ tag: 'path', d: VEIL, fill: '#E8ECF2', opacity: 0.28 }]
      // 镜面：高对比横向明暗带 + 锐利白光带
      ;[[8, 16, '#FFFFFF', 0.4], [22, 9, '#9AA6B8', 0.3], [34, 14, '#FFFFFF', 0.34], [52, 10, '#8E9AAC', 0.28], [66, 8, '#F4F7FB', 0.3]]
        .forEach(([y, h, c, o]) => out.push({ tag: 'path', d: `M 8 ${y} Q 30 ${y - 3} 52 ${y} L 52 ${y + h} Q 30 ${y + h - 3} 8 ${y + h} Z`, fill: c, opacity: o }))
      out.push({ tag: 'path', d: 'M 20 10 C 17 26 16.5 40 20 54', stroke: '#FFFFFF', 'stroke-width': 3, fill: 'none', 'stroke-linecap': 'round', opacity: 0.7 })
      return out
    }
  },
  'special-liquid': {
    name: '液态金属',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 101 + 31)
      const out = [{ tag: 'path', d: VEIL, fill: '#D9DEE8', opacity: 0.22 }]
      for (let i = 0; i < 3; i++) {
        const y = 14 + i * 20 + rnd() * 8
        out.push({
          tag: 'path',
          d: `M 8 ${f1(y)} C 20 ${f1(y - 7 - rnd() * 5)}, 40 ${f1(y + 7 + rnd() * 5)}, 52 ${f1(y)}`,
          stroke: i === 1 ? '#FFF3D6' : '#F5F8FC', 'stroke-width': f1(3.5 + rnd() * 3), fill: 'none', 'stroke-linecap': 'round', opacity: 0.5
        })
      }
      return out
    }
  },
  'special-glass': {
    name: '玻璃糖',
    gen: ({ seed }) => {
      const rnd = seededRandom(seed * 101 + 32)
      const out = [{ tag: 'path', d: VEIL, fill: '#EAF2FA', opacity: 0.16 }]
      for (let i = 0; i < 4; i++) {
        const x = f1(12 + rnd() * 32), y = f1(12 + rnd() * 52), r = f1(4.5 + rnd() * 5)
        out.push({ tag: 'circle', cx: x, cy: y, r, fill: '#FFFFFF', opacity: 0.13 })
        out.push({ tag: 'circle', cx: f1(x - r * 0.3), cy: f1(y - r * 0.35), r: f1(r * 0.45), fill: '#FFFFFF', opacity: 0.5 })
      }
      return out
    }
  },
  'special-jelly': {
    name: '果冻质感',
    gen: () => [
      { tag: 'path', d: VEIL, fill: '#FFEFF5', opacity: 0.2 },
      // 果冻透亮感：中段大面积柔光 + 边缘透亮
      ...glow(30, 42, 26, '#FFFFFF', 0.12),
      { tag: 'path', d: 'M 12 8 C 8 26 8 52 15 68', stroke: '#FFFFFF', 'stroke-width': 3, fill: 'none', 'stroke-linecap': 'round', opacity: 0.3 },
      { tag: 'path', d: 'M 48 8 C 52 26 52 52 45 68', stroke: '#FFFFFF', 'stroke-width': 3, fill: 'none', 'stroke-linecap': 'round', opacity: 0.3 }
    ]
  }
}

/**
 * 生成覆盖层元素（未知 code 返回空数组，保证渲染安全）
 * @param {string} code OVERLAY 素材 code
 * @param {object} opts { seed } 同一设计每次渲染一致
 */
export function genOverlay(code, opts = {}) {
  const def = POLISH_OVERLAYS[code]
  return def ? def.gen({ seed: opts.seed || 1 }) : []
}
