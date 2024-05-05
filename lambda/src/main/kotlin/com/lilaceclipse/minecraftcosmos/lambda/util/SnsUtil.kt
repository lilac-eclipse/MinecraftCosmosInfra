package com.lilaceclipse.minecraftcosmos.lambda.util

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import javax.inject.Inject

class SnsUtil @Inject constructor(
    private val snsClient: AmazonSNS,
    private val envVarProvider: EnvVarProvider
) {

    fun sendSmsAlert(message: String) {
        val request = PublishRequest(envVarProvider.smsAlertTopicArn, message)

        try {
            val result = snsClient.publish(request)
            println("Message ${result.messageId} published to ${envVarProvider.smsAlertTopicArn}")
        } catch (e: Exception) {
            println("Exception caught while publishing message to ${envVarProvider.smsAlertTopicArn}")
            e.printStackTrace()
        }
    }
}