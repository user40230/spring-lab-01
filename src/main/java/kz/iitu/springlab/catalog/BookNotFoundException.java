
package kz.iitu.springlab.catalog;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(long id) {
        super("Book with ID " + id + " was not found");
    }
}

