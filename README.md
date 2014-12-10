# gce2retrofit

Generates [retrofit](http://square.github.io/retrofit/) interfaces and related models from a
[Google Cloud Endpoint (GCE)](https://cloud.google.com/endpoints/) discovery file.

## Compile the core jar file
    ./gradlew core:jar
    
## Generate retrofit classes for the sample project
    java -jar core/build/libs/gce2retrofit.jar \
      sample-java/config/helloworld.json \
      sample-java/src/main/java \
      --methods sync \
      --classmap sample-java/config/classmap.tsv

## Run the sample project
    ./gradlew sample-java:run
