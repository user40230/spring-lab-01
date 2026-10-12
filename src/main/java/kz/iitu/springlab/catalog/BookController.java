
package kz.iitu.springlab.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Book> findAll(
            @RequestParam(required = false) String author) {
        return service.findAll(author);
    }

    @GetMapping("/{id}")
    public Book findById(@PathVariable long id) {
        return service.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Book book) {
        Book created = service.create(book);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> update(
            @PathVariable long id,
            @RequestBody Book book) {

        if (service.findById(id).isEmpty()) {
            throw new BookNotFoundException(id);
        }

        Book updated = service.create(
                new Book(id, book.title(), book.author(), book.year())
        );

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {

        if (!service.delete(id)) {
            throw new BookNotFoundException(id);
        }

        return ResponseEntity.noContent().build();
    }
}
