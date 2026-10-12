
package kz.iitu.springlab.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = BookPageController.class)
public class BookHtmlExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(
            BookNotFoundException exception,
            Model model) {

        model.addAttribute("message", exception.getMessage());
        return "books/not-found";
    }
}
