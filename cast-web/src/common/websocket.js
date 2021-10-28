import baseHost from './baseHost.js'

let connect = {}
let uid = uuid()

function createWebsocket(pingCode, obj, callback) {
    if ('WebSocket' in window) {
        let url = baseHost.getSocketHost() + '/' + uid + "/" + pingCode + "/web"
        let ws = new WebSocket(url)
        connect.uid = uid
        connect.ws = ws
        connect.pingCode = pingCode
        ws.onopen = function () {
            console.log('成功建立websocket连接')
            callback.call(obj, {connected: true, message: "连接成功"})
        }
        ws.onmessage = function (message) {
            console.log("收到消息：" + message.data)
            handleReceivedMessage(JSON.parse(message.data), obj, callback)
        }
        ws.onerror = function () {
            callback.call(obj, {connected: false, message: "连接失败请检查你的网络或服务器发生错误"})
        }
        ws.onclose = function () {
            console.log('关闭了连接')
            ws = null
        }
        return connect
    }
}

function handleReceivedMessage(message, obj, callback) {
    switch (message.head.type) {
        case MessageType.REFUSE:
            closeConnect(false, "设备拒绝投屏", obj, callback)
            break;
        case MessageType.READY:
            connect.pc.createOffer().then(function (des) {
                connect.pc.setLocalDescription(des).then(function () {
                    sendMessage(MessageType.DESC, connect.pc.localDescription)
                })
            })
            break;
        case MessageType.CLOSE:
            closeConnect(false, message.body.message, obj, callback)
            break;
        case MessageType.DESC:
            if (message.body.type === "answer") {
                connect.pc.setRemoteDescription(new RTCSessionDescription(message.body), function () {
                })
            }
            break;
        case MessageType.CANDIDATE:
            connect.pc.addIceCandidate(message.body).then(function () {
            })
            break;
        default:
            break;
    }
}

function createPeerConnection(obj, callback) {
    let rtcConfig = {
        iceServers: [
            {'urls': 'stun:stun.ekiga.net'},
            {'urls': 'stun:stun.fwdnet.net'}
        ]
    }
    let pc = new RTCPeerConnection(rtcConfig)
    connect.pc = pc
    pc.onicecandidate = function (candidate) {
        if (candidate.candidate) {
            sendMessage(MessageType.CANDIDATE, candidate.candidate)
        }
    }
    pc.onnegotiationneeded = function () {
    }

    pc.ontrack = function () {
    }

    let browser = getBrowserInfo()
    let hasAudioOut = false
    let source = "desktop"
    let constraints
    if (browser.browser === 'chrome') {
        if (source === "desktop") {
            navigator.mediaDevices.enumerateDevices()
                .then(function (devices) {
                    devices.forEach(function (deviceInfo) {
                        if (deviceInfo.kind === 'audiooutput') {//检测是否有声音输出设备
                            hasAudioOut = true
                        }
                    })
                })
            chrome.runtime.sendMessage(sessionStorage.getScreenMediaJSExtensionId, {
                type: 'getScreen',
                options: ['screen', 'window', 'tab', 'audio']
            }, function (message) {
                constraints = {
                    video: {
                        mandatory: {
                            chromeMediaSource: 'desktop',
                            maxWidth: 1920,
                            maxHeight: 1080,
                            maxFrameRate: 30,
                            sourceId: message.sourceId
                        }
                    }
                }
                if (hasAudioOut) {
                    constraints.audio = {
                        mandatory: {
                            chromeMediaSource: 'desktop',
                            sourceId: message.sourceId
                        }
                    }
                }
                if (!message.sourceId) {
                    closeConnect(false, "获取媒体源失败", obj, callback)
                    return ''
                }
                createOffer(constraints, pc, obj, callback)
            })
        } else {
            constraints = window.constraints = {
                audio: true,
                video: true
            }
            createOffer(constraints, pc, obj, callback)
        }
    } else {
        closeConnect(false, "不支持当前浏览器，请使用Chrome浏览器", obj, callback)
    }
}

function createOffer(constraints, pc, obj, callback) {
    if (navigator.mediaDevices) {
        navigator.mediaDevices.getUserMedia(constraints)
            .then(function (stream) {
                connect.stream = stream
                stream.getVideoTracks()[0].onended = function () {
                    closeConnect(true, "关闭投屏", obj, callback)
                }
                // 发送投屏请求
                sendMessage(MessageType.OFFER, {confirm: true})
                stream.getTracks().forEach(function (track) {
                    pc.addTrack(track, stream)
                })
            })
    }
}

function createMessageHead(from, to, type) {
    return {from: from, to: to, type: type}
}

function sendMessage(type, body) {
    let head = createMessageHead(connect.uid, connect.pingCode, type)
    let message = JSON.stringify({head: head, body: body})
    console.log("发送消息：" + message)
    connect.ws.send(message)
}

window.onbeforeunload = function () {
    if (connect.ws) {
        connect.ws.close()
    }
    connect = {}
}

window.onunload = function () {
    if (connect.ws) {
        connect.ws.close()
    }
    connect = {}
}

function closeConnect(send, message, obj, callback) {
    if (connect) {
        if (connect.stream) {
            connect.stream.stop()
        }
        if (send) {
            sendMessage(MessageType.CLOSE, {message: message})
        }
        connect.ws.close()
        connect.ws = null
        console.log('WebSocket closing...')
        callback.call(obj, {closed: true, message: message, connect: connect})
    }
}

function getBrowserInfo() {
    let Sys = {}
    let ua = navigator.userAgent.toLowerCase()
    let re = /(msie|firefox|chrome|opera|version).*?([\d.]+)/
    let m = ua.match(re)
    Sys.browser = m[1].replace(/version/, '\'safari')
    Sys.ver = m[2]
    return Sys
}

function uuid(len, radix) {
    var chars = '0123456789abcdefghijklmnopqrstuvwxyz'.split('')
    let uuid = []
    let i
    radix = radix || chars.length
    if (len) {
        for (i = 0; i < len; i++) uuid[i] = chars[0 | Math.random() * radix]
    } else {
        let r;
        uuid[8] = uuid[13] = uuid[18] = uuid[23] = '-'
        uuid[14] = '4'
        for (i = 0; i < 36; i++) {
            if (!uuid[i]) {
                r = 0 | Math.random() * 16
                uuid[i] = chars[(i === 19) ? (r & 0x3) | 0x8 : r]
            }
        }
    }
    return uuid.join('')
}

const MessageType = {
    OFFER: "OFFER", //投屏请求
    READY: "READY", //接受投屏
    REFUSE: "REFUSE", //拒绝投屏
    CLOSE: "CLOSE", //关闭投屏
    DESC: "DESC",
    CANDIDATE: "CANDIDATE",
}

Object.freeze(MessageType);

MediaStream.prototype.stop = function () {
    this.getTracks().forEach(function (track) {
        track.stop()
    })
}

export default {
    createWebsocket: function (pingCode, obj, callback) {
        return createWebsocket(pingCode, obj, callback)
    },
    createPeerConnection: function (obj, callback) {
        return createPeerConnection(obj, callback)
    }
}
