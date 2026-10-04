package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll(String author) {
        if (author == null) {
            return repository.findAll();
        }

        return repository.findAll()
                .stream()
                .filter(book -> book.author().equalsIgnoreCase(author))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book create(Book book) {
        return repository.save(book);
    }

    public boolean delete(long id) {
        return repository.deleteById(id);
    }
}

