package com.dima.notesscanner.utils

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk
import com.yandex.authsdk.YandexAuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YandexAuthManager(context: Context) {

    private val sdk = YandexAuthSdk.create(YandexAuthOptions(context))

    fun getLoginContract(): ActivityResultContract<YandexAuthLoginOptions, YandexAuthResult> {
        return sdk.contract
    }

    fun createLoginOptions(): YandexAuthLoginOptions = YandexAuthLoginOptions()

    suspend fun getJwtToken(token: YandexAuthToken): String = withContext(Dispatchers.IO) {
        sdk.getJwt(token)
    }
}