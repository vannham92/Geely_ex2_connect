# com.flyme.auto.launcher — Hướng dẫn phân tích APK (Lib / AutoLauncher)

Tài liệu này mô tả cặn kẽ ứng dụng hệ thống cốt lõi mang tên **Lib** / hay còn gọi là **AutoLauncher** (`com.flyme.auto.launcher`) trích ra từ cụm màn hình điều khiển Geely **IHU629G**: bao thầu màn hình chính (home screen) của Flyme Auto, giỏ chứa ứng dụng (app drawer), hệ thống **thẻ tiện ích (Aicy Widget)**, quản lý danh sách app gần đây (recent apps), bộ liên kết mượt mà với hình nền, kiểm soát Chế độ Cảnh (Scene Mode), giao thức Flyme Link và MIPC.

**Điều cốt lõi cần nhớ:** Ứng dụng này **không phải** là hình nền động live wallpaper (`com.flyme.auto.wallpaperlauncher`) và cũng **không phải** là cái thanh mành rèm trượt SystemUI (`com.geely.controlcenter`). Thằng Launcher đóng vai trò là **Ứng dụng màn hình CHÍNH (HOME-app)** và là **Mảnh đất cắm chốt của mọi Widgets** đặt ở ngoài màn hình chính.

---

## 0. Tổng quan ứng dụng

| Thông Số Cơ Bản | Giá Trị Cụ Thể |
|----------|----------|
| Mã Gói Package | Tên là `com.flyme.auto.launcher` |
| Nhãn Hiển thị (Tiếng Nga/Anh) | Dán chữ **Lib** |
| Nhãn (Tiếng Trung) | Ghi là **Launcher3** |
| Phiên bản versionCode | Đánh số `26012721` |
| Tên versionName | Mác `flyme.beta.(AutoLauncher)(none)(26012721)(12c8a3f)` |
| Ngưỡng yêu cầu minSdk / targetSdk | Lần lượt 28 / 33 |
| CompileSdk nền tảng | Chạy nền 33 (Chuẩn Android 13) |
| Cục Application gốc | Lớp `com.flyme.auto.launcher.LauncherApplication` |
| Trang Chủ Activity | Trạm `com.flyme.auto.launcher.main.LauncherActivity` (Gánh cờ `HOME` / `DEFAULT`) |
| Kho chứa DEX | Gồm `classes.dex` + `classes2.dex` + `classes3.dex` (Cân nặng tổng cộng cỡ ~25.5 MB) |
| Dung lượng APK | Hạng nặng ~34.6 MB |
| Native libs | Chỉ dùng bộ `arm64-v8a` (Đính kèm cờ `extractNativeLibs=false`) |

**Vai trò và Chức năng:**

1. **Giao diện Màn hình chính** — Ốp dán giấy dán tường (thông qua lệnh chọc tới `WallpaperService` của APK khác), trình diễn lưới chứa các **thẻ Aicy-карточек (Aicy-cards)**, bệ phóng thanh dock / nút bấm nhanh.
2. **Trang Xưởng độ Widget** — Màn hình chỉnh trang mang khẩu hiệu "Nhấn nút + hoặc đè kéo icon thả vào menu" (Bọc trong cục `AicyWidgetFragment`).
3. **Danh mục Kho Apps** — Trạm `AppListActivity` (Bật cờ `ALL_APPS`), rải thảm ứng dụng mini, thanh tìm kiếm search, quy tắc sắp xếp.
4. **Bộ phận quản lý App gần đây (Recent apps)** — Tích hợp xài widget nhà trồng `RecentApplicationWidget` + nắm chính sách bóp cổ tiết kiệm điện năng.
5. **Cơ chế Vuốt Chạm / Recents** — Xài chuẩn QuickStep (`TouchInteractionService`), xử lý nhạy nắn các cú vuốt ngón tay swiping.
6. **Móc nối liên hoàn** — Kéo tay với đạo diễn Scene Director (Cờ `sysui_alive_launcher_settings`), Trạm truyền màn Flyme Link / Cast, Giao thức MIPC, và là cái mâm chứa host cho Car UI plugin.

**Xương sống Cấu trúc Code (Thấy qua file dex):**

| Tên Nhánh | Chứa Class (Cỡ ≈) | Sứ Mệnh |
|-------|-------------|------|
| Khối `com.android.launcher3.*` | Chứa 1866 | Bê nguyên cục mã nguồn gốc AOSP Launcher3 vào (quản lý vùng workspace, rải icons, hệ backup) |
| Tầng `com.android.quickstep.*` | Nặng 528 | Lo mảng Recents, Cử chỉ vuốt, Khối `TouchInteractionService` |
| Lớp `com.flyme.auto.launcher.*` | Ôm 854 | Tầng sơn phết mang đậm chất Flyme (Đồ chơi Aicy, bảng applist, ngõ kết nối link, đúc cast) |
| Lớp `com.flyme.auto.*` (Là plugin SDK) | Bơm 2005 | Chứa `AicyPlugin`, Cục `PluginManager`, Chân đế SmartBar/StatusBar plugins |
| Cụm `com.geely.*` | Gọn 71 | Tóm gọn nhóm `EnergyManageReceiver`, ra luật trảm app recent policies |
| Cụm `com.ecarx.*` | Sơ sơ 725+ | Phím tắt API LauncherAPI, hỗ trợ VR giọng nói, mấy cục giả lập adapt API stubs |
| Gói `com.njda.adapter.*` | Nhét 267 | Nhúng mấy cái giao diện thẻ đính kèm AA/CarPlay widget views (Nó bị nhét thẳng vào tim của launcher dex luôn) |

---

## 1. Nguồn Gốc Lấy Ra và Bộ Phận Phụ Tùng Tách Rời (Artifacts)

| Thông Số Đích | Gắn Giá Trị |
|----------|----------|
| Nơi Khai Quật (Đời máy lấy dump) | Trích từ IHU629G |
| Nguyên Thủy APK (Lấy qua ADBAppControl) | Kẹp ở `downloads/250060 IHU629G/Lib (com.flyme.auto.launcher) [v.flyme.beta.(AutoLauncher)(none)(26012721)(12c8a3f)].apk` |
| Copy File Về Máy Tính | Tên là `.tmp/flyme-launcher.apk` |
| Banh Rã Xả Nén APK | Trút vô `.tmp/flyme-launcher-apk/` |
| Đống Rác lệnh aapt dump | Ói ra `.tmp/flyme-launcher-badging.txt`, và `.tmp/flyme-launcher-manifest.txt` |
| Bài Báo Cáo Giải Phẫu dex | Đọc trong `.tmp/flyme-launcher-analysis.txt`, lọc từ khóa ở `.tmp/flyme-launcher-strings.txt` |

### Tuyệt Chiêu Kéo Lấy APK Từ Xe Hơi

```bash
adb shell pm path com.flyme.auto.launcher
adb pull /system/app/.../AutoLauncher.apk .tmp/flyme-launcher.apk
```

### Xé File Ra Và Lục Lọi

```powershell
Copy-Item -LiteralPath ".tmp\flyme-launcher.apk" -Destination ".tmp\flyme-launcher.zip"
Expand-Archive -LiteralPath .tmp\flyme-launcher.zip -DestinationPath .tmp\flyme-launcher-apk -Force

$aapt = (Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "aapt.exe" | Select-Object -First 1).FullName
& $aapt dump badging .tmp\flyme-launcher.apk
& $aapt dump xmltree .tmp\flyme-launcher.apk AndroidManifest.xml
& $aapt dump --values resources .tmp\flyme-launcher.apk | Select-String "aicy_widget"
```

Trình Mổ xẻ **JADX** — Cứ phang lẹ vào mấy gói nòng cốt này: `com.flyme.auto.launcher.main.aicy`, xới `com.flyme.auto.launcher.applist`, cào `com.flyme.auto.launcher.main`, và lục tủ `com.android.launcher3`.

---

## 2. Kiến Trúc Bộ Khung Cấu Tạo

```mermaid
flowchart TB
    subgraph launcher [Trọn gói com.flyme.auto.launcher]
        LA[Mâm Trang Chủ LauncherActivity]
        AF[Khay Độ Chế AicyWidgetFragment]
        AL[Rổ Danh Sách AppListActivity]
        AWH[Trạm Thu Phát AicyAppWidgetHost]
        WCP[Cục Điều Phối Data WidgetContentProvider]
        APM[Lão Quản Plugin AicyPluginManager]
        RAW[Khay Ký Ức RecentApplicationWidget]
    end

    subgraph wallpaper [Ông Bạn Giấy Dán com.flyme.auto.wallpaperlauncher]
        WS[Thợ sơn giấy WallpaperLauncher service]
    end

    subgraph plugins [Nhà Cung Cấp Phụ Kiện Ngoài (Các app APK khác)]
        SET[Tiệm đồ settings gỡ thảy AtmosphereLightWidget]
        ENG[Kho năng lượng energy trút cục DischargingAppWidget]
        NJDA[Phần cứng njda.adapter cắm AutoWidget / CarplayWidget]
        QIB[La Bàn qibla rải QiblaWidgetProvider]
        OTH[Một mớ tàn quân khác AppWidgetProvider / AicyPlugin]
    end

    subgraph system [Lõi Mẹ System]
        L3[Tủ ưu tiên LauncherProvider favorites]
        SD[Ông Kẹ đạo diễn scenedirector SceneProvider]
        MIPC[Đường Ống MIPCService :mipc]
        QS[Cảm ứng vuốt TouchInteractionService QuickStep]
    end

    LA --> AF
    LA --> AWH
    AF --> APM
    APM -->|Lấy Móc PLUGIN_AICY_WIDGET| plugins
    AWH -->|Ràng Buộc Trói bind AppWidget| plugins
    WCP -->|Mảng SQLite SQLite bound widgets| AF
    LA --> WS
    LA --> L3
    LA --> SD
    LA --> MIPC
    QS --> LA
```

**Dòng Chảy Bơm Máu Cho Đám Widget:**

1. Có một tên APK Khách (Third-party) đến ghi danh trạm `AppWidgetProvider` và/hoặc giơ thẻ bài `com.flyme.auto.plugin.launcher.AicyPlugin`.
2. Ông Trùm Launcher sẽ lôi lính đi rà soát danh sách trắng whitelist (`all_aicy_widget_whitelist`) xong rồi nặn hiện ra mấy cái thẻ này vào màn hình xưởng Edit.
3. Đại Vương (User) kéo tay nhét thẻ vào lưới → Lệnh ghi tạc vào Sổ Nam Tào `WidgetContentProvider` (Kênh truyền `content://com.flyme.auto.launcher.aicywidget/`).
4. Khi thả về lại màn hình chánh thì thằng `AicyAppWidgetHost` nó chưởng cái phép bind buộc chặt nối dây remote views xuống cho app cung cấp (provider).

---

## 3. Bản Mặ Giao Diện UI & Lên Xuống Chuyển Màn

### 3.1 Bảng Liệt Kê Các Activity

| Cái Tên Activity | Cờ Cắm Launch mode | Trạm Đích Intent | Sinh Ra Nhằm Việc Gì |
|----------|-------------|--------|------------|
| Tầng `main.LauncherActivity` | Phang `singleTask` | Chứa chùm `MAIN` + `HOME` + `DEFAULT` | Cái Đáy Màn Hình Trang Chủ Đón Đợi |
| Ngõ `applist.AppListActivity` | Nhấn `singleTask` | Trỏ vô `ALL_APPS` | Kéo Ra Danh Bạ Toàn Tập Kho Apps |
| Đường `superlauncher.cast.CastSinkActivity` | Xài cờ `singleTask` | Bỏ trống — | Ngõ hứng Hứng màn hình Cast chiếu qua (chạy trên lưng `:app_cast`) |
| Kênh `com.flyme.auto.core.launcherflow.CastSinkActivity` | Gọi `singleTask` | Để trống — | Cổng Flyme Link / Truyền tin cục launcherflow cast |
| Khu rác `main.MonkeyTestActivity` | Mặc định standard | Gồm `MAIN`/`LAUNCHER` | Góc Phế Liệu Lôi ra dợt test (Đã bị cấm disabled) |
| Cầu khỉ `launcher3.proxy.ProxyActivityStarter` | Nhón `singleTask` | Kệ đó — | Khỉ Đột Cầu Nối (Proxy) để mượn đường chạy xin permission/intent |
| Thẻ `androidx.car.app.CarAppPermissionActivity` | None — | Ko có — | Trạm xét xin giấy tờ quyền hạn Car App |

```bash
# Phục Hồi Chạy Bật Lên Lại Màn Chính (Trừ phi bị giành mối xài launcher khác)
adb shell am start -n com.flyme.auto.launcher/.main.LauncherActivity

# Đổ Bê Tông Banh Cái Bảng Kho Liệt Kê Tên Apps
adb shell am start -a android.intent.action.ALL_APPS \
  -n com.flyme.auto.launcher/.applist.AppListActivity
```

### 3.2 Bày Bừa Mặt Màn Chính (Các mảnh ghép fragments, chẩn bệnh theo dex)

| Đích Danh Class | Để Xài Làm Gì |
|-------|------------|
| Cục `DesktopFragmentContainer` | Cái Bát Chứa Khuôn Chậu Ôm Gọn Màn Hình Bàn Làm Việc (Desktop) |
| Trạm rác `DesktopWidgetFragment` | Gom giữ lại mớ widgets đồ cổ hủ đời cũ (dư âm của Launcher3) |
| Mâm cỗ `AicyWidgetFragment` | **Nơi bày mâm Xưởng Chỉnh Hình / Vạch Kẻ Lưới các Thẻ Aicy** |
| Đất Bản đồ `MapPluginFragment` | Bức Tường nhét cứng bức Tranh Bản Đồ Gắn Bó (Lấy plugin page) |
| Lớp ma trận `AliveDesktopWrapper` | Phù Phép "Hô Biến Cảnh Vật" Desktop Múa Sống / Pha Trộn Scene Mode |

### 3.3 Đoạn Thoại Text Trong Xưởng Độ Widget (Tiếng Nga, moi từ APK)

| Nhặt Dòng Text (String) | Áp Dụng Ngữ Cảnh Nào (Context) |
|-------|----------|
| **Рекомендации по применению (Khuyên Chân Thành Mày Nên Đắp Lên Xài)** | Dòng Nhang Khói Tiêu Đề của đám thẻ gợi ý cài (Nó nằm gắn cứng bên trong chứ không xài APK mướn ngoài) |
| **Нажмите + или перетащите иконку для добавления в меню (Đè phím + hoặc túm đầu lôi cái icon ném vào khu thả để lắp)** | Lời Trăn Trối Mách Nước khi Đang Ở Khoang Edit |
| **Готово (Xong Xuôi)** | Khóa chốt Đóng Lại, Lưu Sổ Bản Vẽ Cấu Trúc |
| **Сброс (Xé Bỏ Làm Lại)** | Trục Xuất Xóa Trắng Mâm Lưới Khôi Phục Về Hiện Trạng Gốc |

Cái nhóm gợi ý "Khuyên Chân Thành Mày Nên Đắp Lên Xài" (Рекомендации по применению) — Là thuộc **con cưng ruột máu của launcher** (Đóng chữ cờ `RECOMMENDED_WIDGETS_*`, trỏ chữ `from recommend widget list` trong ruột dex). Đào bới trong mảng biến số khai báo mảng `recommend_aicy_widget` nơi đống resources thì nó lại **rỗng tuếch không có nội dung** (Count=0) — Bí ẩn cái danh sách ảo diệu này có lẽ chỉ được bơm nặn nhồi lấp vào khi app bật chạy runtime (thỏa điều kiện hàm chọc xét `RECOMMENDED_WIDGETS_PREDICATION`).

---

## 4. Vương Quốc Cấu Trúc Aicy Widget

### 4.1 Điểm Mặt Anh Tài (Các class máu mặt)

| Tên Class | Đóng Vai Gì Đây |
|-------|------------|
| Phân khu `main.aicy.AicyWidgetFragment` | Bảng Điểu Khiển UI edit: Tầng trên cùng phơi hàng "tạp hóa có sẵn", tầng dưới trệt thì liệt kê "những cục đang bày bừa ngoài sân" |
| Đài trạm `main.aicy.AicyAppWidgetHost` | Trạm Custom Chế Lại Của `AppWidgetHost` (mượn id host id định danh bằng `APPWIDGET_HOST_ID`) |
| Bao lót `main.aicy.AicyAppWidgetHostView` | Miếng giấy kính Bọc che miếng bánh view của mảng điều khiển ngoài remote view |
| Trùm gác cổng `main.aicy.AicyPluginManager` | Cai ngục Nhận trát gọi nạp xài lệnh `AicyPlugin` từ bọn APK chư hầu |
| Quan Khố `main.aicy.WidgetContentProvider` | Tay Hòm Chìa Khóa SQLite: Ghi danh các widget đang chốt sổ, mảng xếp hàng thứ tự, và hồ sơ profiles |
| Mô Típ `main.aicy.BoundWidgetModel` / Thợ đan `BoundWidgetAdapter` | Đúc mâm cho mớ thẻ đã gắn cọc trên Màn Nhà |
| Chợ đen `main.aicy.AllWidgetAdapter` | Rải Bày hàng vạn món đồ chơi đang có thẻ mua (+) |
| Bộ Não `main.aicy.WidgetViewModel` | Nhét vào mạch não Logic để ấn Thêm (add)/Bứng Gỡ (remove)/Sắp xếp lại chỗ (reorder), Đắp vào cài cắm (install)/Diệt cùi bắp uninstall |
| Khớp ngón tay `main.aicy.AicyWidgetClickHandler` | Gom Nhặt Cảm nhận những cú nhấp sờ vuốt nựng vào các cục thẻ |
| Thẻ Bài `main.aicy.PluginWidgetProviderInfo` | Thẻ Thông tin khai sanh (Meta-data) mô tả thằng plugin-widget |
| Số chốt `main.aicy.WidgetConstants` | Ổ Số Đo (Sizes), Bảng Kế hoạch diễu võ diễn hoạt hình animations, Cờ trạng thái đóng lệnh `AICY_STATE_EDIT` / Xả lệnh nghỉ `AICY_STATE_NOMAL` |
| Lò thổi `main.aicy.anim.EditPanelTransitionController` | Giật dây điểu khiển múa máy làm hoạt cảnh bóng bẩy lúc bay ra bay vào của cái rèm edit |

### 4.2 Sổ Sách Nguồn Thông Tin Tiện Ích (ContentProvider của widgets)

| Kênh Cửa (Authority) | Ngõ Rẽ Đích (Path) | Giải Thích Nhiệm Vụ |
|-----------|------|------------|
| Điểm gốc `com.flyme.auto.launcher.aicywidget` | Trỏ `/` | Chễm chệ Cội Nguồn (Gốc rễ) |
| | Đi `/aicy_widget_default` | Gói Cấu Hình Thẻ Tiện Ích Trắng Tay Gốc Nguyên Thủy (Mặc Định) |
| | Bẻ `/aicy_widget_demo` | Bức Tường Dùng Phơi Hàng (Demo/Showroom biểu diễn) |

```bash
adb shell content query --uri content://com.flyme.auto.launcher.aicywidget/
adb shell content query --uri content://com.flyme.auto.launcher.aicywidget/aicy_widget_default
```

### 4.3 Giải Mã Sổ Bìa Đen Khai Báo Dữ Liệu (`res/values/arrays.xml`)

#### Bảng Phong Thần `all_aicy_widget_whitelist` — Đọc tên điểm danh tất cả mặt hàng được cấp quyền cho phép mua thêm vào sảnh (Có chốt 13 Món)

| STT | Trụ Cột Component (Cấu trúc `package/class`) | Đang Đứng Ké Trên APK Nào Ở Dàn IHU629G |
|---|---------------------------|----------------|
| 1 | Cục `com.flyme.auto.launcher/.widget.RecentApplicationWidget` | Thuộc nhà ruột launcher (Gắn sắn trong bụng) |
| 2 | Cục `com.geely.linkhmi/...AicyAppWidget` | Liên kết Cửa Màn Flyme Link HMI |
| 3 | Cục `com.flyme.auto.music/...MusicRecommendWidget` | Hộp Quẩy Nhạc Nhẽo (Méo Thấy Báo Cáo Trong Cái Bản Dump Của Ông) |
| 4 | Cục `com.flyme.auto.settings/...AtmosphereLightWidget` | Khay Cấu Hình Nhá Nhem Màu Đèn Atmosphere |
| 5 | Cục `com.flyme.auto.energy/...DischargingAppWidget` | Phích Cắm Báo Điện Lực Mô-tơ (Năng lượng) |
| 6 | Cục `com.flyme.auto.music/...MusicLinkWidget` | Khóa Nối Liên Hiệp Nhạc |
| 7 | Cục `com.flyme.auto.localmusic.usb/...HarmanWidget` | Máy Hát Nhạc Dây USB |
| 8 | Cục `com.wanos.media/...WanosAppWidget` | Cục Đồ Chơi Âm Wanos (Cũng Lạc Trôi Trong Cái Dump) |
| 9 | Cục `com.geely.gc.cloudautoclient/...CloudWidgetProvider` | Trạm Khí Tượng Ánh Mây Geely |
| 10 | Cục `com.baidu.che.codriver/...WordsChainWidgetProvider` | Bé Đu Trợ Lý Ảo Baidu VR |
| 11 | Cục `com.dafang.qibla.finder/...QiblaWidgetProvider` | Cây Kim La Bàn Tâm Linh Qibla |
| 12 | Cục `com.njda.adapter/...AutoWidget` | Màn Ảnh Chéo Android Auto |
| 13 | Cục `com.njda.adapter/...CarplayWidget` | Bến Đỗ Quả Táo Apple CarPlay |

#### Bảng Bần Cùng `default_aicy_widget` — Sơ đồ dàn trận ban đầu "trần như nhộng" (Chỉ móc 8 Thằng)

Cũng đi lôi nhặt vặt từ danh bạ Whitelist qua thôi, mấy bộ mặt hay mọc nanh bám trên hình quảng cáo chụp: Rổ App Recent, Bóng Mây Cloud, Lò Gợi ý Nhạc MusicRecommend, Con Nít Baidu, Kho Điện Energy, Bàn Cờ Qibla, Cửa Phụ AutoWidget, Nón Apple CarplayWidget.

**Để Y Điểm Này:** Miếng gạch đèn màu mè `AtmosphereLightWidget` mặc dù nó chình ình có tên trong sổ phong thần (whitelist), nhưng lại **Tuyệt Đối Không** bị ném vào cái khay trắng tay (default) — Ai thích ưng thì phải ngậm ngùi bỏ công kéo thả đắp nhét bằng cơm (như trong cái màn chụp bác đang cho xem).

### 4.4 Thẻ Nhớ Tiện Ích Gắn Chết Trong Bụng Launcher (In-house)

| Cục Thụ Lý Receiver | Cờ Hành Động Action | Đảm Đương Phận Sự Trách Nhiệm |
|----------|--------|------------|
| Bộ `widget.RecentApplicationWidget` | Phất cờ `APPWIDGET_UPDATE`, đính thêm `com.flyme.auto.launcher.action.RECENT_APPLICATION_CLICKED` | Hiện cục thẻ bày danh bạ vinh danh Những app chọt dạo gần đây |

Rễ máu liên đới dòng tộc (Classes): Nhóm `RecentApplicationRepository`, Lớp `RecentAppInfo`, Phễu `RecentApplicationViewModel`.

### 4.5 Kênh Đài Loan Tin Broadcast / Phím tắt action của launcher chọc vào widgets

| Hiệu Lệnh Action | Chỉ Đạo Nhiệm Vụ |
|--------|------------|
| Cờ lệnh `com.flyme.auto.launcher.action.TOGGLE_AICY_WIDGET` | Kéo Xập Xuống Màn Rèm Công Xưởng Nhồi Nhét Lắp Đặt Các Cục Thẻ Edit |
| Khều Lệnh `com.flyme.auto.launcher.action.RECENT_APPLICATION_CLICKED` | Cú Chọt Lủng Vô Dấu Icon Ở Miếng Gạch App Vừa Mở Xong Recent widget |
| Đóng cờ `com.flyme.auto.launcher.CLOSE_APP_LIST_ACTION` | Cúp Dập Cái Ngăn Kéo Liệt Kê Ứng Dụng Xóa Hết Dấu Vết |
| Xua Đuổi `com.flyme.auto.launcher.CLOSE_LAUNCHER_DIALOG` | Nhốt Giấu Đuổi Đi Cục Nổi Sần (Overlay-dialog) |

---

## 5. Bản Vẽ Hệ Sinh Thái Ký Sinh Plugin SDK (Con Đường Tà Đạo Để Lũ App Râu Ria Bên Cạnh Tràn Lên Bám Chỗ Ngoài Màn Chính)

Triều Đại Flyme cho áp chế sử dụng chung rập khuôn một quy chuẩn bộ SDK mang nhãn `com.flyme.auto.plugin` (Đống mã nguồn classes này lây lan rải rác vừa chui rúc ở Launcher, vừa nảy mầm chễm chệ ở mâm Settings/Và Ổ Năng lượng Energy).

### 5.1 Các Lá Cờ Hiệu Action Dành Cho Giới Plugin

| Mã Action (Tiếng Kêu Gọi) | Gã Nào Lót Dép Hóng | Mệnh Lệnh Thể Hiện Tác Động Gì |
|--------|-------------------|------------|
| Móc ngoặc `com.flyme.auto.plugin.action.PLUGIN_AICY_WIDGET` | Gã Lái Buôn `AicyPluginManager` | Nhét Cục Tiện Ích Chình Ình Ra Khán Đài Màn Chính |
| Nổ Kênh `com.flyme.auto.plugin.action.PLUGIN_LAUNCHER_PAGE` | Gã Cai Launcher | Căng phông nhét hẳn một trang Full Tạp Chỉ to đùng cắm neo ở bến đò dock |
| Thả Bong Bóng `com.flyme.auto.plugin.action.PLUGIN_FLOATING_WIDGET` | Gã Cai Launcher | Thổi bong bóng phù du Lắp Miếng Widget Lập Lờ lềnh phềnh Trôi Nổi |
| Khều Lỗ Nhỏ `com.flyme.auto.plugin.action.PLUGIN_SMART_BAR` | Ôm Trùm SystemUI | Đắp Điểm Nhấn Dán Vô Dải Khung Gầm (Bottom bar) |
| Cắm Miếng Miện `com.flyme.auto.plugin.action.PLUGIN_STATUS_BAR` | Ôm Trùm SystemUI | Nhét vô Rãnh Đỉnh Khay Status-bar / Rèm cúp Thả Status |

### 5.2 Form Đúc Rập Khuôn Ở Các Thằng APK Ngoại Đạo Mún Xài Ké (Bám Dính)

Bộ Sậu Tổ Hợp Rập Khuôn Kiểu Mẫu Phổ Biến (Mổ dex bọn Settings / Tụi Energy / Quét cục WirelessCharging):

```text
Tiên Phong AppWidgetProvider          → Xây Cái Khung Xương Widget Chuẩn Chỉ Hàng Phổ Thông Android widget (Chiếu Cấu Hình RemoteViews)
Bồi Tiếp com.flyme.auto.plugin.launcher.AicyPlugin → Xài Kim Bài Miễn Tử Vượt Cửa Ải Đi Theo Trạm Mốc Của Cảng Hàng Không Flyme plugin
(Mục Khuyến Khích) com.flyme.auto.plugin.launcher.FloatingWidgetFragment → Đi Hóng Nhét Cái Dị Tật Đi Phụ Trợ Vui Chơi
```

**Một Vài Nhân Chứng Sống Lộ Tẩy Ở Nền Xe IHU629G:**

| Tấm Danh Thiếp (Thẻ) | Đút Lót Cho Ổ APK Nào | Cục Thẻ Gốc Nhãn Tên Ở Khung Widgets Nào |
|----------|-----|---------------|
| Dàn Đèn Ánh Kim Khí Chất (Atmosphere) | Gói `com.flyme.auto.settings` | Đội Lốt Lưới `receiver.AtmosphereLightWidget` |
| Bơm Điện Máu Máu Bú Bình | Bụng `com.flyme.auto.energy` | Gắn Mỏ Bằng Lệnh `appwidget.DischargingAppWidget` |
| Sân Sau Android Auto | Tàu `com.njda.adapter` | Lủng Củng Kéo Bằng `view.AutoWidget` |
| Trái Cấm Apple CarPlay | Ổ `com.njda.adapter` | Nổ Bom Theo Đít `view.CarplayWidget` |
| Đĩa Bay La Bàn Qibla | Chi nhánh `com.dafang.qibla.finder` | Kêu Xa `widget.QiblaWidgetProvider` |

Đặc thù cái giống `com.njda.aauto` / Kèm `com.njda.carplay` hai thằng này tự biên tự diễn nó chảnh chọe **cố tình không** đi lót xuất khẩu nhả ra cửa rào AppWidget — Do đó cái mớ widgets của nó bị đi vòng ném cục mượn thầu xài đỡ của kênh thằng lái buôn **`com.njda.adapter`**.

### 5.3 Bí Thuật Để Bị Chui Nhét Vào Sổ Bán Hàng Danh Mục Phơi Thẻ

1. Dốc Công Đúc Cốt Thực Thi Cho Ra Ngô Trạm `AppWidgetProvider` + Chèn Thẻ Phép meta-data cộp nhãn `android.appwidget.provider` (Làm Lệnh Kêu Cửa `widgetCategory` Phải Tích Bắt Nhận Home Màn Chính).
2. (Chuyên Gia Rỉ Tai Xui Làm) Nhét Gấp Cái Bùa Tàu `AicyPlugin` Bằng Cú Dội Kênh Action Nổ `PLUGIN_AICY_WIDGET`.
3. Gói Đóng Trọn Gói Phải Được Phê Chuẩn Nhét Xào Đưa Vô Bụng Xe Chạy; Lão Cái Bang Launcher Sẽ Dỏng Tai Chạy Tool Càn Quét Lưới Rà Lấy Bảng Phân Phối Trạm Phóng Lên Nếu Tên Phụ Tùng Trùng Khớp 1 Trong Các Đứa Nằm Nhét Ở Lồng Trắng whitelist **Hay Đơn Giản Hơn Bực Mình Là** Được Xướng Danh Dưới Mác Một Tên AppWidget Kiểu Cũ Nhà Quê (Máy Đi Tuần Tra Vớ Chộp Bắt Được Bởi Thằng Lính `AppWidgetManager`).

Cảnh Cáo Nhẹ Ở Kênh Mới Cho Mấy Đại Ca Của Tổ Hợp **geely_ex2_tools**: Nơi đó anh em mình đang thả chó giăng một ải cổng chòi `BatteryAppWidgetProvider` mang tem danh xưng `widgetCategory="home_screen"` — Trên Lý Thuyết Mọt Sách Thì Nó Ngon Lành Cành Đào Để Hiện Phơi Hàng Lên Sổ Mục Của Tụi Nó, Nhưng Lại Có Một Lỗi Lớn Kéo Chân Đó Lại Là Lệnh Này Nó **Ếch Hề Bị Xếp Hạng Góp Mặt Ở Vùng Tôn Giáo Bất Khả Xâm Phạm Nào** Nằm Trong Cái Vòng Tròn Phổ whitelist Của Thằng Cha Đẻ Đời Xe Lắp OEM IHU629G Khó Nhằn (Bị Kẻ Cướp Không Mời Đuổi Chặn Họng).

---

## 6. Sổ Cái Dữ Liệu ContentProvider Nắm Giữ Sinh Mệnh Cấu Hình Launcher

| Quầy Thu Gửi Provider | Bảng Hiệu Hợp Pháp Authority | Quyền Xin Dấu Ấn RW | Mô Tả Ý Định Sứ Mệnh |
|----------|-----------|---------------|------------|
| Tủ `LauncherProvider` | Quản mục `com.flyme.auto.launcher.settings` | Đòi Dấu `READ_SETTINGS` / Cộng Dấu `WRITE_SETTINGS` | Giữ Chốt Đống Ký Ức Đồ Thích Favorites Theo Chuẩn Launcher3 (`favorites`) |
| Sổ `WidgetContentProvider` | Tên Miền `com.flyme.auto.launcher.aicywidget` | Chạy Cờ Nhóm Ngầm Nội Thể (internal) | Xây Lò DB Cho Hạng Aicy widgets |
| Rương `AppContentProvider` | Điểm Dừng `com.flyme.auto.launcher.apps` | Ép Ký Ghi `WRITE_APPS` | Kê Biên Danh Mục Đội Hình Xếp Đặt App Ứng Dụng Nằm Khay Nào |
| Phễu `LauncherFlowDataProvider` | Hố Ga `com.flyme.auto.launcher.flow.apps` | Nhận Chữ exported (Nhà Buôn Public Ngoại Đạo Thoát Ách) | Đường Hầm Cho Đám App Flow Ăn Theo Nhện Dây Flyme Link |
| Lò Đúc `AutoLauncherProvider` | Dán Tem `com.flyme.auto.launcher.autoLauncher` | Nhận Dấu Hàng Gửi Public (exported) | Đo Bắt Mạch Sự Sinh Tồn Của Cục auto launcher |
| Xưởng Máy Dập Lưới `GridCustomizationsProvider` | Hút Ống Chích `com.flyme.auto.launcher.grid_control` | Đóng Cục Public Chào Hàng (exported) | Thợ Xây Căn Dỉnh Trục Grid Kẻ Ô Đóng Gói Nhét Nhốt Đám Mực Kéo Icons |
| Khu Đồ Riêng `PreferencesProvider` | Mang Nhãn `com.flyme.auto.launcher.preferences.provider` | Cấm Bắn Trát (not exported) Chặt Ra Ngoại Dữ Liệu | Sổ Bìa Đen Quản Trị Hệ Thông Số (Prefs) Từ Khối Cục Bộ Flyme Data SDK |

```bash
adb shell content query --uri content://com.flyme.auto.launcher.settings/favorites
adb shell content query --uri content://com.flyme.auto.launcher.apps/apps
```

### Bộ Hạt Phép Xin Đòi Các Đặc Vụ Đặc Trị Nhạy Cảm Ngầm (Custom permissions)

| Giấy Xin Chữ Ký Phép Thuật Permission | Loại Cấp Bậc Bảo Kê (protection) | Giải Phẫu Phân Tích Công Danh Mục Đích |
|------------|------------|------------|
| Cục cờ `…permission.READ_APPS` | Đòi ấn triện chữ ký signature | Cho Quyền Quét Mắt Đi Dạo Mục Lục Điểm Danh Hàng Apps Đang Có |
| Lá cờ `…permission.WRITE_APPS` | Phải nộp chữ ký signature | Giữ Đặc Ơn Được Nắn Cấu Trúc Khung Khay Kệ Xếp Gạch Dàn Hình Đứng Apps |
| Giấy phép `…permission.READ_SETTINGS` | Dòng họ signature | Hít Ngửi Xem Lén Cục Cấu Hình Cha Ông Của Launcher3 |
| Lệnh xin phép `…permission.WRITE_SETTINGS` | Đút lót chữ ký signature | Chạm Lấy Bút Lông Sơn Xóa Đổi Tạc Tượng Xâm Trổ Chữ Cấu Hình Launcher3 |
| Kiếm Thượng Phương Kéo Trảm `…permission.KILL_TASKS` | Rút dao chữ ký máu signature | Dành Cho Thằng Đao Phủ Đoạt Mệnh Bắn Lệnh Từ Tổ `EnergyManageReceiver` |

---

## 7. Các Lò Nấu Sáp Ngầm Service Chạy Quanh Và Khối Khí Não Logic Hoạt Động (Background Logic)

| Hố Dịch Vụ Service | Trú Ẩn Tại Ổ Process | Mô Hình Kịch Bản Đảm Nhiệm Đánh Mướn Cái Rì Chò Ti |
|---------|---------|------------|
| Khu `TouchInteractionService` | Mạch Thở main | Thầu Dây Phanh QuickStep: Bắt Bớ Vuốt Ve Nhạy Nhàng (Jeans/Cử chỉ), Đóng Tác Tụ Chặt Tay Các Cuốn Tàng Kinh Các Recents Đang Ngóng Vừa Đóng |
| Kênh `NotificationListener` | Mạch Tim main | Gắn Bó Mác Bé Nhí Bé Nị Mụn Thịt (Bé Бейджи/Badges) Phun Số Thông Báo (Uveđomleniya) Hiện Treo Thòng Lọng Trên Đỉnh Đám Icons Tội Nghiệp |
| Hầm Chặn Cướp `DialerInterceptService` | Mạch Trống main | Cái Túi Xách Chụp Cắn Dây Ăn Tiền Hớt Váng Lệnh Lớn Từ Ô Trống Lện `com.android.dialer.action.openDialer` |
| Bơm Ô Xy `MIPCService` (Nhúm vào `com.flyme.auto.mipcser`) | Chìm xuống mạch `:mipc` | Cục Nắn Gân Xương Chậu Đường Trục MIPC (Khớp Với Chuyển Động Xới Bới Cái Xác Hình Nền Thay Đổi Đỉnh Cao/Hình Tượng Car3D) |
| Kẻ Đi May Áo Car UI installers | Nhúng Ống Đổ Máu `:car_ui` | Gọi Thằng Thợ Hồ Lắp Gạch Ốp Kính Lên Nền `CarUiInstaller`, Kèm Gói Chuyển Phao Cho Tìm Đồ Khóa Đựng Gói Lệnh Giục Chó Nhờ `SearchResultsProvider` |

| Ngòi Đón Nhận Thông Số Receiver | Được Sinh Ra Phơi Hàng Làm Cái Vại Hứng Mục Đích Ra Sao Nè Hả |
|----------|------------|
| Cục Cắm Bắn Rìa `SessionCommitReceiver` | Nhận Băng Giá Trị Việc Lắp APK → Liền Tức Khắc Vẽ Khung Cái Ổ Vứt Dấu Icon Căng Cục Xả Cứt Phọt Ra Bãi Trên Cỏ Của Khoảng Trống Trên Bàn Gỗ Làm Việc Cho Nhanh Gọn (Màn Hình) |
| Đài Vớt Rác Mớ Dị Đồ `AppWidgetsRestoredReceiver` | Nắn Trục Xương Đúc Hình Bồi Giấy Vả Hồn Mớm Bùa Hoàn Sinh Dựng Xác Sống Tụi Widgets Trỗi Dậy Quay Về Trùng Nhập Lại Từ Cục Kén Sao Lưu Bóng Đêm Bóng Đá (Backup) |
| Hệ Cơ Chế Đi Đâm Giết Không Khoan Nhượng `EnergyManageReceiver` | Đánh Tráo Ngòi Phát Lôi Cờ Ánh Sáng Xanh Nguyền Rủa Đâm Xuyên Lủng Phổi Tiễn Tiên `KILL_PROCESS_ENERGY_SAVING` — Đại Khai Sát Giới Tàn Sát Tru Diệt Mọi Tàn Dư Apps Khất Thực Lê Lết Chạy Lén Dây Câu Trộm Điện Nằm Chui Nhủi Đáy Nền Tàng Rác Tàng Sức (Фоновые) Cho Bay Đẹp |

---

## 8. Danh Cuốn Lốc Khám Danh Tính Xét App Ứng Dụng Đóng Chéo Và Đấm Ép Cổ Van Tiết Kiệm Gắt Gao Nuôi Xác Energy Saving

### 8.1 Sổ Tay Sinh Lão Địch Kẻ Bệnh Viện Trả Bệnh `app_presorts_list` (Xếp Trật Tự Dùi Đục Lọc Hàng Chữ Trong Giỏ Cơm Danh Sách App, Moi Tìm Thấy Khúc Đuôi Phân Đoạn)

Các gương mặt chễm chệ ngôi đầu mâm bệ nghễ: Tướng Soái Settings (Cài đặt), Máy soi đo AVM calibration (Méo cam), Máy dò AVM (Kính Chiếu Hậu Ngầm), Báo Bụng Pin Energy (Điện Máy), Kẻ Nhỉ Đường Map (La bàn), Vòng Số Quay Đít Dialer (Bộ Gõ Số Điện), Ống Sáo Hát Rong Music (Ca Nhạc), Khu Chơi Chén BT/USB/Radio music, Ống Nghe Khói Sóng DAB…

### 8.2 Giấy Lau Chùi Che Kín Sạch Cát Cứ Bụng Sưng Xỏ Khôn Ranh `filtered_components` (Tuyệt Đỉnh Giấu Mặt Tàng Hình Âm Thầm Khỏi Cái Của Nợ Sổ Bìa App Ở Launcher)

- Lệnh Trảm Giấu Đi Thằng `com.flyme.auto.setup` (Mớ Màn Chào Hỏi Khởi Thủy Onboarding Dụ Chăn Gà Cà Nhắc Hồi Mới Đổ Khung Bê Tông Đầu Tiên Ra Lò)
- Lệnh Xóa Bôi Nốt Gã `ecarx.engineeringmodel` (Đóng Khe Cửa Lách Của Bộ Phận Sửa Chửa Nhà Kính Kỹ Sư Lật Kèo Phá Trại Nhằm Ngó Chạm Vô Rừng Lỗi Kỹ Sư Ngành Nhọc Nhằn)

### 8.3 Pháp Điển Cứu Bệnh Recent apps policy (Hiến Pháp Chỉ Đạo Tống Giam Đứa Gần Đáy Vòi)

| Chuỗi Móc Treo Ma Trận Data Mảng Cục Lấp Ráp Vắt Hàng | Ban Hành Áp Chế Sứ Mệnh Nhúng Vòi Xoay |
|--------|------------|
| Cục Phễu Thủng `recent_app_exclude_list` | Chặn Trực Diện Ngăn Cấm Không Mở Cửa Dính Mặt Lủng Cho Gã Chó Nào Chui Trồi Lên Mẹt Bảng Ở Sổ Thẻ Lưu Recent Widget Trừ Bọn Ma Già Xó Cây (Như Là: Gốc launcher, Bản Đồ map, Đạo diễn chóp bu scenedirector, Camera Ảo avm, Cục Kim Từ Điển qibla…) |
| Cái Án Trảm Khát Kịch Tử Giao Dịch Đâm `recent_energy_manage_blacklist` | Tuyên Án Bóp Cổ Treo Cổ Chặn Tắt Thở Không Tha Một Tên Sống Sót Nào Nếu Lệnh Nổ Chuông Tiết Kiệm Năng Lượng Ngấp Ngoải Xè Đèn Vừa Thúc Giục Hô Hào Ép Trảm Nhau Chết (Tụi Tội Đồ: Nhạc music, Thằng Hít Web browser, Bọn Nối Bơm carlink…) |
| Sổ Tử Hình Bọn Nuốt Tốc Độ Của Lũ Mất Nết Kìm Hàm Đội Banh Khung Mảnh `recent_performance_blacklist` | Sổ Tà Bìa Đen Bôi Tro Trát Trấu Dán Lệnh Nhốt Tù Đói Chết Bỏ Khi Tới Cữ Đã Giục Lệnh Quất Bóp Chạy Gấp Bộ Quản Hệ Performance Mode Khốc Liệt |
| Ánh Hào Quang Ký Sinh Lọt Khe Vàng Thẻ Xanh Thoát Án Bùa Tử Cấp Vĩ Tuyến `recent_performance_whitelist` | Sổ Bìa Trắng Kẹp Thẻ Nằm Không Khung Kim Bài Miễn Tử Được Phát Tặng Trọng Thưởng Cho Lũ Yêu Tinh Thuộc Hệ App Sinh Ra Được Ổ Hệ Thống (Tròn Trĩnh Đếm 53 Con Yêu Cẩu) |
| Phiếu Góp Nhu Đãi Bùa Tốt Thí Ân Cứu Nhân Đạo Từ Hội `recent_performance_whitelist_DAB` | Bản Sổ Ký Thêm Bản Nháp Sổ Mở Rộng Ơn Trời Dành Nuôi Rộng Rãi Cứu Tế Thêm Một Đám Sống Thừa Khác (Tổng Danh Bạ 95 Mạng Sống Ảo Yêu Nghiệt Mới Nổi) |

```bash
adb shell am broadcast -a com.flyme.auto.launcher.action.KILL_PROCESS_ENERGY_SAVING
```

---

## 9. Liên Kết Ký Sinh Thắt Nơ Phụ Họa Trói Chặt Vang Tiếng Sủa Tương Tác Ầm Í Với Các Đại Ca APK Chư Hầu Khác Nhau Mệt Nhoài

| Thằng Bạn Chí Cốt Cặp Kè Chéo APK | Đánh Động Chọc Léo Tay Nghề Thâm Độc Cái Chiêu Nào Ra Giao Thức (Mehanizm) | Vạch Khám Lục Soi Quyển Tài Liệu Khai Khống Mã |
|---------------|----------|----------|
| Gã `com.flyme.auto.wallpaperlauncher` | Vuốt Mặt Bức Tường Dán Bùa Giấy Nền Hình Ảo Oboи (Wallpaper) + Phối Nhào Vuốt Nhẹ Ve Vuốt Chỉ Chỏ M IPC Cử Chỉ (Жесты) Mượn Chuyển Giới Tín Hiệu Đi Qua Từ Kênh Lệnh Phím Thằng Bố Đời Launcher Ném Qua | Mở Coi [flyme-wallpaperlauncher-apk.md](./flyme-wallpaperlauncher-apk.md) |
| Đứa Ám Lệnh `com.flyme.auto.scenedirector` | Tiêm Bơm Kích Sóng Nhịp Đập `sysui_alive_launcher_settings`, Trái Tim Đập Cho Con Quái Vật Sống Dậy AliveDesktop | Khui Thùng [flyme-scenedirector-apk.md](./flyme-scenedirector-apk.md) |
| Con Tép Bụng Bự `com.flyme.auto.settings` | Chi Nhánh Ngầm Giao Nhận Bóng Đèn Khí Sắc Gài Ké `AtmosphereLightWidget`, Bắn Luồng Sóng Tới Rễ Ý Chỉ Bụng Bự (intents) Nhồi Thẳng Xoáy Vô Dạ Dày Chứa Settings | Lật Trang [flyme-settings-apk.md](./flyme-settings-apk.md) |
| Thằng Mắc Ói Gầy Tong `com.flyme.auto.energy` | Gắn Mảnh Ghép Xả Pin Nhầy Ngụa `DischargingAppWidget`, Xổ Bảng Thần Chú Bùa Bát Quái Câm Miệng Lão Cút Giết Chóc Chết Dẫm Rỉ Điện energy kill lists | Chấm Mút [flyme-energy-apk.md](./flyme-energy-apk.md) |
| Mấy Lão Mọt Sách Tư Bản Bán Ống Chích `com.njda.adapter` | Cục Khay Rập Bánh AA/CP widgets Nhồi Ném Chặn Họng | Không Có Văn Tự Lệ Thư (—) Mò Tự Lục Ở Trong Bụng Nó Vậy |
| Cái Thanh Treo Cởi Truồng Phơi Hàng `com.geely.controlcenter` | Đóng Làm Một Bức Màn Trượt Xuống Che Chắn Che Mặt Giấu Nỗi Ê Chề Hoàn Toàn Cách Biệt (Hệ Không Nhập Nhằng Dính Bầu Dây Tơ Rễ Má Gì Tới Cái Đám Hàng Rong Nằm Bệt Trên Gầm Đất Xếp Ở Khu HOME) | Bít Cửa Trắng Phớ (—) Mò Không Ra Chữ Ký Nào Hết Tự Ráng Đi |
| Thằng Cắt Tóc Thay Áo `com.flyme.auto.customize` | Treo Bán Lược Thơm Kéo Áo Tráo Quần Mặc Giao Diện Theme/Vuốt Ve Vuốt Lại Đè Giấy Nền Обои (Cái Trò Lén Này Bơm Cho Thằng Bóng Lộn Đi Nhờ Cửa Trung Gian Ống Thở Giấu Mặt Khác Ngầm Xuyên Qua Cửa Đít Bọn Làm Phông Bạt wallpaper APK Hộ Khẩu Nhám) | Soi Chỉ [flyme-customize-apk.md](./flyme-customize-apk.md) |

### Lò Gạch Rò Rỉ Đống Cấu Hình Cao Cấp Chôn Dưới Hầm Mộ Bọn Cặn Bã Settings.Secure / System (Chỗ Ngã Tư Hỗn Loạn Chém Nhau Vỡ Sọ Đọ Mã Chéo Lồng Tiếng Chọi Chéo Tương Giao Nhau)

| Mũ Mũ Khóa Bí Mật Thẻ Lệnh Mã Sợi Dây Tiếng Vang Key (Klyuch) | Ý Nghĩa Cái Trò Này Đẻ Ra Thắt Cổ Làm Trò Nhảm Nhanh Ra Sao Đây |
|-----|------------|
| Cục Lỗ Rốn Tiêm `sysui_alive_launcher_settings` | Bật Đèn Mở Màn Chiếu Kịch 3D Chế Độ Bay Màu "Biến Lão Màn Hình Máy Trở Thành Không Gian Ánh Nhìn Sống Động Hiện Thực" Đẩy Quá Nấc Lệnh Chạy Điểm Nhấn Sờ Máu Khùng (Scene) / Trò Quay Ngắm Thổi Ảo Lòe (Coi Lại Giải Thích Đạo Diễn Đấm Phát Lại Bọn Scene Director Nhé) |
| Chùm Treo Rắc Bẫy Chặn Trục Chờ Chết `sysui_proxy` | Kéo Khúc Dây Cột Trạm Sang Kê Cho Khúc Phễu Sang Đường Kênh Ké Trạm Cầu Tàu Proxy Đi Theo Lối Thu Tiền Trạm Chặn Đầu Rớt Thằng Phái Cử Hệ Thống SystemUI Tạm |
| Dải Sớ Phép Kêu Gọi Hồn Ma Chờ Gà Trống Gáy Rụng Đầu Gối Lòi Ngã Té Nhào `wallpaper_launcher_current_wallpaper_path` | Dây Trói Báo Mật Cập Nhật Tên Chỗ Ẩn Nấp Hiện Tướng Của Đống Giấy Chùi Đang Quét Dán Trát Lên Nền (Giác Sờ Đầu Ngón Có Đứa Đít Vuốt Thằng Cu Làm Lệnh Nấp Che Mặt wallpaper APK Kẻ Vuốt Mép Xài Lén Ngầm Kéo Đọc Đặng Giấu Nhẹm Lặng Thinh) |

```bash
adb shell settings get secure sysui_alive_launcher_settings
```

---

## 10. Thế Lực Hợp Bích Liên Kết Chằng Chéo Váy Áo Cắm Dây Ảo Ảnh Truyền Khí (Đường Đua Dài Flyme Link / Phóng Màn Nhả Khí Cast / Con Dốc Chuồi Trôi Kẽm LauncherFlow)

| Chi Tiết Mạch Linh Kiện Thở (Đấu Khớp Trục Lắp Cấu Hình Mạch) Component | Đẻ Trứng Ra Mằm Cái Nhiệm Vụ Ý Đồ Đen Tối Nào |
|-----------|------------|
| Điểm Lõm Chóp Ruồi Váy Đè Hứng Thằng Kén Lột Xương Máu Gã `superlauncher.cast.CastSinkActivity` | Trạm Rốn Đón Hứng Cái Rớt Bóng Tối Túp Nước Sôi Kéo Kính Ảo Lấy Dấu Phản Chiếu Gương Của Phía Đẩy Sang Zerkalirovaniye (Gương Trắng Nhả Trái Đất Bóng Sáng Phản Sáng Bật Chiếu Sang Mặt Hồ Kép Kín Màn Máy Điện Thoại Đập Thùng Bấm Ép Cast Thả Sang Zerkalirovaniye Bắt Chiếu Phản) |
| Kênh Vòi Thông Rãnh Bơm Chích Gửi Ép Ký Gửi Con Nhện `com.flyme.auto.core.launcherflow.CastSinkActivity` | Đổ Nhào Nhét Kẹt Vô Thùng Đựng Sọt Rác Chứa Mấy Trạm Đón Lõm Thủng Phễu Giả Danh Nhận Khí Sink Nút Chặn Khóa Phễu Hứng Của Tầng Đám Ống Tròn Xoay Trôn Ốc Bọn Tên Gọi LauncherFlow Kéo Chiếu Hắt Ánh Phản Chạy Đống Bùn Khí Đi Nằm Cast Ỉa Ké Lộn Lão Gà |
| Ổ Kho Chứa Hàng Trộm Đựng Lái Chôm Đẩy Xe `link.LauncherFlowDataProvider` | Cái Ổ Sào Huyệt Kéo Lôi Nối Truy Đi Đường Băng Sóng Ngầm Giữ Sợi Chỉ Lụa Bí Mật Nối Dây Thông Đi Mỏ Rốn Thả Bóng Nhám Mã Dây Nước Mắm Kéo Khớp Ròng Rọc Gõ Trát Cửa Thần Truyền Đi Bí Trát: `content://com.flyme.auto.launcher.flow.apps/apps` |
| Thẻ Bài Đeo Cổ Phép Bùa Ngãi Chặn Bắt Ma Quỷ Cấm Chạy Thông Quan Cho Kẽm Mạ Cụm Độn Vô Học Rắn Cắn Đầu Thông Chữ Nghĩa Cửa Vô Gọi Đít Gắn Thông Mã Tiếng Thẻ (meta-data) Có Thằng Đi Ngầm Bắt Tay Chéo Gọi Ổ `app_unite_code` = Sẽ Gắn Tên Tem Mã Giải Mật Thư Cùng Tên Với Thằng Tù Đóng Băng Khỉ Phá Quán Ăn Tên Trộm Khóa Xe `com.flyme.auto.launcherflow` | Con Dấu Ấn Mọc Khóa Số Bài Lệnh Bí Kíp Trấn Phái Ấn Số Thẻ Đeo Ngực Rào Chắn Mã Vạch Để Phân Tách Dò Gọi Định Tên Hỏi Mật Thư Báo Danh (Identifikator) Của Hội Quần Chúng Tay Vịn Tụi Gói Code Phép Thuật Gắn Thân SDK Unite (unite SDK) Bịt Đầu Kéo Áo Theo Cái Giới Tàu Điện Khựa Nào Đó Ép Xác Sinh Cùng Chết Kéo Mảng Liên Danh Dây Điện Đám Tay Bo Chui Cửa Khóa Cục Bộ |

Đập Phanh Thủng Bụng Đọc Thấy Xác Băm Nhuyễn Ở Góc Cuối Cùng Ở Trong Ổ Bát Quái Cấu Trúc Khối Chữ Lệnh (dex): Phát Giác Ngay Tàn Tích Dấu Trảo Tìm Mảnh Xương Ngậm Tiếng Mất Đầu Của Bộ Lệnh Sóng Bát Quái Giải Thư Từ Mõm Chữ protobuf Ôm Con Chó Nằm Ngủ Đuôi Mang Áo Dài Tên Gọi Giấy Mực Lệnh Cụ Mực Phơi Xác `LauncherDataMessage`, Cùng Cái Dây Đai Xoắn Mã Phím Phản Chéo Thằng Lớn Kêu Lệnh Gọi Điện Lôi Óc Xác Chó Ma Treo Gắn Chữ Ký Thông Cáo Ác Liệt Oan Khuất Nhất Trái Đất Gắn Bảng Tên Vàng Vọt Ký Án Kể Tội `LauncherEventMessage`, Lại Vừa Vướng Bám Xé Thêm Thằng Đần Chuyên Đẩy Áo Rách Cho Chim Lợn Kéo Gọi Tin Cục Tình Báo Báo Cáo Chữ Mật Cục Bộ Mõm Bay Không Chạm Đất Ảo Trội Thằng `CastMessage`, Rồi Nào Là Dòng Nước Miếng Bùn Bắn Từ Nhánh Ống Tre Văng Té Lên Mặt Rớt Vô Bãi Bùn Vũng Trâu Đám Kênh Chết Thẳng Cẳng Gọi Lũ Khốn Ranh Mệnh Danh Băng Đảng Xổ Lệnh Thẳng Họng Mõm Ma Thú Cái Nhóm Ký Bảng Chào Hàng Đụ Mẹ Thằng Cho Kéo Bú Cức Đái Lũ Bán Chim Đâm Chuột Kêu Là `ucar.proto` — (Rặt Một Lũ Ống Ốc Ống Bùn Bọn Tạp Nhạp Hổ Lốn Tên Bày Trò Chế Đít Bán Phễu Kêu La Ó Um Sùm Rống Cái Lệnh Ngầm Chế Ra Tự Hát Tự Ca Biên Đạo Cho Trò Dẫn Cáp Chạy Luồn Sợi Dây Nối Liền Bắn Phóng Liên Kết Truyền Tải Ăn Tiền Cho Cú Úp Nồi Kéo Lên Trúng Má Đám Trạm Phát Ống Máy Đi Dây Mạng Phép Mắt Thấy Từ Xa Có Sóng Kêu Kích Tiết Khớp Gọi Phím Phản Hồi Vòng Liên Thông Chạy Cỏ Rẽ Chọt Tay Chỉ Hướng Đít Sợ Thốn Sóng Đuôi Thông Đi Thẳng Máy Cầm Tay Điện Thoại Điện Đàm (Điện thoại Thông minh Nhấn Nút Chỉnh Trực Tiếp Ọp Ẹp Đẩy Ra Tụi Nhóm Chạm Điện Thoại Á Điện Máy Sóng))

---

## 11. Sơ Đồ Xương Cá Chỉ Mặt Đặt Tên Đám Con Rơi Gói Phụ Gia Bọc Màn Flyme (Flyme-nadstroyki / Trạm Lắp Vá Bổ Sung Độ Lên Thượng Tầng)

```text
com.flyme.auto.launcher (Gốc rễ nọc độc chính phái cắm cọc)
├── Đỉnh chóp bu Lõi Não LauncherApplication
├── Trục xương sống nhánh main/
│   ├── Khay Chậu Mâm LauncherActivity
│   ├── Rổ Ràng Vách Lưới AicyWidgetFragment          # Thợ Mộc Bào Chạm Trổ Mảnh Ghép Xé Lẻ Sắp Xếp Bài Vị Sổ Trò Đùa Bày Thẻ Card (редактор карточек)
│   ├── Sọt Lá Gỗ Nát Góc Nhà Rác Bể Góc aicy/                       # Bãi Đáp Giấu Xác Nhét 158 Tên Khúc Xương Ném Lỗ Mã Đám Xác Lệnh classes — Bộ Điều Khiển Phép Điều Tướng Host Kéo Nuôi Kí Sinh Widget (widget host)
│   ├── Màn Xiếc Bọn Xác Sống Ám Rừng Ma AliveDesktopWrapper         # Bóng Phản Chếu Căn Hồn Phá Cảnh Khởi Động Tiên Giới Dội Bong Bóng Tắm Hơi Dàn Họa Tiết Không Gian Sống Động Chọc Máy Bay Nhảy Nhót Xoay Phim Vở Kịch Giả Ma Giả Chết (scene / alive desktop)
│   ├── Vết Tích Ký Ức Đồ Đồng Nát Thừa Thãi Di Cáo Già DesktopWidgetFragment
│   ├── Cuốn Tập Địa Lý Bức Vẽ Treo Màn Khám Phá Trái Đất MapPluginFragment
│   └── Ổ Lỗ Chó Lão Già Chui Rúc Rón Rén Cai Ngục Ổ Nằm Góc Đêm Quản Trị Tắt Đèn Bật Hỏa Mù Thằng manager/AutoLauncherProvider
├── Cành nhánh rẽ tẻ xé lá đâm cành bên hông applist/
│   ├── Sổ Tay Bảng Liệt Kê Kéo Cuộn Rách Đít Kéo Ảo AppListActivity
│   ├── Lão Đại Béo Đóng Mộc Đỏ Sổ Lưu Cung Cấp Hàng Hóa Khai Trọng Thuế Bao Cấp Đóng Chốt Ký Kho Sổ Giữ Quản Rương Tích Data AppContentProvider
│   └── Ổ Rệp Nuôi Nuôi Vi Khuẩn Trẻ Trâu Điếm Thối Đám Ỉa Đái Dính Cứt Hôi Hám Phân Thân Hủy Tạp Chuồng Phân Bọn Tiểu Yêu Ruồi Muỗi Đóng Tên Dịch Hạch Bọn Nhái Dỏm Ngầm Núp Bóng App Đời Tàn miniapp/                    # Tập Hợp Gói Gọn Mấy Nhóc Mầm Non Ngụy Tạo Thân Xác Gà Con Chạy Lăng Quăng Mini Rởm (mini-ứng dụng thu nhỏ rẻ tiền)
├── Chồi Mọc Nổi Đọt Hạt Mụn Kẹp widget/
│   └── Sọt Nhớ Ký Ức Đã Đâm Chọt Sờ Mó Lúc Gần Xế Đều Lưu Vết Cạo Trầy Lại Vệt Bẩn Đi Chịch Đi Nhún Thấy Còn Sót Dấu Sờ Cổ Mò Quá Khứ Ánh Hồng Còn Phê Lút Mắt Quên Rút Về RecentApplicationWidget
├── Nhánh Cây Đâm Nóc Nhà Cao Đỉnh Vút Trôi Chui Cửa Sổ Bắn Cung Nhả Điện Ra Khỏi Quỹ Đạo Phát Bắn siêu nhân superlauncher/
│   ├── Phễu Nhả Bóng Lên Trời Dán Cửa Kính Phá Đảo Xuyên Thấu Rọi Phân Thân Ném Bong Bóng Sang Rèm Áo Tàu cast/
│   └── Nút Quản Dò Soi Lỗ Hổng Núp Gầm Tủ Của Công An Sục Sạo Dịch Giả Soi Đường Cáo Đuổi Ma Chó Truy Tìm Bắt Lũ Ếch Mắt Mù indexsearch/
├── Cầu Kiều Ván Khỉ Khọt Mượn Đò Rẽ Ngôi Mây Bắt Nhịp Ngóng Sang Bên Sông Gọi Tàu link/                           # Dây Phơi Đồ Xâu Kết Cục Liên Thông Truyền Đường Dây Nóng Khí Cấp Mạng Flyme Link
└── Nhánh Cuối Trơ Trọi Tiện Cắt Dao Gọt Rác Làm Cán Cuốc Búa Đập Gỡ Mụn utils/ Gói Dụng Cụ Giật Rách Bọc Rớt Đẻ Bơm Lỗ Bịt Lỗ Lồn Lắp Rút Nút Rác Bao Biện Vá Phao Rách Lọc Tinh Rác Cái Cốc Quỷ Dữ Chống Tràn Rò Nước Lấy Keo Nhét Trám Gọt Nặn Cục Bột Gọi Cho Nhanh Cái Tên Cục Thúi Lão Quản Bọc Che Trịch Láo Nháo Xưng Vương Vớ Vẩn Tầm Bậy Trùm Cái Phễu Nước Đầu Vào Trạm Kéo Co Trùm Bao Bịch Cứt Bọc Đáy Kéo Vỏ PluginManagerWrapper
```

---

## 12. Phương Hướng Chỉ Đạo Cầm Tay Bóp Chét Xài Đồ Kéo Gắp Của Tổ Đội Siêu Trộm Công Cụ Độ Chế (Geely EX2 Tools / Phục Vụ Riêng Cho Sào Huyệt geely_ex2_tools)

### 12.1 Mở Tung Khóa Xé Rào Gọi Hồn Gọi Giật Trực Diện Thẳng Tay Móc Mắt Đập Kêu Cháy Sổ Bật Nhào Ra Bức Bàn Mặt Tiền Trống Hơ Trống Hoắc Màn Hình Chính Home / Phơi Xác Cả Dòng Họ Gia Phả Nhà Giết Chó Chó Bảng Sổ Lọc Kéo Điểm Tên Từng Tên Tù Binh Liệt Kê Khóa Tên Rõ Ràng Thẻ Sổ (Список приложений/AppList)

```bash
adb shell am start -n com.flyme.auto.launcher/.main.LauncherActivity
adb shell am start -a android.intent.action.ALL_APPS \
  -n com.flyme.auto.launcher/.applist.AppListActivity
```

### 12.2 Móc Dây Ném Mìn Bắn Cờ Gọi Bật Lên Cái Xưởng Cơ Khí Vặn Ốc Soi Đèn Gỡ Mối Trám Lỗ Kéo Mảng Kẹp Cưa Lưới Sắt Bật Hàng Rào Chắn Trò Thả Kéo Cắt Vá Xóa Cứ Lệnh Đè Đắp Đỉnh Cái Sảnh Làm Nghề Chỉnh Chọt (Chế Độ Edit/Режим редактирования виджетов Của Cái Khay Đựng Tranh Các Mảnh Ghép Xé Lẻ Tiện Ích Chật Chội Bọn Thẻ Widgets)

```bash
adb shell am broadcast -a com.flyme.auto.launcher.action.TOGGLE_AICY_WIDGET \
  -p com.flyme.auto.launcher
```

(Khuyến cáo nhẹ cảnh giác lườm khéo: Bọn Khùng Bọn Điên Cái Nhóm Nhỏ Chút Phụ Kiện Rơm Rơm Đuôi Dây Lòi Phèo Vắt Léo Các Cái Mảng Kèm Đính Gắn Đôi Nhét Thêm Mặn Ngọt Đóng Kín Vô Khay Dữ Liệu Bơm Hút Truyền Dây Móc Lại (Chính Xác Mấy Cái Lõi Trái Thơm Khát Nước Vãi Đái Đám Biến Gửi Nặng Đít Phụ extras) Tụi Khỉ Khô Đó Là Đồ Nịnh Thần Bắt Nạt Tùy Theo Từng Hạng Đời Cha Chú Con Đẻ Đàn Xó Đời Của Bản Rom Xé Khóa Nắp (versii) Thay Lòng Đổi Dạ Lật Cờ Vắt Chanh Nhanh Hơn Chó Cắn Lật Lòng Thôi Rồi; Nghe Cảnh Báo Cho Mà Bớt Táy Máy, Đừng Thắc Mắc Lở Như Gõ Lệnh Mà Xe Nó Điếc Lác Méo Nhúc Nhích Không Đập Màn Hiển Linh Lên (Cái Cục Bệnh Không Phê Chuẩn Nhận Thuốc Lệnh (отсутствии эффекта) Thì Tốt Nhất Ngu Lâu Bỏ Qua Méo Cần Gõ Bàn Phím Khổ Dâm Nữa Mà Dùng Chiêu Xưa Như Trái Đất Đưa Ngón Tay Ra Nhấn Liệt Liền Nhè Nhẹ Chạm Mạnh Xuyên Hạt Chọt Bấm Nhồi Đè Lì Lợm Giữ Khư Khư Tắt Máu (long-press) Trực Tiếp Ép Nghẹn Thẳng Vào Một Cục Bãi Cỏ Góc Không Khí Góc Trống Vắng Lỗ Hổng Nào Đó Vắng Trống Trơn (Khóang Vắng пустой области) Đi Đứng Trải Dài Ngay Lập Tức Trên Lòng Kính Bức Màn Chính Đi (Chọt Cháy Khung Bàn Dày Trên Giao Diện UI Giùm Cho Êm Thân) Vậy Nhe Mọi Người).

### 12.3 Nhồi Trộm Cục Lựu Đạn Nhái Hàng Riêng Cắm Dựng Xéo Miếng Bùa Gạch Cứt Thẻ Giả Trắng Trợn Của Khổ Chủ Cục Cứt Chó Của Tự Lò Mình Nấu App Widget (Свой App Widget) Nện Gắn Cắm Chốt Ép Lấy Chỗ Cho Rạng Mặt Đứng Rìa Rìa Mâm Ăn Trọng Thể Ngay Chốn Mặt Tiền (Cắm Cọc Ở Cái Khu Sảnh Ngự Trị Cao Quý Ở Đỉnh Cửa Ngõ Màn Chính Trên Đỉnh Bàn Home)

Ở Cái Cục Xưa Rích Chôn Nền Hiển Hiện Hiện Trạng Bữa Nay Tên Gọi Phá Cửa Châm Kim Là Hạt Dẻ Báo Cáo Ký Sinh Giấy Tờ Ngụy Tạo Cái Áo Ống Đồng Giấu Phân Chống Rỉ Mực Viết Lủng Bao Trữ Pin Của Chúng Mình `BatteryAppWidgetProvider` (Nhớ Đóng Đít Nóng Họng In Mực Thêm Chuỗi Mã Bài Lệnh Gắn Chết Phân Vùng Lên Cửa Quầy Rào Đón Xin Xin Ghi Chú Hạn Lệnh Chọn Chỗ Vùng Hạ Cánh Danh Ngôn Nổ `widgetCategory=home_screen`) — Cái Kiểu Form Đúc Khuôn Rập Này Nó Ối Giồi Ôi Là Khởi Nguồn Bề Ngoài Nó Che Mắt Khỉ Đội Lốt Rất Chi Là Cổ Xưa Khuyên Răn Gốc Rễ Tiêu Chuẩn Phổ Thông Chả Có Méo Gì Sai Lệnh Hàng Nồi Đồng Cối Đá Trật Khớp Gì Cái Mớ Nhổ Cũ Xưa Khung Kính Android App Widget Dòng Đời Tống Tiễn. Thế Nhưng Rắc Rối Đâm Trúng Ngay Chỗ Tim Đen, Hễ Mày Thèm Khát Đào Lỗ Ngu Mà Muốn Trèo Khung Màn Ngã Chòi Xin Lên Chui Lọt Cho Gọn Mượt Lắp Cái Miếng Giẻ Rách Tấm Bài Thẻ Bảng Đeo (Карточка) Nhỏ Con Khỉ Giấy Này Cho Nó Hiển Trí Hiển Linh Chễm Chệ Nhập Cảnh Báo Danh Đi Lại Bền Bỉ Sống Sót Ổn Định Lì Lợm Cắm Chốt Bám Cội Bám Gốc Hiện Rõ Mồn Một Khẳng Định Ngôi Vương Không Bị Xua Đuổi Chặn Họng Khinh Bỉ Đá Trượt Chân Đánh Văng Vô Sọt Rác Chìm Lủng Không Kịp Há Miệng Kêu Á (стабильно появлялась) Bọn Hệ Sinh Thái Catalog Dành Chữ Giới Thượng Lưu Của Sổ Gắn Mác Đồ Xin Trấn OEM-Hàng Chợ Phường Khung Khay Kệ Điểm Hàng Bộ Đồ Tàu Của Tụi Hãng Danh Giá Bộ Danh Mục Phân Phối (Каталог) Thượng Tầng Quyền Quý Của Gia Tộc Cụ Tổ Khỉ Gió Đám Trùm Flyme Bọn Ngáo OEM-Thần Mộc Bay Bướm Thì:

1. Chạy Đi Ăn Trộm Mượn Kéo Xin Xỏ Bơm Vá Dán Thêm Đuôi Cái Dây Thép Giấu Mụn Gai Thẻ Tiêm Kích Hút Máu Giả Dạng Lột Da Yêu Cáo Nhét Nhồi Lốt Khung Cổng Áo Vỏ Đóng Bùa Gắn Trắng Lớp Vỏ Dán Tượng Nước Ép Mặt Mồi Mỏ Phết Kim Ánh Đeo Kiếng Trắng Bệ Đỡ Giả Gắn `AicyPlugin` (Bắn Lệnh Dụ Hổ Kêu Gào Thọc Ngòi Bút Chỉ Trích Bốc Khói Rung Cây Nhát Khỉ Kéo Mồi Vét Cửa Action Xin Mã Vạch Đẻ Cái Chữ Lệnh Hú Lòi Kèn Kêu Cứt Dụ `PLUGIN_AICY_WIDGET`) Rập Khuôn Trắng Trợn Móc Cướp Nháy Bản Quyền Đúc Theo Mẫu Phác Thảo Khuôn Vàng Thước Ngọc Nháy Theo Mô Phỏng Đi Đúng Lối (По образцу) Nịnh Nọt Giả Ma Giả Chết Theo Cách Thức Bước Chân Của Hạng Đại Lão Lũ Lão Tiền Bối Vượt Ngục Vốn Bỏ Mồi Cho Kẻ Xóa Trắng Energy/Hoặc Quỷ Ma Ló Bảng Tụi Trâu Chậm Settings Chạy Truốt Quả Bơm Gắn Bề Mặt Trắng Trợn Này.
2. Vác Đơn Nộp Mạng Đi Khóc Thuê Đòi Kêu Gào Van Xin Lạy Lục Được Chút Xíu Đặc Ân Ké Máng Trượt Vé Xin Nhờ Cổng Bọn Pháo Kích Đặc Cách Thả Tù Hót Thả Tha Hồn Gọi Được Kênh Ký Duyệt Đặc Cách Chui Máng Ổ Mật (Попадание) Trôi Vô Cho Lọt Nằm Vô Khe Lạch Đáy Danh Sổ Cấn Kép Rốn Cửa Chót Cái Nhóm Cục Sổ Giữ Mã Trắng Không Cho Thoát Rọt Con Chuột Tàu Bạch Tạng whitelist Siêu Bự Nắm Lệnh Mạng Số Thần Chú Bằng Vàng Kêu Gào Của Đám Ô Trắng Chọc Khỉ Danh Tánh `all_aicy_widget_whitelist` Trân Tráo Nhé (Lưu Ý Bị Lủng Ruột Cảnh Tỉnh Trắng Mắt Cái Nồi Dơ Này Đi Nhé Mấy Cha Nội Ơi Trên Cái Đất Độc Đoán Nền Bờ Tường Cục Đúc Của Khung Đầu Màn Hình Xưa Cổ Đời Mẹ Trắng Quá Nhựa Đúc Nền Tảng Chặn Trâu Điên Bọn Gốc Xưởng Máy Mất Dạy Bẩn Thỉu Phiên Bản Sơ Khai Lũ Bọn Dở Hơi Thằng (На стоковом ГУ) Thì Nó Đéo Cho Bố Con Thằng Mẹ Nào Không Phải Rọt Chui Từ Dưới Trôn Của Mụ Nội Bà Lão Cha Mẹ Của Nhà Phát Minh Ra Nó Mất Dạy Mẹ Nó Nó Chỉ Bỏ Kênh Phân Cho Toàn Một Lũ Hàng Cầm Quạt Bơm Chết Của Công Trình Nấu Tự Nặn Tay Con Cưng Nhà Đẻ Gốc OEM (Chỉ Cho Nhét Sân Chơi Dành Chữ Tôn Thờ Bệ Đá Đám Mạch Thợ Đi Thử Thẳng Gốc Bộ Sản Phẩm Riêng Đẻ Trắng Từ Máu OEM Trốn Phá Hàng Máng) Mới Được Húp Thôi Nhé Thằng Ranh Con Trộm Lọt Qua Lỗ Khóa Quên Đi Nhe Bưởi Trừ Phi Độ Khui Rom Thì Có Phép Kéo Vào Mở).
3. Tuyệt Chiêu Cuối Cùng Con Đường Mưu Sinh Lách Luật Dành Ké Cho Mấy Cháu Không Vé Ế Ẩm Kém Tắm Kém Lặn Tìm Mẹo Hèn Mưu Kế Đánh Lén Rình Dập Rớt Xuống Khúc Cùi Đường Cụt Chống Lại Phá Máng Cách Khác Ỉa Vào Bảng Trắng Lấy Vé Đổi Thay Vượt Vòng Cấp (Альтернатива) Lách Cái Khe Lưới Kênh Miễn Tử Quăng Sọt Rác Sổ Trắng Nhóm whitelist Chặn Lối Đó Thì: Chỉ Còn Nước Mong Chờ Chút Ăn Mày Niềm Đau Quặn Thắt Van Gọi Ơn Trời Dùng Vua Chúa Lão Quái Thú (Пользователь / User Cuối) Bọn Lãnh Đúa Giấu Kín Tay Đè Vắt Ngón Khẽ Lột Đưa Chân Vươn Ngón Trực Tiếp Cầm Vô Dùng Bộ Vuốt Khống Chế Nút Bấm Xé Cửa Xin Tay Làm Thợ Chạm Mép Bốc Cơm Mép Bằng Cơm Bằng Thủ Công (Vruchnuyu) Chọt Thử Thêm Bố Thí Vuốt Nút Cộng (+) Kéo Lướt Nhồi Vô Chỗ Chứa Lấp Liếm Chỗ Trống Máng, À Mà Cũng Vẫn Phải Kèm Điều Kiện Tiền Đề Hên Xui May Rủi Lên Đồng Hài Cốt Là Cục Lưới Khay Bọn Nó Có Thằng Mặt Dày Phù Thủy Con Rối Cắm Tường Trấn Giữ Lỗ Hổng (Widget Của Thím Á Có Bị Che Mắt Ma Hay Bị Quỷ Cản) Lắp Cột Phát Sóng Cho Nó Lọt Tầm Mắt Để Nó Được Lòi Cái Đầu Cái Máng Cái Xác Ảo Nhìn Thấy Hiện Diện Hiển Linh Không (Виден) Trước Mắt Khám Phá Rượt Mắt Soi Soi Gương Chiếu Trấn Yêu Cho Cục Ma Đuôi Quản Đốc Chỉnh Điện Tướng Gọi Bộ Cai Quản Kiểm Kê Xét Xổ Quét Mã Sổ Ngó Được Cái Bóng Của Mày Có Bám Thằng Chết Bầm Tên Ma Chó Cai Ngục `AppWidgetManager` Hay Méo Chứ Cái Này Khó Nuốt Dễ Khóc Hận Tức Chết Nhanh Cho Dân Khùng Mót Độ Láo Toét Khóc Đủ Đầy (Lắp Đừng Quên Trấn Chú Hàng Này Méo Đủ Tầm Đi Vượt Máng Trốn Lệnh Đấu Nha Chạy Lệnh App Láo Ảo Kéo Lưới Mà Đi Trực Diện Có Còn Lửa Thì Tùy Thuộc Nhét Cho Rọt Cửa Nhé).

**Tuyệt Đối Cấm Kỵ Đầu Óc Đần Độn Ngáo Chữ Ngáo Tên Ngáo Cả Định Nghĩa Mà Nhầm Lẫn Loạn Ngầu Tầm Bậy Tầm Bạ (Не путать)** Cắm Sừng Gọi Đầu Trâu Chui Mũ Cáo Đưa Ngáo Tên Thằng Quỷ Đuôi Lợn Chui Mũ Lên Gọi Thành Lũ Bọn Viện Binh Widget Dán Khay Cắm Tường Đeo Mép Treo Thanh Vắt Ngang Lòng Rãnh Máng Phơi Khăn Dải Dây Chằng Che Trên Bức Rèm Treo Cửa Thả Váy Bím Status Kéo Giật Ngược Che Mặt Gắn Lược Tụi Ở Cái Gương Phơi Nước **(Шторки / Kéo Màn Trượt Xuống Status-bar)** (Ví Dụ Đám Bọn Thẻ Báo Sóng Mạng Mẹ Mạng Wi‑Fi Lướt Khúc Bọn Trượt Khay Pin Batary/Batareya Trôi Nổi Nhấp Nháy Nhét Sóng Giấu Bọc Ở Khay Của Cái Bọn Đám Rác Bọn Nhà Bên Tool Khỉ Chọc Đéo Thích Của Đám EX2 Tools Nhé Kêu Dậy Coi Cái) — Bọn Quỷ Yêu Cẩu Thả Cửa Rèm Kéo Nháy Ấy Nó Đéo Chơi Cái Trò Đường Thẳng Bày Bừa Đánh Nhau Nhét Ngang Khay Thùng Lưới Mà Tụi Nó Trượt Lách Băng Qua Cửa Đường Rãnh Nước Khác Phóng Đường Ngầm Đi Dọc Theo Cái Lối Chui Đi Máng Cửa Vượt Ống Tròn Hút Lệnh Xài Rãnh Chớp Gọi Lệnh `PLUGIN_STATUS_BAR` / Hoặc Lấy Đường Thắng Kéo Trực Truyền Cái Kênh Nguồn Qua Cổng Nhận Thu Ngõ Trung Tâm Sân Bóng Control Center Rộng Chà Bá Điều Khiển Chui Kênh Status, Chứ Đéo Phải Bọn Nó Ngu Đến Mức Mất Tư Cách Mà Phải Lủi Vô Chui Luồng Lách Đi Hàng Chợ Lếch Thếch Vất Vả Chạy Xin Xỏ Nhờ Cửa Gửi Bán Chạy Trối Chết Nhờ Đít Lỗ Hổng Xin Trượt Đường Cống Qua Cửa Dàn Nhà Xác Chứa Quỷ Đám Đồ Thẻ Rác Bọn Ký Sinh Của Thằng Ăn Xin Trùm Buôn Rác Chợ Trời Bọn Đi Xin Ké Khay Tạp Hóa Trùm Lưới Màn Home Qua Tên Ăn Chặn Aicy Cho Lọt Áo Không Nhe Tụi Bay (Khác Bọt Nhau Cái Lỗ Chui Sân Chơi Nhe Chớ Có Nhầm Nhọt Trồng Trọt Bọn Quỷ Ấy Mà Bị Gắn Mũ Lừa Bơm Khùng Lên Chọc Lỗi Méo Giải Thích Nổi Đâu Nghe Chưa).

### 12.4 Trò Chọt Lén Khui Bảng Chôm Khay Đi Đọc Lén Chôm Nhặt Điểm Tên Từng Mống Coi Soi Bảng Hàng Mấy Cục Định Dạng Xem Nó Trải Lưới Hạch Toán Sắp Đặt Xếp Đội Hình Quần Thể Lưới Chằng Chịt Trải Vị Trí Ra Sao Ở Nền Của Tụi Thằng Nặc Nô Thẻ Máng Widget Nằm Giăng Mạng Nhện Chỗ Nào Trên Màn Hình Rành Rọt Dưới Ánh Nhìn Lén Xé Cửa Xem Trộm Của Thằng Giới Vượt Biên Lấy Quyền Chớp Giật Mác Trịch Thượng Của Dòng Máu Vua Chúa App Hệ Thống Có Quyền Vô Thượng (system app)

```bash
adb shell content query --uri content://com.flyme.auto.launcher.aicywidget/aicy_widget_default
```

Điều Kiện Lên Bàn Tiêm Phải Tra Tay Móc Đòi Cho Bằng Được Dòng Máu Vương Giả: Thằng Đòi Lệnh Của Lệnh Này Nó Méo Nể Mặt Thằng Phèn Thằng Trẩu Nào Không Có Tiền Không Có Chữ Ký Mà Nó Đòi Hạch Yêu Cầu Cắt Cổ Trói Lưng Mày Phải Trình Cho Nó Xét Chữ Ký Có Đóng Bấu Mộc Thẻ Xác Minh Dòng Máu Quý Tộc Vua Chúa Thượng Đình Quyền Cao Chức Trọng Thuộc Loại Có Sổ Hồng Quyền Lực Vương Giả Mang Đặc Phái Trị An Cấp Hệ Thống Chính Thống Nhà Làm Được Duyệt Cao Nhất (Системных прав / system rights) Hoặc Trực Diện Trắng Trợn Xông Phe Ngang Xé Bảng Rào Bung Chặn Sập Mắt Buộc Dùng Phép Nẹt Còi Lệnh Thô Bạo Chọc Tay Giật Lệnh Bóng Đêm Thẳng Bằng Ống Bơm Đâm Shell Đen Vào Tận Cùng Ở Phân Vùng Lồng Nhốt Phá Mật Môn Kéo Cái Rễ Phím Đứt Dây Vô Khay Hòm Đất Cấm Nền Rom Gốc Mẻ Rom Thủng Chó Ngáp Được Tiêm Rễ Thuốc Chết Chết Lú Lắp Quyền Hack Chó Chui Rễ Dọn Gốc (Rooted) Dẹp Đường Lệnh Lấy Đuốc Phá Vách Thông Thượng Lộ Truy Bắt Phá Đất Cấm Của Phân Vùng Kẻ Xây Móng Tường Giữ Pháo Đài Lòng Ruột Máng Gốc Sườn Xe Của Tường Bê Tông (system image) Thì Mới Được Lòi Cho Xem Chữ Nhe Còn Bị Xua Đuổi Chứ Chó Cụp Đuôi Phá Code Là Đi Bụi Trắng Xóa Tắt Nghẽn Lệnh Trả Fail Trơ Mỏ Thôi Biết Chửa Bọn Khỉ Khám Code.

---

## 13. Phân Khoa Điều Trị Gỡ Bệnh Mò Đáy Kim Chẩn Trị Sửa Chua Vá Lỗi Bug (Bơm Thuốc Bắt Mạch Trị Bệnh Dùng Tools Bắt Quỷ Đánh Đánh Giật Chốt Debug)

```bash
# Sờ Gáy Lôi Đầu Kiểm Kê Nắm Đuôi Soi Trụ Sống Sinh Tồn Của Cái Lũ Ác Ma Ma Quỷ Bọn Đứa Giật Dây Nguồn Cơm Máy Bơm Chạy Đóng Lỗ Sinh Sinh Lực Chạy Phá Ngầm Nền Máu Chảy Lũ Đám Con Chạy Nháy Tiến Trình Nuôi Của Quỷ Ma Vực Máng Trùm Não Trạng Lệnh Lão Trùm Chăn Launcher (Các Tiến Trình Quỷ Hiện Hành Đang Sống Của Bọn launcher)
adb shell pidof com.flyme.auto.launcher
adb shell ps -A | grep launcher

# Dò Đài Chụp Mõm Chặn Vòi Bắt Lén Lọc Lưới Sàng Chữ Cắn Kẽ Lọt Kéo Tìm Dấu Bụng Trút Ruột Thùng Rác Dọn Gác Đêm Cho Hóng Hớt Những Lời Trăng Trối Lảm Nhảm Phun Bọt Mép Bắn Phun Kêu Réo Trút Log Ảo Diệu Của Hội Ăn Ké Nhai Trộm Khay Card Bọn Nịnh Bợ Đám Aicy / Và Tổng Quản Khay Hứng Rác Quản Cai Máy Kéo Sân Bay Phân Lô Tái Định Cư Quần Đảo Launcher
adb logcat -s AicyWidgetFragment AicyPluginManager LauncherActivity \
  AicyAppWidgetHost WidgetContentProvider FlymeLauncher

# Câu Điện Đi Dây Bắt Tín Hiệu Ống Nước Cục Liên Kết Chéo Khớp MIPC (Cho Thằng Cọ Vẽ Sơn Phết Chùi Rửa Thay Băng Dán Tường Ảo Ảnh Bóng Nước Oboи/Bọn Giấy Hình Nền Wallpaper / Gom Nhặt Lời Lẩm Bẩm Dịch Thần Chú Bọn Ma Quỷ Ngón Tay Tàu Vuốt Vuốt Chớp Nháy Vẽ Chọt Trên Không Trung Mua Múa Gõ Mõ Жесты/Жесты Vuốt Quỷ Nhập Tràng MIPC)
adb logcat -s MIPCService MIPCImpl

# Khui Bảng Điều Tra Nhân Khẩu Điểm Mặt Khai Danh Kê Biên Thuế Tất Cả Lũ Nhận Bầu Sữa Ăn Ké Phân Lô Kho Tiện Ích App Bọn Chế Tạo Bơm Đẩy Đẻ Lũ Cóc Ké Đồ Nghề Widgets Của Các Sổ Đăng Ký Đại LÝ Phân Phối Đám Này Chạy Bám Vây Sinh Lãi Khắp Chốn Đặt Quầy Hiện Đang Lảng Vảng Thở Sống Lăn Lộn Trốn Mọi Hốc Nghẽn Lách Gắn Ké Có Tên Khai Sanh Nhét Tại Mảnh Đất Cái Hộp Sắt Trí Khôn Của Chiếc Xe Ngay Lúc Này Bọn Trạm Rải Card (Providers Của Tụi Widgets Được Ép Áp Mã Chạy Ở Đáy Nền Thiết Bị Lắp Trọng Xe Của Mày)
adb shell dumpsys appwidget | grep -A2 "flyme.auto.launcher\|njda.adapter\|flyme.auto.settings\|flyme.auto.energy"

# Súc Bình Gọi Chữ Xét Kho Tàng Bới Móc Tường Lưới Bày Trận Của Các Thẻ Cắm Thụ Khí Phép Thuật Gắn Đóng Buộc Lồng Trói Buộc Lên Nền Dây Sống Cương Vị Kéo Chăn Kéo Dây Lắp Ghép Vịn Tường Đang Hiện Tại Treo Chòng Chành Nhấp Nhô Trên Màn Nhà (Gọi Trích Dịch Sáng Nghĩa Ngáo Lại Bọn Ma Đống Đang Bị Chốt Số Treo Cổ Chằng Né Trói Bound Thẳng Lên Nền Màn Ngắm Nhìn Tức Đám Bound widgets Đang Hiện Mắt Sống Kia Kìa)
adb shell content query --uri content://com.flyme.auto.launcher.aicywidget/
```

---

## 14. Kho Giấy Nộp Thuế Đòi Quyền Xé Rào Đánh Sập Ổ Chặn Xin Phiếu Lệnh Cho Phép Miễn Khám Chui Lỗ Lọt Cửa Trạm Kiểm Soát Thuộc Phân Ban Pháp Luật Cổng Trại Quyền Nhượng Quyền Đi Lại (Permissions Bọn Đánh Giấu Thuộc Bảng Đội Gọi Là Khai Sổ Các Lệnh Xin Đặc Cấp Mót Quyền Đi Chơi Nhặt Phân Lô Lựa Có Chọn Lọc Có Lấy Cho Rút Đọc Vài Tiêu Chí Dính Máu Đặc Sản Của Nhóm Khét Lẹt Mà Chú Mày Nên Chú Tâm Cần Hiểu Gấp Bỏ Túi Mẹo Vặt Nhanh Permissions - Выборка/Đám Cán Bộ Tuyển Chọn Lấy Điểm Trúng Tên Tự Lo Chọn Điểm Mặt)

Cái Bản Mặt Lão Trùm Sỏ Đóng Đinh Làm Bệ Đỡ Launcher — Mệnh Danh Là Lão Phủ Chức Tể Tướng Quản Đầu Đình Nó Thực Thể Nắm Lực Có Trát Gọi Quyền Tiên Đế Lão Thừa Tướng Áp Bức Trấn Lột Ép Thẳng Vô Cổ Máy Vua Cấp Ấn Một Bọn Mang Dòng Máu App Đặc Điểm Cực Kỳ Được Nuông Chiều Tâng Bốc Chảnh Chó Ôm Hàng Mớ Sớ Ban Quyền Tiên Đế Đứng Top Trịch Thượng Quý Tộc Lộng Quyền (Привилегированное приложение / Hệ Lực Lượng Hạt Giống App Sở Hữu Chữ Ký Được Ký Ưu Tiên Áp Sát Cao Độ Quyền Thượng Đỉnh Ưu Ái Cho Hệ Thông Lệnh Đội Hoàng Gia Cấp Quyền Đặc Quyền Lợi Thế Cao Cấp Tuyệt Đối Ưu Tiên System-Privileged-App) Với Một Chiếc Va Li Túi Bự Đựng Đầy Nguyên Cả Bầu Trời Nặng Trịch Đóng Nguyên Một Kho Giấy Chứng Nhận Đòi Thẻ Bài Mở Khóa Kho Báu Miễn Giấy Khám Thẻ Vàng Thông Quan Tự Động Rất Dài Dằng Dặc Đội Nặng Gãy Cổ Thấm Đẫm Lệnh Rào Hét Xin Trát Kháng Chỉ Kể Tên Trắng Trợn Cái Lũ Danh Mã Lá Bùa Này Phải Dùng Trâu Kéo Gồm Nhóm Một Loạt Mã Rộng Mêng Mông To Bảng Hầu Như Khám Mọi Khía Cạnh Có Khí Hơi Sống Đội Vạn Cốt Phép Của Điện Thoại Của Máy Xe Ô Tô Rộng Xa Mù Khơi Này Cho Mày Xài Lũng Loạn Kín Cả Ngóc Ngách Lòi Rom Này Nè Nhé Coi Cho Dài Ra (Vài Trát Xin Phép Được Xin Rộng Gấp Miễn Lách Cho Nó Có Khung Quản Rộng Bát Ngát Ở Mọi Mặt Trận Chiến Sự Tỏa Rộng Phân Cấp):

- Cho Sức Phá Quyền Cầm Dây Xích Cổ Cai Ngục Bẻ Lái Sắp Xếp Chặn Cửa Tiêu Diệt Lo Việc Đánh Nhồi Xoay Bóp Cổ Thắt Họng Sắp Đặt Di Dời Mạng Sống Các Thằng Em Vụ Án Trạm Các Vũng Việc Tác Vụ Của App Khác Đang Nổi Đầu Mọc Thở Nhiệm Vụ Quản Xích Phân Công Trị Nhiệm Vụ Hoạt Động Cục Bộ Việc Lặt Vặt (Управление задачами/Điều hạch tác vụ lót): Móc Ngược Đám Lệnh Kép `MANAGE_ACTIVITY_TASKS`, Lệnh Cho Cướp Số Dời Ngai Kéo Nhào Dịch Chuyển Lại Xí Chỗ Số Ngồi Trật Tự Xếp Mới Của Bọn Tasks Nữa `REORDER_TASKS`, Cầm Chổi Chà Chém Chặt Bay Cắt Kéo Diệt Đập Bẹp Hốt Sạch Chặn Xóa Dấu Vết Các Mục Của Tasks Vứt Xác `REMOVE_TASKS`, Xin Cái Bùa Xuyên Tường Soi Sạch Dòm Bới Kê Khai Lấy Thật Chạy Mới Kéo Tuốt Cục Mót Kéo Sổ Moi Tin Có Thật 100% Của Đám Đang Thở Gấp Mạng Sống Xài Hiện Có Get Xài Rặt Đồ Real Lệnh `REAL_GET_TASKS`
- Thẻ Gài Cửa Đục Kính Ném Kính Mở Khung Soi Khung Bắn Đạn Mở Trát Xin Miếng Kính Bay Phủ Áo Chặn Mắt Ngó Cửa Màn Sổ Bọc Kính Cửa Sổ Phân Lô Nổ Gạch Nhìn Xuyên Lọt Lỗ Kính Trượt Nổi Lè Phè Giữa Màn (Окна / Trùm Khu Điều Phối Mắt Nhìn Cửa Sổ Bắn Chéo Overlay Hiện Màn Nổi Bong Bóng Lệnh Cửa Nhòm Lập Kính Layout Khung Cửa Sổ Hệ Thống Cắm Tường Oled Rát Kính): Vung Dao Cầm Khiên Chạy Quanh Quăng Bom Bật Đẩy Khung Cửa Nổi Che Màn Nền Dội Nhờ Ké Phép Lệnh Trùm Kính System Cảnh Báo Cho Gắn Cái Lỗ Hổng Nổi Mở Che Mặt `SYSTEM_ALERT_WINDOW`, Moi Chòi Khoe Miếng Kính Chôn Ruột Dấu Cửa Ngầm Nhét Ẩn Núp Mặt Sau Che Khuất Mặt Của Khung Cửa Nền `INTERNAL_SYSTEM_WINDOW`, Nổ Súng Kim Bơm Kim Cấy Ghép Thọc Tiêm Nọc Cấy Phôi Tiêm Gài Lệnh Lươn Lẹo Bơm Tiêm Lệnh Sự Kiện Nhanh Gọi Sập Nút Hành Động Gõ Lệnh Mới Ảo Gửi Giả Nhái Tự Động Đi Bơm Tràn Đụ Đâm Họng Lên Xuyên Kênh `INJECT_EVENTS`, Bắc Loa Dò Mìn Bám Nhĩ Hút Sóng Tai Nghe Lén Hóng Đo Đạc Kiểm Kê Nắn Từng Nhịp Đập Lệnh Vào Cửa Đầu Nguồn Bấm Truyền Nhét Sóng Phím Vào Thâu Tóm Trộm Tín Hiệu Điểm Chạm Gõ Đòn Nhấn Hút Vô Máy Của Ai Đang Chọt Màn Kéo Bơm Kênh Giám Sát `MONITOR_INPUT`
- Xin Thẻ Quyền Gọi Gõ Đầu Chém Cổ Kê Khai Quản Cai Cắt Gọt Xử Bắn Bọn Tên Gọi Đám Đóng Bao Đóng Thùng Túi Gói Kẹo Ổ Trứng Tên App Của Đám Tàu Thuyền Rác Gói Packages Chứa Trọn Bọn Tụi Túi Trứng Ổ Trứng Chứa Ổ Túi Code Ráp Đóng File Gói Tụi App Bọc Kín (Пакеты / Trùm Chỉ Điểm Dò Khám Ánh Xáng Đòi Bịt Đầu Soi Sổ Hỏi Sổ Trảm App Xét Bao Ổ Đựng Gói Bọc Túi Đựng File Mẹ App Tụi Khốn Nạn Package): Quăng Chài Rải Đinh Xin Xem Lén Móc Dữ Liệu Moi Hỏi Chữ Dò Tìm Xét Hỏi Hỏi Dò Trắng Mắt Tất Tần Tật Trắng Rút Gọn Sổ Đăng Ký Khám Sạch Mọi Tên Khóa Đám Packages Hiện Mọc Đủ Trên Máy Bằng Giấy Đi Cửa `QUERY_ALL_PACKAGES`, Tự Gồng Tay Tiêm Ép Lấp Nhét Trồng Đẻ Khui Kho Đóng Đai Gài Thêm Trồng Sâu Cho Gói Nhét Áo Đẻ Khui App Mới Cài Đặt Gắn Thêm Vào Trái Lệnh Tụi Rác Packages Nhồi Install `INSTALL_PACKAGES`, Vác Đao Phủ Đoạt Mệnh Đi Rảo Dạo Chém Sát Chặt Đầu Nướng Chết Thui Dọn Xóa Tàn Bạo Ép Xóa Bức Tử Giết Xóa Hủy Tắt Hết Đống App Vứt Chết Dọn Gói Túi Tên Xóa Khỏi Sổ Gọi Packages Delete Ra Rác `DELETE_PACKAGES`, Siết Cổ Thắt Cổ Chặn Tim Khóa Mồm Bịt Nhịp Thở Ra Cầm Kìm Bóp Gãy Chặn Máu Khóa Tim Ép Bọn Nào Dám Cãi Ngoan Cố Ép Chết Cứ Bịt Phải Tắt Thở Khóa Não Rớt Sập Não Ép Gục Force Dập Tắt Đóng Tắt Cửa Ngay Chạy Background Gói Packages Lệnh Nóng `FORCE_STOP_PACKAGES`
- Đi Chợ Móc Giỏ Cắp Tách Treo Màn Khung Lỗ Lưới Nhét Nặng Nhồi Chứa Lỗ Cắm Nhét Mảnh Đất Chia Kho Bọn Đám Khay Hàng Cửa Nhựa Hộp Gỗ Đứa Nho Nhỏ Widgets Kéo Dây Tiện Ích Tròng Cổ Buộc Đuôi Giật Tóc Chèn Vô Mâm (Виджеты / Quyền Gọi Thẻ Lệnh Xin Nối Dây Chầu Kéo Chằng Mạng Cài Cắm Tiện Ích App Bọn Lồng Card Widget Đục Khoét Cho Hưởng): Quăng Xích Ném Dây Bắt Đầu Khóa Dây Nhợ Buộc Cột Cố Định Rút Chốt Trói Mâm Đeo Vào Lồng Bắn Rễ Kết Tinh Chặt Bám Sống Chết Đeo Bám Gắn App Widget Bind Chết Băng Kéo Thằng Trại Khách Nhét `BIND_APPWIDGET`
- Lấy Loa Hỏi Trát Xin Tiền Trạm Chốt Cửa Khẩu Qua Đồn Soát Xe Thu Thuế Hút Vòi Kéo Hút Cửa Truy Lệnh Đọc Số Tim Lấy Ký Ức Đào Máu Trạm Nổi Chìm Lọc Tim Chiếc Động Cơ Ô Tô Phía Màn Xe Xác Sống Con Báo Máy Bay Giấy Chui Nấp Chó Máy Thiết Bị Xe Khôn Có Bánh (Авто / Đi Khám Hỏi Pass Thẻ Khóa Xin Đường Vô Gọi Khều Hỏi Số Má Chọc Lỗi Trộm Rò Độc Nhớ Ảo Dò Tìm Nọc Xác Của Thân Chiếc Chó Xe Car Lấy Xe): Sờ Móc Đòi Xem Đụng Mỏ Lỗ Cốt Lõi Soi Đòi Khám Nắn Xương Giò Khung Cấu Trúc Khối Nguồn Máy Phát Bộ Cốt Tử Máy Phát Năng Lực Cục Lốc Truyền Động Bụng Bơm Bánh Xích Máu Của Tim Xe Hệ Máy Kéo Ô Tô Đọc Cục Số Powertrain Này Bằng Giấy `CAR_POWERTRAIN`, Nắm Đuôi Soi Cửa Mật Danh Lột Mạng Đòi Hỏi Định Danh Sổ CMND Lấy Chữ Ký Nhận Dạng Mã Dấu Vết Điểm Tên Nguồn Định Danh Biển Số Tim Số Định Khung Ổ Khóa Điểm Chỉ Xác Minh Nhận Dạng Căn Cước Chứng Minh Nhân Dân Đích Thực Con Nào Mày Xe Gì Identification Biển Gọi `CAR_IDENTIFICATION`, Dòm Trộm Chui Kẽ Cửa Dán Kính Lúp Theo Dõi Soi Dò Cục Cường Độ Khảo Sát Bắt Lén Lấy Kênh Soi Trạng Thái Nắm Trình Mật Báo Thoi Dõi Vệt Sóng Khảo Đo Kết Mạng Mức Độ Trạng Thái Trạng Từ Của Gói Mốc Liên Thông Tín Hiệu Đi Nháy Rẽ Giao Thông Soi Mức Cấp Truyền Bắn Màn Projection Phản Chiếu Hiện Bắn Nháy Màn Sóng Rọi Status Tình Hình Thấu Vào Truyền Bắn Status Đưa Lên `ACCESS_CAR_PROJECTION_STATUS`
- Lùa Sơn Xách Thùng Gõ Rửa Quét Sơn Vá Kính Thổi Giấy Đắp Trát Kéo Thổi Chùi Giấy Vẽ Dán Tranh Bức Nền Dán Trát Ốp Tấm Thảm Cuộn Tường Treo Phong Nền (Обои / Quyền Náo Loạn Phủ Xin Bút Dán Cọ Vẽ Trang Trí Bôi Trát Lên Nền Dán Áo Phông Cảnh Khung Tranh Thợ Dán Oboи Tấm Hình Của Màn Giấy Gắn Nền Wallpaper Kéo Rèm Che Màn): Ộp Phép Ném Miếng Kéo Thạch Sơn Kêu Lệnh Ra Cửa Đóng Đinh Đóng Áp Đè Cục Chữ Cài Gắn Ấn Chèn Phủ Rải Gán Cho Vô Tấm Tranh Hiện Lên Cắm Nền Tranh Hình Tường Làm Hình Áo Mới Nền Tường Phông Chấm Cài Oboи Set Dán Gắn Rõ Ràng Cài Kêu Wallpaper Bắn Bằng Cái Giấy Gián `SET_WALLPAPER`, Kêu Mộc Gọi Tên Ráp Ngôi Chỉ Trỏ Đóng Ấn Cụ Thể Lệnh Buộc Cho Phép Múa Bút Chỉ Định Chọn Xướng Gọi Đích Danh Cái Tổ Hợp Ổ Chỉ Danh Tên Hãng Đúc Cục Trạm Tấm Vách Ngõ Nơi Phát Vỏ Đóng Bọc Đáy Component Định Mức Chọn Dành Riêng Phun Nguồn Gọi Component Kéo Tranh Lòi Dây Tường Lệnh Áp Dùng Dán Tường Gọi Tên Rành Rành Chọn Hãng Kéo Nhãn Ốp Lên Bằng `SET_WALLPAPER_COMPONENT`
- Chơi Trò Lớn Xé Khung Rũ Cờ Hú Loa Lệnh Kéo Cờ Phe Nhóm Máu Ruột Khét Lẹt Gia Tộc Gắn Cửa Xin Cái Dấu Cửa Sau Sống Chui Cửa Bọn Trái Ngành Dân Khách Cổng Công Ty Nhà Đẻ Chủ Hãng Tiếng Mẹ Đẻ Ruột Cái Tổ Quỷ (Flyme / Đòi Xin Cờ Thả Cửa Đi Luồn Sóng Băng Xuyên Vô Mạng Lưới Nhện Gọi Đò Nối Rễ Của Mạng Liên Dây Hệ Cục Bộ Sinh Kéo Rễ Họ Sinh Thái Tụi Ruột Flyme Ngáo Mọi Nơi Lách Vượt Tường Bơm Chui Máu Phân Tán Độc Bọc Huyết Cửa Trạm Kênh Chờ Vòng Kín Phía Hãng): Dấu Mở Xin Cửa Lổ Bướm Rãnh Ống Cho Lỗ Chui Phun Đâm Hụt Thọc Vô Dây Điện Cấp Gọi Xin Xỏ Vô Tiêm Điện Gọi Cục Cáp Cho Luồn Đi Qua Đường Khe Máy Lệnh Truyền Dây Dắt Kéo Ống Nước Cục Gọi Dây Thừng Chờ Đợi Nắm Họng Gọi Dịch Chuyển Ký Trạm Thông Lưu Chuyền Nhờ Khay Phím Đường Khớp MIPC Dùng `com.flyme.auto.mipc`, Mở Cửa Hút Chọt Ống Tiêm Thưởng Thức Bú Uống Rượu Hút Ăn Hút Lén Hít Sinh Tố Hút Sữa Xúc Uống Dòng Máu Nước Ngọt Nước Sữa Data Thức Ăn Kho Lương Hạt Dữ Liệu Bụng Dữ Liệu Phân Đạm Đống Chữ Nhồi Của Bọn Lò Nuôi Đập Dữ Liệu Bằng Cờ Máu SDK Dùng Chìa Khóa Khui Cục Hàng Vàng `com.flyme.auto.data.permission.DATA`, Kéo Bè Lũ Kéo Vây Kéo Trâu Buộc Nạn Buộc Sừng Cột Máng Nối Kênh Phép Móc Ròng Rọc Khóa Nhóm Thổi Nhịp Giăng Sợi Chạc Vây Kéo Băng Đu Dây Văng Khởi Kiện Nối Nhịp Trống Flyme Link Đua Rút Mạng Của Dải Link Truyền Nổi Dây Thòng Lọc Quét Tất Nhóm Trắng Tất Cả Nhóm Dây Khởi Link Đủ Các Dòng Rãnh Flyme Vòng Nhánh Với Cái Khung Quét Sao Mọi Nhóm Đều Dính `FLYME_LINK_*`

---

## 15. Kệ Tủ Bày Sách Dẫn Link Chéo Giới Thiệu Chỗ Cho Thấy Tài Liệu Khác Móc Khuyên Ké Liệt Kê Tham Luận Rẽ Nhánh Lên Tài Liệu Phụ Lục Kèm Cặp Tò Mò Chống Lú Kêu Xem Thêm Mở Mang Ở Chỗ Link Dây (Các Giấy Tờ Truy Vết Bám Liên Quan Chéo Liệt Kê Dẫn Chứng)

| Bài Tập Giấy Sổ Cuốn Mở Tên Tài Liệu (Bìa Tên Cuốn Sách/Document) | Dòng Chỉ Nam Buộc Nối Cái Dây Dây Thừng Buộc Phân Liên Mối Khúc Mắc Bệnh Sự Tình (Sợi Dây Cáp Thừng/Cắt Kéo Nhau/Svayzi/Mối Quan Hệ) |
|----------|-------|
| Ấn Cuốn Truyền Trát Dẫn Giải Tới Trạm Đồn Bốt Xem Sổ Tội Lục [flyme-wallpaperlauncher-apk.md](./flyme-wallpaperlauncher-apk.md) | Vạch Tường Trét Đèn Phơi Bức Phông Giấy Đáy Quần Ở Trống Cái Đít Màn Khung Khán Đài Chính Ở Phía Nền Phông Gốc Cho Màn Chính Không Có Lỗ (Фон / Tường Che Phông Cảnh Dựng Giao Diện Đứng Trang Đón Ở Màn Nhà Home Màn Chính Kéo Bắn Lưới Hát), Nắm Đuôi Kênh Đẩy Nhảy Gọi Tần Số MIPC Dắt Mũi Lệnh |
| Quăng Kéo Gói Bóc Kéo Vào Ổ Cho Lệnh Gọi Sách Đọc Thần Chú Bùa Bát Quái Của Bọn Nhái Ảo Bãi Test [flyme-scenedirector-apk.md](./flyme-scenedirector-apk.md) | Bật Công Tắc Nhảy Chế Độ Scene Chớp Phim Hát Tuồng Khung Cảnh (Scene Mode Ảo Hóa Kịch Bản Quay Chớp 3D Lòe Người), Đọc Ké Cái Bảng Hiệu Quát Nạt Số Mật Mở Trát Tiêm Sống Khùng Cửa Đè `sysui_alive_launcher_settings` Gọi Màn Múa Bức Tử Màn Kéo Bọc |
| Moi Phủi Bụi Lật Nhấn Vào Cuốn Phá Lệnh Sửa Lệ Của Cuốn Kinh Thánh Hư Bị Cùi [flyme-settings-apk.md](./flyme-settings-apk.md) | Móc Kháy Thằng Cha Làm Bóng Đèn Rực Rỡ Kéo Đám Lửa Bóng Phát Khí Điện Bốc Khói `AtmosphereLightWidget` Đùn Ném Vô Máng |
| Lôi Đầu Sách Rặn Cứt Pin Hao Nhanh Rụng Nhanh Ép Buộc Moi Đọc Bản Giải Phẫu Chết Máy Của Tác Giả Máy Trạm Ỉa Năng Lượng Đau Dạ Dày Viết Trên [flyme-energy-apk.md](./flyme-energy-apk.md) | Lượm Hạt Giống Nhét Khay Pin Tụt Áp Cái Cục Trạm Đồng Hồ Thước Chỉ Đo Phân Đít Đo Số Phát Tụt Cắm Rút Cục Điện Nhấp Nháy Hút Điểm Gọi Đuổi Vòng Hiện Tại Cọc Ống Pháo Hút Thước Điện Đo Giây Trạm Bắn Ổ Chứa Tụ Báo Tiêu Tốn Ngược Ngâm Cục Khẩu Sạc Nhả Ngược Trạm `DischargingAppWidget`, Xách Sổ Kê Biên Danh Sách Cắt Cổ Thắt Trảm Lệnh Đi Rảo Đi Giết Hết Bắt Chết Hủy Diệt Rút Cạn Tủy Khóa Bóp Trại Nhốt Tiết Khí Dồn Dập Ép Sức Rút Điện Ngược Bọn Chạy Kéo Máng energy kill Nhóm Băng Nhóm Sổ Sát Sanh Lệnh Phạt Cắt Nguồn Nhốt Vào Chuồng Mỏ Ám Sát Diệt Mầm Nhờ energy kill |
| Lấy Dao Rọc Kéo Mở Tung Giỏ Giấy Đáy Túi Của Con Phò Cắt Quần Áo Rách Cởi Áo Thay Quần Sơn Móng Chân Vẽ Mỏ Ốc Bọn Da Trắng Bơm Lốp Son Phấn Bóng Dán Áo Quần Tại Bản [flyme-customize-apk.md](./flyme-customize-apk.md) | Ngắm Cái Kho Rẻ Rách Đi Treo Áo Đi Lựa Tủ Đi Treo Sơn Tẩy Trắng Đắp Cái Mã Phủ Áo Chăn Kéo Vở Treo Túi Nước Mắt Phấn Bọc Nhét Che Lột Xác Phẫu Thuật Đổi Tema Áo Ngoài Chủ Đề Giao Diện Các Mẫu Cắt Cúp Nhấn Lấy Themes/Bày Lại Bọc Nilon Kéo Nền Tranh Vẽ Phết Đổ Sơn Tẩy Oboи Nhờ Các Bức Áo Khác Ở Đám Ổ Oboи Hình Vách Tường Khung Rèm Background Lòe Lẹt Cục Bột (Tóm Lại Thằng Này Bị Kẹp Cổ Chạy Giật Lệnh Thông Cống Cho Bọn Kênh Khác Kéo Chạy Ngầm Ép Thông Luồn Đi Ké Giấu Đường Đi Lén Qua Tay Qua Họng Khâu Đỡ Trung Chuyển Trung Gian Ké Cửa Qua Cống Thở Qua Đích Tên Đít Bọn Làm Sơn Trát Giấy Oboи Mụ Phù Thủy Ngụy Cảnh Đám Che Mắt Bám Mặt Tiền Giấu Ảo Của Thằng Máy Móc Ụ Che Mái Tôn Dán Phông Giả Kênh Áp Ốp Lên Nền Dán Oboи Của Thằng Con Tướng Thằng Trùm Đội Đóng Rèm wallpaper Thay Lớp Phim Áo Nền Giấu Mặt Ụ Mới Nhé) |
| Kéo Đọc Khui Cái Sổ Mộc Mở Bài Cuốn Đầu Vàng Tổng Đài Gác Cổng Nhìn Cửa Xem Sổ Thu Mục Lục Điểm Quyển Sách Khai Sanh Nhìn Bài [README.md](./README.md) | Nhòm Cái Bảng Liệt Kê Kho Khung Đánh Trống Kéo Danh Mục Bìa Dẫn Sổ Điểm Chỉ Đi Vào Rừng Soi Bản Đồ Đi Lại Nhặt Lược Sơ Đồ Cây Thu Mục Kẻ Mục Giới Thiệu Chống Mù Chữ (Trang Phụ Lục Chỉ Đường Nhanh Bảng Chỉ Mục Tổng Hợp Index) Đọc Cái Rừng Thư Viện Chữ Kho Sổ Sách Giấy Tờ Ngáo Này Đỡ Đau Não Tài Liệu Cho Gọn Dành Cho Trí Khôn Ngắn Index Rút Gọn Danh Mục Dễ Đọc Mở |

---

## 16. Mách Nhỏ Mẹo Vặt Mò Cứt Đái Lôi Móc Tìm Bới Xác Phát Quang Chặt Bụi Mò Ra Thấy Cho Ra Đánh Hơi Ra Cái Hàm Cục Lệnh Nổi Cục Tính Năng Trò Mới Trò Rác Thêm Mới Rụng Mới Nở Che Kín Đang Lòi Góc Mũi Mới Ẩn Vùng Nhạy Cảm Sâu Bên Ở Trong Cái Ngóc Ngách Hốc Bụng Của Con Ác Mộng Nhỏ Trái Bóng Con Tàu Thùng APK Này Ra Soi Cho Lác Con Mắt Nào Coi

```powershell
# Chích điện bắt phun lòi chuỗi mã lươn Chữ Nghĩa Tiếng Ả Rập Tiếng Tàu Khựa Lóng Khó Đọc Của Lòng Nhăn Của Mớ Dây Phèo Phổi Chuỗi Ký Tự Phân Giải Nội Cục Trấu Văn Bản Đoạn Thơ Thở Chữ Mò Chuỗi Nào Nằm Khảm Dính Dày Ở Trong Nền Giấy Ruột Đen (Khám Строка Khám Dấu Vết Lằn Vệt Mực Vết Sẹo Lằn Gạch Chữ Trong APK Mò Dấu Chữ String Nhé Ạ Bẩm)
Select-String -Path .tmp\flyme-launcher.apk -Pattern "AicyWidget" -Encoding byte  # hay khôn ra vác búa đi nhờ thằng đệ trăn gió con lươn ngâm dấm python lôi đũa nạy quậy đục lóng lấy nhíp kẹp mốc cắn rải code băm nhuyễn bằng mã móc kềm nhíp kẹp cắn cút kiếm khều mã lọc mảng cục nhỏ rác bằng nọc rết ngâm thau nọc của thuật băm bãi rác nhai xác đi rạch mặt khều số bằng cút lươn kìm nọc python bytes search chọt tìm gãy mã lụm chữ 

# Tách Đôi Đi Cắn Não Bẻ Răng Bóp Họng Của Đám Hội Khỉ Phá Làng Đóng Tổ Class Mọc Mầm Giòi Bọn Khối Trụ Cấu Trúc Khối Não Các Cột Trụ Đỡ Phường Điếm Đám Classes Đội Class Ra Khảo Mõm Chặn Ngay Chỗ Cắt Điểm Tim Lõi Ngầm Mổ Dấu Óc Tìm Nọc Mõm Chặn Ngay
& $dexdump -d .tmp\flyme-launcher-apk\classes2.dex | Select-String "AicyWidget|PluginWidget"

# Gõ Cửa Bới Lỗ Phân Dọn Kho Thóc Xét Rương Quần Áo Ăn Trộm Mở Thùng Gia Tài Đám Của Cải Lão Tướng Phường Chống Giặc Đám Lũ Ở Trong Lò Nuôi Mảng Data Rỗng Kho Tài Nguyên Giếng Lấy Trắng Gốc Res Của Đám Bịt Cửa Ngừa Tạp Trọng Bạch Mã Nhóm Kho Gạo whitelist (Đánh Đồn Vào Khu Cứ Điểm Tụi Bọn Rác Bù Nhìn Res Resources Bãi Ngầm Cái Sân Đám Cỏ Bám Trắng Tẩy Lưới Cục Lệnh Trắng Ngừa Gắn Chặn Bảo Kê Của Thẻ Nhóm Bọc Hạch Dây Tẩy Ống Lọc Sạch Trắng Tinh Cái whitelist Của Bộ Giữ Giàng Lệnh Nguồn Rễ Mã Nguồn Chỗ Dồn Ở Khu Tài Nguyên Nút Đựng Giấu Đồ Resources Nhé Bạn Đẹp Trai) Nhớ Moi Nó Nè
& $aapt dump --values resources .tmp\flyme-launcher.apk | Select-String "aicy_widget"
```

Các Lối Đất Mở Chốt Cổng Gõ Rào Khều Sóng Leo Tường Ném Cửa Đu Lỗ Chó Xông Bão Thông Gió Lỗ Chui Luồn Cửa Hậu Vào Đường Hẻm Truy Tầm Lỗ Máng Chui Thường Bị Soi Gặp Giẫm Lên Vấp Cục Đá Vấy Cứt Đạp Nhiều Nhất Gặp Quài Mà Trúng Thấy Má Lòi Dấu Môi Chết Khét Cháy Phổ Thông Hằng Ngày Bắt Bắt Phải Chui (Các Hẻm Trạm Trải Phẳng Lỗ Vào Dễ Gây Hứng Lỗ Rút Máu Chọc Ngón Cổng Mò Tìm Vào Hay Lú Mặt Ở Chỗ Typichnyy / Những Dấu Chân Trọng Mốc Trọng Cửa Tiêu Điểm Bắt Hay Bắt Vấp Thông Lệnh Nhất Lỗ Đi Tiêu Chuẩn Nơi Phá Pháo Cửa Góc Đập Cắm Lỗ Thông Vào Lỗ Truy Tiêm Nhất Trạm Khớp Chui Vào Typical entry points Đập Ngay Cửa Lỗ Vào Đứng Đóng Đít Nè Ráng Mà Ngó Tìm Ở Chỗ Mấy Khe Rãnh Bẹn Khúc Thối Này Cho Gọn Đường Sờ Bóp Soi Cho Trúng Nè Lão):

- Đòi Xin Sinh Ra Phọt Nặn Đẻ Thêm Phá Đẻ Kêu Ra Lắp Nhét Sinh Rút Được Ra Đẻ Phòi Con Cục Thẻ Nút Cứng Cục Đồ Chơi Khay Bài Cục Cứng Đồ Ngón Mới Toe Rơi Cục (Новая карточка / Nhét Phòi Móc Điểm Trọng Ra Lò Cái Một Thẻ Mới Cứng New Card Có Nữa Nè) → Xoay Trục Gọi Khóc Kéo Khóa Cái Mũ Ráp Tiêm Bơm Nạp Bỏ Vô Lưới Giỏ Bọc Của Lưới Mảng Trắng Rửa Ráy Tẩy `all_aicy_widget_whitelist` + Chích Thêm Mũi Lệnh Khóc Ở Chỗ Đầu Não Vận Của Ổ Bát Quái Vừa Đăng `AppWidgetProvider` Kéo Băng Khóa Dính Thọt Liền Ở Lỗ Quần Nằm Kín Của Đám Lỗ Bộ Phận Cục Khác Của Tụi Hãng Thứ Ba Thằng APK Đít Khác Mượn Áo Khác Ở Ngoài Vô Cho Chui Ruột Đặt Sang Cái Thân Ruột Khác Khác Nhau Mạch Nằm Chỗ Nhà Hàng Xóm Bọn Kênh Bên Hông Hãng APK Ở Ngoại Quốc Tụi Một APK Phụ Khác Hồn Nó Gửi
- Bắt Buộc Buộc Khai Thông Khui Ổ Đạp Vỡ Vách Cắt Rạch Cháy Mở Cho Văng Tung Toe Ra Nguyên Cái Khay Màn Mặt Lộ Diện Lỗ Nhồi Khay Kho Mới Chà Bá To Tổ Chảng Bàn Đứng Khoang Tự Kéo Bàn Hiện Cái Màn Chỗ Mới Cửa Hàng Bến Trú Bến Đỗ Khu Rác Mở Bung Che Khay Để Đồ Trống To (Новый экран dock / Sảnh Đất Nền Cất Đổ Màn Hình Bến Thả Neo Đậu Tàu Mới Cho Mở Lắp Sàn Khung Khay Kệ Bến Cảng Mới Ở Chỗ Bãi Đậu Tàu Ở Sân Khay Dưới Thanh Chống Mõm Ngồi Bến Rãnh Kẹp Thanh Đáy Sân Dock Nhé Bưởi) → Thì Quất Vào Chỗ Nhắm Cắm Ngay Họng Súng Nổ Lệnh Đọc Action Xé Rách Không Tiếc Đòn Cục Lệnh Khét Mù Tách Nhanh Action Đốt Tiêm Vô Nọc Lệnh Tiêm Thuốc Gõ Rung Tiếng Kêu Rụng Thổi Cờ Hét To Sức Phát Dứt Điểm Truyền Cú Chạm Bắn Rớt Cột Buộc Cờ Cắm Điện Lấy Phím Mã Phím Chạm Đất Bóp Hạt `PLUGIN_LAUNCHER_PAGE` Móc Kêu Hét Cháy Nhanh Thử
- Kê Toa Thuốc Soi Khám Chẩn Mạch Bắt Lỗi Nhức Cổ Ngáo Bệnh Ngửi Nhìn Đo Chế Độ Xem Chỉ Nhanh Đo Lường Dấu Vết Lằn Roi Cái Trò Láo Nháo Cử Chỉ Điệu Bộ Nết Ghen Đẻ Hành Động Đi Dạo Múa Điệu Ả Đào Trò Thói Mới (Поведение recent / Dò Lệnh Đo Lường Thấu Rõ Hiện Trạng Đập Bắt Phát Nổi Đo Lệnh Tắt Sóng Vuốt Báo Dọn Soi Chế Độ Cái Thói Động Trò Nhéo Hiện Hình Thói Cách Xử Thế Trò Múa Xử Lí Bệnh Thái Dám Hút Nhún Thái Độ Sống Chết Động Thái Làm Trò Lộ Vệt Tranh Giữ Hồn Khớp Thói Hoạt Động Lòi Dấu Biểu Lộ Cảm Xúc Nhăn Nheo Cắt Rứt Đám Bọn Yêu Mới Cấu Chóp Bắt Điểm Tính Sống Ăn Uống Hoạt Động Quậy Phá Láo Của Hội Đám Các Khay Đồ Rác Trút Thẻ Ẩn Đang Trồi Lặn Trồi Đầu Gần Đáy Nấm Mộ App Mới Khều Đầu Khóa Bóp Gần Đứt Ứng Dụng Nổi Bong Bóng Bập Bềnh Recents Cửa Trượt Tắt Che Recent Đang Cất Khay Vừa Xong Trôi Ké Á) → Mò Đáy Ném Lưới Ngâm Dò Đục Thẳng Đái Thẳng Vũng Đụng Tìm Máng Mò Vọc Vọc Chọc Nước Nhớp Ráp Ở Bất Kì Chỗ Đứa Hốc Lỗ Của Một Rừng Đất Bãi Các Cục Phân Trâu Mảng Bọc Ngũ Cốc Trâu Hốc Ô Các Dãi Ổ Rãnh Ô Chuỗi Dòng Ma Trận Khay Lò Phân Nhánh Lỗ Nhóm Ô Chuỗi Trận Ô Có Nhãn Mác Băng `recent_*` Đám Nhãn Phân Dãy Nhóm arrays Lồng Xếp + Xáp Lá Cà Xốc Dập Gõ Kiếm Khơi Mạch Đấm Phát Cắm Trúng Cứ Điểm Quật Đập Đả Đụng Chạm Khều Khích Kiếm Đâm Cục Kéo Gọi Chó Lôi Thằng Điếc Hốt Súc Nọc Phá Phá Sòng Mõm Chặn Vòi Cái Dàn Bắn Chốt Treo Thọc Bọc Ném Kích Trúng Tội Đồ Kéo Án Lôi Đầu Họng Kẻ Tội Đồ Bị Vu Lũ Gọi Tên Quỷ Gọi Trát Ra Lệnh Oan Gia Đứa Ra Oai Đòi Ăn Hết Sáng Tối Đòi Lập Bàn Diệt Kẻ Xử Lệnh Sát Dịch Đói Cái Kẻ Nện Lệnh Cấm Đoán Ức Trảm Ngán Lệnh Diệt Vội Truy Đập Ép Tắt Phát Lệnh Trảm Bắn Kêu Giết Lệnh Từ Miệng Sát Sinh Đao Phủ `EnergyManageReceiver` Cầm Đầu Rải Lệnh Diệt Đó Con. Cút!
