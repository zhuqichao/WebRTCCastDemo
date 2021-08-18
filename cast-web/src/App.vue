<template>
  <div id="app">
    <!--  展示错误信息  -->
    <h1 v-if="isError">{{ error }}</h1>
    <!--  router-view：用于存放子组件的容器  -->
    <router-view v-else/>
  </div>
</template>

<script>
  export default {
    name: 'app',
    data() {
      return {
        isError: false,
        error: '当前浏览器不支持'
      }
    },
    mounted() {
      var ua = navigator.userAgent.toLowerCase()
      if (!this.contains(ua, 'chrome')) {
        this.isError = true
      }
    },
    methods: {
      contains: function (src, sub) {
        return src.indexOf(sub) !== -1
      },
    }
  }

</script>

<style lang="scss">
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: #2c3e50;
    margin-top: 60px;
  }

  h1, h2 {
    font-weight: normal;
  }

  ul {
    list-style-type: none;
    padding: 0;
  }

  li {
    display: inline-block;
    margin: 0 10px;
  }

  a {
    color: #42b983;
  }
</style>
