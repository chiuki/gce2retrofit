# Contributing

## Test-driven development

The easiest way to add new functionality to the plugin is to write a new test case.

  1. Add a new folder to `test/resources` with `discovery.json` and the expected output.
  2. Add a test case to [`gce2retrofit/src/test/java/com/sqisland/gce2retrofit/GeneratorTest.java`](gce2retrofit/src/test/java/com/sqisland/gce2retrofit/GeneratorTest.java)
  3. `./gradlew gce2retrofit:test`


## Gradle plugin

To compile the plugin locally:

    # Publish the gradle plugin to a local repo
    ./gradlew gce2retrofit:uploadArchives

    # Run the sample Java project
    ./gradlew sample-java:run

    # Install the sample Android app
    ./gradlew sample-android:installDebug

If you are not seeing your changes after modifying the `gce2retrofit` code,
`./gradlew clean` and try again.


## Command line

Alternatively, you can compile a jar file and generate the retrofit classes manually.

    # Compile the gce2retrofit jar file
    ./gradlew gce2retrofit:jar
    
    # Generate retrofit classes for the sample Java project
    java -jar gce2retrofit/build/libs/gce2retrofit.jar \
      sample-java/src/main/gce2retrofit/helloworld/discovery.json \
      sample-java/src/main/java \
      --methods sync \
      --classmap sample-java/src/main/gce2retrofit/helloworld/classmap.tsv
