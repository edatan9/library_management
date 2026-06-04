package com.example.library.integration;

import com.example.library.model.Book;
import com.example.library.model.Genre;
import com.example.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION TEST - Repository Layer
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    private Book createBook(String isbn, String title, String author, int copies, Genre genre) {
        Book book = new Book(isbn, title, author, copies, genre);
        book.setPublishedDate(LocalDate.of(2020, 1, 1));
        return bookRepository.save(book);
    }

    // =========================================================================
    // EXAMPLE: Basic CRUD and custom query tests — filled in
    // =========================================================================

    @Nested
    @DisplayName("Basic CRUD operations")
    class CrudTests {

        @Test
        @DisplayName("should save and retrieve a book by ID")
        void shouldSaveAndFindById() {
            Book saved = createBook("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);

            Optional<Book> found = bookRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Clean Code");
            assertThat(found.get().getIsbn()).isEqualTo("978-0-13-468599-1");
        }

        @Test
        @DisplayName("should find book by ISBN")
        void shouldFindByIsbn() {
            createBook("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);

            Optional<Book> found = bookRepository.findByIsbn("978-0-13-468599-1");

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Clean Code");
        }

        @Test
        @DisplayName("should return empty when ISBN not found")
        void shouldReturnEmpty_WhenIsbnNotFound() {
            Optional<Book> found = bookRepository.findByIsbn("non-existent");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Custom query methods")
    class CustomQueryTests {

        @Test
        @DisplayName("should search books by keyword in title or author (case insensitive)")
        void shouldSearchByKeyword() {
            createBook("978-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
            createBook("978-2", "Clean Architecture", "Robert C. Martin", 2, Genre.TECHNOLOGY);
            createBook("978-3", "Design Patterns", "Gang of Four", 5, Genre.TECHNOLOGY);

            List<Book> results = bookRepository.searchBooks("clean");

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Book::getTitle)
                    .containsExactlyInAnyOrder("Clean Code", "Clean Architecture");
        }

        @Test
        @DisplayName("should find available books (copies > 0)")
        void shouldFindAvailableBooks() {
            Book available = createBook("978-1", "Available Book", "Author A", 3, Genre.FICTION);
            Book unavailable = createBook("978-2", "Unavailable Book", "Author B", 1, Genre.FICTION);
            unavailable.setAvailableCopies(0);
            bookRepository.save(unavailable);

            List<Book> results = bookRepository.findAvailableBooks();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Available Book");
        }
    }

    // =========================================================================
    // Genre and author query tests
    // =========================================================================

    @Nested
    @DisplayName("Genre and author queries")
    class FilterTests {

        @Test
        @DisplayName("should find books by genre")
        void shouldFindByGenre() {
            createBook("978-1", "A Brief History of Time", "Stephen Hawking", 5, Genre.SCIENCE);
            createBook("978-2", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
            createBook("978-3", "Cosmos", "Carl Sagan", 4, Genre.SCIENCE);

            List<Book> scienceBooks = bookRepository.findByGenre(Genre.SCIENCE);

            assertThat(scienceBooks).hasSize(2);
            assertThat(scienceBooks).extracting(Book::getTitle)
                    .containsExactlyInAnyOrder("A Brief History of Time", "Cosmos");
        }

        @Test
        @DisplayName("should find books by author (case insensitive, partial match)")
        void shouldFindByAuthor() {
            createBook("978-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
            createBook("978-2", "Clean Architecture", "Robert C. Martin", 2, Genre.TECHNOLOGY);
            createBook("978-3", "Design Patterns", "Gang of Four", 5, Genre.TECHNOLOGY);

            List<Book> results = bookRepository.findByAuthorContainingIgnoreCase("robert");

            assertThat(results).hasSize(2);
            assertThat(results).extracting(Book::getAuthor)
                    .allMatch(a -> a.toLowerCase().contains("robert"));
        }

        @Test
        @DisplayName("should search by author name using searchBooks()")
        void shouldSearchByAuthorKeyword() {
            createBook("978-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
            createBook("978-2", "Design Patterns", "Gang of Four", 5, Genre.TECHNOLOGY);

            List<Book> results = bookRepository.searchBooks("Gang of Four");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Design Patterns");
        }

        @Test
        @DisplayName("should return empty list when no books match search")
        void shouldReturnEmpty_WhenNoMatch() {
            createBook("978-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);

            List<Book> results = bookRepository.searchBooks("nonexistentkeywordxyz");

            assertThat(results).isEmpty();
        }
    }

    // =========================================================================
    // Edge case tests
    // =========================================================================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should enforce unique ISBN constraint")
        void shouldEnforceUniqueIsbn() {
            createBook("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);

            assertThrows(DataIntegrityViolationException.class, () -> {
                Book duplicate = new Book("978-0-13-468599-1", "Another Book", "Another Author", 2, Genre.FICTION);
                duplicate.setPublishedDate(LocalDate.of(2020, 1, 1));
                bookRepository.saveAndFlush(duplicate);
            });
        }

        @Test
        @DisplayName("should handle deleting a book")
        void shouldDeleteBook() {
            Book saved = createBook("978-0-13-468599-1", "Clean Code", "Robert C. Martin", 3, Genre.TECHNOLOGY);
            Long id = saved.getId();

            bookRepository.deleteById(id);

            assertThat(bookRepository.findById(id)).isEmpty();
        }
    }
}
