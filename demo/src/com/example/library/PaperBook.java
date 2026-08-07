package com.example.library;

public class PaperBook extends Book {

    private final int shelfNumber;

    public PaperBook(String title, Author author, String isbn, int shelfNumber) {
        super(title, author, isbn);
        this.shelfNumber = shelfNumber;
    }

    public int getShelfNumber() {
        return shelfNumber;
    }
}
