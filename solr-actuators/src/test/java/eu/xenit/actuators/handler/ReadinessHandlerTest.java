package eu.xenit.actuators.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ReadinessHandlerTest {
    ReadinessHandler readinessHandler = new ReadinessHandler();
    @Mock
    SolrQueryRequest req;
    @Mock
    SolrQueryResponse rsp;
    @Mock
    SolrCore solrCore;
    @Mock
    CoreContainer coreContainer;
    @Mock
    AlfrescoCoreAdminHandler coreAdminHandler;
    @Mock
    TrackerRegistry trackerRegistry;
    @Mock
    MetadataTracker metadataTracker;
    @Mock
    AclTracker aclTracker;
    @Mock
    SolrParams solrParams;
    @Mock

    TrackerState metadataTrackerState;
    @Mock

    TrackerState aclsTrackerState;

    @Test
    void handleRequestBodyNullPointerExceptionLog() {
        ListAppender<ILoggingEvent> listAppender = setupAppender();
        //will return null pointer exception on call to anything because nothing is set up
        readinessHandler.handleRequestBody(req, rsp);
        List<ILoggingEvent> logsList = listAppender.list;
        validate(logsList.get(0), Level.ERROR, "solr readiness probe failed with status :'SERVICE_UNAVAILABLE' and message: 'null' ");
    }

    @Test
    void handleRequestBodySolrExceptionLog() {
        ListAppender<ILoggingEvent> listAppender = setupAppender();
        Mockito.when(req.getCore()).thenReturn(solrCore);
        Mockito.when(solrCore.getCoreContainer()).thenReturn(coreContainer);
        Mockito.when(req.getParams()).thenReturn(solrParams);
        Mockito.when(coreContainer.getMultiCoreHandler()).thenReturn(coreAdminHandler);
        Mockito.when(coreAdminHandler.getTrackerRegistry()).thenReturn(trackerRegistry);
        Mockito.when(solrCore.getName()).thenReturn("solr");
        Mockito.when(trackerRegistry.getTrackerForCore("solr", MetadataTracker.class)).thenReturn(metadataTracker);
        Mockito.when(trackerRegistry.getTrackerForCore("solr", AclTracker.class)).thenReturn(aclTracker);

        Mockito.when(metadataTracker.getTrackerState()).thenReturn(metadataTrackerState);
        Mockito.when(aclTracker.getTrackerState()).thenReturn(aclsTrackerState);

        Mockito.when(metadataTrackerState.getLastTxCommitTimeOnServer()).thenReturn(0L);


        readinessHandler.handleRequestBody(req, rsp);
        List<ILoggingEvent> logsList = listAppender.list;
        validate(logsList.get(0), Level.ERROR, "solr readiness probe failed with status :'503' and message: 'Solr did not yet get latest values from alfresco server' ");
    }

    private ListAppender<ILoggingEvent> setupAppender() {
        Logger fooLogger = (Logger) LoggerFactory.getLogger(ReadinessHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        fooLogger.addAppender(listAppender);
        return listAppender;
    }


    private static void validate(ILoggingEvent logEntry, Level logLevel, String expectedMessage) {
        Assertions.assertEquals(expectedMessage, logEntry.getFormattedMessage());
        Assertions.assertEquals(logLevel, logEntry.getLevel());
    }
}