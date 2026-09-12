// 开发专用手模预览 harness（不入业务路由）：直接渲染 HandModel + NailArt，
// 绑定与 Designer.vue 手模视图分支一致，用于甲片贴合度的视觉验收
import { createApp, h, ref } from 'vue'
import HandModel from './components/HandModel.vue'
import NailArt from './components/NailArt.vue'

const SHAPES = [
  { id: 's-round', name: '圆形', code: 'round' },
  { id: 's-squoval', name: '方圆形', code: 'squoval' },
  { id: 's-square', name: '方形', code: 'square' },
  { id: 's-oval', name: '椭圆形', code: 'oval' },
  { id: 's-almond', name: '杏仁形', code: 'almond' },
  { id: 's-ballerina', name: '芭蕾形', code: 'ballerina' },
  { id: 's-stiletto', name: '尖形', code: 'stiletto' },
  { id: 's-russian', name: '俄罗斯形', code: 'russian' }
]

function mkFinger(shapeId, color, length) {
  return {
    shapeId, length, colorId: color,
    overlays: [], patternIds: [],
    gradient: { enabled: false, color2: '', position: 50 },
    catEye: { enabled: false, angle: 25, width: 34, brightness: 72 },
    adjust: { hue: 0, sat: 0, light: 0 },
    french: { enabled: false, color: '#FFFFFF', height: 26, curvature: 72 },
    dots: { enabled: false, kind: 'dots', density: 7, size: 2.5, color: '' },
    patternStyle: null, decorations: [], patternPos: {}
  }
}

const state = ref({
  fingers: [
    mkFinger('s-round', '#F5EFE3', 1),      // 小指
    mkFinger('s-oval', '#EFD9C8', 1),       // 食指
    mkFinger('s-almond', '#E8A9B8', 1),     // 中指
    mkFinger('s-squoval', '#F5EFE3', 1),    // 无名指
    mkFinger('s-round', '#F5EFE3', 1)       // 拇指
  ],
  activeFinger: 2,
  shapeIdx: 4 // 当前选中的甲型（almond）
})

const App = {
  setup() {
    const st = state.value
    const shapeById = id => SHAPES.find(s => s.id === id) || null
    const setFinger = (key, val) => { st.fingers[st.activeFinger][key] = val }
    const cycleShape = () => {
      st.shapeIdx = (st.shapeIdx + 1) % SHAPES.length
      st.fingers.forEach(f => { f.shapeId = SHAPES[st.shapeIdx].id })
    }
    const setLen = e => { setFinger('length', Number(e.target.value)) }
    return { st, SHAPES, shapeById, cycleShape, setLen }
  },
  render() {
    return h('div', { style: 'display:flex;gap:40px;padding:24px;align-items:flex-start' }, [
      // 左：手模（与 Designer 手模分支相同的绑定）
      h('div', { style: 'width:420px' }, [
        h(HandModel, {
          activeFinger: this.st.activeFinger,
          fingers: this.st.fingers,
          onSelect: i => { this.st.activeFinger = i }
        }, {
          default: ({ finger, index, width, fit }) => h(NailArt, {
            shape: this.shapeById(finger.shapeId),
            color: finger.colorId,
            patterns: [],
            active: index === this.st.activeFinger,
            index,
            width,
            length: finger.length * fit,
            outlined: false,
            overlays: finger.overlays,
            overlayCatalog: [],
            gradient: finger.gradient,
            catEye: finger.catEye,
            adjust: finger.adjust,
            french: finger.french,
            dots: finger.dots,
            patternStyle: finger.patternStyle,
            decorations: finger.decorations,
            patternPos: finger.patternPos
          })
        })
      ]),
      // 右：控制面板（验收用：选中指/甲型切换/长度滑杆）
      h('div', { style: 'width:280px;display:flex;flex-direction:column;gap:14px;font-family:sans-serif' }, [
        h('div', 'active = ' + this.st.activeFinger),
        h('button', { onClick: this.cycleShape, style: 'padding:8px' },
          '全部切换甲型：' + this.SHAPES[this.st.shapeIdx].name),
        h('label', [
          '长度 ' + this.st.fingers[this.st.activeFinger].length.toFixed(2),
          h('input', {
            type: 'range', min: 0.6, max: 1.6, step: 0.05,
            value: this.st.fingers[this.st.activeFinger].length,
            onInput: this.setLen, style: 'width:100%'
          })
        ]),
        h('div', { style: 'display:flex;flex-wrap:wrap;gap:6px' },
          this.st.fingers.map((f, i) => h('button', {
            key: i, onClick: () => { this.st.activeFinger = i },
            style: 'padding:6px 10px' + (i === this.st.activeFinger ? ';background:#333;color:#fff' : '')
          }, '指 ' + i)))
      ])
    ])
  }
}

createApp(App).mount('#app')
