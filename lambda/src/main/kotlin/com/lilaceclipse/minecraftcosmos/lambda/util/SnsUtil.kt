package com.lilaceclipse.minecraftcosmos.lambda.util

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import mu.KotlinLogging
import javax.inject.Inject

class SnsUtil @Inject constructor(
    private val snsClient: AmazonSNS,
    private val envVarProvider: EnvVarProvider
) {
    private val log = KotlinLogging.logger {}

    fun sendSmsAlert(message: String) {
        val request = PublishRequest(envVarProvider.smsAlertTopicArn, message)

        try {
            val result = snsClient.publish(request)
            log.info { "Message ${result.messageId} published to ${envVarProvider.smsAlertTopicArn}" }
        } catch (e: Exception) {
            log.info { "Exception caught while publishing message to ${envVarProvider.smsAlertTopicArn}" }
            e.printStackTrace()
        }
    }
}