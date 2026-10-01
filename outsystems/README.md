# Mini app browser cho OutSystems (MABS)

Bản fork của [apache/cordova-plugin-inappbrowser](https://github.com/apache/cordova-plugin-inappbrowser) tại nhánh `master` (7.0.1-dev), dùng để mở website của đối tác như một mini app trong app mobile OutSystems. Kèm theo là code cho module wrapper trong OutSystems và script cho phía đối tác.

## Các thay đổi so với bản gốc

Bản hiện tại: `7.0.1-os.7`. Chi tiết từng bản ở [RELEASENOTES.md](../RELEASENOTES.md).

### Option mới của `cordova.InAppBrowser.open`

| Option | Nền tảng | Ý nghĩa |
|---|---|---|
| `statusbarcolor=#RRGGBB` | iOS, Android | Màu nền vùng status bar. |
| `statusbarstyle=lightcontent\|darkcontent` | iOS, Android | Màu chữ và icon status bar. Trên iOS, option này được ưu tiên hơn preference `InAppBrowserStatusBarStyle`. |
| `closebuttonicon=yes` | iOS, Android | Nút Đóng là icon X 18pt/dp, trắng đục, dùng chung trên hai nền tảng và tô theo `closebuttoncolor`. Chữ X cách mép 16pt/dp, vùng bấm 44pt/dp. `closebuttoncaption` khi đó chỉ là nhãn cho VoiceOver/TalkBack. |
| `closebuttonsize=<số>` | iOS, Android | Kích thước chữ X (khi `closebuttonicon=yes`) tính bằng pt/dp, hoặc cỡ chữ của caption (pt trên iOS, sp trên Android). Giá trị được giữ trong khoảng 8–40; `0` hoặc giá trị sai thì dùng mặc định. Mặc định: X 18, chữ 17 (iOS) và 20 (Android). Chữ X vẫn cách mép 16pt/dp. |
| `toolbarheight=<số>` | iOS, Android | Chiều cao toolbar, tính bằng dp trên Android và pt trên iOS, không gồm safe area. Trên iOS, toolbar không thấp hơn chiều cao thật của thanh. |
| `toolbartitle=<chữ>` | iOS, Android | Title trên toolbar, ở phía không có nút Đóng (với wrapper là bên trái), cách mép 16pt/dp. Cỡ 17, chữ đậm vừa, cùng màu nút Đóng, bị cắt bằng "…" nếu quá dài. Trên Android chỉ hiện khi thanh URL bị ẩn (`hideurlbar=yes`). |
| `permissionorigins=<origin>\|<origin>` | iOS, Android | Các origin HTTPS được xin quyền camera, micro và vị trí. Mục không phải HTTPS bị bỏ, nên nếu không còn mục nào thì mọi trang đều bị từ chối. Không truyền thì mọi origin đều được xin, nhưng luôn phải được user đồng ý. |
| `httpsonly=yes` | iOS, Android | Chặn trang chính điều hướng sang `http:` (link, redirect, `window.open`) và báo `loaderror` mã `-1`. Iframe không bị ảnh hưởng. Android không huỷ được lần submit form, back hay reload sang `http:` (Android 6 trở xuống: mọi lần điều hướng sang `http:`). Khi đó plugin tự trả về một trang rỗng, nên không có gì được gửi qua `http`, rồi quay về trang trước đó. Plugin hiện trang trắng thay vì quay về khi không có trang trước, hoặc khi cùng trang `http:` đó bị chặn lại trong vòng 10 giây, để form tự submit lúc tải trang (ví dụ form SSO) không bị lặp. Bấm Back từ trang trắng đó thì quay về trang trước; nếu trang đó lại tự submit thì lại dừng ở trang trắng, lối thoát là nút Đóng. |

Wrapper OutSystems tự truyền các option này. Khi gọi plugin trực tiếp thì truyền tay.

### Sửa lỗi và thay đổi hành vi

**Android:**

- **Status bar:** bản gốc không tô được status bar. Nó sao chép thuộc tính cửa sổ trước `setContentView` rồi ghi lại sau `show()`, làm mất các cờ được đặt ở giữa, nên dialog bị đặt phía dưới status bar và phần tai thỏ. Khi đó vùng phía trên toolbar để lộ app phía sau.
  - Từ Android 11, bản fork vẽ dialog tràn viền (edge-to-edge), kể cả vùng tai thỏ, với system bar trong suốt. Một view nền tô vùng phía sau status bar bằng `statusbarcolor` (không có thì dùng màu toolbar).
  - Plugin cũng chừa lề cho toolbar, thanh điều hướng và bàn phím.
  - Từ Android 10 trở xuống, màu được đặt bằng `setStatusBarColor`.
  - Với app targetSdk 35 chạy trên Android 15 trở lên, `setStatusBarColor` luôn trong suốt, nên đây là cách duy nhất.
- **Android 11:** màu icon status bar được đặt bằng cả `WindowInsetsController` lẫn cờ kiểu cũ, vì trên một số máy Android 11 chỉ một cách là không đủ.
- **Quyền camera, micro và vị trí:** bản gốc tự cấp mọi quyền cho mọi trang. Bản fork chỉ cho trang thuộc `permissionorigins` xin quyền, rồi hiện hộp thoại "partner.com muốn dùng camera của bạn" để user quyết định.
  - Nếu app chưa có quyền runtime của Android thì hệ thống hỏi tiếp.
  - Mỗi origin chỉ bị hỏi một lần cho mỗi loại quyền, cho tới khi đóng mini app.
  - Chỉ camera, micro và vị trí được cấp. Protected media và MIDI luôn bị từ chối.
  - `InAppChromeClient` mặc định từ chối.
- **Nút Đóng:** với `closebuttonicon=yes`, nút dùng icon X 18dp trắng đục, có vùng bấm 44dp. Chữ X, hoặc chữ caption khi không dùng icon, cách mép 16dp, bằng khoảng cách của title. Icon gốc của bản gốc là `#333` với alpha 60%, nên khi tô trắng chỉ ra màu trắng nhạt.
- **Message từ web:** khi WebView hỗ trợ `WebMessageListener` (hầu hết máy hiện nay), plugin nhận message qua listener này thay cho `JavascriptInterface`. Khi đó chỉ trang chính gửi được message, iframe thì không, và mỗi message kèm `origin` thật của trang gửi. WebView cũ không hỗ trợ thì plugin vẫn dùng `JavascriptInterface` như bản gốc, message không kèm `origin`. Plugin cần thư viện `androidx.webkit`, đã có sẵn trong cordova-android 10 trở lên.
- **Bộ nhớ:** WebView được destroy khi đóng và khi mở lại mini app. WebView tạm của `window.open` được destroy ngay sau khi điều hướng đã chuyển về WebView chính, và chỉ giữ tối đa 3 cái (trang mở liên tục cửa sổ trống thì cái cũ nhất bị destroy). Bản gốc giữ chúng tới khi GC chạy.
- **Đóng nhầm mini app mới mở:** ở bản gốc, nếu trang `about:blank` (tải lúc đóng) xong muộn, nó có thể đóng nhầm mini app vừa được mở lại. Lỗi này đã được sửa.
- Plugin yêu cầu cordova-android ≥ 10.0.0, vì code dùng API 30 (`WindowInsets.Type`).

**iOS:**

- **Vùng status bar:** khi không truyền `statusbarcolor`, vùng này được tô cùng màu toolbar (nếu toolbar ở trên), hoặc theo màu nền hệ thống. Bản gốc để trống vùng này, nên status bar thường hiện nền đen.
- **Nút Đóng (icon hoặc chữ):** được vẽ thành một nút riêng phía trên nền toolbar, không còn là item của `UIToolbar`.
  - Chữ X hoặc chữ caption cách mép 16pt trên mọi bản iOS, bằng khoảng cách của title ở phía bên kia, và đúng màu được truyền vào.
  - Trước đây `UIToolbar` tự thêm lề (chữ X cách mép 31pt trên iOS 18 và 39pt trên iOS 26), còn iOS 26 làm nhạt màu nút và thêm nền kính dạng viên thuốc.
  - Icon là chữ X 18pt dùng chung với Android. Ở bản gốc, iOS dưới 26 luôn hiện chữ ("Done" hoặc caption).
- **Đường kẻ mảnh** ở mép trên toolbar (iOS 18 trở xuống) đã bị tắt.
- **Chiều cao toolbar:** nếu `toolbarheight` thấp hơn chiều cao thật của thanh (44pt, cao hơn trên iOS 26), vùng toolbar tự giãn ra, nên nút không tràn ra ngoài.
- **WebContent process bị kill** (thường do thiếu bộ nhớ): plugin báo `loaderror` rồi tự tải lại trang. Nếu process lại bị kill trong vòng 10 giây thì không tải lại nữa, để tránh vòng lặp. Bản gốc chỉ để trang trắng.
- **Option dạng chuỗi** (caption, màu, ...) luôn được giữ là chuỗi. Ở bản gốc, `closebuttoncaption=1` hay `closebuttoncaption=No` làm app crash, vì giá trị bị đọc thành số hoặc Boolean. Cũng nhờ vậy `beforeload=no` giờ được hiểu đúng là tắt.
- **Quyền camera và micro (iOS 15 trở lên):** chỉ được xin khi trang thuộc `permissionorigins`, sau đó WebKit tự hỏi user. Với quyền vị trí, và với iOS dưới 15, không có API để lọc theo origin, nên WebKit hỏi user như bình thường.
- **Message và log:**
  - Chỉ nhận message từ trang chính, bỏ qua message từ iframe. Mỗi message kèm `origin` của trang gửi.
  - Không ghi script hay kết quả của `executeScript` vào log, vì script giao token có chứa JWT.
  - Message handler được gỡ đúng cách khi đóng.

### Wrapper OutSystems (`outsystems/`)

- **Màu:** lúc mở mini app, status bar và toolbar lấy màu primary của app (biến CSS `--color-primary` của OutSystems UI). Chữ và icon status bar, cùng nút Đóng, dùng màu trắng nếu đạt độ tương phản ít nhất 3:1, không thì dùng màu tối. Nếu app không khai báo `--color-primary`, màu theo chế độ Sáng/Tối của máy.
- **Chiều cao toolbar:** lấy từ `ToolbarHeight`, không có thì lấy `--header-size`. `ToolbarHeight` nhận số, độ dài CSS hoặc biến CSS (`56`, `"56px"`, `"3.5rem"`, `"var(--header-size)"`), hoặc tên class có dấu chấm (`".header-top"`) để đọc giá trị `height` khai báo cho class đó. Tối thiểu là 44.
- **Nút Đóng:** nằm bên phải trên cả hai nền tảng. `CloseButtonIcon` để dùng icon X thay cho chữ.
- **Title:** input `Title` hiện chữ ở bên trái toolbar.
- **JWT:** web lấy token bằng `MiniAppBridge.getToken()`. App trả token qua `AuthToken`, hoặc qua `OnTokenRequest` và `MiniApp_SetToken`. Origin của trang được kiểm tra ngay lúc giao token.
- **HTTPS:** `Url` và `AllowedOrigins` bắt buộc là HTTPS. `AllowedOrigins` được quy về origin, và cũng là danh sách origin được xin quyền thiết bị. Wrapper truyền `httpsonly=yes`, nên trang chính không chuyển sang được trang `http:`.
- **Kiểm tra người gửi:** message được nhận khi origin của trang gửi (plugin gửi kèm) nằm trong `AllowedOrigins`. Chỉ khi WebView Android cũ không gửi kèm origin thì wrapper mới dùng URL vừa tải gần nhất như trước.
- **URL trong event:** `OnLoaded` và `OnError` nhận URL đã bỏ phần `#...` và `user:password@`, và giá trị của các tham số giống thông tin đăng nhập (`code`, `token`, `access_token`, `id_token`, `password`, ...) được thay bằng `hidden`, để app có ghi log cũng không lộ token.
- **Android:** wrapper truyền thêm `shouldPauseOnSuspend=yes`, để WebView tạm dừng (animation, vị trí) khi app chạy nền.

### Đã kiểm tra

- Khai báo engine khớp với MABS: cordova-ios 7.1.1 và cordova-android 14.0.1.
- Code Android compile với Android SDK 35 và 36.
- Code iOS compile với header cordova-ios 7.1.1 (target iOS 11 và 15), không có warning.
- App test (cordova-ios 7.1.1, chạy nguyên văn `Open.js`) đã được build và chạy trên simulator iOS 18.0 (iPhone 16 Pro) và iOS 26.2 (iPhone 17 Pro). Đo bằng pixel trên ảnh chụp:
  - title cách mép trái 16pt, chữ X cách mép phải 16pt;
  - chữ X 18×18pt, đúng màu `#FFFFFF`, căn giữa theo chiều dọc với toolbar;
  - toolbar cao đúng `--header-size` (56pt);
  - title dài bị cắt bằng "…" mà không đè lên nút;
  - không còn đường kẻ mảnh, không còn nền kính.
- App test Android (cordova-android 14.0.1, build bằng Gradle) đã chạy trên emulator Android 16 (API 36, màn 1080×2400, có tai thỏ):
  - vùng status bar và tai thỏ có màu primary;
  - title cách mép trái 15.6dp, chữ X cách mép phải 15.6dp (16dp sau khi làm tròn pixel), chữ X 18dp màu `#FFFFFF`;
  - với nền vàng thì icon status bar màu tối;
  - bấm nút X thì mini app đóng, và app nhận event `closed`.
- `CloseButtonSize` (bản `os.6`) đã chạy trên simulator iOS 26.2 và emulator Android 16:
  - icon cỡ 24 cho chữ X 24pt/dp, icon cỡ 12 cho chữ X 12pt/dp, cả hai đều cách mép 16;
  - chữ cỡ 15 cho chữ "Đóng" nhỏ hơn mặc định, vẫn sát mép.
- Các sửa đổi bảo mật của bản `os.6` đã chạy trên simulator iOS 18.0, iOS 26.2 và emulator Android 16 (WebView 133), với trang `https://example.com` được nạp `miniapp-bridge.js`:
  - message từ trang chính tới app kèm `origin` `https://example.com`; message từ iframe bị bỏ (iframe có handler, nhưng message không tới app);
  - `getToken()` nhận đúng `AuthToken`;
  - link `http://example.com/?code=secret#frag` bị chặn, trang giữ nguyên, `OnError` nhận URL `http://example.com/?code=hidden`;
  - `OnLoaded` của `https://example.com/?code=abc&x=1#f` nhận `https://example.com/?code=hidden&x=1`;
  - `closebuttonsize=100` cho chữ X 40pt/dp, `closebuttonsize=8` cho chữ X 8pt/dp, cả hai vẫn cách mép phải 16.
  - `httpsonly` với form `POST` tới `http:` (Android 16, form submit bằng script và bằng thao tác bấm thật): app chỉ nhận một `OnError`, không có request nào đi qua `http`, trang quay về trang `https` trước đó; form tự submit mỗi lần trang tải chỉ chạy hai lần rồi dừng ở trang trắng, không lặp, và bấm phím Back trên trang trắng chỉ chạy lại đúng một vòng; Forward vào trang vừa bị chặn trong 10 giây ra trang trắng, Back từ đó thì về trang trước;
  - bridge gọi sớm: script inline (lúc trang còn `loading`) và handler `DOMContentLoaded` gửi được message, trước khi trang tải xong, và `getToken()` gọi lúc đó vẫn nhận đúng token.
- **Chưa test:** thao tác bấm nút Đóng trên simulator iOS; Android 15 trở xuống (trong đó có `httpsonly` trên Android 6 trở xuống); WebView Android cũ không có `WebMessageListener` (đường dự phòng `JavascriptInterface`).

## 1. Publish plugin

Plugin được publish từ repo [khoado-rgb/cordova-plugin-inappbrowser](https://github.com/khoado-rgb/cordova-plugin-inappbrowser) (remote `origin`). Sau khi commit, push nhánh và tạo tag cho bản phát hành:

```sh
git push origin master
git tag 7.0.1-os.7
git push origin 7.0.1-os.7
```

MABS lấy plugin theo tag, nên mỗi lần sửa plugin phải tạo tag mới (`7.0.1-os.8`, ...) và cập nhật URL trong Extensibility Configurations. Không sửa lại một tag đã dùng để build.

Nếu repo để private, MABS phải có quyền đọc repo. Khi đó dùng URL có token, hoặc để repo public.

## 2. Module `MiniAppBrowser` (Mobile Library)

### 2.1 Extensibility Configurations

```json
{
  "plugin": {
    "url": "https://github.com/khoado-rgb/cordova-plugin-inappbrowser.git#7.0.1-os.7"
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
| `MiniApp_Open` | `Url` (Text, bắt buộc, HTTPS), `AllowedOrigins` (Text), `AllowedTypes` (Text), `Title` (Text), `CloseButtonText` (Text, mặc định `"Đóng"`), `CloseButtonIcon` (Boolean, mặc định `False`), `CloseButtonSize` (Integer hoặc Text), `StatusBarColor` (Text), `ToolbarColor` (Text), `ToolbarHeight` (Integer hoặc Text), `AuthToken` (Text) | `IsOpened` (Boolean) | [client-actions/Open.js](client-actions/Open.js) |
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
- `Title`: chữ hiện ở bên trái toolbar, ví dụ tên đối tác. Để trống thì không có title. Dấu phẩy và dấu `=` được thay bằng khoảng trắng, vì hai ký tự này dùng để tách các option. Title dùng cùng màu với nút Đóng, và bị cắt bằng "…" nếu dài quá chỗ trống.
- `CloseButtonText`: không được chứa dấu phẩy.
- Nút Đóng nằm **bên phải** toolbar trên cả iOS và Android. Wrapper tự truyền `lefttoright=yes` cho iOS; Android mặc định đã đặt nút bên phải.
- `CloseButtonIcon`: `True` thì nút Đóng là icon X, cùng màu với chữ khi không dùng icon. Cả iOS và Android dùng chung một icon (khung 32pt/dp, chữ X 18pt/dp), nên nút có cùng kích thước trên hai nền tảng. Trên iOS 26, nút không có nền kính dạng viên thuốc. `CloseButtonText` khi đó không hiện ra mà chỉ là nhãn cho trình đọc màn hình, nên vẫn nên để "Đóng".
- `CloseButtonSize`: kích thước chữ X (khi `CloseButtonIcon = True`) hoặc cỡ chữ của `CloseButtonText`, tính bằng CSS px (bằng pt trên iOS, dp trên Android). Nhận số hoặc chuỗi (`24`, `"24px"`). Giá trị được giới hạn trong khoảng 8 đến 40 để vừa toolbar. Để trống hoặc `0` thì dùng mặc định: chữ X cỡ 18, chữ cỡ 17 trên iOS và 20 trên Android. Chữ X luôn cách mép 16, và vùng bấm rộng ít nhất 44 khi khoảng tới mép cho phép.
- `StatusBarColor`: màu CSS bất kỳ, ví dụ `#1068EB`, `rgb(16,104,235)` hay `red`. Để trống thì dùng màu primary của app.
- `ToolbarColor`: màu CSS bất kỳ. Để trống thì dùng màu của status bar. Truyền `"#FFFFFF"` nếu muốn toolbar trắng. Khi đó nút Đóng dùng màu primary.
- `ToolbarHeight`: chiều cao toolbar, tính bằng CSS px. Trong app OutSystems, 1 CSS px bằng 1dp trên Android và 1pt trên iOS, nên cứ truyền đúng chiều cao header của app là khớp. Input nhận một trong các dạng sau:
  - Số hoặc độ dài CSS: `56`, `"56"`, `"56px"`, `"3.5rem"`, `"calc(...)"`.
  - Biến CSS: `"var(--header-size)"`.
  - Tên class, **viết có dấu chấm ở đầu**: `".header-top"`. Wrapper đọc giá trị `height` được khai báo cho class đó trong stylesheet của app, không đo cả khối phần tử, nên padding và border không bị cộng vào. Nhiều class thì viết liền nhau, ví dụ `".a.b"`.
    - Chỉ tính rule có selector **đúng bằng** class, kể cả khi nằm trong danh sách selector (`.a, .header-top`). Rule có selector lồng như `.layout .header-top` không được tính.
    - Nếu có nhiều rule như vậy thì rule khai báo sau cùng được dùng. Rule trong `@media` không khớp với máy thì bị bỏ qua. Rule trong `@import` và `@supports` vẫn được đọc.
    - Với OutSystems UI, dùng `".header-top"` (class này khai báo `height: var(--header-size)`). Class `header` không khai báo height.
  - Để trống, `0`, hoặc giá trị không đọc được (kể cả class không có rule nào): wrapper dùng biến `--header-size` của theme OutSystems UI (mặc định `56px`, không gồm status bar). Nếu theme không có biến này, plugin dùng chiều cao mặc định.

  Giá trị nhỏ hơn **44** được nâng lên 44, vì các nút của toolbar iOS cần tối thiểu 44pt; thấp hơn thì nút tràn lên status bar, còn trên Android chữ bị cắt.

  Input `ToolbarHeightClass` của các bản trước đã được gộp vào đây. Nếu đang truyền `ToolbarHeightClass: "header-top"`, hãy đổi thành `ToolbarHeight: ".header-top"` và xoá input cũ.
- `AuthToken`: JWT giao cho web khi web gọi `MiniAppBridge.getToken()`. Để trống thì app chỉ lấy token khi web cần, qua event `OnTokenRequest` (xem mục 3).

`MiniApp_SetToken` lưu token mới và trả lời các lần gọi `getToken()` đang chờ. Truyền `Token` rỗng nghĩa là không lấy được token, khi đó các lần gọi đang chờ bị reject. Output `Success` là False nếu không có mini app nào đang mở.

### 2.3 Block `MiniAppEvents`

Block này nhận kết quả từ mini app và đẩy lên screen chứa nó qua các event.

- **Events** (Is Mandatory = No):
  - `OnLoaded(Url: Text)`: URL không có `#...` hay `user:password@`, và các tham số giống token bị thay bằng `hidden`.
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
  var stopListening = MiniAppBridge.onMessage(function (msg) {
    if (msg.type === 'paymentDone') { /* cập nhật UI, rồi xác nhận lại với backend */ }
  });
  // Khi không cần nữa (ví dụ rời màn hình trong SPA): stopListening(), hoặc MiniAppBridge.offMessage(fn).
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

`getToken()` trả về Promise. Promise bị reject nếu app không có token, nếu trang không chạy trong app, nếu quá 30 giây không có trả lời, hoặc nếu đã có 20 lần gọi đang chờ.

Message phải được gửi từ trang chính. Message gửi từ iframe bị bỏ qua (trên Android cần WebView hỗ trợ `WebMessageListener`).

Danh sách `type` và format `payload` phải được hai bên thống nhất trước, và phải khớp với `AllowedTypes` phía app. Không dùng các type dành riêng cho nghiệp vụ: `close`, `getToken` (web gửi app) và `token` (app gửi web).

## 5. Bảo mật và hành vi cần biết

- **Không dùng `clearcache` hay `clearsessioncache`.** InAppBrowser dùng chung kho cookie với WebView của app. Xoá cookie ở đây sẽ làm app OutSystems mất phiên đăng nhập.
- Vì dùng chung cookie, **session của đối tác vẫn còn sau khi user logout khỏi app.** Khi logout, phải huỷ session phía đối tác, qua API server-to-server hoặc URL logout của họ.
- Coi mọi message từ web là input không tin cậy. Message từ iframe bị bỏ qua trên iOS, và trên Android khi WebView hỗ trợ `WebMessageListener`. Chỉ với WebView Android cũ, iframe trong trang mới gửi được message. Với thanh toán và kết quả giao dịch, luôn xác nhận qua backend (server của mình gọi server đối tác), không tin số liệu do web gửi lên.
- **JWT:**
  - Không bao giờ đưa JWT vào URL, vì URL bị lưu trong lịch sử, log server và header `Referer`. Dùng `AuthToken`/`getToken` hoặc SSO bằng code.
  - JWT cho đối tác nên do backend của app phát riêng cho từng đối tác. Token sống ngắn (5–15 phút), `aud` là đối tác, chỉ chứa claim cần thiết, và ký bất đối xứng (RS256/ES256) để đối tác verify bằng public key. Không đưa session token hay refresh token của app.
  - Token chỉ được giao cho trang chính khi trang đang ở một origin trong `AllowedOrigins`. Origin được kiểm tra ngay trong trang, tại lúc giao. `AllowedOrigins` chỉ nên gồm origin HTTPS chính xác của đối tác. Nếu iframe gửi được yêu cầu `getToken` (WebView Android cũ), token vẫn chỉ được giao cho trang chính.
  - Khi trang đối tác đã nhận token, mọi script chạy trong trang đó (analytics, SDK bên thứ ba) đều đọc được token. Vì vậy đối tác nên đổi token lấy session của họ ở backend ngay, và không lưu token vào `localStorage`.
  - Backend đối tác phải kiểm tra chữ ký, `exp`, `aud` và `iss` của token.

## 6. Checklist test trên máy thật

Màu:

- [ ] iOS: vùng status bar và toolbar cùng màu primary của app. Với primary đậm, chữ status bar và nút Đóng màu trắng.
- [ ] iOS với primary sáng (thử `StatusBarColor: "#FFD600"`): chữ status bar và nút Đóng màu tối.
- [ ] Chiều cao toolbar bằng chiều cao header của app (mặc định 56). Thử thêm `ToolbarHeight: "64px"`: toolbar cao hơn, nút Đóng vẫn nằm giữa theo chiều dọc. `ToolbarHeight: "20px"` cho ra 44, và trên iOS nút không tràn ra ngoài toolbar.
- [ ] `ToolbarHeight: ".header-top"`: toolbar cao bằng phần header dưới status bar (56 với theme mặc định).
- [ ] Nút Đóng nằm bên phải toolbar trên cả iOS và Android.
- [ ] `CloseButtonSize: 24` với `CloseButtonIcon: True`: chữ X to hơn (24), vẫn cách mép phải 16 và vẫn bấm được. Thử thêm `CloseButtonSize: 12`, và `CloseButtonSize: 15` với nút chữ.
- [ ] Chữ X (hoặc chữ "Đóng") cách mép phải 16pt/dp, bằng khoảng title cách mép trái, trên cả iOS và Android. Bấm vào thì đóng mini app.
- [ ] `Title: "Đối tác ABC"`: title nằm bên trái, cách mép khoảng 16, căn giữa theo chiều dọc với nút Đóng, cùng màu với nút. Title rất dài thì bị cắt bằng "…" và không đè lên nút Đóng.
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
- [ ] Trong mini app, bấm một link `http://...`: trang không chuyển, `OnError` báo "Only https pages may load (httpsonly)".
- [ ] Trang đối tác có iframe gọi `MiniAppBridge.send(...)`: app không nhận được message (Android: trên máy có WebView mới).
- [ ] Trang redirect về `https://partner.com/cb?code=abc`: `OnLoaded` nhận `https://partner.com/cb?code=hidden`.
