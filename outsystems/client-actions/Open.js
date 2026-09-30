// Client action: MiniApp_Open
// Inputs:  Url (Text, mandatory)
//          AllowedOrigins (Text) - comma separated, e.g. "https://a.partner.com,https://pay.partner.com".
//                                  Empty = only the origin of Url.
//          AllowedTypes (Text)   - comma separated message types the web may send, e.g. "openPayment,share".
//                                  "close" and "getToken" are always allowed.
//          CloseButtonText (Text, default "Đóng") - must not contain commas.
//          StatusBarColor (Text) - any CSS color. Empty = the app primary color (--color-primary).
//          ToolbarColor (Text)   - any CSS color. Empty = same as the status bar.
//          AuthToken (Text)      - JWT handed to the web on MiniAppBridge.getToken().
//                                  Empty = asked for through OnTokenRequest on the first getToken().
// Output:  IsOpened (Boolean)
//
// Results are delivered as DOM events on `document`, picked up by the MiniAppEvents block:
//   miniapp:loaded       {url}
//   miniapp:message      {type, payloadJson}
//   miniapp:tokenrequest {}  - the web needs a (new) token, answer with MiniApp_SetToken
//   miniapp:closed       {}
//   miniapp:error        {message}

function emit(name, detail) {
    document.dispatchEvent(new CustomEvent('miniapp:' + name, { detail: detail || {} }));
}

function splitList(text) {
    return (text || '').split(',').map(function (s) { return s.trim(); }).filter(Boolean);
}

function originOf(url) {
    try { return new URL(url).origin; } catch (e) { return ''; }
}

// Any CSS color (hex, rgb(), hsl(), name) as #RRGGBB, the only format the native side reads.
// Returns '' when the color is empty or invalid.
function toHex(color) {
    var probe = document.createElement('span');
    probe.style.color = (color || '').trim();
    if (!probe.style.color) return '';
    document.body.appendChild(probe);
    var rgb = getComputedStyle(probe).color.match(/^rgba?\(([\d.]+),\s*([\d.]+),\s*([\d.]+)/);
    document.body.removeChild(probe);
    if (!rgb) return '';
    return '#' + rgb.slice(1, 4).map(function (c) {
        return ('0' + Math.round(Number(c)).toString(16)).slice(-2);
    }).join('').toUpperCase();
}

// True when white text and icons get less than 3:1 contrast on this color (WCAG minimum for
// icons and large text), so they must be dark. Brand reds and oranges keep white.
function isLight(hex) {
    var n = parseInt(hex.slice(1), 16);
    var l = [n >> 16, (n >> 8) & 255, n & 255].map(function (c) {
        c /= 255;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    });
    var luminance = 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
    return 1.05 / (luminance + 0.05) < 3;
}

$parameters.IsOpened = false;

if (!(window.cordova && cordova.InAppBrowser)) {
    emit('error', { message: 'InAppBrowser plugin is not available' });
    return;
}
if (window.__miniApp) {
    emit('error', { message: 'A mini app is already open' });
    return;
}

var url = $parameters.Url;
var allowedOrigins = splitList($parameters.AllowedOrigins);
if (allowedOrigins.length === 0) {
    allowedOrigins = [originOf(url)];
}
var allowedTypes = splitList($parameters.AllowedTypes);
var currentUrl = url;

// The status bar follows the app primary color. Without one, the device light/dark mode.
var dark = !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
var primaryColor = toHex(getComputedStyle(document.documentElement).getPropertyValue('--color-primary'));
var statusBarColor = toHex($parameters.StatusBarColor) || primaryColor || (dark ? '#1C1C1E' : '#FFFFFF');
var toolbarColor = toHex($parameters.ToolbarColor) || statusBarColor;
// Buttons: white on a dark toolbar; on a light one the primary color when it is dark enough.
var buttonColor = !isLight(toolbarColor) ? '#FFFFFF'
    : (primaryColor && !isLight(primaryColor) ? primaryColor : '#1C1C1E');
var closeText = ($parameters.CloseButtonText || 'Đóng').replace(/[,=]/g, ' ');

var common = 'toolbarcolor=' + toolbarColor +
    ',statusbarcolor=' + statusBarColor +
    ',statusbarstyle=' + (isLight(statusBarColor) ? 'darkcontent' : 'lightcontent') +
    ',closebuttoncolor=' + buttonColor +
    ',navigationbuttoncolor=' + buttonColor +
    ',closebuttoncaption=' + closeText +
    ',hidenavigationbuttons=yes';

var options = cordova.platformId === 'android'
    // location=yes is required for toolbarcolor; hideurlbar hides the URL but keeps the toolbar.
    // fullscreen=no keeps the app status bar visible (default hides it).
    ? 'location=yes,hideurlbar=yes,fullscreen=no,zoom=no,hardwareback=yes,' + common
    : 'location=no,toolbar=yes,toolbarposition=top,toolbartranslucent=no,presentationstyle=fullscreen,' + common;

var ref = cordova.InAppBrowser.open(url, '_blank', options);

// Delivers {type, payload} to window.onAppMessage in the mini app (see partner/miniapp-bridge.js).
// The origin check runs inside the page when the code executes, so nothing reaches a page that
// navigated to another origin in the meantime. Never pass a callback to executeScript: the plugin
// then wraps the code in eval(), which the page can override to read it (and the token in it).
function post(type, payload) {
    var guard = allowedOrigins.map(function (o) { return 'o===' + JSON.stringify(o); }).join('||');
    var message = JSON.stringify({ type: type, payload: payload || {} })
        .replace(/\u2028/g, '\\u2028').replace(/\u2029/g, '\\u2029');
    ref.executeScript({
        code: '(function(){var o=location.origin;if(!(' + guard + '))return;' +
            'try{window.onAppMessage(' + message + ')}catch(e){}})();'
    });
}

// JWT for the web, requested with MiniAppBridge.getToken().
var token = $parameters.AuthToken || '';
var tokenWaiting = []; // requestIds waiting for MiniApp_SetToken
var tokenRequestedAt = 0; // when OnTokenRequest was raised for them

function answerToken(requestId) {
    post('token', token ? { requestId: requestId, token: token } : { requestId: requestId, error: 'unavailable' });
}

function onTokenRequest(payload) {
    var requestId = typeof payload.requestId === 'string' ? payload.requestId : '';
    if (token && !payload.refresh) {
        answerToken(requestId);
        return;
    }
    if (tokenWaiting.length < 20) {
        tokenWaiting.push(requestId);
    }
    // One OnTokenRequest per refresh; raised again only if the app has not answered within 30 s.
    if (Date.now() - tokenRequestedAt > 30000) {
        tokenRequestedAt = Date.now();
        emit('tokenrequest');
    }
}

function setToken(newToken) {
    token = newToken || '';
    tokenRequestedAt = 0;
    tokenWaiting.splice(0).forEach(answerToken);
}

// Used by MiniApp_Close, MiniApp_PostToWeb and MiniApp_SetToken.
window.__miniApp = {
    close: function () { ref.close(); },
    post: post,
    setToken: setToken
};
$parameters.IsOpened = true;

ref.addEventListener('loadstart', function (e) { currentUrl = e.url; });
ref.addEventListener('loadstop', function (e) {
    currentUrl = e.url;
    emit('loaded', { url: e.url });
});
ref.addEventListener('loaderror', function (e) {
    emit('error', { message: (e.message || 'loaderror') + ' (' + e.url + ')' });
});

ref.addEventListener('message', function (e) {
    // Only accept messages while the main frame is on a trusted origin.
    if (allowedOrigins.indexOf(originOf(currentUrl)) === -1) return;

    var data = e.data || {};
    var type = typeof data.type === 'string' ? data.type : '';
    if (type === 'getToken') {
        onTokenRequest(data.payload || {});
        return;
    }
    if (type !== 'close' && allowedTypes.indexOf(type) === -1) return;

    emit('message', { type: type, payloadJson: JSON.stringify(data.payload || {}) });
    if (type === 'close') {
        ref.close();
    }
});

ref.addEventListener('exit', function () {
    window.__miniApp = null;
    token = '';
    tokenWaiting = [];
    emit('closed');
});
