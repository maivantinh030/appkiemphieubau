package com.example.ungdungkiemphieu.detector

/**
 * 🧪 CONFIGURATION FILE - Dùng để so sánh hiệu suất
 *
 * Thay đổi giá trị USE_OPTIMIZED_VERSION để switch giữa 2 phiên bản:
 * - true: Sử dụng Coroutines (Tối ưu - Main Thread rảnh) ✅
 * - false: Xử lý trực tiếp (Block Main Thread) ❌
 */
object PerformanceConfig {
    // ⚙️ THAY ĐỔI ĐÂY ĐỂ TEST
    const val USE_OPTIMIZED_VERSION = true  // true = Optimized ✅, false = Blocking ❌

    val mode: String = if (USE_OPTIMIZED_VERSION) "✅ OPTIMIZED (Coroutines)" else "❌ BLOCKING (Main Thread)"
}