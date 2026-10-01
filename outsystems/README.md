# Mini app browser cho OutSystems (MABS)

Bản fork của [apache/cordova-plugin-inappbrowser](https://github.com/apache/cordova-plugin-inappbrowser) tại nhánh `master` (7.0.1-dev), dùng để mở website của đối tác như một mini app trong app mobile OutSystems. Kèm theo là code cho module wrapper trong OutSystems và script cho phía đối tác.

## Các thay đổi so với bản gốc

Bản hiện tại: `7.0.1-os.4`. Chi tiết từng bản ở [RELEASENOTES.md](../RELEASENOTES.md).

### Option mới của `cordova.InAppBrowser.open`

| Option | Nền tảng | Ý nghĩa |
|---|---|---|
| `statusbarcolor=#RRGGBB` | iOS, Android | Màu nền vùng status bar. |
| `statusbarstyle=lightcontent\|darkcontent` | iOS, Android | Màu chữ và icon status bar. Trên iOS, option này được ưu tiên hơn preference `InAppBrowserStatusBarStyle`. |
| `closebuttonicon=yes` | iOS, Android | Nút Đóng là icon X. Hai nền tảng dùng chung một icon (khung 32pt/dp, chữ X 18pt/dp). `closebuttoncaption` khi đó chỉ là nhãn cho VoiceOver/TalkBack. |
| `toolbarheight=<số>` | iOS, Android | Chiều cao toolbar, tính bằng dp trên Android và pt trên iOS, không gồm safe area. Trên iOS, toolbar không thấp hơn chiều cao thật của thanh. |
| `permissionorigins=<origin>\|<origin>` | iOS, Android | Các origin được xin quyền camera, micro và vị trí. Không truyền thì mọi origin đều được xin, nhưng luôn phải được user đồng ý. |

Wrapper OutSystems tự truyền các option này. Khi gọi plugin trực tiếp thì truyền tay.

### Sửa lỗi và thay đổi hành vi

**Android:**

- **Android 15 trở lên (targetSdk 35):** Android ép cửa sổ InAppBrowser vẽ tràn viền (edge-to-edge). Ở bản gốc, toolbar bị status bar đè, còn web bị thanh điều hướng và bàn phím che. Bản fork chừa lề theo system bar và bàn phím, rồi tô vùng status bar bằng `statusbarcolor`, không có thì dùng màu toolbar. Từ Android 14 trở xuống, màu được đặt bằng `setStatusBarColor`.
- **Android 11:** màu icon status bar được đặt bằng cả `WindowInsetsController` lẫn cờ kiểu cũ, vì trên một số máy Android 11 chỉ một cách là không đủ.
- **Quyền camera, micro và vị trí:** bản gốc tự cấp mọi quyền cho mọi trang. Bản fork chỉ cho trang thuộc `permissionorigins` xin quyền, rồi hiện hộp thoại "partner.com muốn dùng camera của bạn" để user quyết định.
  - Nếu app chưa có quyền runtime của Android thì hệ thống hỏi tiếp.
  - Mỗi origin chỉ bị hỏi một lần cho mỗi loại quyền, cho tới khi đóng mini app.
  - Chỉ camera, micro và vị trí được cấp. Protected media và MIDI luôn bị từ chối.
  - `InAppChromeClient` mặc định từ chối.
- **Bộ nhớ:** WebView được destroy khi đóng và khi mở lại mini app. WebView tạm của `window.open` được destroy ngay sau khi điều hướng đã chuyển về WebView chính. Bản gốc giữ chúng tới khi GC chạy.
- **Đóng nhầm mini app mới mở:** ở bản gốc, nếu trang `about:blank` (tải lúc đóng) xong muộn, nó có thể đóng nhầm mini app vừa được mở lại. Lỗi này đã được sửa.
- Plugin yêu cầu cordova-android ≥ 10.0.0, vì code dùng API 30 (`WindowInsets.Type`).

**iOS:**

- **Vùng status bar:** khi không truyền `statusbarcolor`, vùng này được tô cùng màu toolbar (nếu toolbar ở trên), hoặc theo màu nền hệ thống. Bản gốc để trống vùng này, nên status bar thường hiện nền đen.
- **Nút Đóng dạng icon:** dùng chung icon với Android, nên kích thước giống nhau. Trên iOS 26, nút không có nền kính dạng viên thuốc. Ở bản gốc, iOS dưới 26 luôn hiện chữ ("Done" hoặc caption).
- **Chiều cao toolbar:** nếu `toolbarheight` thấp hơn chiều cao thật của thanh (44pt, cao hơn trên iOS 26), vùng toolbar tự giãn ra, nên nút không tràn ra ngoài.
- **WebContent process bị kill** (thường do thiếu bộ nhớ): plugin báo `loaderror` rồi tự tải lại trang. Nếu process lại bị kill trong vòng 10 giây thì không tải lại nữa, để tránh vòng lặp. Bản gốc chỉ để trang trắng.
- **Option dạng chuỗi** (caption, màu, ...) luôn được giữ là chuỗi. Ở bản gốc, `closebuttoncaption=1` hay `closebuttoncaption=No` làm app crash, vì giá trị bị đọc thành số hoặc Boolean. Cũng nhờ vậy `beforeload=no` giờ được hiểu đúng là tắt.
- **Quyền camera và micro (iOS 15 trở lên):** chỉ được xin khi trang thuộc `permissionorigins`, sau đó WebKit tự hỏi user. Với quyền vị trí, và với iOS dưới 15, không có API để lọc theo origin, nên WebKit hỏi user như bình thường.
- **Message và log:**
  - Chỉ nhận message từ trang chính, bỏ qua message từ iframe.
  - Không ghi script hay kết quả của `executeScript` vào log, vì script giao token có chứa JWT.
  - Message handler được gỡ đúng cách khi đóng.

### Wrapper OutSystems (`outsystems/`)

- **Màu:** lúc mở mini app, status bar và toolbar lấy màu primary của app (biến CSS `--color-primary` của OutSystems UI). Chữ và icon status bar, cùng nút Đóng, dùng màu trắng nếu đạt độ tương phản ít nhất 3:1, không thì dùng màu tối. Nếu app không khai báo `--color-primary`, màu theo chế độ Sáng/Tối của máy.
- **Chiều cao toolbar:** chọn theo thứ tự `ToolbarHeight`, rồi `ToolbarHeightClass`, rồi `--header-size`.
  - `ToolbarHeight` nhận số hoặc chuỗi (`56`, `"56px"`, `"3.5rem"`), tối thiểu là 44.
  - `ToolbarHeightClass` đọc giá trị `height` được khai báo cho class trong CSS.
- **Nút Đóng:** nằm bên phải trên cả hai nền tảng. `CloseButtonIcon` để dùng icon X thay cho chữ.
- **JWT:** web lấy token bằng `MiniAppBridge.getToken()`. App trả token qua `AuthToken`, hoặc qua `OnTokenRequest` và `MiniApp_SetToken`. Origin của trang được kiểm tra ngay lúc giao token.
- **HTTPS:** `Url` và `AllowedOrigins` bắt buộc là HTTPS. `AllowedOrigins` được quy về origin, và cũng là danh sách origin được xin quyền thiết bị.

### Đã kiểm tra

- Khai báo engine khớp với MABS: cordova-ios 7.1.1 và cordova-android 14.0.1.
- Code Android compile với Android SDK 35 và 36.
- Code iOS tới bản `7.0.1-os.3` compile với header cordova-ios 7.1.1 (target iOS 11 và 15).
- **Phần iOS sửa trong `7.0.1-os.4` chưa được compile trên máy phát triển**, cần xác nhận qua bản build MABS.

## 1. Publish plugin

Plugin được publish từ repo [khoado-rgb/cordova-plugin-inappbrowser](https://github.com/khoado-rgb/cordova-plugin-inappbrowser) (remote `origin`). Sau khi commit, push nhánh và tạo tag cho bản phát hành:

```sh
git push origin master
git tag 7.0.1-os.4
git push origin 7.0.1-os.4
```

MABS lấy plugin theo tag, nên mỗi lần sửa plugin phải tạo tag mới (`7.0.1-os.5`, ...) và cập nhật URL trong Extensibility Configurations. Không sửa lại một tag đã dùng để build.

Nếu repo để private, MABS phải có quyền đọc repo. Khi đó dùng URL có token, hoặc để repo public.

## 2. Module `MiniAppBrowser` (Mobile Library)

### 2.1 Extensibility Configurations

```json
{
  "plugin": {
    "url": "https://github.com/khoado-rgb/cordova-plugin-inappbrowser.git#7.0.1-os.4"
  }
}
```

- Không cần khai báo `InAppBrowserStatusBarStyle`. Wrapper truyền `statusbarstyle` mỗi lần mở.
- Plugin này chạy song song được với InAppBrowser Plugin trên Forge (`com.outsystems.plugins.inappbrowser`), vì hai plugin khác ID và khác namespace JS.

### 2.2 Client actions (Public = Yes)

Mỗi action chỉ gồm một JavaScript node. Copy code từ file tương ứng vào node, rồi khai báo input/output **trên JS node** đúng tên như trong bảng. Sau đó map với input/output của action.

| Action | Input | Output | Code |
|---|---|---|---|
| `MiniApp_IsAvailable` | – | `IsAvailable` (Boolean) | [client-actions/CheckPlugin.js](client-actions/CheckPlugin.js) |
| `MiniApp_Open` | `Url` (Text, bắt buộc, HTTPS), `AllowedOrigins` (Text), `AllowedTypes` (Text), `CloseButtonText` (Text, mặc định `"Đóng"`), `CloseButtonIcon` (Boolean, mặc định `False`), `StatusBarColor` (Text), `ToolbarColor` (Text), `ToolbarHeight` (Integer hoặc Text), `ToolbarHeightClass` (Text), `AuthToken` (Text) | `IsOpened` (Boolean) | [client-actions/Open.js](client-actions/Open.js) |
| `MiniApp_Close` | – | – | [client-actions/Close.js](client-actions/Close.js) |
| `MiniApp_PostToWeb` | `Type` (Text, bắt buộc), `PayloadJson` (Text, mặc định `"{}"`) | `Success` (Boolean) | [client-actions/PostToWeb.js](client-actions/PostToWeb.js) |
| `MiniApp_SetToken` | `Token` (Text) | `Success` (Boolean) | [client-actions/SetToken.js](client-actions/SetToken.js) |

Ý nghĩa các input của `MiniApp_Open`:

- `Url`: bắt buộc là `https://`. Với `http:`, `data:`, `file:` hay URL sai, mini app không mở và `OnError` được bắn.
- `AllowedOrigins`: danh sách origin HTTPS, cách nhau bằng dấu phẩy. Để trống thì chỉ gồm origin của `Url`. Cần khai báo thêm nếu web đối tác redirect sang domain khác.
  - Mỗi mục được quy về origin, nên `https://partner.com/`, `https://partner.com/app` hay `https://Partner.com` đều hiểu là `https://partner.com`.
  - Nếu có mục không phải HTTPS (ví dụ `http://...`), mini app không mở và `OnError` liệt kê các mục sai.
  - Danh sách này dùng cho ba việc:
    - Message từ web chỉ được nhận khi trang chính đang ở một origin trong danh sách.
    - Message từ app (`MiniApp_PostToWeb`, token) chỉ được giao khi trang chính đang ở một origin trong danh sách.
    - Chỉ trang ở một origin trong danh sách mới được xin quyền camera, micro và vị trí, và luôn phải được user đồng ý.
- `AllowedTypes`: danh sách `type` mà web được phép gửi, ví dụ `openPayment,share`. Type `close` và `getToken` luôn được chấp nhận. Message có type ngoài danh sách sẽ bị bỏ qua.
- `CloseButtonText`: không được chứa dấu phẩy.
- Nút Đóng nằm **bên phải** toolbar trên cả iOS và Android. Wrapper tự truyền `lefttoright=yes` cho iOS; Android mặc định đã đặt nút bên phải.
- `CloseButtonIcon`: `True` thì nút Đóng là icon X, cùng màu với chữ khi không dùng icon. Cả iOS và Android dùng chung một icon (khung 32pt/dp, chữ X 18pt/dp), nên nút có cùng kích thước trên hai nền tảng. Trên iOS 26, nút không có nền kính dạng viên thuốc. `CloseButtonText` khi đó không hiện ra mà chỉ là nhãn cho trình đọc màn hình, nên vẫn nên để "Đóng".
- `StatusBarColor`: màu CSS bất kỳ, ví dụ `#1068EB`, `rgb(16,104,235)` hay `red`. Để trống thì dùng màu primary của app.
- `ToolbarColor`: màu CSS bất kỳ. Để trống thì dùng màu của status bar. Truyền `"#FFFFFF"` nếu muốn toolbar trắng. Khi đó nút Đóng dùng màu primary.
- `ToolbarHeight`: chiều cao toolbar, tính bằng CSS px. Trong app OutSystems, 1 CSS px bằng 1dp trên Android và 1pt trên iOS, nên cứ truyền đúng chiều cao header của app là khớp. Input này nhận được Integer (`56`) hoặc Text (`"56"`, `"56px"`, `"3.5rem"`). Giá trị nhỏ hơn **44** được nâng lên 44, vì các nút của toolbar iOS cần tối thiểu 44pt; thấp hơn thì nút tràn lên status bar, còn trên Android chữ bị cắt. Để trống hoặc `0` thì wrapper lấy chiều cao theo thứ tự: giá trị `height` khai báo cho `ToolbarHeightClass`, rồi biến `--header-size` của theme OutSystems UI (mặc định `56px`, không gồm status bar), rồi chiều cao mặc định của plugin.
- `ToolbarHeightClass`: tên class CSS để đọc giá trị `height` được khai báo cho class đó trong stylesheet của app, ví dụ `header-top` (có hay không có dấu `.` đều được, nhiều class thì cách nhau bằng dấu cách). Wrapper chỉ đọc giá trị `height`, không đo cả khối phần tử, nên padding và border không bị cộng vào. Giá trị `var(...)`, `rem` hay `calc()` được quy đổi ra px.
  - Chỉ tính rule có selector **đúng bằng** class, kể cả khi nằm trong danh sách selector (`.a, .header-top`). Rule có selector lồng như `.layout .header-top` không được tính.
  - Nếu có nhiều rule như vậy thì rule khai báo sau cùng được dùng. Rule trong `@media` không khớp với máy thì bị bỏ qua. Rule trong `@import` và `@supports` vẫn được đọc.
  - Không tìm thấy rule nào, hoặc rule không khai báo `height`, thì wrapper chuyển sang `--header-size`.
  - Với OutSystems UI, dùng `header-top` (khai báo `height: var(--header-size)`). Class `header` không khai báo height.
- `AuthToken`: JWT giao cho web khi web gọi `MiniAppBridge.getToken()`. Để trống thì app chỉ lấy token khi web cần, qua event `OnTokenRequest` (xem mục 3).

`MiniApp_SetToken` lưu token mới và trả lời các lần gọi `getToken()` đang chờ. Truyền `Token` rỗng nghĩa là không lấy được token, khi đó các lần gọi đang chờ bị reject. Output `Success` là False nếu không có mini app nào đang mở.

### 2.3 Block `MiniAppEvents`

Block này nhận kết quả từ mini app và đẩy lên screen chứa nó qua các event.

- **Events** (Is Mandatory = No):
  - `OnLoaded(Url: Text)`
  - `OnMessage(Type: Text, PayloadJson: Text)`
  - `OnTokenRequest()`
  - `OnClosed()`
  - `OnError(Message: Text)`
- **Local variable:** `ListenerId` (Text)
- **Client actions:** mỗi action chỉ có một node Trigger Event:
  - `TriggerOnLoaded(Url)` trigger `OnLoaded`
  - `TriggerOnMessage(Type, Payload)` trigger `OnMessage`
  - `TriggerOnTokenRequest()` trigger `OnTokenRequest`
  - `TriggerOnClosed()` trigger `OnClosed`
  - `TriggerOnError(Message)` trigger `OnError`
- **OnReady:** JS node chứa code [block/OnReady.js](block/OnReady.js), output `ListenerId`. Sau node này, thêm Assign `ListenerId = JS.ListenerId`.
- **OnDestroy:** JS node chứa code [block/OnDestroy.js](block/OnDestroy.js), input `ListenerId` lấy từ biến local.

Widget của block để trống (không hiển thị gì), Public = Yes.

## 3. Dùng trong app

1. Kéo block `MiniAppEvents` vào screen sẽ mở mini app, rồi gắn handler cho các event.
2. Nút mở mini app gọi `MiniApp_Open(Url, AllowedTypes: "openPayment")`. Có hai cách đăng nhập cho đối tác:
   - **JWT qua bridge** (khuyến nghị): web gọi `MiniAppBridge.getToken()` để lấy JWT.
     - Nếu đã có token lúc mở, truyền vào `AuthToken`.
     - Nếu không, gắn handler cho `OnTokenRequest`. Handler gọi server action `GetPartnerToken(PartnerId)`, rồi gọi `MiniApp_SetToken(Token)`. Nếu server action lỗi, gọi `MiniApp_SetToken("")` để web không phải chờ.
     - `OnTokenRequest` cũng được bắn khi web báo token hết hạn (`getToken({ refresh: true })`). Vì vậy nên luôn gắn handler này, kể cả khi đã truyền `AuthToken`.
   - **SSO bằng code dùng một lần:** server action `CreateAuthCode(PartnerId)` trả về `Url` dạng `https://partner.com/sso?code=...`, rồi truyền `Url` đó vào `MiniApp_Open`.
3. Trong handler `OnMessage`, dùng Switch theo `Type`. Payload đọc bằng `JSONDeserialize` vào structure tương ứng.
   - `close`: mini app đã đóng. `PayloadJson` là kết quả web gửi kèm, nếu có.
   - Các type khác: xử lý nghiệp vụ. Muốn trả lời web thì gọi `MiniApp_PostToWeb`.
4. Muốn đóng mini app từ phía app thì gọi `MiniApp_Close`.

Mỗi lúc chỉ mở được một mini app. Gọi `MiniApp_Open` khi đang có mini app mở sẽ bắn `OnError`.

## 4. Phía đối tác

Nhúng [partner/miniapp-bridge.js](partner/miniapp-bridge.js) vào mọi trang của mini app.

```js
if (MiniAppBridge.isInApp()) {
  MiniAppBridge.send('openPayment', { orderId: 'A123', amount: 150000 });
  MiniAppBridge.onMessage(function (msg) {
    if (msg.type === 'paymentDone') { /* cập nhật UI, rồi xác nhận lại với backend */ }
  });
}
// Kết thúc mini app:
MiniAppBridge.close({ result: 'success' });
```

Lấy JWT của user rồi đổi lấy session ở backend đối tác:

```js
function login(refresh) {
  return MiniAppBridge.getToken({ refresh: refresh }).then(function (jwt) {
    return fetch('/api/miniapp/session', { method: 'POST', headers: { Authorization: 'Bearer ' + jwt } });
  });
}

login(false).then(function (res) {
  // Backend trả 401 vì token hết hạn: xin app token mới rồi thử lại một lần.
  return res.status === 401 ? login(true) : res;
});
```

`getToken()` trả về Promise. Promise bị reject nếu app không có token, nếu trang không chạy trong app, hoặc nếu quá 30 giây không có trả lời.

Danh sách `type` và format `payload` phải được hai bên thống nhất trước, và phải khớp với `AllowedTypes` phía app. Không dùng các type dành riêng cho nghiệp vụ: `close`, `getToken` (web gửi app) và `token` (app gửi web).

## 5. Bảo mật và hành vi cần biết

- **Không dùng `clearcache` hay `clearsessioncache`.** InAppBrowser dùng chung kho cookie với WebView của app. Xoá cookie ở đây sẽ làm app OutSystems mất phiên đăng nhập.
- Vì dùng chung cookie, **session của đối tác vẫn còn sau khi user logout khỏi app.** Khi logout, phải huỷ session phía đối tác, qua API server-to-server hoặc URL logout của họ.
- Coi mọi message từ web là input không tin cậy. Trên iOS, message từ iframe bị bỏ qua. Trên Android, iframe trong trang vẫn gửi được message, vì `JavascriptInterface` không cho biết frame gửi. Với thanh toán và kết quả giao dịch, luôn xác nhận qua backend (server của mình gọi server đối tác), không tin số liệu do web gửi lên.
- **JWT:**
  - Không bao giờ đưa JWT vào URL, vì URL bị lưu trong lịch sử, log server và header `Referer`. Dùng `AuthToken`/`getToken` hoặc SSO bằng code.
  - JWT cho đối tác nên do backend của app phát riêng cho từng đối tác. Token sống ngắn (5–15 phút), `aud` là đối tác, chỉ chứa claim cần thiết, và ký bất đối xứng (RS256/ES256) để đối tác verify bằng public key. Không đưa session token hay refresh token của app.
  - Token chỉ được giao cho trang chính khi trang đang ở một origin trong `AllowedOrigins`. Origin được kiểm tra ngay trong trang, tại lúc giao. `AllowedOrigins` chỉ nên gồm origin HTTPS chính xác của đối tác. Iframe trong trang vẫn gửi được yêu cầu `getToken`, nhưng token chỉ được giao cho trang chính.
  - Khi trang đối tác đã nhận token, mọi script chạy trong trang đó (analytics, SDK bên thứ ba) đều đọc được token. Vì vậy đối tác nên đổi token lấy session của họ ở backend ngay, và không lưu token vào `localStorage`.
  - Backend đối tác phải kiểm tra chữ ký, `exp`, `aud` và `iss` của token.

## 6. Checklist test trên máy thật

Màu:

- [ ] iOS: vùng status bar và toolbar cùng màu primary của app. Với primary đậm, chữ status bar và nút Đóng màu trắng.
- [ ] iOS với primary sáng (thử `StatusBarColor: "#FFD600"`): chữ status bar và nút Đóng màu tối.
- [ ] Chiều cao toolbar bằng chiều cao header của app (mặc định 56). Thử thêm `ToolbarHeight: "64px"`: toolbar cao hơn, nút Đóng vẫn nằm giữa theo chiều dọc. `ToolbarHeight: "20px"` cho ra 44, và trên iOS nút không tràn ra ngoài toolbar.
- [ ] `ToolbarHeightClass: "header-top"`: toolbar cao bằng phần header dưới status bar (56 với theme mặc định).
- [ ] Nút Đóng nằm bên phải toolbar trên cả iOS và Android.
- [ ] `CloseButtonIcon: True`: iOS và Android hiện icon X đúng màu và cùng kích thước (trên iOS 26 không có nền kính quanh nút), bấm vào thì đóng mini app. Bật VoiceOver/TalkBack: nút được đọc là "Đóng".
- [ ] iOS: nếu chữ status bar không đổi màu, kiểm tra `Info.plist` của app. Nếu `UIViewControllerBasedStatusBarAppearance` là `NO`, iOS bỏ qua style riêng của InAppBrowser và dùng style chung của app.
- [ ] Android 14 trở xuống: status bar màu primary, icon status bar đúng màu (trắng hoặc tối).
- [ ] **Android 15 trở lên:** toolbar nằm dưới status bar, vùng status bar màu primary, web không bị thanh điều hướng che.
- [ ] Android 15 trở lên: bấm vào ô nhập liệu ở cuối trang của đối tác. Bàn phím không được che ô đó.
- [ ] App không có `--color-primary` (hoặc theme không phải OutSystems UI): toolbar theo chế độ Sáng/Tối. Trên Android, nếu theme app không phải DayNight, WebView luôn báo chế độ sáng nên toolbar luôn trắng.

Token:

- [ ] Mở với `AuthToken`: `getToken()` trả về đúng token, `OnTokenRequest` không bắn.
- [ ] Mở không có `AuthToken`: `OnTokenRequest` bắn một lần. Sau `MiniApp_SetToken`, `getToken()` nhận được token.
- [ ] Gọi `getToken({ refresh: true })` hai lần liền: `OnTokenRequest` chỉ bắn một lần, và cả hai lần gọi đều nhận token mới.
- [ ] `MiniApp_SetToken("")`: `getToken()` đang chờ bị reject.
- [ ] Trang redirect sang domain không có trong `AllowedOrigins`: `getToken()` không nhận được token (timeout sau 30 giây).

Quyền thiết bị (dùng một trang test trên origin của đối tác gọi `getUserMedia({ video: true, audio: true })` và `navigator.geolocation.getCurrentPosition(...)`):

- [ ] Android: hiện hộp thoại "… muốn dùng camera và micro của bạn". Chọn "Không cho phép" thì trang báo lỗi quyền. Chọn "Cho phép" thì camera chạy; nếu app chưa có quyền camera thì Android hỏi thêm một lần.
- [ ] Android: gọi lại `getUserMedia` trong cùng phiên thì không hỏi lại. Đóng rồi mở lại mini app thì hỏi lại.
- [ ] Android: vị trí hiện hộp thoại "… muốn biết vị trí của bạn". Xin lại trong cùng phiên thì không hỏi nữa.
- [ ] Trang ở một origin **không** có trong `AllowedOrigins` (ví dụ sau khi bấm link ra ngoài) gọi camera hoặc vị trí: bị từ chối ngay, không có hộp thoại.
- [ ] iOS 15 trở lên: camera hiện hộp thoại của WebKit với origin trong `AllowedOrigins`, và bị từ chối với origin khác.

Hành vi chung:

- [ ] `MiniAppBridge.close()` đóng mini app, và `OnMessage` nhận được type `close` kèm payload.
- [ ] Gửi một type ngoài `AllowedTypes`: app không nhận được message.
- [ ] Nút back phần cứng trên Android: lùi lịch sử web, về tới trang đầu thì đóng mini app.
- [ ] Sau khi đóng mini app, app OutSystems vẫn giữ phiên đăng nhập.
- [ ] Mở và đóng mini app 10–20 lần liên tiếp, kể cả luồng có `window.open` như thanh toán: app không chậm dần, không bị kill vì hết bộ nhớ.
- [ ] `Url` là `http://...`: mini app không mở, `OnError` báo "Url must be an https URL".
