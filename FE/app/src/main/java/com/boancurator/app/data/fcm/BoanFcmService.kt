package com.boancurator.app.data.fcm

import android.util.Log
import com.boancurator.app.data.api.ApiService
import com.boancurator.app.util.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BoanFcmService : FirebaseMessagingService() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var tokenManager: TokenManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d("FCM", "New FCM token: $token")
        scope.launch {
            try {
                val jwt = tokenManager.getToken()
                if (jwt != null) {
                    apiService.registerFcmToken(com.boancurator.app.data.model.FcmTokenRequest(token))
                    Log.d("FCM", "FCM token registered to server")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Message from: ${message.from}")
        // 추후 알림 처리 구현
    }
}
