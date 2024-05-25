package com.lilaceclipse.cosmos.config

data class EnvironmentConfig(
    val dynamoDbTableName: String,
    val s3Bucket: String
)