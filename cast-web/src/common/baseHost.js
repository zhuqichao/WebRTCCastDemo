import HOST from "../assets/host";

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
