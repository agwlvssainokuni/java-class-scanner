package com.example.library;

public class Author {

    private final String name;
    private final String country;

    public Author(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }
}
