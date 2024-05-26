package com.lilaceclipse.cosmos.docker.config

data class EnvironmentConfig(
    val dynamoDbTableName: String,
    val s3Bucket: String
)