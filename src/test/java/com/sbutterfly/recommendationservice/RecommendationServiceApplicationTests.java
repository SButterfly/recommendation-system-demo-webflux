package com.sbutterfly.recommendationservice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.util.IOUtils;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient(timeout = "PT5M")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationServiceApplicationTests {
    @Autowired
    WebTestClient webClient;

    public static MockWebServer itunesServer;
    public static MockWebServer googleServer;

    @BeforeAll
    static void beforeAll() throws IOException {
        itunesServer = new MockWebServer();
        itunesServer.start();
        googleServer = new MockWebServer();
        googleServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        itunesServer.shutdown();
        googleServer.shutdown();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("itunes_base_uri", () -> "http://localhost:" + itunesServer.getPort());
        r.add("google_books_base_uri", () -> "http://localhost:" + googleServer.getPort());
    }

    @Test
    void simpleRun() {
        // assume
        itunesServer.enqueue(new MockResponse()
            .setBody(readFromResources("/love_and_hate_itunes_response.json"))
            .addHeader("Content-Type", "text/javascript;charset=UTF-8")
        );
        googleServer.enqueue(new MockResponse()
            .setBody(readFromResources("/love_and_hate_google_response.json"))
            .addHeader("Content-Type", "application/json")
        );

        // act
        var body = webClient.get().uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("name", "Love and Hate")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();

        // assert
        body.json(readFromResources("/love_and_hate_result.json"), true);
    }

    @Test
    void itunesIsNotResponding() {
        // assume
        googleServer.enqueue(new MockResponse()
            .setBody(readFromResources("/love_and_hate_google_response.json"))
            .addHeader("Content-Type", "application/json")
        );

        // act
        var body = webClient.get().uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("name", "Love and Hate")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();

        // assert
        body.json(readFromResources("/love_and_hate_result_without_itunes.json"), true);
    }

    @Test
    void itunesReturn400Code() {
        // assume
        itunesServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("Something is wrong with request")
        );
        googleServer.enqueue(new MockResponse()
            .setBody(readFromResources("/love_and_hate_google_response.json"))
            .addHeader("Content-Type", "application/json")
        );

        // act
        var body = webClient.get().uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("name", "Love and Hate")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();

        // assert
        body.json(readFromResources("/love_and_hate_result_without_itunes.json"), true);
    }

    @Test
    void itunesAndGoogleAreUnavailable() {
        // assume
        itunesServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("Something is wrong with request")
        );

        // act
        var body = webClient.get().uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("name", "Love and Hate")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();

        // assert
        body.json(readFromResources("/love_and_hate_result_without_itunes_and_google.json"), true);
    }

    @Test
    @Timeout(6)
    void itunesAndGoogleResponseIsSlow() {
        // imagine, that they answer in 4 seconds, and we expect an answer within 6 seconds
        // here is this test we check, that our requests run in parallel

        // assume
        itunesServer.enqueue(new MockResponse()
            .setBodyDelay(4, TimeUnit.SECONDS)
            .setBody(readFromResources("/love_and_hate_itunes_response.json"))
            .addHeader("Content-Type", "text/javascript;charset=UTF-8")
        );
        googleServer.enqueue(new MockResponse()
            .setBodyDelay(4, TimeUnit.SECONDS)
            .setBody(readFromResources("/love_and_hate_google_response.json"))
            .addHeader("Content-Type", "application/json")
        );

        // act
        var body = webClient.get().uri(uriBuilder -> uriBuilder.path("/search")
                .queryParam("name", "Love and Hate")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody();

        // assert
        body.json(readFromResources("/love_and_hate_result.json"), true);
    }

    private String readFromResources(String resourcePath) {
        try (var stream = this.getClass().getResourceAsStream(resourcePath)) {
            return IOUtils.toString(Objects.requireNonNull(stream), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
