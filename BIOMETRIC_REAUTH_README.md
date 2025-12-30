## Hướng dẫn sử dụng tính năng Xác thực lại khi mở app

### Tính năng đã triển khai:

✅ **Session Management**: App theo dõi trạng thái session của người dùng

✅ **Auto Lock**: Khi người dùng gạt app ra ngoài (swipe away), session sẽ bị đánh dấu là inactive

✅ **Biometric Re-authentication**: Khi mở app lại, hiển thị màn hình xác thực với vân tay tự động

✅ **Password Fallback**: Người dùng có thể chọn "Sử dụng mật khẩu" nếu không muốn dùng vân tay

### Cách hoạt động:

1. **Khi đăng nhập thành công**:
   - Session được đánh dấu là "active"
   - Token được lưu với thời gian hết hạn

2. **Khi app vào background** (onPause):
   - Session được đánh dấu là "inactive"
   - Lưu thời gian app vào background

3. **Khi mở app lại**:
   - App kiểm tra `needsReauthentication()`
   - Nếu session inactive → Hiển thị màn hình xác thực (ReauthScreen)
   - Tự động hiển thị biometric prompt nếu đã bật vân tay

4. **Xác thực thành công**:
   - Session được set về "active" 
   - Navigate về màn hình home

### Test thử:

1. Đăng nhập vào app bình thường
2. Bật đăng nhập bằng vân tay (nếu chưa bật)
3. Gạt app ra ngoài (swipe away từ recent apps)
4. Mở app lại → Sẽ thấy màn hình xác thực với biometric prompt tự động
5. Xác thực vân tay thành công → Vào lại app ngay

### Các file đã thay đổi:

- ✅ `AuthManager.kt`: Thêm session management functions
- ✅ `MainActivity.kt`: Thêm lifecycle callbacks (onPause, onResume)
- ✅ `NavGraph.kt`: Thêm route "reauth" và logic kiểm tra
- ✅ `ReauthScreen.kt`: Màn hình xác thực mới (NEW FILE)

### Customization:

Bạn có thể điều chỉnh timeout trong AuthManager.kt:
```kotlin
private val SESSION_TIMEOUT = 5 * 60 * 1000L // 5 phút (hiện tại không dùng)
```

Hiện tại app sẽ yêu cầu xác thực **mỗi khi mở lại** sau khi gạt app, giống như app ngân hàng.
