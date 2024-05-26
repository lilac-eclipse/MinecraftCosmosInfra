package com.lilaceclipse.cosmos.lambda.util

class EnvVarProvider {
    val stage: String = System.getenv("STAGE")
    val smsAlertTopicArn: String = System.getenv("STATUS_ALERT_TOPIC_ARN")
    val clusterArn: String = System.getenv("CLUSTER_ARN")
    val taskDefinitionArn: String = System.getenv("TASK_DEFINITION_ARN")
    val securityGroupId: String = System.getenv("SECURITY_GROUP_ID")
    val subnetId: String = System.getenv("SUBNET_ID")
    val serverTableName: String = System.getenv("SERVER_TABLE_NAME")
}