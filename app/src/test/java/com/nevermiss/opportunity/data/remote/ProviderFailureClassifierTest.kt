package com.nevermiss.opportunity.data.remote

import com.nevermiss.opportunity.domain.Indicator
import java.io.IOException
import java.util.concurrent.CancellationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ProviderFailureClassifierTest {
    @Test
    fun `cancellation is rethrown`() {
        val cancellation = CancellationException("cancelled")

        val thrown = assertThrows(CancellationException::class.java) {
            ProviderFailureClassifier.failureFor(Indicator.KOSPI, cancellation)
        }

        assertSame(cancellation, thrown)
    }

    @Test
    fun `network IO is retryable`() {
        val failure = ProviderFailureClassifier.failureFor(Indicator.KOSPI, IOException("offline"))

        assertTrue(failure.retryable)
    }

    @Test
    fun `transient HTTP status is retryable`() {
        val failure = ProviderFailureClassifier.failureFor(Indicator.KOSPI, httpException(503))

        assertTrue(failure.retryable)
    }

    @Test
    fun `permanent HTTP status is not retryable`() {
        val failure = ProviderFailureClassifier.failureFor(Indicator.KOSPI, httpException(404))

        assertFalse(failure.retryable)
    }

    @Test
    fun `parse and schema failures are not retryable`() {
        val failure = ProviderFailureClassifier.failureFor(Indicator.KOSPI, IllegalArgumentException("bad payload"))

        assertFalse(failure.retryable)
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody()))
}
