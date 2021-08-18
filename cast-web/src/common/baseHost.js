
function getHostname() {
  return "http://localhost:8080/cast"
}

function getSocketHostname() {
  return "ws://localhost:8080/websocket"
}

export default {
  getHost: function () {
    return getHostname()
  },
  getSocketHost: function () {
    return getSocketHostname()
  }
}
