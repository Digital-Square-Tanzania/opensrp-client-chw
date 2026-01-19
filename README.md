 
[![Build Status](https://travis-ci.org/OpenSRP/opensrp-client-chw.svg?branch=master)](https://travis-ci.org/OpenSRP/opensrp-client-chw) [![Coverage Status](https://coveralls.io/repos/github/OpenSRP/opensrp-client-chw/badge.svg?branch=master)](https://coveralls.io/github/OpenSRP/opensrp-client-chw?branch=master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/f68511a1ac164d58a3a48c1926c2326a)](https://www.codacy.com/app/OpenSRP/opensrp-client-chw?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=OpenSRP/opensrp-client-chw&amp;utm_campaign=Badge_Grade)
 
## OpenSRP CHW Client
An open source digital health platform for frontline health workers.

The CHW Client is an OpenSRP application used by Community Health Workers (CHWs), Community Health Assistants (CHAs) and their supervisors to enumerate all households in their catchment area and provide routine child health, antenatal care (ANC), and postnatal care (PNC) services in the community.

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

## Prerequisites
[Tools and Frameworks Setup](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/6619207/Tools+and+Frameworks+Setup)

## Development setup

### Steps to set up
[OpenSRP android client app build](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/6619236/OpenSRP+App+Build)

### Building the app locally

1. Install the Android SDK tools and a Java 11+ runtime, then export `JAVA_HOME` so Gradle can locate it.
2. Populate the offline Maven mirror from the cached artifacts:
   ```bash
   python3 scripts/sync_local_maven.py
   ```
   Pass a destination to mirror directly into another Maven repository if needed, for example your local `~/.m2/repository` cache:
   ```bash
   python3 scripts/sync_local_maven.py ~/.m2/repository
   ```
   Rerun this command whenever `legacy-opensrp-libs/` changes. The generated `local-maven/` directory is ignored by Git when using the default destination.
3. Ensure the prepackaged AARs remain under `opensrp-chw/libs/` (`circleprogressbar-1.0.8-SNAPSHOT.aar`, `MonthAndYearPicker-1.3.0.aar`, `hellocharts-android-1.5.8.aar`). Replace them if you rebuild those libraries locally.
4. Build the client from the repository root:
   ```bash
   ./gradlew assemble
   # or assemble a specific flavor, e.g.
   ./gradlew assembleNacpDebug
   ```
5. The generated APKs appear under `opensrp-chw/build/outputs/apk/`.

### Running the tests

[Android client unit tests](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/65570428/OpenSRP+Client)

## Deployment
[Production releases](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/1141866503/How+to+create+a+release+APK)

## Features
-   Child health care
-   Antenatal care (ANC)
-   Postnatal care (PNC) 
-   Malaria module
-   Stock Management
-   Service Activity
-   Family Planning
-   All Families register
-   Peer to peer sync

## Versioning
We use SemVer for versioning. For the versions available, see the tags on this repository.
For more details check out <https://semver.org/>

## Authors/Team 
-   The OpenSRP team
-   See the list of contributors who participated in this project from the [Contributors](../../graphs/contributors) link

## Contributing
[Contribution guidelines](https://smartregister.atlassian.net/wiki/spaces/Documentation/pages/6619193/OpenSRP+Developer+s+Guide)

## Documentation
Wiki [OpenSRP Documentation](https://smartregister.atlassian.net/wiki/spaces/Documentation)

## Support
Email: <mailto:support@ona.io>
Slack workspace: <opensrp.slack.com>

## License
This project is licensed under the Apache 2.0 License - see the LICENSE.md file for details
