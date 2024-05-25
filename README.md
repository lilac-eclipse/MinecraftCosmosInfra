
# MinecraftCosmosInfra

https://medium.com/geekculture/serverless-aws-with-kotlin-gradle-and-cdk-d6bfe820b85

## Bootstrapping
This creates the necessary resources in the aws account to execute the deployment:

    cdk bootstrap

## Prod deployment

1. Tag the commit to deploy with a new version number. Use format:

   `major.minor.fixrelease`
    - Major revision (new UI, lots of new features, conceptual change, etc.)
    - Minor revision (maybe a change to a search box, 1 feature added, collection of bug fixes)
    - Bug fix release

2. Execute the following:
   ```
   ./gradlew clean build shadowJar
   cdk synth
   cdk deploy --app cdk/cdk.out MinecraftCosmosStack-prod
   ```
## Test/beta deployment
Use the deployment script `.\deploy_stack.bat` or execute the following:
```
./gradlew clean build shadowJar
cdk synth
cdk deploy --app cdk/cdk.out MinecraftCosmosStack-beta
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

recreate the symlink with `mklink /D cdk.out cdk\cdk.out`

# MinecraftCosmosDocker

## Manually building the server dockerfile
If you would like to manually build the server dockerfile, use the following command:
```
docker build -t mc-cosmos .
```
You can then get into the shell (without automatically starting it), by using:
```
docker run -it --entrypoint /bin/bash mc-cosmos
```

## Publishing the image to the Beta ECR repo
Execute the following commands:
```
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 252475162445.dkr.ecr.us-west-2.amazonaws.com
docker build -t mc-cosmos .
docker tag mc-cosmos:latest 252475162445.dkr.ecr.us-west-2.amazonaws.com/mc-cosmos-repo-beta:latest
docker push 252475162445.dkr.ecr.us-west-2.amazonaws.com/mc-cosmos-repo-beta:latest
```

## Prod deployment

1. Tag the commit to deploy with a new version number. Use format:

   `major.minor.fixrelease`
   - Major revision (new UI, lots of new features, conceptual change, etc.)
   - Minor revision (maybe a change to a search box, 1 feature added, collection of bug fixes)
   - Bug fix release

2. Execute the following:
   ```
   aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 252475162445.dkr.ecr.us-west-2.amazonaws.com
   docker build -t mc-cosmos .
   docker tag mc-cosmos:latest 252475162445.dkr.ecr.us-west-2.amazonaws.com/mc-cosmos-repo-prod:latest
   docker push 252475162445.dkr.ecr.us-west-2.amazonaws.com/mc-cosmos-repo-prod:latest
   ```
