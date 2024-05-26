package com.lilaceclipse.cosmos.docker.util


import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.DescribeTasksRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import javax.inject.Inject

class EcsUtil @Inject constructor(
    private val amazonEC2: AmazonEC2,
    private val amazonECS: AmazonECS
) {
    private val log = KotlinLogging.logger {}

    fun getContainerPublicIp(): String {
        // Retrieve container metadata
        val metadataUrl = URL(System.getenv("ECS_CONTAINER_METADATA_URI_V4"))
        val containerMetadata = metadataUrl.readText()


        val taskArnRegex = Regex("\"com\\.amazonaws\\.ecs\\.task-arn\"\\s*:\\s*\"([^\"]+)\"")
        val taskArn = taskArnRegex.find(containerMetadata)?.groupValues?.getOrNull(1)

        val clusterName = taskArn?.split("/")?.getOrNull(1)
        val taskId = taskArn?.split("/")?.lastOrNull()

        log.info("Task ARN: $taskArn")
        log.info("Cluster Name: $clusterName")
        log.info("Task ID: $taskId")

        if (taskArn != null && clusterName != null) {
            // Describe the task to get the network interface details
            val describeTasksRequest = DescribeTasksRequest()
                .withCluster(clusterName)
                .withTasks(taskArn)
            val describeTasksResult = amazonECS.describeTasks(describeTasksRequest)

            val task = describeTasksResult.tasks?.firstOrNull()
            val attachmentDetails = task?.attachments?.flatMap { it.details }
            val networkInterfaceId = attachmentDetails?.find { it.name == "networkInterfaceId" }?.value

            if (networkInterfaceId != null) {
                // Describe the network interface to get the public IP address
                val describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest()
                    .withFilters(
                        Filter("network-interface-id").withValues(networkInterfaceId)
                    )
                val describeNetworkInterfacesResult = amazonEC2.describeNetworkInterfaces(describeNetworkInterfacesRequest)

                val publicIp = describeNetworkInterfacesResult.networkInterfaces?.firstOrNull()
                    ?.association
                    ?.publicIp

                return publicIp!!
            }
        }
        throw RuntimeException("Failed to get IP!!!")
    }
}