package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.data.ProviderResult
import com.nevermiss.opportunity.domain.Indicator
import java.io.IOException
import java.util.concurrent.CancellationException
import retrofit2.HttpException

internal object ProviderFailureClassifier {
    fun failureFor(indicator: Indicator, throwable: Throwable): ProviderResult.Failure =
        when (throwable) {
            is CancellationException -> throw throwable
            is HttpException -> ProviderResult.Failure(
                indicator = indicator,
                reason = "http-${throwable.code()}",
                retryable = throwable.code() == 429 || throwable.code() in 500..599,
            )
            is IOException -> ProviderResult.Failure(
                indicator = indicator,
                reason = throwable.javaClass.simpleName,
                retryable = true,
            )
            else -> ProviderResult.Failure(
                indicator = indicator,
                reason = throwable.javaClass.simpleName.ifBlank { "provider-error" },
                retryable = false,
            )
        }
}
