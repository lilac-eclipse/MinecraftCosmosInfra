# Introduction

This is the package containing everything for minecraft cosmos.

# Deploying code
To deploy to beta/prod, use the root level gradle tasks


# Infrastructure
Based on the following guide:
https://medium.com/geekculture/serverless-aws-with-kotlin-gradle-and-cdk-d6bfe820b85

## CDK Setup

### Bootstrapping
This creates the necessary resources in the aws account to execute the deployment:

    cdk bootstrap

### Troubleshooting
#### Could not create output directory cdk.out (EEXIST: file already exists, mkdir '...\MinecraftCosmos\MinecraftCosmosInfra\cdk.out')

recreate the symlink with `mklink /D cdk.out cdk\cdk.out`

# Docker

## Manually building the server dockerfile
If you would like to manually build the server dockerfile, use the following command:
```
docker build -t mc-cosmos .
```
You can then get into the shell (without automatically starting it), by using:
```
docker run -it --entrypoint /bin/bash mc-cosmos
```

# Client

## Deployment guide
1. Update the version in the common package
2. Build client using gradle task generateClientJar
3. Upload jar to S3
4. Invalidate the cloudfront cache for the jar
5. Deploy lambda