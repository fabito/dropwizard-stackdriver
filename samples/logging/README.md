

```bash

PROJECT_ID=my-gcp-project

docker build -t gcr.io/${PROJECT_ID}/dropwizard-stackdriver/samples/logging:0.1.0  --build-arg ARTIFACT_ID=logging --build-arg VERSION=0.1.0 .

gcloud docker push gcr.io/${PROJECT_ID}/dropwizard-stackdriver/samples/logging:0.1.0

kubectl run sample-logging --image=gcr.io/${PROJECT_ID}/dropwizard-stackdriver/samples/logging:0.1.0  --env="LOG_APPENDER_TYPE=GKEConsole"


kubectl get pods

kubectl port-forward <pod-name> 8080:8080

curl localhost:8080/api/echo?msg=Hi

```


### Running locally


```bash

export LOG_APPENDER_TYPE=GKEConsole && ./gradlew :samples:logging:runShadow

curl localhost:8080/api/echo?msg=Hi

```