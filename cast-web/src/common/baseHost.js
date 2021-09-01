let json = require('../assets/host.json');

function getHostname() {
    return "http://" + json.host + "/cast"
}

function getSocketHostname() {
    return "ws://" + json.host + "/websocket"
}

export default {
    getHost: function () {
        return getHostname()
    },
    getSocketHost: function () {
        return getSocketHostname()
    }
}
