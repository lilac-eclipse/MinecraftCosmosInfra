package com.lilaceclipse.minecraftcosmos.lambda.dagger

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.lilaceclipse.minecraftcosmos.lambda.util.EnvVarProvider
import dagger.Module
import dagger.Provides

@Module
class CosmosModule {
    @Provides
    fun provideEnvVarProvider(): EnvVarProvider {
        return EnvVarProvider()
    }

    @Provides
    fun provideAmazonSNS(): AmazonSNS {
        return AmazonSNSClientBuilder.defaultClient()
    }

    @Provides
    fun provideAmazonEC2():  AmazonEC2 {
        return AmazonEC2ClientBuilder.defaultClient()
    }

    @Provides
    fun provideAmazonECS(): AmazonECS {
        return AmazonECSClientBuilder.defaultClient()
    }
}
