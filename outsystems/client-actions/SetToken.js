// Client action: MiniApp_SetToken
// Input:  Token (Text) - the JWT for the mini app. Empty = no token available: pending
//                        MiniAppBridge.getToken() calls in the web are rejected.
// Output: Success (Boolean) - false when no mini app is open.
//
// Call it from the OnTokenRequest handler of the MiniAppEvents block. It stores the token for
// the next getToken() calls and answers the ones that are waiting.

$parameters.Success = false;

var miniApp = window.__miniApp;
if (!miniApp) return;

miniApp.setToken($parameters.Token);
$parameters.Success = true;
