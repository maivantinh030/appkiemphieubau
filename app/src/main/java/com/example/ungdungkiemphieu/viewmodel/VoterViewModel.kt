package com.example.ungdungkiemphieu.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import com.example.ungdungkiemphieu.data.model.Voter
import com.example.ungdungkiemphieu.repository.VoterRepository

class VoterViewModel(private val repository: VoterRepository) : ViewModel() {

    private val _voters = MutableStateFlow<List<Voter>>(emptyList())
    val voters: StateFlow<List<Voter>> = _voters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 50
    private var currentSearch: String? = null
    private var currentFilter: Boolean? = null
    private var currentPollId: Int? = null

    fun loadVoters(
        pollId: Int,
        search: String? = null,
        checkedInFilter: Boolean? = null,
        refresh: Boolean = false
    ) {
        if (refresh) {
            currentOffset = 0
            _voters.value = emptyList()
            _hasMore.value = true
        }

        // Kiểm tra nếu đang tải hoặc không còn dữ liệu
        if (_isLoading.value || _isLoadingMore.value || (!refresh && !_hasMore.value)) {
            return
        }

        currentPollId = pollId
        currentSearch = search
        currentFilter = checkedInFilter

        viewModelScope.launch {
            if (refresh) {
                _isLoading.value = true
            } else {
                _isLoadingMore.value = true
            }
            _error.value = null

            try {
                Log.d("VoterViewModel", "Gọi API với offset: $currentOffset, pageSize: $pageSize")

                val result = repository.getVoterList(
                    pollId = pollId,
                    limit = pageSize,
                    offset = currentOffset,
                    search = search,
                    checkedIn = checkedInFilter
                )

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        val newVoters = response.voters

                        // QUAN TRỌNG: Không sắp xếp ở đây, để server xử lý thứ tự
                        // Chỉ thêm dữ liệu mới vào cuối danh sách
                        _voters.value = if (refresh) {
                            newVoters
                        } else {
                            _voters.value + newVoters
                        }

                        _totalCount.value = response.count
                        currentOffset += newVoters.size
                        _hasMore.value = _voters.value.size < response.count

                        Log.d("VoterViewModel", "Tải thành công ${newVoters.size} cử tri, " +
                                "danh sách hiện tại: ${_voters.value.size}, " +
                                "tổng: ${response.count}, " +
                                "còn tiếp: ${_hasMore.value}, " +
                                "offset tiếp theo: $currentOffset")
                    } else {
//                        _error.value = response?.message ?: "Không thể tải danh sách cử tri"
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _error.value = error?.message ?: "Đã xảy ra lỗi"
                }

            } catch (e: Exception) {
                Log.e("VoterViewModel", "Lỗi khi tải danh sách cử tri", e)
                _error.value = e.message ?: "Đã xảy ra lỗi"
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
            }
        }
    }

    fun loadMore() {
        Log.d("VoterViewModel", "loadMore() được gọi - " +
                "isLoadingMore: ${_isLoadingMore.value}, " +
                "isLoading: ${_isLoading.value}, " +
                "hasMore: ${_hasMore.value}")

        if (!_isLoadingMore.value && !_isLoading.value && _hasMore.value) {
            currentPollId?.let { pollId ->
                Log.d("VoterViewModel", "Bắt đầu tải trang tiếp theo")
                loadVoters(pollId, currentSearch, currentFilter, refresh = false)
            }
        }
    }

    fun checkinVoter(pollId: Int, voterId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.checkinVoter(pollId, voterId)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        // Cập nhật voter trong danh sách mà không thay đổi thứ tự
                        _voters.value = _voters.value.map { voter ->
                            if (voter.voter_id == voterId) {
                                response.voter
                            } else {
                                voter
                            }
                        }

                        onSuccess()
                    } else {
                        onError(response?.message ?: "Check-in thất bại")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    onError(error?.message ?: "Đã xảy ra lỗi")
                }

            } catch (e: Exception) {
                Log.e("VoterViewModel", "Lỗi check-in", e)
                onError(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun undoCheckin(pollId: Int, voterId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.undoCheckinVoter(pollId, voterId)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        // Cập nhật voter trong danh sách mà không thay đổi thứ tự
                        _voters.value = _voters.value.map { voter ->
                            if (voter.voter_id == voterId) {
                                response.voter
                            } else {
                                voter
                            }
                        }

                        onSuccess()
                    } else {
                        onError(response?.message ?: "Hủy check-in thất bại")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    onError(error?.message ?: "Đã xảy ra lỗi")
                }

            } catch (e: Exception) {
                Log.e("VoterViewModel", "Lỗi hủy check-in", e)
                onError(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Thêm hàm để reset trạng thái khi cần
    fun resetPagination() {
        currentOffset = 0
        _voters.value = emptyList()
        _hasMore.value = true
        _isLoading.value = false
        _isLoadingMore.value = false
    }

    // ============================================
    // PHẦN TEST - Lấy tất cả voters (không phân trang)
    // ============================================
    
    fun loadAllVoters(
        pollId: Int,
        search: String? = null,
        checkedInFilter: Boolean? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _voters.value = emptyList()

            try {
                Log.d("VoterViewModel", "[TEST] Gọi API getAllVoters - search: $search, checkedIn: $checkedInFilter")

                val result = repository.getAllVoters(
                    pollId = pollId,
                    search = search,
                    checkedIn = checkedInFilter
                )

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        _voters.value = response.voters
                        _totalCount.value = response.count
                        _hasMore.value = false // Không có phân trang nên không còn dữ liệu để tải

                        Log.d("VoterViewModel", "[TEST] Tải thành công ${response.voters.size} cử tri, " +
                                "tổng: ${response.count}")
                    } else {
                        _error.value = "Không thể tải danh sách cử tri"
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _error.value = error?.message ?: "Đã xảy ra lỗi"
                }

            } catch (e: Exception) {
                Log.e("VoterViewModel", "[TEST] Lỗi khi tải tất cả cử tri", e)
                _error.value = e.message ?: "Đã xảy ra lỗi"
            } finally {
                _isLoading.value = false
            }
        }
    }
}