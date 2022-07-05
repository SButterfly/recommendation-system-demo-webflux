package com.sbutterfly.recommendationservice.clients;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sbutterfly.recommendationservice.pojo.Item;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GoogleBooksClient {
    private final int limit;
    private final WebClient client;

    public GoogleBooksClient(WebClient.Builder webclientBuilder,
                             @Value("${google_books_base_uri}") String baseUrl,
                             @Value("${response.limit:5}") int limit,
                             MeterRegistry meterRegistry) {
        this.limit = limit;
        var metricsWebClientFilterFunction =
            new MetricsWebClientFilterFunction(meterRegistry, new DefaultWebClientExchangeTagsProvider(), "googleBooks", AutoTimer.ENABLED);
        this.client = webclientBuilder
            .baseUrl(baseUrl)
            .filter(metricsWebClientFilterFunction)
            .build();
    }

    public Mono<List<Item>> search(String bookName) {
        var request = client.get();
        var spec = request.uri("books/v1/volumes", uriBuilder ->
            uriBuilder
                .queryParam("q", "intitle:" + bookName)
                .queryParam("maxResults", limit)
                .queryParam("printType", "books")
                .queryParam("projection", "lite")
                .build()
        );

        return spec.retrieve()
            .bodyToMono(GoogleBooksResponse.class)
            .map(new Function<GoogleBooksResponse, List<Item>>() {
                @Override
                public List<Item> apply(GoogleBooksResponse response) {
                    return response.getItems().stream()
                        .map(v -> {
                            var title = v.getVolumeInfo().getTitle();
                            var authors = String.join(", ", v.getVolumeInfo().getAuthors());
                            return new Item(title, authors, Item.Type.BOOK);
                        })
                        .collect(Collectors.toList());
                }
            });
    }

    private static class GoogleBooksResponse {
        private List<Item> items;

        public List<Item> getItems() {
            return items == null ? List.of() : items;
        }

        private static class VolumeInfo {
            private String title;
            private List<String> authors;

            public String getTitle() {
                return title;
            }

            public List<String> getAuthors() {
                return authors == null ? List.of() : authors;
            }
        }

        private static class Item {
            private VolumeInfo volumeInfo;

            public VolumeInfo getVolumeInfo() {
                return volumeInfo;
            }
        }
    }
}
