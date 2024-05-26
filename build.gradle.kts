import java.io.ByteArrayOutputStream

tasks.register("buildAll") {
    group = "cosmos"
    description = "Builds all modules"

    dependsOn(":cdk:build", ":common:build", ":lambda:build",  ":docker:build", ":client:build")
}

tasks.register("buildAndDeployCdkStackBeta") {
    group = "cosmos"
    description = "Synthesizes and deploys the CDK stack"

    dependsOn(":lambda:build", ":lambda:shadowJar", ":cdk:build")

    doLast {
        // Synthesize the CDK stack
        exec {
            commandLine("cdk.cmd", "synth")
        }

        // Deploy the CDK stack
        exec {
            commandLine("cdk.cmd", "deploy", "--app", "cdk.out", "MinecraftCosmosStack-beta")
        }
    }
}

tasks.register("buildAndPushDockerImageBeta") {
    group = "cosmos"
    description = "Builds and pushes the Docker image to Amazon ECR"

    dependsOn(":docker:build")

    doLast {
        val region = "us-west-2"
        val registryUrl = "252475162445.dkr.ecr.us-west-2.amazonaws.com"
        val imageName = "mc-cosmos"
        val imageTag = "latest"
        val repositoryName = "mc-cosmos-repo-beta"

        exec {
            commandLine("docker", "build", "-t", "$imageName:$imageTag", "-f", "docker/Dockerfile", ".")
        }

        // Get the ECR login password
        val loginPasswordStream = ByteArrayOutputStream()
        exec {
            commandLine("aws", "ecr", "get-login-password", "--region", region)
            standardOutput = loginPasswordStream
        }
        val loginPassword = loginPasswordStream.toString().trim()

        // Log in to the ECR registry
        exec {
            commandLine("docker", "login", "--username", "AWS", "--password-stdin", registryUrl)
            standardInput = loginPassword.byteInputStream()
        }

        // Tag the Docker image
        exec {
            commandLine("docker", "tag", "$imageName:$imageTag", "$registryUrl/$repositoryName:$imageTag")
        }

        // Push the Docker image to ECR
        exec {
            commandLine("docker", "push", "$registryUrl/$repositoryName:$imageTag")
        }
    }
}