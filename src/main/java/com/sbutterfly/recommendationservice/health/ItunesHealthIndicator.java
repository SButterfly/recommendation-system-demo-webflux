package com.sbutterfly.recommendationservice.health;

import java.time.Duration;

import com.sbutterfly.recommendationservice.clients.ItunesClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ItunesHealthIndicator implements ReactiveHealthIndicator {
    private final ItunesClient itunesClient;

    public ItunesHealthIndicator(ItunesClient itunesClient) {
        this.itunesClient = itunesClient;
    }

    @Override
    public Mono<Health> health() {
        return itunesClient.search("Queen")
            .timeout(Duration.ofSeconds(5))
            .map(a -> new Health.Builder().up().build())
            .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()));
    }
}
