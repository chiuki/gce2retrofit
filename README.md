# gce2retrofit

Generates [retrofit](http://square.github.io/retrofit/) interfaces and related models from a
[Google Cloud Endpoint (GCE)](https://cloud.google.com/endpoints/) discovery file.

## Publish the gradle plugin to a local repo
    cd core
    ../gradlew publish
    cd ..

## Run the sample project
    cd sample-java
    ../gradlew run
    cd ..

Alternatively, you can compile a jar file and generate the retrofit classes manually.

## Compile the core jar file
    cd core
    ./gradlew jar
    cd ..
    
## Generate retrofit classes for the sample project
    java -jar core/build/libs/gce2retrofit.jar \
      sample-java/gce2retrofit/helloworld/discovery.json \
      sample-java/src/main/java \
      --methods sync \
      --classmap sample-java/gce2retrofit/helloworld/classmap.tsv