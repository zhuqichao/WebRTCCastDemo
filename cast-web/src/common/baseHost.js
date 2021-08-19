const HOST = "10.4.152.66:8080"

function getHostname() {
  return "http://" + HOST + "/cast"
}

function getSocketHostname() {
  return "ws://" + HOST + "/websocket"
}

export default {
  getHost: function () {
    return getHostname()
  },
  getSocketHost: function () {
    return getSocketHostname()
  }
}
