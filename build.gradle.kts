import java.io.ByteArrayOutputStream
import javax.swing.JOptionPane

// Define common variables
val region = "us-west-2"
val registryUrl = "252475162445.dkr.ecr.us-west-2.amazonaws.com"
val imageName = "mc-cosmos"

tasks.register("buildAll") {
    group = "cosmos"
    description = "Builds all modules"

    dependsOn(":cdk:build", ":common:build", ":lambda:build", ":docker:build", ":client:build")
}

tasks.register("deployAllProd") {
    group = "cosmos"
    description = "Builds all modules and pushes Docker image and CDK stack to production"

    dependsOn(":buildAll")

    doLast {
        val confirmProd = JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to deploy to production?",
            "Confirm Production Deployment",
            JOptionPane.YES_NO_OPTION
        )

        if (confirmProd == JOptionPane.YES_OPTION) {
            val imageTag = "latest"
            val repositoryName = "mc-cosmos-repo-prod"
            val stackName = "MinecraftCosmosStack-prod"

            buildAndPushDockerImage(imageTag, repositoryName)
            synthesizeAndDeployCdkStack(stackName)
        } else {
            println("Production deployment canceled.")
        }
    }
}

tasks.register("deployCdkBeta") {
    group = "cosmos"
    description = "Synthesizes and deploys the CDK stack"

    dependsOn(":lambda:build", ":lambda:shadowJar", ":cdk:build")

    doLast {
        val stackName = "MinecraftCosmosStack-beta"
        synthesizeAndDeployCdkStack(stackName)
    }
}

tasks.register("deployDockerBeta") {
    group = "cosmos"
    description = "Builds and pushes the Docker image to Amazon ECR"

    dependsOn(":docker:build")

    doLast {
        val imageTag = "latest"
        val repositoryName = "mc-cosmos-repo-beta"
        buildAndPushDockerImage(imageTag, repositoryName)
    }
}

tasks.register("generateClientJar") {
    group = "cosmos"
    description = "Builds and pushes the Docker image to Amazon ECR"

    dependsOn(":client:shadowJar")

    doLast {
        // Open the client/build/libs directory in the file explorer
        val outputDir = project.file("client/build/libs")
        openFileExplorer(outputDir)
    }
}

// Function to get ECR login password
fun getEcrLoginPassword(): String {
    val loginPasswordStream = ByteArrayOutputStream()
    exec {
        commandLine("aws", "ecr", "get-login-password", "--region", region)
        standardOutput = loginPasswordStream
    }
    return loginPasswordStream.toString().trim()
}

// Function to build and push Docker image
fun buildAndPushDockerImage(imageTag: String, repositoryName: String) {
    exec {
        commandLine("docker", "build", "-t", "$imageName:$imageTag", "-f", "docker/Dockerfile", ".")
    }

    val loginPassword = getEcrLoginPassword()

    exec {
        commandLine("docker", "login", "--username", "AWS", "--password-stdin", registryUrl)
        standardInput = loginPassword.byteInputStream()
    }

    exec {
        commandLine("docker", "tag", "$imageName:$imageTag", "$registryUrl/$repositoryName:$imageTag")
    }

    exec {
        commandLine("docker", "push", "$registryUrl/$repositoryName:$imageTag")
    }
}

// Function to synthesize and deploy CDK stack
fun synthesizeAndDeployCdkStack(stackName: String) {
    exec {
        commandLine("cdk.cmd", "synth")
    }

    exec {
        commandLine("cdk.cmd", "deploy", "--app", "cdk.out", stackName)
    }
}

// Function to open a directory in the file explorer
fun openFileExplorer(directory: File) {
    val os = System.getProperty("os.name").toLowerCase()
    when {
        os.contains("win") -> {
            Runtime.getRuntime().exec("explorer.exe ${directory.absolutePath}")
        }
        os.contains("mac") -> {
            Runtime.getRuntime().exec("open ${directory.absolutePath}")
        }
        else -> {
            // Assume Linux or Unix-based system
            Runtime.getRuntime().exec("xdg-open ${directory.absolutePath}")
        }
    }
}
