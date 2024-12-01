package com.starry.myne.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

// SummaryGenerator is an object responsible for handling API requests to generate summaries
object SummaryGenerator {
    // Create an OkHttp client for making HTTP requests
    private val client = OkHttpClient()

    // Create a single-threaded executor to run the background network request
    private val executor = Executors.newSingleThreadExecutor()

    // This function generates a summary based on the provided content
    // It accepts content (text), accessToken, maxSummaryLen (maximum summary length),
    // and a callback function onSummaryGenerated which returns the summary.
    fun generateSummary(
        content: String,
        accessToken: String,
        maxSummaryLen: Int,
        onSummaryGenerated: (String?) -> Unit
    ) {
        // Execute the network request in a background thread using the executor
        executor.execute {
            // Construct the API request URL
            val url = "https://aip.baidubce.com/rpc/2.0/nlp/v1/news_summary?access_token=$accessToken"

            // Construct the JSON body for the POST request, containing the content and max summary length
            val json = """
                {
                    "content": "$content",
                    "max_summary_len": $maxSummaryLen
                }
            """.trimIndent()

            // Create the request body with the JSON content
            val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

            // Build the request object with the URL, HTTP method, and body
            val request = Request.Builder()
                .url(url)
                .post(body)// Send a POST request
                .build()

            try {
                // Execute the request and get the response
                val response = client.newCall(request).execute()

                // If the response is successful, parse the summary from the response
                if (response.isSuccessful) {
                    // 解析返回的 JSON 数据
                    val responseBody = response.body?.string()// Get the response body as a string
                    val summary = parseSummaryFromResponse(responseBody)// Parse the summary
                    // Call the callback function with the generated summary
                    onSummaryGenerated(summary)
                } else {
                    // If the request was unsuccessful, log the error and return null
                    Log.e("API Request", "Failed to generate summary: ${response.message}")
                    onSummaryGenerated(null)
                }
            } catch (e: IOException) {
                // If an exception occurs during the request, log the error and return null
                Log.e("API Request", "Error making API request", e)
                onSummaryGenerated(null)
            }
        }
    }

    // This function parses the API response to extract the summary
    // It assumes that the response is a JSON object with a field called "summary"
    private fun parseSummaryFromResponse(response: String?): String? {
        if (response.isNullOrEmpty()) return null// Return null if the response is empty or null
        try {
            // Parse the JSON response and extract the summary
            val jsonObject = JSONObject(response)
            return jsonObject.optString("summary", null)// Get the "summary" field from the JSON
        } catch (e: Exception) {
            // If there's an error parsing the response, log it and return null
            Log.e("API Response", "Error parsing response", e)
            return null
        }
    }
}
