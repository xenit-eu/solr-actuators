package eu.xenit.actuators.integrationtesting;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static java.lang.Thread.sleep;

public class SolrSmokeTests {
    static RequestSpecification spec;
    private static final Log log = LogFactory.getLog(SolrSmokeTests.class);

    @BeforeAll
    public static void setup() {
        String flavor = System.getProperty("flavor");
        String basePathSolr = "solr/alfresco/xenit/actuators/readiness";

        String solrHost = System.getProperty("solr.host");

        int solrPort = 0;
        try {
            solrPort = Integer.parseInt(System.getProperty("solr.tcp.8080"));
        } catch (NumberFormatException e) {
            System.out.println("Solr port 8080 is not exposed, probably ssl is enabled");
        }

        System.out.println("Looking at " + solrHost + ":" + solrPort + "/" + basePathSolr + " where flavor=" + flavor);
        String baseURISolr = "http://" + solrHost;

        spec = new RequestSpecBuilder()
                .setBaseUri(baseURISolr)
                .setPort(solrPort)
                .setBasePath(basePathSolr)
                .build();
    }


    @Test
    void testActuatorsEndpoint() {
        System.out.println("Ready test triggered, will wait maximum 30 seconds");
        String status = "";
        long timeout = 30000;
        long elapsed = 0;
        while ("UP".equals(status) && elapsed < timeout) {
            status = given()
                    .spec(spec)
                    .when()
                    .get()
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("ready");
            System.out.println("elapsed =" + elapsed);
            try {
                sleep(1000);
                elapsed += 1000;
            } catch (InterruptedException e) {
                log.error(e);
            }
        }

    }
}
