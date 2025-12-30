package com.example.ungdungkiemphieu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ungdungkiemphieu.data.model.*
import com.example.ungdungkiemphieu.repository.PollRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.asStateFlow

class PollViewModel(
    private val repository: PollRepository
) : ViewModel() {

    val polls = MutableStateFlow<List<Poll>>(emptyList())
    val loading = MutableStateFlow(false)
    val pollLoading = MutableStateFlow(false)
    private val _selectedPoll = MutableStateFlow<Poll?>(null)
    val selectedPoll: StateFlow<Poll?> = _selectedPoll

    // Pagination states
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
    
    private var currentOffset = 0
    private val pageSize = 20
    private var totalCount = 0

    val candidateStats = MutableStateFlow<List<candidatestats>>(emptyList())
    val totalBallots = MutableStateFlow(0)

    private val _roleUpgradeResult = MutableStateFlow<RequestRoleUpgradeResult?>(null)
    val roleUpgradeResult: StateFlow<RequestRoleUpgradeResult?> = _roleUpgradeResult.asStateFlow()

    private val _isRequestingRole = MutableStateFlow(false)
    val isRequestingRole: StateFlow<Boolean> = _isRequestingRole.asStateFlow()

    fun loadPolls() {
        viewModelScope.launch {
            loading.value = true
            currentOffset = 0
            _hasMore.value = true

            try {
                val response = repository.getPolls(
                    forceRefresh = true,
                    limit = pageSize,
                    offset = 0
                )
                
                if (response.success) {
                    totalCount = response.total
                    currentOffset = pageSize
                    _hasMore.value = currentOffset < totalCount
                    
                    // Polls đã được cache trong repository
                    polls.value = repository.pollsCache.value
                    
                    Log.d("PollViewModel", "Loaded ${polls.value.size}/$totalCount polls, hasMore: ${_hasMore.value}")
                }
            } catch (e: Exception) {
                Log.e("PollViewModel", "Error loading polls", e)
            } finally {
                loading.value = false
            }
        }
    }
    
    fun loadMorePolls() {
        // Prevent multiple simultaneous loads
        if (_isLoadingMore.value || !_hasMore.value || loading.value) {
            return
        }
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            try {
                val response = repository.getPolls(
                    forceRefresh = false,
                    limit = pageSize,
                    offset = currentOffset
                )
                
                if (response.success) {
                    currentOffset += response.count
                    _hasMore.value = currentOffset < response.total
                    
                    // Polls đã được append vào cache trong repository
                    polls.value = repository.pollsCache.value
                    
                    Log.d("PollViewModel", "Load more: now ${polls.value.size}/${response.total}, hasMore: ${_hasMore.value}")
                }
            } catch (e: Exception) {
                Log.e("PollViewModel", "Error loading more polls", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    fun joinPoll(accessCode: String) {
        viewModelScope.launch {
            try {
                val response = repository.joinPoll(accessCode)
                if (response.success.toBoolean()) {

                }
            }
            catch (e: Exception){

            }
        }


    }

    fun loadPollById(pollId: Int?) {
        Log.d("PollViewModel", "loadPollById called with: $pollId")

        if (pollId == null) {
            Log.e("PollViewModel", "pollId is NULL!")
            return
        }

        viewModelScope.launch {
            pollLoading.value = true
            Log.d("PollViewModel", "Starting to load poll...")

            try {
                val poll = repository.getPollById(pollId)
                Log.d("PollViewModel", "Poll loaded successfully: $poll")
                _selectedPoll.value = poll
                Log.d("PollViewModel", "selectedPoll updated to: ${_selectedPoll.value}")
            } catch (e: Exception) {
                Log.e("PollViewModel", "Error loading poll: ${e.message}", e)
                _selectedPoll.value = null
            } finally {
                pollLoading.value = false
                Log.d("PollViewModel", "Loading finished. Final state: ${_selectedPoll.value}")
            }
        }
    }
    fun getResultPollById(pollId: Int?) {
        viewModelScope.launch {
            loading.value = true
            try {
                val response = repository.getResultPollById(pollId)
                if (response.success) {
                    candidateStats.value = response.stats
                    totalBallots.value = response.total_ballots
                } else {
                    candidateStats.value = emptyList()
                    totalBallots.value = 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                candidateStats.value = emptyList()
                totalBallots.value = 0
            } finally {
                loading.value = false
            }
        }

    }
    fun requestRoleUpgrade(pollId: Int?, requestedRole: String) {
        if (pollId == null) {
            _roleUpgradeResult.value = RequestRoleUpgradeResult.Error("Poll ID không hợp lệ")
            return
        }

        viewModelScope.launch {
            _isRequestingRole.value = true
            _roleUpgradeResult.value = null

            try {
                val result = repository.requestRoleUpgrade(pollId, requestedRole)

                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        _roleUpgradeResult.value = RequestRoleUpgradeResult.Success(
                            message = response.message,
                            requestedRole = response.requested_role ?: requestedRole
                        )

                        // Cập nhật lại poll detail để hiển thị trạng thái mới
                        loadPollById(pollId)
                    } else {
                        _roleUpgradeResult.value = RequestRoleUpgradeResult.Error(
                            response?.message ?: "Không thể gửi yêu cầu"
                        )
                    }
                } else {
                    val error = result.exceptionOrNull()
                    _roleUpgradeResult.value = RequestRoleUpgradeResult.Error(
                        error?.message ?: "Đã xảy ra lỗi"
                    )
                }

            } catch (e: Exception) {
                Log.e("PollViewModel", "Request role upgrade error", e)
                _roleUpgradeResult.value = RequestRoleUpgradeResult.Error(
                    e.message ?: "Đã xảy ra lỗi"
                )
            } finally {
                _isRequestingRole.value = false
            }
        }
    }

    fun clearRoleUpgradeResult() {
        _roleUpgradeResult.value = null
    }
}
sealed class RequestRoleUpgradeResult {
    data class Success(val message: String, val requestedRole: String) : RequestRoleUpgradeResult()
    data class Error(val message: String) : RequestRoleUpgradeResult()
}