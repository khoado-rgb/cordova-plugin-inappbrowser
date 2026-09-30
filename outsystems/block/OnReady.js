// Block MiniAppEvents > OnReady > JavaScript node
// Output: ListenerId (Text) - assign it to the block local variable ListenerId.
//
// Requires these block client actions, each with a single Trigger Event node:
//   TriggerOnLoaded(Url)            -> event OnLoaded(Url)
//   TriggerOnMessage(Type, Payload) -> event OnMessage(Type, PayloadJson)
//   TriggerOnTokenRequest()         -> event OnTokenRequest
//   TriggerOnClosed()               -> event OnClosed
//   TriggerOnError(Message)         -> event OnError(Message)

var id = 'miniapp_' + Date.now() + '_' + Math.random().toString(36).slice(2);

var handlers = {
    'miniapp:loaded': function (e) { $actions.TriggerOnLoaded(e.detail.url || ''); },
    'miniapp:message': function (e) { $actions.TriggerOnMessage(e.detail.type || '', e.detail.payloadJson || '{}'); },
    'miniapp:tokenrequest': function () { $actions.TriggerOnTokenRequest(); },
    'miniapp:closed': function () { $actions.TriggerOnClosed(); },
    'miniapp:error': function (e) { $actions.TriggerOnError(e.detail.message || ''); }
};

Object.keys(handlers).forEach(function (name) {
    document.addEventListener(name, handlers[name]);
});

window.__miniAppListeners = window.__miniAppListeners || {};
window.__miniAppListeners[id] = handlers;
$parameters.ListenerId = id;
