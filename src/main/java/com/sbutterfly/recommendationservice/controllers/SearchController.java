package com.sbutterfly.recommendationservice.controllers;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sbutterfly.recommendationservice.clients.GoogleBooksClient;
import com.sbutterfly.recommendationservice.clients.ItunesClient;
import com.sbutterfly.recommendationservice.pojo.Item;
import com.sbutterfly.recommendationservice.pojo.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/search")
public class SearchController {
    private final static Logger log = LoggerFactory.getLogger(SearchController.class);

    private final ItunesClient itunesClient;
    private final GoogleBooksClient googleBooksClient;

    public SearchController(ItunesClient itunesClient, GoogleBooksClient googleBooksClient) {
        this.itunesClient = itunesClient;
        this.googleBooksClient = googleBooksClient;
    }
    @GetMapping
    public Mono<SearchResponse> search(@RequestParam String name) {
        var albumsFlux = itunesClient.search(name)
            .timeout(Duration.ofSeconds(5))
            .doOnNext(items -> log.debug("Got {} items from itunes search", items.size()))
            .doOnError(e -> log.warn("iTunes search throw an error", e))
            .map(SearchResponse::new)
            .onErrorReturn(new SearchResponse("iTunes search is unavailable"));

        var booksFlux = googleBooksClient.search(name)
            .timeout(Duration.ofSeconds(5))
            .doOnNext(items -> log.debug("Got {} items from google books search", items.size()))
            .doOnError(e -> log.warn("Google books search throw an error", e))
            .map(SearchResponse::new)
            .onErrorReturn(new SearchResponse("Google books search is unavailable"));

        return albumsFlux.zipWith(booksFlux, this::merge);
    }

    private SearchResponse merge(SearchResponse response1, SearchResponse response2) {
        var error = Stream.of(response1.getErrorMessage(), response2.getErrorMessage())
            .filter(v -> !v.isEmpty())
            .collect(Collectors.joining("; "));
        var items = Stream.of(response1.getItems(), response2.getItems())
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(Item::getTitle))
            .collect(Collectors.toList());
        return new SearchResponse(error, items);
    }
}
