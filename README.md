# gce2retrofit

Generates [retrofit](http://square.github.io/retrofit/) interfaces and related models from a
[Google Cloud Endpoint (GCE)](https://cloud.google.com/endpoints/) discovery file.

## Gradle plugin

### Publish the gradle plugin to a local repo
    cd gce2retrofit
    ../gradlew publish
    cd ..

### Run the sample Java project
    ./gradlew sample-java:run

### Install the sample Android app
    ./gradlew sample-android:installDebug

Right now Android Studio does not recognize the generated source files. To work around that, add
this to the `.iml` file of your Android project:

    <sourceFolder url="file://$MODULE_DIR$/build/generated/source/gce2retrofit" isTestSource="false" generated="true" />

You need to do that every time you sync your project with gradle files.

## Command line

Alternatively, you can compile a jar file and generate the retrofit classes manually.

### Compile the gce2retrofit jar file
    cd gce2retrofit
    ./gradlew jar
    cd ..
    
### Generate retrofit classes for the sample Java project
    java -jar gce2retrofit/build/libs/gce2retrofit.jar \
      sample-java/gce2retrofit/helloworld/discovery.json \
      sample-java/src/main/java \
      --methods sync \
      --classmap sample-java/gce2retrofit/helloworld/classmap.tsv