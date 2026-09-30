// Client action: MiniApp_Close
// No inputs. The MiniAppEvents block receives OnClosed once the browser is gone.

if (window.__miniApp) {
    window.__miniApp.close();
}
