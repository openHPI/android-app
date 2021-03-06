package de.xikolo.testing.instrumented.mocking

import android.content.Context
import de.xikolo.BuildConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * An Interceptor which mocks request responses.
 * It gets a mocked response object from the {@link MockingData} class and constructs a response.
 * If an exception occurs while handling, the interceptor will stop intercepting by forwarding the request to the next object in the interception chain.
 */
class MockingInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            val mockedResponse = MockingData.getResponse(context, chain.request())!!
            val responseBody = mockedResponse.responseString
                .toByteArray()
                .toResponseBody(
                    mockedResponse.contentType.toMediaTypeOrNull()
                )
            Response.Builder()
                .code(mockedResponse.statusCode)
                .message(mockedResponse.responseString)
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .body(responseBody)
                .addHeader("content-type", mockedResponse.contentType)
                .addHeader("content-length", responseBody.contentLength().toString())
                .build()
        } catch (e: Exception) {
            // make a regular request
            chain.proceed(chain.request())
        }
    }

}
