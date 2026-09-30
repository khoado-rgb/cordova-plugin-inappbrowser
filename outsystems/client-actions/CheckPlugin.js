// Client action: MiniApp_IsAvailable
// Output: IsAvailable (Boolean)

$parameters.IsAvailable = !!(window.cordova && cordova.InAppBrowser);
