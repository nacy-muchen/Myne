package com.starry.myne.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

object ImageGenerator {
    private val HTTP_CLIENT = OkHttpClient().newBuilder().build()

    /**
     * Generates an image based on the user's input text.
     * This function sends a POST request to the image generation API with the given text prompt and resolution.
     *
     * @param accessToken The access token required for the API request.
     * @param prompt The text prompt provided by the user to generate the image.
     * @param resolution The resolution for the generated image.
     * @return taskId The unique task ID assigned to the image generation request.
     * @throws IOException If an error occurs while making the network request or parsing the response.
     */

    @Throws(IOException::class)
    fun generateImageFromText(accessToken: String, prompt: String,resolution:String ): Long {
        val mediaType = "application/json".toMediaType()
        val map: Map<Any?, Any?> = mutableMapOf(
            "text" to prompt,
            "resolution" to resolution
        )

        // Create the request body with the JSON object
        val body = RequestBody.create(mediaType, JSONObject(map).toString())

        // Build the POST request to the API endpoint
        val request = Request.Builder()
            .url("https://aip.baidubce.com/rpc/2.0/wenxin/v1/basic/textToImage?access_token=$accessToken")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        // Make the request and get the response
        val response = HTTP_CLIENT.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        Log.d("Response", "Response JSON: $responseBody")  // 打印响应内容

        // Parse the response to extract the taskId
        val jsonObject = JSONObject(responseBody)

        // Check if there's an error in the API response
        if (jsonObject.has("error_code")) {
            val errorCode = jsonObject.getInt("error_code")
            val errorMessage = jsonObject.getString("error_msg")
            throw IOException("API Error: $errorCode, $errorMessage")
        }

        // Extract and return the taskId from the "data" section of the response
        val data = jsonObject.getJSONObject("data")
        return data.getLong("taskId")  // Return the taskId for tracking the image generation process
    }

    /**
     * Queries the status of the image generation process using the task ID.
     * This function checks if the image has been generated successfully.
     *
     * @param accessToken The access token required for the API request.
     * @param taskId The task ID of the image generation request.
     * @return The URL of the generated image if successful, or a status message.
     * @throws IOException If an error occurs while making the network request or parsing the response.
     */
    @Throws(IOException::class)
    fun queryImageStatus(accessToken: String, taskId: Long): String {
        val mediaType = "application/json".toMediaType()
        val map: Map<Any?, Any?> = mutableMapOf(
            "taskId" to taskId
        )

        val body = RequestBody.create(mediaType, JSONObject(map).toString())

        // Build the POST request to check the image status
        val request = Request.Builder()
            .url("https://aip.baidubce.com/rpc/2.0/wenxin/v1/basic/getImg?access_token=$accessToken")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        // Make the request and get the response
        val response = HTTP_CLIENT.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        Log.d("Response", "Response JSON: $responseBody")  // 打印响应内容

        // Parse the response to check the image status
        val jsonObject = JSONObject(responseBody)

        // Check if there's an error in the API response
        if (jsonObject.has("error_code")) {
            val errorCode = jsonObject.getInt("error_code")
            val errorMessage = jsonObject.getString("error_msg")
            throw IOException("API Error: $errorCode, $errorMessage")
        }

        // Check the status of the image generation
        val status = jsonObject.getJSONObject("data").getInt("status")
        return if (status == 1) {
            // If the status is 1, it means the image generation is complete, return the image URL
            jsonObject.getJSONObject("data").getJSONArray("imgUrls")
                .getJSONObject(0).getString("image")
        } else {
            // If the status is not 1, return an error message or indicate that the image is still in progress
            "Image generation in progress or failed"
        }
    }

}