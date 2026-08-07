package com.example.library;

public class EBook extends Book {

    private final long fileSizeBytes;

    public EBook(String title, Author author, String isbn, long fileSizeBytes) {
        super(title, author, isbn);
        this.fileSizeBytes = fileSizeBytes;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    @Deprecated
    public void download() {
        borrow();
    }
}
