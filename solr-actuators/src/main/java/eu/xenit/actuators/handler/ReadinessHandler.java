package eu.xenit.actuators.handler;


import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.ReplicationHandler;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;

public class ReadinessHandler extends RequestHandlerBase implements SolrCoreAware {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String READY = "ready";
    private static final String DOWN = "DOWN";
    private static final String UP = "UP";
    private static final ReadinessConfig config = new ReadinessConfig();
    SolrCore core;

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) {
        try {
            SolrCore solrCore = req.getCore();
            AlfrescoCoreAdminHandler
                    coreAdminHandler = (AlfrescoCoreAdminHandler) solrCore
                    .getCoreContainer()
                    .getMultiCoreHandler();
            boolean setInfo = req.getParams().get("info") != null;

            checkTransactionResponse(setInfo, rsp, coreAdminHandler.getTrackerRegistry(), solrCore.getName());

            checkReplicationHandler(setInfo, rsp);

        } catch (SolrException e) {
            rsp.add(READY, DOWN);
            rsp.setException(e);
            log.error("solr readiness probe failed with status :'{}' and message: '{}' ", e.code(), e.getMessage());

            return;
        } catch (Exception e) {
            rsp.add(READY, DOWN);
            rsp.setException(new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE, e.getMessage(), e));
            log.error("solr readiness probe failed with status :'{}' and message: '{}' ",
                    SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    e.getMessage());
            return;
        }
        rsp.add(READY, UP);
    }

    private void checkTransactionResponse(boolean setInfo,
                                          SolrQueryResponse rsp,
                                          TrackerRegistry trackerRegistry,
                                          String coreName) {

        checkTxResponse(setInfo, rsp, trackerRegistry, coreName);
        checkChangeSetResponse(setInfo, rsp, trackerRegistry, coreName);
    }

    private void checkTxResponse(boolean setInfo,
                                 SolrQueryResponse rsp, TrackerRegistry trackerRegistry,
                                 String coreName) {
        if (!config.isTxValidationEnabled()) return;

        MetadataTracker metadataTracker = trackerRegistry.getTrackerForCore(coreName, MetadataTracker.class);
        TrackerState metadataTrackerState = metadataTracker.getTrackerState();

        long lastTxCommitTimeOnServer = metadataTrackerState.getLastTxCommitTimeOnServer();
        long lastIndexTxCommitTime = metadataTrackerState.getLastIndexedTxCommitTime();
        long txLag = (lastTxCommitTimeOnServer - lastIndexTxCommitTime);

        if (setInfo) {
            rsp.add("txLag", txLag);
            rsp.add("lastTxCommitTimeOnServer", lastTxCommitTimeOnServer);
            rsp.add("lastIndexTxCommitTime", lastIndexTxCommitTime);
        }
        if (lastTxCommitTimeOnServer == 0) {
            throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    "Solr did not yet get latest Tx values from alfresco server");
        }
        if (txLag >= config.getMaxLag()) {
            throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    MessageFormat.format("Tx lag is larger than permitted: txLag={0}, MAX_LAG={1}"
                            , txLag, config.getMaxLag()));
        }
    }

    private void checkChangeSetResponse(boolean setInfo,
                                        SolrQueryResponse rsp,
                                        TrackerRegistry trackerRegistry,
                                        String coreName) {
        if (!config.isChangeSetValidationEnabled()) return;
        AclTracker aclTracker = trackerRegistry.getTrackerForCore(coreName, AclTracker.class);
        TrackerState aclsTrackerState = aclTracker.getTrackerState();

        long lastChangeSetCommitTimeOnServer = aclsTrackerState.getLastChangeSetCommitTimeOnServer();
        long lastIndexChangeSetCommitTime = aclsTrackerState.getLastIndexedChangeSetCommitTime();
        long changeSetLag = (lastChangeSetCommitTimeOnServer - lastIndexChangeSetCommitTime);

        if (setInfo) {
            rsp.add("changeSetLag", changeSetLag);
            rsp.add("lastChangeSetCommitTimeOnServer", lastChangeSetCommitTimeOnServer);
            rsp.add("lastIndexChangeSetCommitTime", lastIndexChangeSetCommitTime);
        }
        if (lastChangeSetCommitTimeOnServer == 0) {
            throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    "Solr did not yet get latest change set values from alfresco server");
        }
        if (changeSetLag >= config.getMaxLag()) {
            throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    MessageFormat.format("Change set lag is larger than permitted:  changeSetLag={0}, MAX_LAG={1}"
                            , changeSetLag, config.getMaxLag()));
        }
    }

    private void checkReplicationHandler(boolean setInfo, SolrQueryResponse rsp) throws Exception {
        if (!config.isReplicationValidationEnabled()) return;
        SolrRequestHandler handler = core.getRequestHandler(ReplicationHandler.PATH);
        RequestHandlerBase replicationHandler = (RequestHandlerBase) handler;
        NamedList<Object> query = new SimpleOrderedMap<>();
        query.add(ReplicationHandler.COMMAND, ReplicationHandler.CMD_RESTORE_STATUS);
        SolrQueryRequest replicationReq = new LocalSolrQueryRequest(core, query);
        SolrQueryResponse replicationRsp = new SolrQueryResponse();
        replicationHandler.handleRequestBody(replicationReq, replicationRsp);
        NamedList response = replicationRsp.getValues();
        response = (NamedList) response.get(ReplicationHandler.CMD_RESTORE_STATUS);
        String status = (String) response.get(ReplicationHandler.STATUS);
        if (setInfo) {
            rsp.add("replication.status", status);
        }
        if ("In Progress".equals(status)) {
            throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    "Replication handler is doing a restore");
        } else if ("failed".equals(status)) {
            throw new SolrException(SolrException.ErrorCode.SERVICE_UNAVAILABLE,
                    "Replication handler restore has failed");
        }
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public void inform(SolrCore core) {
        this.core = core;
    }
}
