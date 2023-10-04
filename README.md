# Solr actuators

Readiness endpoint to be used as load balancer check.

Solr is considered ready when it is "almost" ready with tracking alfresco. and that is done in 3 steps of validation :

* `TX_LAG_VALIDATION` by calculating the Tx lag and verifying that it is smaller than MAX_LAG.
* `CHANGE_SET_LAG_VALIDATION` by calculating the ChangeSet lag and verifying that it is smaller than MAX_LAG.
* `REPLICATION_VALIDATION` by following the restore status provided by the replication handler.

The script is available at:

    alfresco/xenit/actuators/readiness

In order to check the health of solr server, the out-of-the-box ping handler can be used.

    alfresco/admin/ping

## Usage

Status code is to be used for the health check: 200 if ready or 503 if not yet ready.

The output of the script offers additional information if being ready as well as information about current tracker
status (when parameter info is appended to the query).

* the MAX_LAG can be set via environment variable `READINESS_MAX_LAG` it is set to default to 1800000 in milliseconds (
  30 minutes).
* the `TX_LAG_VALIDATION` can be Disabled via environment variable `READINESS_TX_LAG_VALIDATION_ENABLED` it is set to
  default to true.

* the `CHANGE_SET_LAG_VALIDATION` can be Disabled via environment variable `READINESS_CHANGE_SET_LAG_VALIDATION_ENABLED`
  it is set to default to true.

* the `REPLICATION_VALIDATION` can be Disabled via environment variable `READINESS_REPLICATION_VALIDATION_ENABLED` it is
  set to default to true.

## How to run integration tests

    ./gradlew integrationTest

