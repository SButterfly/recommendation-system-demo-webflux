package com.sbutterfly.recommendationservice.health;

import java.time.Duration;

import com.sbutterfly.recommendationservice.clients.GoogleBooksClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GoogleBooksHealthIndicator implements ReactiveHealthIndicator {
    private final GoogleBooksClient googleBooksClient;

    public GoogleBooksHealthIndicator(GoogleBooksClient googleBooksClient) {
        this.googleBooksClient = googleBooksClient;
    }

    @Override
    public Mono<Health> health() {
        return googleBooksClient.search("To kill a mockingbird")
            .timeout(Duration.ofSeconds(5))
            .map(a -> new Health.Builder().up().build())
            .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()));
    }
}
