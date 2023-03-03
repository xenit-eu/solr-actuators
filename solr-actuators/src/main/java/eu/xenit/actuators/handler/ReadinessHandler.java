package eu.xenit.actuators.handler;


import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.SolrException;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;


public class ReadinessHandler extends RequestHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadinessHandler.class);
    private static final String READY = "ready";
    private static final long MAX_LAG = 1800L;


    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) {
        AlfrescoCoreAdminHandler
                coreAdminHandler = (AlfrescoCoreAdminHandler) req.getCore().getCoreContainer().getMultiCoreHandler();

        long lastTxCommitTimeOnServer;
        long lastChangeSetCommitTimeOnServer;
        TrackerState metadataTrackerState;
        TrackerState aclsTrackerState;

        try {
            TrackerRegistry trackerRegistry = coreAdminHandler.getTrackerRegistry();
            String coreName = req.getCore().getName();

            MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            metadataTrackerState = metadataTracker.getTrackerState();
            aclsTrackerState = aclTracker.getTrackerState();
            lastTxCommitTimeOnServer = metadataTrackerState.getLastTxCommitTimeOnServer();
            lastChangeSetCommitTimeOnServer = aclsTrackerState.getLastChangeSetCommitTimeOnServer();
        } catch (Exception e) {
            rsp.add(READY, "DOWN");
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE, e.getMessage()));
            return;
        }

        if ((lastTxCommitTimeOnServer == 0 || lastChangeSetCommitTimeOnServer == 0)) {
            rsp.add(READY, "DOWN");
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    "Solr did not yet get latest values from server"));
            return;
        }

        long lastIndexChangeSetCommitTime = aclsTrackerState.getLastIndexedChangeSetCommitTime();
        long lastIndexTxCommitTime = metadataTrackerState.getLastIndexedTxCommitTime();
        long txLagSeconds = (lastTxCommitTimeOnServer - lastIndexTxCommitTime) / 1000;
        if (req.getParams().get("info") != null) {
            rsp.add("txLag", txLagSeconds);
            rsp.add("lastTxCommitTimeOnServer", lastTxCommitTimeOnServer);
        }
        long changeSetLagSeconds = (lastChangeSetCommitTimeOnServer - lastIndexChangeSetCommitTime) / 1000;
        if (req.getParams().get("info") != null) {
            rsp.add("changeSetLag", changeSetLagSeconds);
            rsp.add("lastChangeSetCommitTimeOnServer", lastChangeSetCommitTimeOnServer);
        }

        if (txLagSeconds >= MAX_LAG || changeSetLagSeconds >= MAX_LAG) {
            rsp.add(READY, "NO");
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    MessageFormat.format("Lag is larger than permitted: txLag={0}, changeSetLag={1}, MAX_LAG={2}"
                            , txLagSeconds, changeSetLagSeconds, MAX_LAG)));
            return;
        }
        rsp.add(READY, "UP");
    }


    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }
}
