# gce2retrofit

Generates [Retrofit](http://square.github.io/retrofit/) interfaces and related models from a
[Google Cloud Endpoint (GCE)](https://cloud.google.com/endpoints/) discovery file.

## Gradle plugin

### Publish the gradle plugin to a local repo
    ./gradlew gce2retrofit:uploadArchives

### Run the sample Java project
    ./gradlew sample-java:run

### Install the sample Android app
    ./gradlew sample-android:installDebug

## Command line

Alternatively, you can compile a jar file and generate the retrofit classes manually.

### Compile the gce2retrofit jar file
    ./gradlew gce2retrofit:jar
    
### Generate retrofit classes for the sample Java project
    java -jar gce2retrofit/build/libs/gce2retrofit.jar \
      sample-java/src/main/gce2retrofit/helloworld/discovery.json \
      sample-java/src/main/java \
      --methods sync \
      --classmap sample-java/src/main/gce2retrofit/helloworld/classmap.tsv
