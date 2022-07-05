package com.sbutterfly.recommendationservice.pojo;

public class Item {
    private final String title;
    private final String authors;
    private final Type type;

    public Item(String title, String authors, Type type) {
        this.title = title;
        this.authors = authors;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Item{" +
            "title='" + title + '\'' +
            ", authors='" + authors + '\'' +
            ", type=" + type +
            '}';
    }

    public enum Type {
        BOOK,
        ALBUM
    }
}
