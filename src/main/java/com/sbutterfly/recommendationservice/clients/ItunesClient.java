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
public class ItunesClient {
    private final int limit;
    private final WebClient client;

    public ItunesClient(WebClient.Builder webclientBuilder,
                        @Value("${itunes_base_uri}") String baseUrl,
                        @Value("${response.limit:5}") int limit,
                        MeterRegistry meterRegistry) {
        this.limit = limit;
        var metricsWebClientFilterFunction =
            new MetricsWebClientFilterFunction(meterRegistry, new DefaultWebClientExchangeTagsProvider(), "iTunes", AutoTimer.ENABLED);
        this.client = webclientBuilder
            .baseUrl(baseUrl)
            .filter(metricsWebClientFilterFunction)
            .build();
    }

    public Mono<List<Item>> search(String albumName) {
        var request = client.get();
        var spec = request.uri("search", uriBuilder ->
                uriBuilder
                    .queryParam("term", albumName)
                    .queryParam("country", "NL")
                    .queryParam("media", "music")
                    .queryParam("entity", "album")
                    .queryParam("limit", limit)
                    .build()
            );

        return spec.retrieve()
            .bodyToMono(ITunesResponse.class)
            .map(new Function<ITunesResponse, List<Item>>() {
                @Override
                public List<Item> apply(ITunesResponse response) {
                    return response.getResults().stream()
                        .map(v -> new Item(v.getCollectionName(), v.getArtistName(), Item.Type.ALBUM))
                        .collect(Collectors.toList());
                }
            });
    }

    private static class ITunesResponse {
        private int resultCount;
        private List<Result> results;

        public int getResultCount() {
            return resultCount;
        }

        public List<Result> getResults() {
            return results;
        }

        private static class Result {
            private String collectionName;
            private String artistName;

            public String getCollectionName() {
                return collectionName;
            }

            public String getArtistName() {
                return artistName;
            }
        }
    }
}
