// Client action: MiniApp_PostToWeb
// Inputs:  Type (Text, mandatory)
//          PayloadJson (Text, default "{}") - e.g. JSONSerialize(SomeStructure).JSON
// Output:  Success (Boolean)
//
// Calls window.onAppMessage({type, payload}) inside the mini app page
// (defined by partner/miniapp-bridge.js), only while it is on one of the AllowedOrigins.

$parameters.Success = false;

var miniApp = window.__miniApp;
if (!miniApp) return;

var payload;
try {
    payload = JSON.parse($parameters.PayloadJson || '{}');
} catch (e) {
    return;
}

miniApp.post($parameters.Type, payload);
$parameters.Success = true;
