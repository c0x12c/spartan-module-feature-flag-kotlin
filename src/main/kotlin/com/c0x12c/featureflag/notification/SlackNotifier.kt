package com.c0x12c.featureflag.notification

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.NotifierError
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SlackApi {
  @POST(".")
  fun sendMessage(
    @Body payload: SlackPayload,
    @Header("X-Stratos-Api-Key") apiKey: String?,
    @Header("X-Stratos-Client-Id") clientId: String?
  ): retrofit2.Response<Unit>
}

class SlackNotifier(
  private val config: SlackNotifierConfig,
  private val api: SlackApi = createSlackApi(config) // Default implementation
) {

  private companion object {
    fun createSlackApi(config: SlackNotifierConfig): SlackApi {
      val retrofit = Retrofit.Builder()
        .baseUrl(config.webhookUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
      return retrofit.create(SlackApi::class.java)
    }
  }

  /**
   * Sends a Slack notification with details about the given feature flag.
   *
   * @param featureFlag The feature flag instance containing name, code, and status information.
   * @param changeStatus The status of the change for the feature flag.
   * @throws NotifierError If there is an error sending the notification, including HTTP request failures.
   */
  fun send(featureFlag: FeatureFlag, changeStatus: ChangeStatus) {
    if (changeStatus in config.excludedStatuses) {
      return
    }

    val message = buildMessage(featureFlag, changeStatus)
    val payload = SlackPayload(message)

    try {
      val response = api.sendMessage(
        payload = payload,
        apiKey = config.apiKey,
        clientId = config.clientId
      )

      if (!response.isSuccessful) {
        throw NotifierError("Unexpected response from Slack: ${response.code()}")
      }
    } catch (e: Exception) {
      throw NotifierError("Error sending Slack notification: ${e.message}", e)
    }
  }

  private fun buildMessage(featureFlag: FeatureFlag, changeStatus: ChangeStatus): String {
    return "Feature Flag[Code=`${featureFlag.code}`] has been ${changeStatus.name.lowercase()}"
  }
}
