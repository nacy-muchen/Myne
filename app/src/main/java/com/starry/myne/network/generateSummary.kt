package com.starry.myne.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

// SummaryGenerator is an object responsible for handling API requests to generate summaries
object SummaryGenerator {
    private val client = OkHttpClient()

    // Your API key and the new API base URL
    private const val API_KEY = "sk-tX6I6T7LhnJ7ldcr0jn66HqxpwNZWTymSID4b6d1E1Hz8PM8"
    private const val BASE_URL = "https://api.chatanywhere.tech/v1/chat/completions"

    /**
     * Generates a summary using the new API at chatanywhere.tech.
     *
     * @param content The text content to be summarized.
     * @param onSummaryGenerated Callback that returns the generated summary.
     */
    /**
     * Generates a summary using the new API at chatanywhere.tech.
     *
     * @param content The text content to be summarized.
     * @param onSummaryGenerated Callback that returns the generated summary.
     */
    fun generateSummary(
        content: String,
        onSummaryGenerated: (String?) -> Unit
    ) {
        // Construct the URL for the API request
        val url = BASE_URL // Assuming this is the endpoint for the summarization API

        // Create a JSON body for the POST request with the required format
        val json = JSONObject().apply {
            put("model", "gpt-3.5-turbo") // Assuming you're using this model for summaries
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a helpful assistant.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", content) // The text to summarize
                })
            })
        }

        // Set the media type and create the request body
        val mediaType = "application/json".toMediaType()
        val body = RequestBody.create(mediaType, json.toString())

        // Build the request
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        // Make the API request in a background thread
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                // Parse the JSON response
                val responseBody = response.body?.string()
                val summary = parseSummaryFromResponse(responseBody)
                // Pass the summary to the callback
                onSummaryGenerated(summary)
            } else {
                val errorMessage = response.body?.string() ?: "Unknown error"
                Log.e("SummaryGenerator", "API Request failed: $errorMessage")
                throw IOException("API Request failed: $errorMessage")
            }
        }

    }


    /**
     * Parses the response from the API to extract the summary.
     *
     * @param response The raw response string from the API.
     * @return The summary string if successful, otherwise null.
     */
    private fun parseSummaryFromResponse(response: String?): String? {
        if (response.isNullOrEmpty()) return null
        try {
            val jsonObject = JSONObject(response)
            val choicesArray = jsonObject.optJSONArray("choices")
            if (choicesArray != null && choicesArray.length() > 0) {
                val firstChoice = choicesArray.getJSONObject(0)
                val message = firstChoice.optJSONObject("message")
                return message?.optString("content", null)
            }
        } catch (e: Exception) {
            Log.e("API Response", "Error parsing response", e)

        }
        return null
    }
}
