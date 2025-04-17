package com.example.book_service.entity.repository;

import com.example.book_service.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
