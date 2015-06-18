# dockerdeps
This is a project which created a dependency matrix based on the docker parent child dependencies, analyzes code commits and determines what needs to be built by looking up the dependency matrix.

## Design aspects

* All projects use build tools which support dependency management (maven, gradle or ivy)
* The versioning is done through build tools.
* Dockerfiles use the version specified in the pom.xml, ivy.xml.
* Parent versions can be defined in the Dockerfile or in pom.xml, ivy.xml if you some kind of docker maven/ivy plugins.
* dockerdeps will build in-memory dependency tree based on parent-child relationships.
* For each build, it will analyze what Dockerfiles have changed and what maven dependencies they are packaging.
* By traversing the docker parent/child dependency matrix, it will determine what all need to be rebuilt.
* Setup a farm of docker build nodes
* Kick off builds in parallel for all the docker images that need to rebuilt, in parallel, based on dependency order.
* Report failed/succeeded builds and send notifications (mail/sms)
