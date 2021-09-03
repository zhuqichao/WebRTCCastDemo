<template>
  <div id="home">
<!--    <label>-->
<!--      <input style="font-size: large" v-model="code" placeholder="请输入投屏码">-->
<!--    </label>-->
<!--    <button v-on:click="onClick(code)">点击投屏</button>-->
    <br>
    <h3 v-for="device in devices" :key='device.id'>
      {{ "id:" + device.id }}
      {{ "code:" + device.code }}
      {{ "name:" + device.name }}
      <button v-on:click="onClick(device.code)">点击投屏</button>
    </h3>
  </div>
</template>

<script>

import websocket from '../common/websocket.js'
import baseHost from '../common/baseHost.js'

export default {
  name: "Home",
  data() {
    return {
      code: '',
      devices: []
    }
  },
  methods: {
    listDevices: function () {
      fetch(baseHost.getHost() + '/listDevice')
          .then(rsp => rsp.json())
          .then(result => {
            if (result && result.code === 0) {
              this.devices = result.result
            }
          })
    },
    onClick: function (code) {
      console.log(code)
      if (!code) {
        this.toast("请输入投屏码")
        return
      }
      fetch(baseHost.getHost() + '/checkDevice?pingCode=' + code)
          .then(rsp => rsp.json())
          .then(result => {
            if (result.result) {
              websocket.createWebsocket(code, this, function (event) {
                console.log(event)
                this.toast(event.message)
                if (event.connected) {
                  websocket.createPeerConnection(this, function (event) {
                    console.log(event)
                  })
                }
              })
            } else {
              this.toast(result.message)
            }
          })
    },
    toast: function (msg, duration) {
      duration = isNaN(duration) ? 3000 : duration;
      const m = document.createElement('div');
      m.innerHTML = msg;
      m.style.cssText = "font-family:siyuan;max-width:60%;min-width: 150px;padding:0 14px;height: 40px;color: rgb(255, 255, 255);line-height: 40px;text-align: center;border-radius: 4px;position: fixed;top: 50%;left: 50%;transform: translate(-50%, -50%);z-index: 999999;background: rgba(0, 0, 0,.7);font-size: 16px;";
      document.body.appendChild(m);
      setTimeout(function () {
        const d = 0.5;
        m.style["webkitTransition"] = '-webkit-transform ' + d + 's ease-in, opacity ' + d + 's ease-in';
        m.style.opacity = '0';
        setTimeout(function () {
          document.body.removeChild(m)
        }, d * 1000);
      }, duration);
    },
  },
  mounted() {

    this.listDevices()
  }
}
</script>

<style scoped>

</style>
