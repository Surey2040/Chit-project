package com.jothivel.chits.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jothivel.chits.data.repository.GroupRepository
import com.jothivel.chits.data.repository.MemberRepository
import com.jothivel.chits.data.remote.ApiClient
import com.jothivel.chits.data.remote.ApiService
import com.jothivel.chits.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val memberRepository = MemberRepository(application)
    private val groupRepository = GroupRepository(application)
    private val tokenManager = TokenManager(application)
    private val apiService = ApiClient.getClient(tokenManager).create(ApiService::class.java)
    private val db = com.jothivel.chits.data.local.AppDatabase.getDatabase(application)
    private val activityLogDao = db.activityLogDao()

    // Live from Room â€” auto-updates when members/groups are added
    val totalMembers: LiveData<Int> = memberRepository.getMemberCount()
    val activeChitsCount: LiveData<Int> = groupRepository.getGroupCount()
    
    val recentLogs: LiveData<List<com.jothivel.chits.data.local.entity.ActivityLogEntity>> = activityLogDao.getRecentLogs(10)

    // From API (or default 0)
    private val _totalChitValue = MutableLiveData<Int>(0)
    val totalChitValue: LiveData<Int> = _totalChitValue

    private val _monthCollected = MutableLiveData<Int>(0)
    val monthCollected: LiveData<Int> = _monthCollected

    private val _pendingPaymentsCount = MutableLiveData<Int>(0)
    val pendingPaymentsCount: LiveData<Int> = _pendingPaymentsCount

    fun loadDashboardStats() {
        try {
            apiService.getDashboardStats().enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        _totalChitValue.postValue((data["totalChitValue"] as? Number)?.toInt() ?: 0)
                        _monthCollected.postValue((data["monthCollected"] as? Number)?.toInt() ?: 0)
                        _pendingPaymentsCount.postValue((data["pendingPaymentsCount"] as? Number)?.toInt() ?: 0)
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    // API unavailable â€” Room counts still work
                }
            })
        } catch (e: Exception) {
            // Silently fail â€” Room counts still work
        }
    }
}


