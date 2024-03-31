
# MinecraftCosmosInfra

https://medium.com/geekculture/serverless-aws-with-kotlin-gradle-and-cdk-d6bfe820b85

## Bootstrapping
This creates the necessary resources in the aws account to execute the deployment:

    cdk bootstrap

## Prod deployment

1. Verify the website url is pointing to the correct endpoint (https://fufgouqjz9.execute-api.us-west-1.amazonaws.com/prod)
2. Execute the following:
```
./gradlew clean build shadowJar
cdk synth
cdk deploy --app stack/cdk.out MinecraftCosmosStack-prod
```

## Test/beta deployment
1. Verify the website url is pointing to the correct endpoint (https://xww3ls66qh.execute-api.us-west-1.amazonaws.com/prod)
2. Use the deployment script `.\deploy_stack.bat` or execute the following:
```
./gradlew clean build shadowJar
cdk synth
cdk deploy --app stack/cdk.out MinecraftCosmosStack-beta
```
3. Optionally, copy the mod data to the newly created bucket

## Test/beta cleanup
When done testing, perform the following to save costs:
1. Empty the contents of the bucket: `mccosmos-static-site-beta`
2. Delete the stack using:
```
cdk destroy MinecraftCosmosStack-beta
```

## Troubleshooting
### Could not create output directory cdk.out (EEXIST: file already exists, mkdir '...\MinecraftCosmos\MinecraftCosmosInfra\cdk.out')

recreate the symlink with `mklink /D cdk.out stack\cdk.out`