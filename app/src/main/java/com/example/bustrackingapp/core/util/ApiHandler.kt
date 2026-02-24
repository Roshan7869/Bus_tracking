package com.example.bustrackingapp.core.util

import com.example.bustrackingapp.BuildConfig
import com.example.bustrackingapp.core.data.remote.dto.ApiResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

abstract class ApiHandler {
    private val logger = LoggerUtil(c = "ApiHandler")

    fun <T> makeRequest(
        apiCall: suspend () -> ApiResponse<T>,
        onSuccess: (suspend (T?) -> Unit)? = null,
    ): Flow<Resource<T>> = flow {
        try {
            emit(Resource.Loading())
            val apiResponse = apiCall()

            // Only log full response body in DEBUG builds
            if (BuildConfig.DEBUG) {
                logger.info("res : $apiResponse")
            }

            if (onSuccess != null) {
                onSuccess(apiResponse.data)
            }
            emit(Resource.Success(apiResponse.data, apiResponse.message))

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = runCatching {
                Gson().fromJson(errorBody, ApiResponse::class.java)
            }.getOrNull()

            if (BuildConfig.DEBUG) {
                logger.error("HttpException $errorResponse")
            }

            val message = errorResponse?.message ?: e.message ?: e.toString()
            emit(Resource.Error(message))

        } catch (e: IOException) {
            // Network-level failure (no connectivity, DNS, timeout)
            if (BuildConfig.DEBUG) {
                logger.error("IOException ${e.message}")
            }
            emit(Resource.Error("Network error — check your connection"))

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                logger.error("Exception $e")
            }
            emit(Resource.Error(e.message ?: e.toString()))
        }
    }
}