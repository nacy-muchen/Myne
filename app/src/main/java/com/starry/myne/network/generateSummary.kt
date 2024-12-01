package com.starry.myne.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

object SummaryGenerator {
    private val client = OkHttpClient()

    // 创建一个线程池用于执行后台任务
    private val executor = Executors.newSingleThreadExecutor()

    // 生成摘要的 API 请求
    fun generateSummary(
        content: String,
        accessToken: String,
        maxSummaryLen: Int,
        onSummaryGenerated: (String?) -> Unit
    ) {
        // 使用线程池执行网络请求
        executor.execute {
            // 创建请求的 URL
            val url = "https://aip.baidubce.com/rpc/2.0/nlp/v1/news_summary?access_token=$accessToken"

            // 创建请求体
            val json = """
                {
                    "content": "$content",
                    "max_summary_len": $maxSummaryLen
                }
            """.trimIndent()

            // 创建请求体
            val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

            // 创建请求对象
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    // 解析返回的 JSON 数据
                    val responseBody = response.body?.string()
                    val summary = parseSummaryFromResponse(responseBody)
                    // 回调返回的摘要
                    onSummaryGenerated(summary)
                } else {
                    Log.e("API Request", "Failed to generate summary: ${response.message}")
                    onSummaryGenerated(null)
                }
            } catch (e: IOException) {
                Log.e("API Request", "Error making API request", e)
                onSummaryGenerated(null)
            }
        }
    }

    // 解析 API 返回的摘要数据
    private fun parseSummaryFromResponse(response: String?): String? {
        if (response.isNullOrEmpty()) return null
        // 假设返回的 JSON 格式类似：{"summary": "这是生成的摘要"}
        try {
            val jsonObject = JSONObject(response)
            return jsonObject.optString("summary", null)
        } catch (e: Exception) {
            Log.e("API Response", "Error parsing response", e)
            return null
        }
    }
}
