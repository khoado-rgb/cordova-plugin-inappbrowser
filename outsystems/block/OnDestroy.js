// Block MiniAppEvents > OnDestroy > JavaScript node
// Input: ListenerId (Text) - the block local variable ListenerId.

var listeners = window.__miniAppListeners || {};
var handlers = listeners[$parameters.ListenerId];

if (handlers) {
    Object.keys(handlers).forEach(function (name) {
        document.removeEventListener(name, handlers[name]);
    });
    delete listeners[$parameters.ListenerId];
}
