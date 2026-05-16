package com.dima.notesscanner.utils

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk
import com.yandex.authsdk.YandexAuthToken

class YandexAuthManager(context: Context) {

    private val sdk = YandexAuthSdk.create(YandexAuthOptions(context))

    fun getLoginContract(): ActivityResultContract<YandexAuthLoginOptions, YandexAuthResult> {
        return sdk.contract
    }

    fun createLoginOptions(): YandexAuthLoginOptions = YandexAuthLoginOptions()

    fun handleAuthResult(result: YandexAuthResult): YandexAuthToken? {
        return when (result) {
            is YandexAuthResult.Success -> result.token
            is YandexAuthResult.Failure -> {
                result.exception.printStackTrace()
                null
            }
            YandexAuthResult.Cancelled -> null
        }
    }

    fun getJwtToken(token: YandexAuthToken): String = sdk.getJwt(token)
}