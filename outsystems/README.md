# Mini app browser cho OutSystems (MABS)

Bản fork của [apache/cordova-plugin-inappbrowser](https://github.com/apache/cordova-plugin-inappbrowser) tại nhánh `master` (7.0.1-dev), dùng để mở website của đối tác như một mini app trong app mobile OutSystems. Kèm theo là code cho module wrapper trong OutSystems và script cho phía đối tác.

Khác biệt so với bản gốc (`7.0.1-os.2`):

- **Option mới `statusbarcolor` và `statusbarstyle`** (iOS và Android). `statusbarcolor=#RRGGBB` là màu nền vùng status bar. `statusbarstyle=lightcontent|darkcontent` là màu chữ và icon status bar. Option này áp dụng cho từng lần mở và được ưu tiên hơn preference `InAppBrowserStatusBarStyle`.
- **Option mới `closebuttonicon=yes`** (iOS và Android): nút Đóng là icon X thay cho chữ. Khi đó `closebuttoncaption` chỉ dùng làm nhãn cho VoiceOver/TalkBack. Ở bản gốc, iOS dưới 26 luôn hiện chữ ("Done" hoặc caption).
- **iOS:** khi không truyền `statusbarcolor`, vùng status bar được tô cùng màu toolbar (nếu toolbar ở trên), hoặc theo màu nền hệ thống. Bản gốc để trống vùng này, nên status bar thường hiện nền đen.
- **Android 15 trở lên (targetSdk 35):** Android ép cửa sổ InAppBrowser vẽ tràn viền (edge-to-edge), nên ở bản gốc toolbar bị status bar đè, còn web bị thanh điều hướng và bàn phím che. Bản fork chừa lề theo system bar và bàn phím, rồi tô vùng status bar bằng `statusbarcolor` (không có thì dùng màu toolbar). Từ Android 14 trở xuống, màu được đặt bằng `setStatusBarColor`.
- **Option mới `toolbarheight`** (iOS và Android): chiều cao toolbar, tính bằng dp trên Android và pt trên iOS, không gồm safe area. Mặc định của bản gốc là 48dp trên Android và khoảng 60pt trên iOS.
- **iOS:** khi iOS kill WebContent process của mini app (thường do thiếu bộ nhớ), plugin báo `loaderror` rồi tự tải lại trang. Nếu process lại bị kill trong vòng 10 giây thì không tải lại nữa, để tránh vòng lặp. Bản gốc chỉ để trang trắng.
- **iOS:** option dạng chuỗi (caption, màu, ...) luôn được giữ là chuỗi. Ở bản gốc, `closebuttoncaption=1` hay `closebuttoncaption=No` làm app crash, vì giá trị bị đọc thành số hoặc Boolean. Cũng nhờ vậy `beforeload=no` giờ được hiểu đúng là tắt.
- **Android 11:** màu icon status bar được đặt bằng cả `WindowInsetsController` lẫn cờ kiểu cũ, vì trên một số máy Android 11 chỉ một cách là không đủ.
- Plugin yêu cầu cordova-android ≥ 10.0.0, vì code dùng API 30 (`WindowInsets.Type`).

Đã kiểm tra khai báo engine với MABS: cordova-ios 7.1.1 và cordova-android 14.0.1. Code native đã compile với header cordova-ios 7.1.1 và Android SDK 35.

**Màu:** lúc mở mini app, status bar và toolbar lấy màu primary của app, tức biến CSS `--color-primary` của theme OutSystems UI. Chữ và icon status bar, cùng nút Đóng, tự chọn màu trắng hoặc tối. Màu trắng được dùng khi đạt độ tương phản ít nhất 3:1 với màu nền. Nếu app không khai báo `--color-primary`, màu sẽ theo chế độ Sáng/Tối của máy.

## 1. Publish plugin

Plugin được publish từ repo [khoado-rgb/cordova-plugin-inappbrowser](https://github.com/khoado-rgb/cordova-plugin-inappbrowser) (remote `origin`). Sau khi commit, push nhánh và tạo tag cho bản phát hành:

```sh
git push origin master
git tag 7.0.1-os.2
git push origin 7.0.1-os.2
```

MABS lấy plugin theo tag, nên mỗi lần sửa plugin phải tạo tag mới (`7.0.1-os.3`, ...) và cập nhật URL trong Extensibility Configurations. Không sửa lại một tag đã dùng để build.

Nếu repo để private, MABS phải có quyền đọc repo. Khi đó dùng URL có token, hoặc để repo public.

## 2. Module `MiniAppBrowser` (Mobile Library)

### 2.1 Extensibility Configurations

```json
{
  "plugin": {
    "url": "https://github.com/khoado-rgb/cordova-plugin-inappbrowser.git#7.0.1-os.2"
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
| `MiniApp_Open` | `Url` (Text, bắt buộc), `AllowedOrigins` (Text), `AllowedTypes` (Text), `CloseButtonText` (Text, mặc định `"Đóng"`), `CloseButtonIcon` (Boolean, mặc định `False`), `StatusBarColor` (Text), `ToolbarColor` (Text), `ToolbarHeight` (Integer, mặc định `0`), `ToolbarHeightClass` (Text), `AuthToken` (Text) | `IsOpened` (Boolean) | [client-actions/Open.js](client-actions/Open.js) |
| `MiniApp_Close` | – | – | [client-actions/Close.js](client-actions/Close.js) |
| `MiniApp_PostToWeb` | `Type` (Text, bắt buộc), `PayloadJson` (Text, mặc định `"{}"`) | `Success` (Boolean) | [client-actions/PostToWeb.js](client-actions/PostToWeb.js) |
| `MiniApp_SetToken` | `Token` (Text) | `Success` (Boolean) | [client-actions/SetToken.js](client-actions/SetToken.js) |

Ý nghĩa các input của `MiniApp_Open`:

- `AllowedOrigins`: danh sách origin, cách nhau bằng dấu phẩy. Để trống thì chỉ gồm origin của `Url`. Cần khai báo thêm nếu web đối tác redirect sang domain khác. Danh sách này dùng cho cả hai chiều:
  - Message từ web chỉ được nhận khi trang chính đang ở một origin trong danh sách.
  - Message từ app (`MiniApp_PostToWeb`, token) chỉ được giao khi trang chính đang ở một origin trong danh sách.
- `AllowedTypes`: danh sách `type` mà web được phép gửi, ví dụ `openPayment,share`. Type `close` và `getToken` luôn được chấp nhận. Message có type ngoài danh sách sẽ bị bỏ qua.
- `CloseButtonText`: không được chứa dấu phẩy.
- `CloseButtonIcon`: `True` thì nút Đóng là icon X (iOS dùng SF Symbol `xmark`, Android dùng icon có sẵn của plugin), cùng màu với chữ khi không dùng icon. `CloseButtonText` khi đó không hiện ra mà chỉ là nhãn cho trình đọc màn hình, nên vẫn nên để "Đóng".
- `StatusBarColor`: màu CSS bất kỳ, ví dụ `#1068EB`, `rgb(16,104,235)` hay `red`. Để trống thì dùng màu primary của app.
- `ToolbarColor`: màu CSS bất kỳ. Để trống thì dùng màu của status bar. Truyền `"#FFFFFF"` nếu muốn toolbar trắng. Khi đó nút Đóng dùng màu primary.
- `ToolbarHeight`: chiều cao toolbar, tính bằng CSS px. Trong app OutSystems, 1 CSS px bằng 1dp trên Android và 1pt trên iOS, nên cứ truyền đúng chiều cao header của app là khớp. Để `0` thì wrapper lấy chiều cao theo thứ tự: đo từ `ToolbarHeightClass`, rồi biến `--header-size` của theme OutSystems UI (mặc định `56px`, không gồm status bar), rồi chiều cao mặc định của plugin. Trên iOS nên để từ 44 trở lên, vì thấp hơn thì nút có thể tràn ra ngoài toolbar.
- `ToolbarHeightClass`: tên class CSS để lấy chiều cao, ví dụ `header-top` (có hay không có dấu `.` đều được, nhiều class thì cách nhau bằng dấu cách). Wrapper đo phần tử đầu tiên có class đó đang hiển thị trên screen. Nếu screen không có phần tử nào như vậy, wrapper tạo tạm một phần tử ẩn có class đó để đo; cách này chỉ đúng khi rule CSS của class không phụ thuộc phần tử cha. Với OutSystems UI, dùng `header-top` chứ **không dùng `header`**, vì `.header` có thêm padding bằng chiều cao status bar.
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
- Coi mọi message từ web là input không tin cậy. Việc kiểm tra origin chỉ áp cho trang chính, không chặn được iframe bên trong trang. Với thanh toán và kết quả giao dịch, luôn xác nhận qua backend (server của mình gọi server đối tác), không tin số liệu do web gửi lên.
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
- [ ] Chiều cao toolbar bằng chiều cao header của app (mặc định 56). Thử thêm `ToolbarHeight: 64`: nút Đóng vẫn nằm giữa theo chiều dọc.
- [ ] `ToolbarHeightClass: "header-top"` trên một screen có header: toolbar cao bằng phần header dưới status bar.
- [ ] `CloseButtonIcon: True`: iOS và Android hiện icon X đúng màu, bấm vào thì đóng mini app. Bật VoiceOver/TalkBack: nút được đọc là "Đóng".
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

Hành vi chung:

- [ ] `MiniAppBridge.close()` đóng mini app, và `OnMessage` nhận được type `close` kèm payload.
- [ ] Gửi một type ngoài `AllowedTypes`: app không nhận được message.
- [ ] Nút back phần cứng trên Android: lùi lịch sử web, về tới trang đầu thì đóng mini app.
- [ ] Sau khi đóng mini app, app OutSystems vẫn giữ phiên đăng nhập.
