// Client action: MiniApp_Open
// Inputs:  Url (Text, mandatory) - must be https.
//          AllowedOrigins (Text) - comma separated https origins, e.g. "https://a.partner.com,https://pay.partner.com".
//                                  Empty = only the origin of Url. Pages on these origins may exchange
//                                  messages with the app, get the token and ask for the camera,
//                                  microphone and location (the user is asked first).
//          AllowedTypes (Text)   - comma separated message types the web may send, e.g. "openPayment,share".
//                                  "close" and "getToken" are always allowed.
//          Title (Text)          - shown on the left of the toolbar. Empty = no title.
//          CloseButtonText (Text, default "Đóng") - must not contain commas.
//          CloseButtonIcon (Boolean, default False) - X icon instead of the text; the text then
//                                  only names the button for VoiceOver/TalkBack.
//          CloseButtonSize (Integer or Text) - size of the X, or font size of the text, in CSS px
//                                  (dp/pt): 24 or "24px", kept between 8 and 40. Empty or 0 = default
//                                  (X 18; text 17 on iOS, 20 on Android).
//          StatusBarColor (Text) - any CSS color. Empty = the app primary color (--color-primary).
//          ToolbarColor (Text)   - any CSS color. Empty = same as the status bar.
//          ToolbarHeight (Integer or Text) - in CSS px, which are dp on Android and pt on iOS:
//                                  56, "56", "56px", "3.5rem", "var(--header-size)", or a CSS class
//                                  whose declared height to use, with its dot: ".header-top".
//                                  At least 44, the height the iOS toolbar buttons need.
//                                  Empty or 0 = the app header height (--header-size), else the plugin default.
//          AuthToken (Text)      - JWT handed to the web on MiniAppBridge.getToken().
//                                  Empty = asked for through OnTokenRequest on the first getToken().
// Output:  IsOpened (Boolean)
//
// Results are delivered as DOM events on `document`, picked up by the MiniAppEvents block:
//   miniapp:loaded       {url}   - URLs in events have no #fragment or user:password@, and the values
//                                  of token-like query parameters (code, token, access_token, ...) are hidden
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

// The origin of an https URL; '' for anything else (http, data:, file:, invalid).
function httpsOrigin(url) {
    try {
        var parsed = new URL(url);
        return parsed.protocol === 'https:' ? parsed.origin : '';
    } catch (e) {
        return '';
    }
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

// A CSS length (px, rem, calc(), ...) in px. Returns 0 when it is empty or invalid.
function toPx(length) {
    var probe = document.createElement('div');
    probe.style.position = 'absolute';
    probe.style.visibility = 'hidden';
    probe.style.height = (length || '').trim();
    if (!probe.style.height) return 0;
    document.body.appendChild(probe);
    var px = parseFloat(getComputedStyle(probe).height) || 0;
    document.body.removeChild(probe);
    return px;
}

// A height given as a number (56) or as text ("56", "56px", "3.5rem"), in px. 0 when empty or invalid.
function heightPx(value) {
    if (typeof value === 'number') return value;
    var text = String(value || '').trim();
    return /^\d+(\.\d+)?$/.test(text) ? Number(text) : toPx(text);
}

// The height declared for these CSS classes in the app stylesheets, in px: "header-top" reads
// `.header-top { height: var(--header-size) }`. Only rules whose selector is exactly the classes
// count (also inside a selector list); the last one wins, and rules in a @media that does not
// match are skipped. Returns 0 when the classes are empty or invalid, or no such rule sets a height.
function classHeight(classes) {
    var names = (classes || '').split(/[\s.]+/).filter(Boolean);
    if (names.length === 0 || !names.every(function (n) { return /^[\w-]+$/.test(n); })) return 0;
    var selector = '.' + names.join('.');
    var height = '';

    function scan(rules) {
        for (var i = 0; i < rules.length; i++) {
            var rule = rules[i];
            if (rule.media && !window.matchMedia(rule.media.mediaText).matches) continue;
            if (rule.selectorText && rule.style.height &&
                    rule.selectorText.split(',').some(function (s) { return s.trim() === selector; })) {
                height = rule.style.height;
            }
            if (rule.styleSheet) scanSheet(rule.styleSheet); // @import
            if (rule.cssRules) scan(rule.cssRules); // @media, @supports, @layer
        }
    }

    function scanSheet(sheet) {
        try {
            if (!sheet.disabled) scan(sheet.cssRules);
        } catch (e) {
            // Stylesheet from another origin: its rules cannot be read.
        }
    }

    for (var i = 0; i < document.styleSheets.length; i++) {
        scanSheet(document.styleSheets[i]);
    }
    return toPx(height);
}

// For the events the app may log: no fragment and no user:password@, and the values of query
// parameters that look like credentials (OAuth code, tokens, passwords, signatures) replaced. The
// rest of the URL is kept as is.
var SENSITIVE_PARAM = /token|secret|password|passwd|session|signature|^(code|jwt|sig|otp|key|api_?key|auth)$/i;
function redact(url) {
    try {
        var parsed = new URL(url);
        parsed.hash = '';
        parsed.username = '';
        parsed.password = '';
        var sensitive = [];
        parsed.searchParams.forEach(function (value, name) {
            if (SENSITIVE_PARAM.test(name) && sensitive.indexOf(name) === -1) sensitive.push(name);
        });
        sensitive.forEach(function (name) { parsed.searchParams.set(name, 'hidden'); });
        return parsed.href;
    } catch (e) {
        return '';
    }
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

// Only https pages get the token and the device permissions.
var url = $parameters.Url;
if (!httpsOrigin(url)) {
    emit('error', { message: 'Url must be an https URL' });
    return;
}
var allowedOrigins = [];
var invalidOrigins = [];
splitList($parameters.AllowedOrigins).forEach(function (entry) {
    var origin = httpsOrigin(entry);
    if (origin) {
        allowedOrigins.push(origin);
    } else {
        invalidOrigins.push(entry);
    }
});
if (invalidOrigins.length > 0) {
    emit('error', { message: 'AllowedOrigins must be https origins: ' + invalidOrigins.join(' ') });
    return;
}
if (allowedOrigins.length === 0) {
    allowedOrigins = [httpsOrigin(url)];
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
// Commas and = separate the options, so they cannot be part of a value.
var title = ($parameters.Title || '').replace(/[,=]/g, ' ').trim();
// Same height as the app header: given (a length, or the height declared for a ".class"), or
// OutSystems UI --header-size.
var heightText = String($parameters.ToolbarHeight || '').trim();
var givenHeight = heightText.charAt(0) === '.' ? classHeight(heightText) : heightPx($parameters.ToolbarHeight);
var toolbarHeight = Math.round(givenHeight > 0 ? givenHeight
    : toPx(getComputedStyle(document.documentElement).getPropertyValue('--header-size')));
if (toolbarHeight > 0 && toolbarHeight < 44) {
    toolbarHeight = 44;
}
// Kept small enough for the toolbar.
var closeButtonSize = Math.round(heightPx($parameters.CloseButtonSize));
if (closeButtonSize > 0) {
    closeButtonSize = Math.min(Math.max(closeButtonSize, 8), 40);
}

var common = 'toolbarcolor=' + toolbarColor +
    (toolbarHeight > 0 ? ',toolbarheight=' + toolbarHeight : '') +
    ',statusbarcolor=' + statusBarColor +
    ',statusbarstyle=' + (isLight(statusBarColor) ? 'darkcontent' : 'lightcontent') +
    ',closebuttoncolor=' + buttonColor +
    ',navigationbuttoncolor=' + buttonColor +
    ',closebuttoncaption=' + closeText +
    (title ? ',toolbartitle=' + title : '') +
    ($parameters.CloseButtonIcon ? ',closebuttonicon=yes' : '') +
    (closeButtonSize > 0 ? ',closebuttonsize=' + closeButtonSize : '') +
    ',permissionorigins=' + allowedOrigins.join('|') +
    // The page itself stays on https: http navigations of the main frame are blocked.
    ',httpsonly=yes' +
    ',hidenavigationbuttons=yes';

// The close button goes on the right: the Android default, lefttoright=yes on iOS
// (on Android lefttoright=yes would move it to the left instead).
var options = cordova.platformId === 'android'
    // location=yes is required for toolbarcolor; hideurlbar hides the URL but keeps the toolbar.
    // fullscreen=no keeps the app status bar visible (default hides it).
    // shouldPauseOnSuspend pauses what the WebView can (animations, location) while the app is in the background.
    ? 'location=yes,hideurlbar=yes,fullscreen=no,zoom=no,hardwareback=yes,shouldPauseOnSuspend=yes,' + common
    : 'location=no,toolbar=yes,toolbarposition=top,toolbartranslucent=no,presentationstyle=fullscreen,lefttoright=yes,' + common;

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
    emit('loaded', { url: redact(e.url) });
});
ref.addEventListener('loaderror', function (e) {
    emit('error', { message: (e.message || 'loaderror') + ' (' + redact(e.url) + ')' });
});

ref.addEventListener('message', function (e) {
    // Only accept messages from a page on a trusted origin. The plugin sends the origin of the page
    // that posted it (iOS, and Android when the WebView supports WebMessageListener); otherwise the last URL.
    var sender = typeof e.origin === 'string' ? e.origin : originOf(currentUrl);
    if (allowedOrigins.indexOf(sender) === -1) return;

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
