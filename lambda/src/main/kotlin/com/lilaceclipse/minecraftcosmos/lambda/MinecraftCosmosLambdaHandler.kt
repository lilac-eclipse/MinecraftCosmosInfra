package com.lilaceclipse.minecraftcosmos.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.lilaceclipse.minecraftcosmos.lambda.dagger.DaggerCosmosComponent


/**
 * See: https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html
 */
class MinecraftCosmosLambdaHandler: RequestHandler<Map<String, Any>, APIGatewayProxyResponseEvent> {
    private val cosmosRequestRouter = DaggerCosmosComponent.create().provideCosmosRequestRouter()

    override fun handleRequest(input: Map<String, Any>, context: Context): APIGatewayProxyResponseEvent {
        return cosmosRequestRouter.handleRequest(input)
    }

}