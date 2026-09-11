<template>
  <div class="home">
    <!-- Hero 极简大标题 -->
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-copy">
          <span class="eyebrow-pill">NAIL STUDIO · 在线定制美甲</span>
          <h1 class="hero-title">
            Design your<br />
            <span class="italic-pink">perfect nails.</span>
          </h1>
          <p class="hero-desc">
            选甲型、试颜色、贴图案——所见即所得的美甲设计台。<br />
            下单后商家精工制作，到店自提或快递到家。
          </p>
          <div class="hero-actions">
            <button class="btn-primary-lg" @click="goDesign">
              START DESIGNING
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
            </button>
            <button class="btn-ghost-lg" @click="goTemplates">
              BROWSE GALLERY
            </button>
          </div>
          <div class="hero-trust">
            <span>到店自提</span>
            <i class="dot"></i>
            <span>全国邮寄</span>
            <i class="dot"></i>
            <span>订单全程追踪</span>
          </div>
        </div>

        <!-- Hero 右侧：4:5 米色画布 + 内嵌预览图（差异化：自有品牌装饰） -->
        <div class="hero-visual">
          <div class="hero-canvas">
            <img v-if="records[0]?.coverUrl" class="hero-img" :src="records[0].coverUrl" :alt="records[0].name" />
            <div v-else class="hero-img placeholder" />
            <!-- 自有品牌角标：左上角小标签 + 右下角"DIY定制"印章感 -->
            <span class="corner-tl">
              <span class="corner-dot"></span>
              HOT · 热销款式
            </span>
            <span class="corner-br">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 16.8 5.8 21.3l2.4-7.4L2 9.4h7.6z"/></svg>
              Nail DIY
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- 三步流程：大数字 + 极简 -->
    <section class="features">
      <div class="container">
        <div class="features-head">
          <span class="eyebrow-pill">HOW IT WORKS</span>
          <h2 class="section-title">三步拥有你的专属美甲</h2>
        </div>
        <div class="feature-grid">
          <div v-for="(f, i) in FEATURES" :key="i" class="feature-item">
            <span class="feature-num">0{{ i + 1 }}</span>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
            <span class="feature-tag">{{ f.tag }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 精选款式 GALLERY -->
    <section class="templates-section" v-if="records.length">
      <div class="container">
        <div class="section-head">
          <div class="head-left">
            <span class="eyebrow-pill">GALLERY</span>
            <h2 class="section-title">精选款式灵感</h2>
          </div>
          <router-link to="/templates" class="see-all">
            查看全部
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
          </router-link>
        </div>
        <div class="template-grid">
          <router-link
            v-for="tpl in records"
            :key="tpl.id"
            :to="`/designer?templateId=${tpl.id}`"
            class="template-card hoverable"
          >
            <div class="template-cover">
              <img v-if="tpl.coverUrl" :src="tpl.coverUrl" :alt="tpl.name" />
              <div v-else class="template-placeholder">{{ tpl.name }}</div>
            </div>
            <div class="template-meta">
              <div>
                <span class="template-tag" v-if="tpl.styleTag">{{ tpl.styleTag }}</span>
                <h4 class="template-name">{{ tpl.name }}</h4>
              </div>
              <span class="template-price">¥{{ tpl.price }}</span>
            </div>
            <span class="use-overlay">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              USE THIS
            </span>
          </router-link>
        </div>
      </div>
    </section>

    <!-- CTA 极简 -->
    <section class="cta">
      <div class="container cta-inner">
        <span class="eyebrow-pill">READY?</span>
        <h2 class="cta-title">准备好打造第一副专属美甲了吗？</h2>
        <p class="cta-desc">注册账号，保存你的每一次设计，还可以随时追订。</p>
        <button class="btn-primary-lg" @click="goRegister">立即注册体验 →</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '../utils/request'

const records = ref([])
const FEATURES = [
  { tag: 'STUDIO',   title: '在线设计',   desc: '甲型、颜色、图案自由组合。点击手指即可独立编辑每一根指甲的细节。' },
  { tag: 'ARTISAN',  title: '商家制作',   desc: '设计确认后，合作美甲师按你的参数 1:1 精工制作，全程可追踪。' },
  { tag: 'DELIVERY', title: '自提或邮寄', desc: '选择离你最近的门店自提，或者直接快递到家，怎么方便怎么来。' }
]

onMounted(async () => {
  try {
    const res = await request.get('/templates', { params: { page: 1, size: 8 } })
    records.value = res.data?.records || []
  } catch (e) { records.value = [] }
})

function goDesign()   { location.href = '/designer' }
function goTemplates(){ location.href = '/templates' }
function goRegister() { location.href = '/register' }
</script>

<style scoped>
.home { padding-bottom: var(--space-3xl); }

/* ================= Hero ================= */
.hero { padding: var(--space-2xl) 0 var(--space-3xl); }
.hero-inner {
  display: flex; align-items: center;
  gap: var(--space-xl); flex-wrap: wrap;
}
.hero-copy { flex: 1; min-width: 320px; max-width: 560px; }
.hero-title {
  font-size: 68px;
  line-height: 1.02;
  color: var(--color-foreground);
  margin: 20px 0 22px;
  letter-spacing: -1.5px;
}
.italic-pink {
  color: var(--color-primary);
  font-style: italic;
  font-family: var(--font-heading);
}
.hero-desc {
  font-size: 16px;
  color: var(--color-muted-foreground);
  max-width: 480px;
  line-height: 1.85;
  margin-bottom: 32px;
}
.hero-actions { display: flex; gap: 12px; margin-bottom: 28px; flex-wrap: wrap; }
.btn-primary-lg {
  padding: 14px 26px;
  background: var(--color-solid);
  color: var(--color-on-solid);
  border: none;
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1.2px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 6px 20px var(--color-solid-shadow);
  transition: transform .15s, background .15s, box-shadow .15s;
}
.btn-primary-lg:hover {
  background: var(--color-solid-hover);
  transform: translateY(-1px);
  box-shadow: 0 8px 26px var(--color-solid-shadow);
}
.btn-ghost-lg {
  padding: 14px 26px;
  background: #fff;
  color: var(--color-foreground);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1.2px;
  cursor: pointer;
  transition: all .15s;
}
.btn-ghost-lg:hover {
  border-color: var(--color-foreground);
  background: var(--color-foreground);
  color: #fff;
}
.hero-trust {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--color-muted-foreground);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.3px;
}
.hero-trust .dot {
  width: 3px; height: 3px; border-radius: 50%;
  background: var(--color-muted-foreground); opacity: 0.5;
}

/* Hero 右侧：仿参考站中央画布 */
.hero-visual { flex: 1; min-width: 300px; display: flex; justify-content: flex-end; }
.hero-canvas {
  position: relative;
  width: 100%;
  max-width: 440px;
  aspect-ratio: 4 / 5;
  background: linear-gradient(180deg, var(--color-canvas) 0%, var(--color-canvas-deep) 100%);
  border-radius: 6px;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}
.hero-img {
  width: 90%; height: 82%;
  margin: 5% auto 0;
  object-fit: cover;
  border-radius: 4px;
}
.hero-img.placeholder {
  background: linear-gradient(135deg, #fff 0%, #F1E7D8 100%);
}
/* 左上角：HOT 热销标签（带玫粉小圆点） */
.corner-tl {
  position: absolute; top: 12px; left: 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--font-ui);
  font-size: 10px; font-weight: 700;
  letter-spacing: 0.8px;
  color: var(--color-foreground);
  background: rgba(255,255,255,0.92);
  padding: 5px 10px;
  border-radius: var(--radius-full);
  backdrop-filter: blur(2px);
}
.corner-tl .corner-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(229, 62, 140, 0.2);
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.25); }
}
/* 右下角：Nail DIY 品牌印章（带星形 icon） */
.corner-br {
  position: absolute; right: 12px; bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--font-ui);
  font-size: 10px; font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
  background: var(--color-foreground);
  padding: 4px 10px;
  border-radius: 4px;
  opacity: 0.82;
}
.corner-br svg { color: var(--color-primary); }

/* ================= Features ================= */
.features {
  padding: var(--space-2xl) 0;
  border-top: 1px solid var(--color-border);
}
.features-head {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: var(--space-xl);
}
.section-title {
  font-size: 42px;
  color: var(--color-foreground);
  letter-spacing: -0.5px;
  margin: 0;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}
.feature-item {
  padding: var(--space-xl) var(--space-lg);
  border-right: 1px solid var(--color-border);
  transition: background .2s;
}
.feature-item:last-child { border-right: none; }
.feature-item:hover { background: #fff; }
.feature-num {
  display: block;
  font-family: var(--font-ui);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--color-primary);
  margin-bottom: var(--space-md);
}
.feature-item h3 {
  font-family: var(--font-heading);
  font-size: 26px;
  margin: 0 0 12px;
  color: var(--color-foreground);
}
.feature-item p {
  color: var(--color-muted-foreground);
  font-size: 14.5px;
  line-height: 1.85;
  margin: 0 0 16px;
}
.feature-tag {
  display: inline-block;
  font-family: var(--font-ui);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.5px;
  padding: 4px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  color: var(--color-muted-foreground);
}

/* ================= Gallery ================= */
.templates-section { padding: var(--space-2xl) 0; }
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--space-xl);
  gap: 16px;
  flex-wrap: wrap;
}
.head-left { display: flex; flex-direction: column; gap: 12px; }
.section-head .section-title { margin: 0; font-size: 36px; }
.see-all {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--color-primary);
  border-bottom: 1px solid transparent;
  padding-bottom: 2px;
  transition: all .15s;
}
.see-all:hover { border-color: var(--color-primary); }

.template-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-md);
}
.template-card {
  position: relative;
  background: #fff;
  border-radius: var(--radius-md);
  padding: 10px;
  display: block;
  overflow: hidden;
  transition: all .25s ease;
}
.template-cover {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  aspect-ratio: 4 / 5;
  background: var(--color-canvas);
}
.template-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform .3s ease; }
.template-card:hover .template-cover img { transform: scale(1.04); }
.template-placeholder {
  width: 100%; height: 100%;
  display: grid; place-items: center;
  color: var(--color-muted-foreground);
  font-size: 14px;
}
.template-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 4px 4px;
}
.template-tag {
  display: inline-block;
  font-family: var(--font-ui);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--color-primary);
  background: var(--color-primary-light-9);
  padding: 2px 10px;
  border-radius: var(--radius-full);
  margin-bottom: 6px;
}
.template-name {
  font-family: var(--font-heading);
  font-size: 17px;
  color: var(--color-foreground);
  margin: 0;
  font-weight: 600;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.template-price {
  font-family: var(--font-ui);
  font-weight: 700;
  color: var(--color-foreground);
  font-size: 15px;
  padding-top: 4px;
}
.use-overlay {
  position: absolute;
  top: 18px; right: 18px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(255,255,255,0.95);
  border-radius: var(--radius-full);
  font-family: var(--font-ui);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.8px;
  color: var(--color-foreground);
  opacity: 0;
  transform: translateY(-4px);
  transition: all .2s;
  pointer-events: none;
}
.template-card:hover .use-overlay {
  opacity: 1;
  transform: translateY(0);
}

/* ================= CTA ================= */
.cta { padding: var(--space-2xl) 0 0; }
.cta-inner {
  text-align: center;
  padding: var(--space-2xl) var(--space-xl);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.cta-title {
  font-family: var(--font-heading);
  font-size: 40px;
  color: var(--color-foreground);
  margin: 0;
  letter-spacing: -0.5px;
}
.cta-desc {
  color: var(--color-muted-foreground);
  margin: 0 0 8px;
  font-size: 15px;
}

/* ================= 响应式 ================= */
@media (max-width: 1000px) {
  .template-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .hero-title { font-size: 46px; }
  .section-title { font-size: 30px; }
  .cta-title { font-size: 30px; }
  .feature-grid { grid-template-columns: 1fr; }
  .feature-item { border-right: none; border-bottom: 1px solid var(--color-border); padding: var(--space-lg) var(--space-md); }
  .template-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-visual { justify-content: center; }
}
</style>
