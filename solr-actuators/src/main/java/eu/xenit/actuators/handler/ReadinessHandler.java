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

import java.lang.reflect.Method;

public class ReadinessHandler extends RequestHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(ReadinessHandler.class);
    private static final long MAX_LAG = 1800L;

    Object invokeHack(Object target, String... methods) throws Exception {
        for (String methodName : methods) {
            Method method = target.getClass().getMethod(methodName);
            target = method.invoke(target);
        }
        return target;
    }


    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        AlfrescoCoreAdminHandler coreAdminHandler = null;
        if(isSolr6(req)) {
            coreAdminHandler = (AlfrescoCoreAdminHandler) invokeHack(req,"getCore","getCoreContainer","getMultiCoreHandler");
        } else {
            coreAdminHandler = (AlfrescoCoreAdminHandler)invokeHack(req,"getCore","getCoreDescriptor","getCoreContainer","getMultiCoreHandler");
        }

        long lastTxCommitTimeOnServer = 0;
        long lastChangeSetCommitTimeOnServer = 0;
        TrackerState metadataTrackerState = null;
        TrackerState aclsTrackerState = null;

        try {
            TrackerRegistry trackerRegistry = coreAdminHandler.getTrackerRegistry();
            String coreName = req.getCore().getName();

            MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
            AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);

            metadataTrackerState =  metadataTracker.getTrackerState();
            aclsTrackerState = aclTracker.getTrackerState();
            lastTxCommitTimeOnServer = metadataTrackerState.getLastTxCommitTimeOnServer();
            lastChangeSetCommitTimeOnServer = aclsTrackerState.getLastChangeSetCommitTimeOnServer();
        }  catch (Exception e) {
            rsp.add("ready", "DOWN");
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE, e.getMessage()));
            return;
        }

        if((lastTxCommitTimeOnServer == 0 || lastChangeSetCommitTimeOnServer == 0)) {
            rsp.add("ready","DOWN");
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,"Solr did not yet get latest values from server"));
            return;
        }

        long lastIndexChangeSetCommitTime = aclsTrackerState.getLastIndexedChangeSetCommitTime();
        long lastIndexTxCommitTime = metadataTrackerState.getLastIndexedTxCommitTime();
        long txLagSeconds = (lastTxCommitTimeOnServer - lastIndexTxCommitTime) / 1000;
        if(req.getParams().get("info")!=null) {
            rsp.add("txLag", txLagSeconds);
            rsp.add("lastTxCommitTimeOnServer", lastTxCommitTimeOnServer);
        }
        long changeSetLagSeconds = (lastChangeSetCommitTimeOnServer - lastIndexChangeSetCommitTime) / 1000;
        if(req.getParams().get("info")!=null) {
            rsp.add("changeSetLag", changeSetLagSeconds);
            rsp.add("lastChangeSetCommitTimeOnServer", lastChangeSetCommitTimeOnServer);
        }

        if(txLagSeconds >= MAX_LAG || changeSetLagSeconds >= MAX_LAG) {
            rsp.add("ready","NO");
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,"Lag is larger than permitted: txLag=" + txLagSeconds + ", changeSetLag=" + changeSetLagSeconds + ", MAX_LAG=" + MAX_LAG));
            return;
        } else {
            rsp.add("ready","UP");
        }
    }

    private boolean isSolr6(SolrQueryRequest req) {
        try {
            invokeHack(req,"getCore","getCoreContainer");
        } catch (Exception e) {
            return false;
        }
        return true;
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
