package io.github.fabito.dropwizard.samples.resources;

import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.github.fabito.dropwizard.samples.SampleApplication;
import io.github.fabito.dropwizard.samples.SampleConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by fabio on 19/12/16.
 */
public class EchoResourceTest {


    @ClassRule
    public static final DropwizardAppRule<SampleConfiguration> RULE =
            new DropwizardAppRule<>(SampleApplication.class, ResourceHelpers.resourceFilePath("config.yml"));


    @Test
    public void get() throws Exception {
        final HttpClient httpClient = new HttpClientBuilder(RULE.getEnvironment())
                .using(RULE.getConfiguration().getHttpClientConfiguration())
                .build("test http client");
        HttpResponse response = httpClient
                .execute(echoRequest("teste"));
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
    }

    private HttpGet echoRequest(String echo) {
        return new HttpGet(String.format("http://localhost:%d/api/echo?echo=%s", RULE.getLocalPort(), echo));
    }

    @Test
    public void getFail() throws Exception {
        final HttpClient httpClient = new HttpClientBuilder(RULE.getEnvironment())
                .using(RULE.getConfiguration().getHttpClientConfiguration())
                .build("test http client");
        HttpResponse response = httpClient
                .execute(echoFailRequest());
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(500);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    private HttpGet echoFailRequest() {
        return new HttpGet(String.format("http://localhost:%d/api/echofail", RULE.getLocalPort()));
    }

}