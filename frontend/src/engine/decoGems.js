/**
 * 钻饰（JEWEL）参数化渲染引擎 —— 真实水晶切面质感
 *
 * 目标：告别「线性渐变 + 描边」的 UI 图标观感，用
 *   底色 → 冠部切面（人字纹/放射 wedge）→ 彩虹折射斑 → 内缘暗部 → 外缘亮边 → 定点高光
 * 的分层组合，在任意底色上呈现"精品水钻/珠宝"的立体感。
 *
 * 类型约定（与 DECOR 素材 code 一致，颜色变体用后缀）：
 *   圆钻   round / round-pink / round-champagne / round-ab / round-smoke
 *   异形钻 drop / marquise / oval / heart / star / hex / triangle
 *   珍珠   pearl / pearl-pink / pearl-noir（无切面，乳光+高光）
 *   金属   bar / ring-gold（金属明暗带 + 高光，无切面）
 */

/** 基础形 path（局部坐标，原点中心，外接约 ±7，与 NailArt decoTransform 兼容） */
export const GEM_PATHS = {
  round: 'M0,-5.6 A5.6,5.6 0 1 1 -0.01,-5.6 Z',
  drop: 'M0,-6 C3,-2 5.5,1 5.5,3.2 A5.5,5.5 0 1 1 -5.5,3.2 C-5.5,1 -3,-2 0,-6 Z',
  hex: 'M-5.2,-3 L0,-6 L5.2,-3 L5.2,3 L0,6 L-5.2,3 Z',
  bar: 'M-6,-2.5 L6,-2.5 L6,2.5 L-6,2.5 Z',
  heart: 'M0,5.5 C-6,1 -6.5,-2.5 -3.6,-4.4 C-1.8,-5.6 0,-4.4 0,-2.8 C0,-4.4 1.8,-5.6 3.6,-4.4 C6.5,-2.5 6,1 0,5.5 Z',
  oval: 'M0,-6.5 A4.4,6.5 0 1 1 -0.01,-6.5 Z',
  triangle: 'M0,-6 L5.8,4.5 L-5.8,4.5 Z',
  marquise: 'M0,-7 C2.6,-4 3.4,-1 2.2,2.4 L0,7 L-2.2,2.4 C-3.4,-1 -2.6,-4 0,-7 Z',
  star: 'M0,-6 L1.76,-1.85 L6,-1.85 L2.6,0.95 L3.53,5.1 L0,2.7 L-3.53,5.1 L-2.6,0.95 L-6,-1.85 L-1.76,-1.85 Z',
  pearl: 'M0,-5.2 A5.2,5.2 0 1 1 -0.01,-5.2 Z',
  'ring-gold': 'M0,-6.2 A6.2,6.2 0 1 1 -0.01,-6.2 Z M0,-3.4 A3.4,3.4 0 1 0 0.01,-3.4 Z'
}

/** 色调表（变体后缀 → 水晶底色） */
const TINTS = {
  round: '#E7EEF8', 'round-pink': '#F8D8E4', 'round-champagne': '#F4E6C8',
  'round-ab': '#E2E8F8', 'round-smoke': '#CBC6D0',
  drop: '#E5EEF8', marquise: '#E5EEF8', oval: '#E5EEF8', heart: '#F6D7DE',
  star: '#EAF0FA', hex: '#E6EDF9', triangle: '#DDE7F5',
  pearl: '#F7F0EA', 'pearl-pink': '#F8E3E9', 'pearl-noir': '#43434B',
  bar: '#E2C987', 'ring-gold': '#E8C97E'
}

const f1 = v => Number(v.toFixed(2))

/** 冠部人字纹切面（brilliant cut 冠部）：n 行 V 形明暗带 */
function chevrons(tipY, rows) {
  const out = []
  for (let i = 0; i < rows; i++) {
    const y = tipY + i * 3.2
    const dark = i % 2 === 0
    out.push({
      tag: 'path',
      d: `M -7 ${f1(y + 3.2)} L 0 ${f1(y)} L 7 ${f1(y + 3.2)} L 7 ${f1(y + 5.2)} L 0 ${f1(y + 2)} L -7 ${f1(y + 5.2)} Z`,
      fill: dark ? '#4A5B78' : '#FFFFFF',
      opacity: dark ? 0.32 : 0.55
    })
  }
  return out
}

/** 放射 wedge 切面：从中心到边缘的 n 块交替明暗三角 */
function wedges(r, n) {
  const out = []
  for (let i = 0; i < n; i++) {
    const a0 = (i / n) * Math.PI * 2, a1 = ((i + 1) / n) * Math.PI * 2
    const x0 = f1(Math.sin(a0) * r), y0 = f1(-Math.cos(a0) * r)
    const x1 = f1(Math.sin(a1) * r), y1 = f1(-Math.cos(a1) * r)
    out.push({
      tag: 'path',
      d: `M 0 0 L ${x0} ${y0} L ${x1} ${y1} Z`,
      fill: i % 2 === 0 ? '#FFFFFF' : '#556680',
      opacity: i % 2 === 0 ? 0.5 : 0.26
    })
  }
  return out
}

/** 彩虹折射斑（透明水晶内部的轻微色散） */
function refraction(seedShift) {
  return [
    { tag: 'path', d: 'M -4.5 -1 C -2 -3.5 1 -4 3.5 -2.5 C 2 -0.5 -2.5 0.5 -4.5 -1 Z', fill: '#F6C9DA', opacity: 0.32 },
    { tag: 'path', d: 'M -2 2.5 C 0.5 1 3 1.5 4.5 3 C 2.5 4.5 -1 4.5 -2 2.5 Z', fill: '#A8D8EA', opacity: 0.32 }
  ]
}

/**
 * 生成钻饰渲染元素
 * @returns {{ base: string, tint: string, facets: el[], finish: el[] }}
 *   base   —— 基础形 path（第一层底色用它填充）
 *   facets —— 需裁剪进轮廓的切面/纹理层
 *   finish —— 不裁剪的收尾层（外缘亮边、高光点）
 */
export function decoGem(type) {
  const baseKey = GEM_PATHS[type] ? type : type.split('-')[0]
  const base = GEM_PATHS[baseKey] || GEM_PATHS.round
  const tint = TINTS[type] || TINTS[baseKey] || '#E7EEF8'

  // ── 珍珠：无切面，乳光层叠 + 柔和高光 ──
  if (baseKey === 'pearl') {
    const noir = type === 'pearl-noir'
    const facets = [
      ...glowRings(noir ? '#6A6A76' : '#FFFFFF', noir ? 0.35 : 0.4),
      ...(noir
        ? [{ tag: 'path', d: 'M -3.4 -1.6 C -1.6 -3.4 1 -3.8 2.8 -2.6 C 1.4 -1 1 2.2 -0.8 3.4 C -2.2 2.4 -3 0.6 -3.4 -1.6 Z', fill: '#8E8E9C', opacity: 0.4 }]
        : [{ tag: 'path', d: 'M -3.4 -1.6 C -1.6 -3.4 1 -3.8 2.8 -2.6 C 1.4 -1 1 2.2 -0.8 3.4 C -2.2 2.4 -3 0.6 -3.4 -1.6 Z', fill: '#FBEFF2', opacity: 0.5 }]),
      { tag: 'path', d: 'M -2.4 2 C -1 2.8 1 2.8 2.4 2', stroke: noir ? '#88889A' : '#D8BFA8', 'stroke-width': 0.7, fill: 'none', 'stroke-linecap': 'round', opacity: 0.5 }
    ]
    const finish = [
      { tag: 'ellipse', cx: -1.9, cy: -2.4, rx: 1.5, ry: 1.1, fill: '#FFFFFF', opacity: noir ? 0.65 : 0.95 },
      { tag: 'circle', cx: 1.4, cy: 1.6, r: 0.5, fill: '#FFFFFF', opacity: 0.6 }
    ]
    return { base, tint, facets, finish }
  }

  // ── 金属：明暗带 + 高光线，无切面 ──
  if (baseKey === 'bar' || type === 'ring-gold') {
    const ring = type === 'ring-gold'
    const facets = ring
      ? [
          { tag: 'path', d: 'M -6.2 0 A 6.2 6.2 0 0 1 6.2 0 L 3.4 0 A 3.4 3.4 0 0 0 -3.4 0 Z', fill: '#FFF0C4', opacity: 0.65 },
          { tag: 'path', d: 'M -6.2 0 A 6.2 6.2 0 0 0 6.2 0 L 3.4 0 A 3.4 3.4 0 0 1 -3.4 0 Z', fill: '#9A7528', opacity: 0.45 },
          { tag: 'circle', cx: 0, cy: 0, r: 3.4, fill: '#F5E7C2', opacity: 0.25 }
        ]
      : [
          { tag: 'path', d: 'M -6 -2.5 L 6 -2.5 L 6 -0.8 L -6 -0.8 Z', fill: '#FFF0C4', opacity: 0.6 },
          { tag: 'path', d: 'M -6 0.8 L 6 0.8 L 6 2.5 L -6 2.5 Z', fill: '#9A7528', opacity: 0.4 }
        ]
    const finish = [
      { tag: 'path', d: base, fill: 'none', stroke: '#FFF6DC', 'stroke-width': 0.55, opacity: 0.75 },
      { tag: 'circle', cx: ring ? -2.8 : -4, cy: ring ? -3.4 : -1.6, r: 0.7, fill: '#FFFFFF', opacity: 0.9 }
    ]
    return { base, tint, facets, finish }
  }

  // ── 水晶钻：冠部切面 + 折射 + 边缘光 ──
  const facets = []
  const pointy = baseKey === 'drop' || baseKey === 'marquise' || baseKey === 'heart' || baseKey === 'star'
  facets.push(...(pointy ? chevrons(baseKey === 'marquise' ? -5.5 : -4, 4) : wedges(5.4, 10)))
  facets.push(...refraction())
  // 台面（中心高光区）
  facets.push({ tag: 'path', d: 'M -2.2 -1.4 L 0 -2.6 L 2.2 -1.4 L 2.2 1 L 0 2.2 L -2.2 1 Z', fill: '#FFFFFF', opacity: 0.42 })
  // 内缘暗部（底部体积感）
  facets.push({ tag: 'path', d: 'M -5 2.5 C -2.5 5.5 2.5 5.5 5 2.5 C 3.5 5.8 -3.5 5.8 -5 2.5 Z', fill: '#3E4C66', opacity: 0.34 })

  const finish = [
    // 外缘亮边（左上受光）
    { tag: 'path', d: base, fill: 'none', stroke: '#FFFFFF', 'stroke-width': 0.6, opacity: 0.75 },
    // 定点高光 + 副高光
    { tag: 'circle', cx: -2.1, cy: -2.5, r: 1.15, fill: '#FFFFFF', opacity: 0.95 },
    { tag: 'circle', cx: 2.6, cy: 1.8, r: 0.5, fill: '#FFFFFF', opacity: 0.6 },
    { tag: 'path', d: 'M -1.2 -3.6 C 0.4 -4.2 2 -3.9 3 -3', stroke: '#FFFFFF', 'stroke-width': 0.5, fill: 'none', 'stroke-linecap': 'round', opacity: 0.6 }
  ]
  return { base, tint, facets, finish }
}

function glowRings(color, baseOpacity) {
  const out = []
  for (let i = 3; i >= 1; i--) {
    out.push({ tag: 'circle', cx: 0, cy: 0, r: f1(5.2 * i / 3), fill: color, opacity: f1(baseOpacity * (4 - i) / 6) })
  }
  return out
}

/** 材质盘/卡片预览用：平面简化版（形状 + 色调 + 高光点），返回可直接 v-bind 的属性对象 */
export function decoPreview(type) {
  const { base, tint } = decoGem(type)
  return [
    { d: base, fill: tint, 'fill-rule': 'evenodd' },
    { cx: -1.7, cy: -1.9, r: 1.05, fill: '#FFFFFF', opacity: 0.9 }
  ]
}
