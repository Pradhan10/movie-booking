package com.moviebooking.catalog.service;

import com.moviebooking.catalog.model.Movie;
import com.moviebooking.catalog.model.Show;
import com.moviebooking.catalog.model.Theatre;
import com.moviebooking.catalog.repository.MovieRepository;
import com.moviebooking.catalog.repository.ShowRepository;
import com.moviebooking.catalog.repository.TheatreRepository;
import com.moviebooking.common.dto.ShowDTO;
import com.moviebooking.common.enums.ShowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ShowCatalogServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
    
    @Autowired
    private ShowCatalogService catalogService;
    
    @Autowired
    private ShowRepository showRepository;
    
    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private TheatreRepository theatreRepository;
    
    private Movie testMovie;
    private Theatre testTheatre;
    private Show testShow;
    
    @BeforeEach
    void setUp() {
        showRepository.deleteAll();
        movieRepository.deleteAll();
        theatreRepository.deleteAll();
        
        testMovie = movieRepository.save(Movie.builder()
            .title("Inception")
            .language("English")
            .genre("Sci-Fi")
            .durationMinutes(148)
            .releaseDate(LocalDate.of(2010, 7, 16))
            .rating("PG-13")
            .createdAt(Instant.now())
            .build());
        
        testTheatre = theatreRepository.save(Theatre.builder()
            .name("PVR Phoenix")
            .location("Whitefield")
            .city("Bangalore")
            .state("Karnataka")
            .country("India")
            .partnerId(1L)
            .totalScreens(5)
            .status("ACTIVE")
            .createdAt(Instant.now())
            .build());
        
        testShow = showRepository.save(Show.builder()
            .movieId(testMovie.getId())
            .theatreId(testTheatre.getId())
            .screenId(1L)
            .showDate(LocalDate.now().plusDays(1))
            .showTime(LocalTime.of(18, 30))
            .basePrice(BigDecimal.valueOf(250.00))
            .availableSeats(100)
            .status(ShowStatus.ACTIVE)
            .version(0)
            .createdAt(Instant.now())
            .build());
    }
    
    @Test
    void testFindShowsByMovie_ShouldReturnShows() {
        List<ShowDTO> shows = catalogService.findShowsByMovie(
            testMovie.getId(), 
            LocalDate.now().plusDays(1), 
            "Bangalore"
        );
        
        assertThat(shows).hasSize(1);
        assertThat(shows.get(0).getMovieTitle()).isEqualTo("Inception");
        assertThat(shows.get(0).getTheatreName()).isEqualTo("PVR Phoenix");
        assertThat(shows.get(0).getAvailableSeats()).isEqualTo(100);
    }
    
    @Test
    void testFindShowsByTheatre_ShouldReturnShows() {
        List<ShowDTO> shows = catalogService.findShowsByTheatre(
            testTheatre.getId(), 
            LocalDate.now().plusDays(1)
        );
        
        assertThat(shows).hasSize(1);
        assertThat(shows.get(0).getTheatreId()).isEqualTo(testTheatre.getId());
    }
    
    @Test
    void testGetShowDetails_ShouldReturnDetails() {
        ShowDTO show = catalogService.getShowDetails(testShow.getId());
        
        assertThat(show).isNotNull();
        assertThat(show.getShowId()).isEqualTo(testShow.getId());
        assertThat(show.getMovieTitle()).isEqualTo("Inception");
        assertThat(show.getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
    }
    
    @Test
    void testSearchShows_ShouldReturnShowsInRange() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<ShowDTO> shows = catalogService.searchShows(
            "Bangalore", 
            tomorrow, 
            tomorrow.plusDays(7)
        );
        
        assertThat(shows).hasSize(1);
        assertThat(shows.get(0).getCity()).isEqualTo("Bangalore");
    }
}
