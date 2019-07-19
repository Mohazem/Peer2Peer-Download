package fr.ujm.ptorrent.model;

import lombok.Data;

@Data
public class Book {
    private int size;
    private byte[] book;
    private int pos;

    public Book(int size) {
        this.size = size;
        this.book = new byte[size];
    }

    public Book(byte[] book, int size) {
        this.size = size;
        this.book = book;
    }

    public Book(byte[] book, int size, int pos) {
        this(book, size);
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "Book{" +
                "size=" + size +
                ", pos=" + pos +
                '}';
    }
}
