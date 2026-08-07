package com.example.library;

public abstract class Book implements Borrowable {

    public static final int UNKNOWN_YEAR = -1;

    private final String title;
    private final Author author;
    private final String isbn;
    private boolean available = true;

    protected Book(String title, Author author, String isbn) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public Author getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void borrow() {
        available = false;
    }
}
