/*
 * Mini app bridge for partner websites opened inside the host app.
 * Include this script on every page of the mini app.
 *
 *   MiniAppBridge.isInApp()                  -> true when running inside the host app
 *   MiniAppBridge.send('openPayment', {...}) -> send a message to the app
 *   MiniAppBridge.close({ result: 'ok' })    -> close the mini app, optionally with a result
 *   MiniAppBridge.onMessage(function (msg) { msg.type, msg.payload })
 *   MiniAppBridge.getToken()                 -> Promise of the user JWT handed over by the app
 *   MiniAppBridge.getToken({ refresh: true }) -> same, after the backend rejected the last one
 *
 * Only message types agreed with the host app are delivered; anything else is dropped.
 */
(function (w) {
    var TOKEN_TIMEOUT_MS = 30000;

    function nativeHandler() {
        // iOS: WKScriptMessageHandler. Android: JavascriptInterface, available before page load
        // finishes (window.webkit is only aliased on Android after onPageFinished).
        return (w.webkit && w.webkit.messageHandlers && w.webkit.messageHandlers.cordova_iab) || w.cordova_iab || null;
    }

    function send(type, payload) {
        var handler = nativeHandler();
        if (!handler) return false;
        // Must be a JSON string of an object: Android parses it with JSONObject.
        handler.postMessage(JSON.stringify({ type: String(type), payload: payload || {} }));
        return true;
    }

    var listeners = [];
    var tokenRequests = {};
    var tokenRequestCount = 0;

    function answerTokenRequest(payload) {
        var request = tokenRequests[payload.requestId];
        if (!request) return;
        delete tokenRequests[payload.requestId];
        clearTimeout(request.timer);
        if (payload.token) {
            request.resolve(payload.token);
        } else {
            request.reject(new Error('MiniAppBridge: token unavailable'));
        }
    }

    w.MiniAppBridge = {
        isInApp: function () {
            return !!nativeHandler();
        },
        send: send,
        close: function (result) {
            return send('close', result || {});
        },
        onMessage: function (fn) {
            if (typeof fn === 'function') listeners.push(fn);
        },
        getToken: function (options) {
            return new Promise(function (resolve, reject) {
                var id = 'token-' + Date.now().toString(36) + '-' + (++tokenRequestCount);
                var timer = setTimeout(function () {
                    delete tokenRequests[id];
                    reject(new Error('MiniAppBridge: getToken timed out'));
                }, TOKEN_TIMEOUT_MS);
                tokenRequests[id] = { resolve: resolve, reject: reject, timer: timer };
                if (!send('getToken', { requestId: id, refresh: !!(options && options.refresh) })) {
                    clearTimeout(timer);
                    delete tokenRequests[id];
                    reject(new Error('MiniAppBridge: not running inside the app'));
                }
            });
        }
    };

    // Called by the host app (MiniApp_PostToWeb, and the answers to getToken).
    w.onAppMessage = function (message) {
        if (message && message.type === 'token') {
            answerTokenRequest(message.payload || {});
            return;
        }
        listeners.forEach(function (fn) {
            try { fn(message); } catch (e) { if (w.console) console.error(e); }
        });
    };
})(window);
