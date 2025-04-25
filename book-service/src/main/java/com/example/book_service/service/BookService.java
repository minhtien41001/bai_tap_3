package com.example.book_service.service;

import com.example.book_service.entity.Book;
import com.example.book_service.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;

    @Cacheable(value = "books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @CacheEvict(value = "books", allEntries = true)
    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    @CacheEvict(value = "books", allEntries = true)
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}
