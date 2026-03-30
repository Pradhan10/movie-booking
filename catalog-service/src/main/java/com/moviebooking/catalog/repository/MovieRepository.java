package com.moviebooking.catalog.repository;

import com.moviebooking.catalog.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByLanguage(String language);
    
    List<Movie> findByGenreContaining(String genre);
    
    List<Movie> findByLanguageAndGenreContaining(String language, String genre);
}
