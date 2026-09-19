/**
 * 甲型轮廓几何（8 种纯轮廓）
 *
 * 画布约定：y=0 为甲根（指根侧），y 沿指甲长轴朝指尖增大；viewBox 高度随长度系数伸缩
 * 甲型 = 8 种纯轮廓（决定前端形状）；长度 = 独立系数 k∈[0.6,2.8]（决定甲片长短）
 * 统一几何基准：最宽处 x∈[8,52]（44 单位，手模按此映射甲床宽度），甲根缘 x∈[9,51]（宽 42，
 * 与最宽处仅差 5%——穿戴甲必须完整覆盖天然甲床，根部不能明显收窄露甲）、根缘中心 y≈7
 * 轮廓带轻微侧边收窄与较大的尖端圆弧，保证在小尺寸（手模 ~15px 宽）下仍读得出甲型而非矩形
 *
 * 甲型素材属于「代码渲染素材」：material 表 image_url 为空，形状由本模块的 SVG 路径生成。
 * 设计台（NailArt.vue）与运营端素材管理页的预览缩略图共用此模块，保证两处形状一致。
 */

/** 保留 1 位小数，避免路径字符串过长 */
const f1 = (v) => Number(v.toFixed(1))

/** 长度系数收敛到 [0.6, 2.8] */
export const clampLength = (k) => Math.min(2.8, Math.max(0.6, Number(k) || 1))

/** 甲片 SVG viewBox 高度（随长度系数伸缩） */
export const contourHeight = (k) => Math.round(76 * clampLength(k) + 8)

/** code → 轮廓生成函数 */
export const SHAPE_CONTOURS = {
  // 圆形：前端呈自然圆弧，甲面饱满
  round(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 9 4 C 7.5 ${m} 9.5 ${f1(t - 14)} 16 ${f1(t - 6)} Q 30 ${f1(t + 7)} 44 ${f1(t - 6)} C 50.5 ${f1(t - 14)} 52.5 ${m} 51 4 C 41 7.5 19 7.5 9 4 Z`
  },
  // 方圆形：整体偏方，但边角圆润（squoval）
  squoval(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 9.5 4 C 8 ${m} 10 ${f1(t - 12)} 17 ${f1(t - 5)} Q 30 ${f1(t + 4)} 43 ${f1(t - 5)} C 50 ${f1(t - 12)} 52 ${m} 50.5 4 C 41 7.5 19 7.5 9.5 4 Z`
  },
  // 方形：前端平直，边角圆润收拢
  square(k) {
    const t = 76 * k + 4
    return `M 9.5 4 L 9.5 ${f1(t - 7)} Q 9.5 ${f1(t)} 15.5 ${f1(t)} L 44.5 ${f1(t)} Q 50.5 ${f1(t)} 50.5 ${f1(t - 7)} L 50.5 4 C 41 7.5 19 7.5 9.5 4 Z`
  },
  // 椭圆形：整体修长，前端为柔和椭圆
  oval(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 10 4 C 8 ${m} 11.5 ${f1(t - 10)} 19 ${f1(t - 3)} Q 30 ${f1(t + 5)} 41 ${f1(t - 3)} C 48.5 ${f1(t - 10)} 52 ${m} 50 4 C 40.5 7.5 19.5 7.5 10 4 Z`
  },
  // 杏仁形：两侧逐渐收窄，前端圆润尖细
  almond(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 10.5 4 C 8.5 ${m} 15 ${f1(t - 12)} 25 ${f1(t - 4)} Q 30 ${f1(t)} 35 ${f1(t - 4)} C 45 ${f1(t - 12)} 51.5 ${m} 49.5 4 C 40 7.5 20 7.5 10.5 4 Z`
  },
  // 芭蕾形：两侧收窄，前端平直，类似芭蕾舞鞋（coffin）
  ballerina(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 9 4 C 8 ${m} 11 ${f1(t - 13)} 17 ${f1(t - 5.5)} L 21.5 ${f1(t - 1.5)} Q 23 ${f1(t)} 25 ${f1(t)} L 35 ${f1(t)} Q 37 ${f1(t)} 38.5 ${f1(t - 1.5)} L 43 ${f1(t - 5.5)} C 49 ${f1(t - 13)} 52 ${m} 51 4 C 41 7.5 19 7.5 9 4 Z`
  },
  // 尖形：两侧明显收窄，前端形成尖角
  stiletto(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 10.5 4 C 8.5 ${m} 16.5 ${f1(t - 14)} 27 ${f1(t - 2.5)} L 30 ${f1(t)} L 33 ${f1(t - 2.5)} C 43.5 ${f1(t - 14)} 51.5 ${m} 49.5 4 C 40 7.5 20 7.5 10.5 4 Z`
  },
  // 俄罗斯方：整体修长，两侧略微收窄，前端保持平直
  russian(k) {
    const t = 76 * k + 4, m = f1((4 + t) / 2)
    return `M 10 4 C 9 ${m} 13 ${f1(t - 11)} 17.5 ${f1(t - 4.5)} Q 19 ${f1(t)} 22 ${f1(t)} L 38 ${f1(t)} Q 41 ${f1(t)} 42.5 ${f1(t - 4.5)} C 47 ${f1(t - 11)} 51 ${m} 50 4 C 40.5 7.5 19.5 7.5 10 4 Z`
  }
}

/**
 * 甲型代码清单（新增/编辑甲型素材时下拉可选；顺序与设计台、种子数据一致）
 * 只有这 8 个 code 有对应的前端渲染器，所以运营端不能凭空发明新的甲型代码
 */
export const SHAPE_OPTIONS = [
  { code: 'round', name: '圆形' },
  { code: 'squoval', name: '方圆形' },
  { code: 'square', name: '方形' },
  { code: 'oval', name: '椭圆形' },
  { code: 'almond', name: '杏仁形' },
  { code: 'ballerina', name: '芭蕾形' },
  { code: 'stiletto', name: '尖形' },
  { code: 'russian', name: '俄罗斯方' }
]

/** 甲型名称关键字 → code（旧数据无 code 时回退；顺序敏感：方圆→椭圆 先于 圆） */
const NAME_RULES = [
  ['方圆', 'squoval'],
  ['椭圆', 'oval'],
  ['芭蕾', 'ballerina'],
  ['俄罗斯', 'russian'],
  ['杏仁', 'almond'],
  ['方', 'square'],
  ['尖', 'stiletto'],
  ['圆', 'round']
]

/**
 * 解析甲型素材对应的轮廓 code
 * 优先用素材自带的 code；缺失时按名称关键字回退（兼容早期无 code 的数据）
 * @param {{code?: string, name?: string}} shape 甲型素材
 * @returns {string} SHAPE_CONTOURS 的键，兜底 'oval'
 */
export function resolveShapeCode(shape) {
  const code = shape?.code
  if (code && SHAPE_CONTOURS[code]) return code
  const name = shape?.name || ''
  for (const [kw, key] of NAME_RULES) {
    if (name.includes(kw)) return key
  }
  return 'oval'
}

/**
 * 生成甲片轮廓 SVG path
 * @param {{code?: string, name?: string}} shape 甲型素材
 * @param {number} [length=1] 长度系数 0.6~2.8
 * @returns {string} SVG path d 属性
 */
export function shapePathOf(shape, length = 1) {
  return SHAPE_CONTOURS[resolveShapeCode(shape)](clampLength(length))
}
