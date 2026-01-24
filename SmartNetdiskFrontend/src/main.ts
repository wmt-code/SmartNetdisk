import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Element Plus 样式
import 'element-plus/dist/index.css'
// 单独引入消息组件样式（确保按需导入时也能正常显示）
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

import './styles/main.scss'

// Monaco Editor 配置
import { loader } from '@guolao/vue-monaco-editor'

// 使用 CDN 加载 Monaco Editor（避免打包体积过大）
loader.config({
  paths: {
    vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.52.0/min/vs'
  }
})

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
