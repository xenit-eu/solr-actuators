package eu.xenit.actuators.handler;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static java.lang.Thread.sleep;
import static org.hamcrest.core.StringContains.containsString;

public class SolrSmokeTests {
    static RequestSpecification spec;

    @BeforeClass
    public static void setup() {

        String basePathSolr = "solr/alfresco/xenit/actuators/readiness";
        String solrHost = System.getProperty("solr.host");

        int solrPort = 0;
        try {
            solrPort = Integer.parseInt(System.getProperty("solr.tcp.8080"));
        } catch(NumberFormatException e) {
            System.out.println("Solr port 8080 is not exposed, probably ssl is enabled");
        }

        String baseURISolr = "http://" + solrHost;

        spec = new RequestSpecBuilder()
                .setBaseUri(baseURISolr)
                .setPort(solrPort)
                .setBasePath(basePathSolr)
                .build();
    }


    @Test
    public void testActuatorsEndpoint() {
        // wait until solr tracks
        long sleepTime = 30000;
        try {
            sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        given()
                .spec(spec)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body(containsString("UP"))
                .toString();

    }
}
