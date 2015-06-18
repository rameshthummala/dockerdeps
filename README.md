# dockerdeps
This is a project which created a dependency matrix based on:
* Build dependencies (from pom.xml/ivy.xml/build.gradle)
* Docker parent child dependencies
* Code commit change lists

generates a dependency matrix and determines what needs to be built and deployed by looking up the dependency matrix.

This is a highly pluggable framework with plugins from different SCMs(SVN, Git, Perforce, mercurial), plugins for build tools (Maven, Gradle, ivy) and deployment orchestration tools (Rundeck, puppet, chef, ansible).

## Design aspects

Build time
-----------
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

Deployment time:
----------------
* Reads stack descriptions based on SDL (stack description language)
* Determine what containers need to redeployed based on rebuilt containers in "Build time" section
* Generates rundeck/puppet/chef/ansible runlists to deploy the new container image versions

