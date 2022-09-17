
# MinecraftCosmosInfra

https://medium.com/geekculture/serverless-aws-with-kotlin-gradle-and-cdk-d6bfe820b85

## Build + deploy process
Note: Ignore the error `ENOENT: no such file or directory, open 'cdk.out\manifest.json'`

    ./gradlew clean build shadowJar
    cdk synth
    cdk deploy --app stack/cdk.out

## Bootstraping

    cdk bootstrap --app stack/cdk.out
