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

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
