package com.lilaceclipse.minecraftcosmos.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler


/**
 * See: https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html
 */
class MinecraftCosmosLambdaHandler: RequestHandler<Map<String, String>, String> {

    override fun handleRequest(input: Map<String, String>?, context: Context?): String {
        return "hello there!"
    }
}