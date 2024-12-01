package com.starry.myne.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

object ImageGenerator {
    private val HTTP_CLIENT = OkHttpClient().newBuilder().build()

    // 根据用户输入的文字生成图片
    @Throws(IOException::class)
    fun generateImageFromText(accessToken: String, prompt: String,resolution:String ): Long {
        val mediaType = "application/json".toMediaType()
        val map: Map<Any?, Any?> = mutableMapOf(
            "text" to prompt,  // 用户选中的文字
            "resolution" to resolution
        )

        val body = RequestBody.create(mediaType, JSONObject(map).toString())
        val request = Request.Builder()
            .url("https://aip.baidubce.com/rpc/2.0/wenxin/v1/basic/textToImage?access_token=$accessToken")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        val response = HTTP_CLIENT.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        Log.d("Response", "Response JSON: $responseBody")  // 打印响应内容

        // 解析返回的 JSON，获取生成的图片 URL 或 图片数据
        val jsonObject = JSONObject(responseBody)

        if (jsonObject.has("error_code")) {
            val errorCode = jsonObject.getInt("error_code")
            val errorMessage = jsonObject.getString("error_msg")
            throw IOException("API Error: $errorCode, $errorMessage")
        }

        val data = jsonObject.getJSONObject("data")
        return data.getLong("taskId") // 从 'data' 节点获取 taskId
    }

    @Throws(IOException::class)
    fun queryImageStatus(accessToken: String, taskId: Long): String {
        val mediaType = "application/json".toMediaType()
        val map: Map<Any?, Any?> = mutableMapOf(
            "taskId" to taskId
        )

        val body = RequestBody.create(mediaType, JSONObject(map).toString())
        val request = Request.Builder()
            .url("https://aip.baidubce.com/rpc/2.0/wenxin/v1/basic/getImg?access_token=$accessToken")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        val response = HTTP_CLIENT.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        Log.d("Response", "Response JSON: $responseBody")  // 打印响应内容

        // 解析返回的 JSON，获取生成的图片 URL 或 图片状态
        val jsonObject = JSONObject(responseBody)

        if (jsonObject.has("error_code")) {
            val errorCode = jsonObject.getInt("error_code")
            val errorMessage = jsonObject.getString("error_msg")
            throw IOException("API Error: $errorCode, $errorMessage")
        }

        // 检查图片是否生成完成
        val status = jsonObject.getJSONObject("data").getInt("status")
        return if (status == 1) {
            // 图片生成成功，返回图片 URL
            jsonObject.getJSONObject("data").getJSONArray("imgUrls")
                .getJSONObject(0).getString("image")
        } else {
            // 返回错误信息或等待状态
            "Image generation in progress or failed"
        }
    }

}