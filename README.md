# gce2retrofit

Generates [Retrofit](http://square.github.io/retrofit/) interfaces and related models from
[Google Cloud Endpoint (GCE)](https://cloud.google.com/endpoints/) discovery files.

## Usage

### Configuaration files

Put the configuration files for each GCE server in a directory under `src/main/gce2retrofit`.

`discovery.json` (required)

The discovery doc from GCE.
e.g. https://2-dot-test-pont.appspot.com/_ah/api/discovery/v1/apis/helloworld/v1/rest

`methods.csv` (optional)

Valid values are `sync`, `async` and `reactive`. If omitted, both synchronous and asynchronous
interfaces will be generated.

`classmap.tsv` (optional)

Map fields with the specified names to the specified types.

Code will be generated in `build/generated/source/gce2retrofit/`
 
See [`sample-java/src/main/gce2retrofit/helloworld`](sample-java/src/main/gce2retrofit/helloworld)
for an example.

### Gradle plugin

Apply the plugin in your `build.gradle`:

    buildscript {
      repositories {
        jcenter()
        maven {
          url 'http://oss.sonatype.org/content/repositories/snapshots/'
        }
      }
      dependencies {
        classpath 'com.sqisland:gce2retrofit:1.0.0-SNAPSHOT'
      }
    }

    apply plugin: 'com.sqisland.gce2retrofit'

## Contributing

Please see [CONTRIBUTING.md](CONTRIBUTING.md).
