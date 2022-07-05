package com.sbutterfly.recommendationservice.pojo;

import java.util.List;
import java.util.Objects;

public class SearchResponse {

    private final String errorMessage;
    private final List<Item> items;

    public SearchResponse(String errorMessage) {
        this(errorMessage, List.of());
    }

    public SearchResponse(List<Item> items) {
        this("", items);
    }

    public SearchResponse(String errorMessage, List<Item> items) {
        this.errorMessage = Objects.requireNonNull(errorMessage);
        this.items = Objects.requireNonNull(items);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<Item> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "SearchResponse{" +
            "errorMessage='" + errorMessage + '\'' +
            ", items=" + items +
            '}';
    }
}
