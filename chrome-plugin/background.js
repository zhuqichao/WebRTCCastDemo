/* background page, responsible for actually choosing media */
var closedback;
var obj;
chrome.runtime.onMessageExternal.addListener(function (message, sender, callback) {
    switch (message.type) {
        case 'getScreen':
            var pending = chrome.desktopCapture.chooseDesktopMedia(message.options || ['screen', 'window'], sender.tab, function (streamid) {
                // communicate this string to the app so it can call getUserMedia with it
                message.type = 'getScreen';
                message.sourceId = streamid;
                console.log(sender.tab)
                closedback = message.closedback;
                obj = message.obj;
                message.tab = sender.tab;
                message.requestId = pending;
                callback(message);
                return false;
            });
            return true; // retain callback for chooseDesktopMedia result
        case 'cancelGetScreen':
            chrome.desktopCapture.cancelChooseDesktopMedia(message.request);
            message.type = 'cancelGetScreen';
            callback(message);
            return true; //dispose callback
    }
});

chrome.browserAction.onClicked.addListener(function (tab) {
    var newURL = "http://localhost:8089/";
    chrome.tabs.create({url: newURL});
});
