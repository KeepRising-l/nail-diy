import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'
import './styles/global.css'

const app = createApp(App)

// 全局注册 Element Plus 图标（避免 emoji）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
// 组件库统一中文语言包（分页器/日期选择/空状态等内置文案）
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
