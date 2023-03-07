# Solr actuators

Readiness endpoint to be used as load balancer check.

Solr is considered ready when it is "almost" ready with tracking alfresco.  
This is computed subjectivelly using the lag reported by the SUMMARY screen: solr is ready when the lag is smaller than MAX_LAG.
also it is following the restore status provided by the replication handler

The script is available at:

    alfresco/xenit/actuators/readiness

In order to check the health of solr server, the out-of-the-box ping handler can be used.

    alfresco/admin/ping

## Usage

Status code is to be used for the health check: 200 if ready or 503 if not yet ready.

The output of the script offers additional information if being ready as well as information about current tracker status (when parameter info is appended to the query).

the MAX_LAG can be set via environment variable `READINESS_MAX_LAG` it is set to default to 1800000 in milliseconds (30 minutes).

## How to run integration tests

    ./gradlew integrationTest

