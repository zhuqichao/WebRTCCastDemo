import Vue from 'vue'
import App from './App.vue'
import VueRouter from "vue-router"
import VueResource from "vue-resource"

//开启debug模式
Vue.config.debug = true

Vue.use(VueRouter)
Vue.use(VueResource)

import Home from "./component/Home";

// 创建一个路由器实例
// 并且配置路由规则
const router = new VueRouter({
  routes: [
    {
      path: '/',
      component: Home
    }
  ]
})

new Vue({
  el: '#app',
  router: router,
  render: h => h(App)
})
