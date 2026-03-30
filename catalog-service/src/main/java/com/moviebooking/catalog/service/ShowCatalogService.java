package com.moviebooking.catalog.service;

import com.moviebooking.catalog.model.Show;
import com.moviebooking.catalog.repository.ShowRepository;
import com.moviebooking.common.dto.ShowDTO;
import com.moviebooking.common.enums.ShowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowCatalogService {
    
    private final ShowRepository showRepository;
    private final RedisCacheService cacheService;
    
    @Value("${cache.ttl.show-list:60}")
    private long showListTtlSeconds;
    
    @Value("${cache.ttl.show-details:300}")
    private long showDetailsTtlSeconds;
    
    @Transactional(readOnly = true)
    public List<ShowDTO> findShowsByMovie(Long movieId, LocalDate date, String city) {
        String cacheKey = String.format("shows:movie:%d:date:%s:city:%s", movieId, date, city);
        
        List<ShowDTO> cachedResult = cacheService.get(cacheKey, List.class).orElse(null);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        log.info("Fetching shows from database for movie: {}, date: {}, city: {}", movieId, date, city);
        List<Show> shows = showRepository.findByMovieAndDateAndCity(movieId, date, city, ShowStatus.ACTIVE);
        
        List<ShowDTO> result = shows.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        cacheService.set(cacheKey, result, Duration.ofSeconds(showListTtlSeconds));
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public List<ShowDTO> findShowsByTheatre(Long theatreId, LocalDate date) {
        String cacheKey = String.format("shows:theatre:%d:date:%s", theatreId, date);
        
        List<ShowDTO> cachedResult = cacheService.get(cacheKey, List.class).orElse(null);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        log.info("Fetching shows from database for theatre: {}, date: {}", theatreId, date);
        List<Show> shows = showRepository.findByTheatreAndDate(theatreId, date, ShowStatus.ACTIVE);
        
        List<ShowDTO> result = shows.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        cacheService.set(cacheKey, result, Duration.ofSeconds(showListTtlSeconds));
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public ShowDTO getShowDetails(Long showId) {
        String cacheKey = String.format("show:details:%d", showId);
        
        ShowDTO cachedResult = cacheService.get(cacheKey, ShowDTO.class).orElse(null);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        log.info("Fetching show details from database for show: {}", showId);
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new RuntimeException("Show not found with id: " + showId));
        
        ShowDTO result = convertToDTO(show);
        
        cacheService.set(cacheKey, result, Duration.ofSeconds(showDetailsTtlSeconds));
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public List<ShowDTO> searchShows(String city, LocalDate startDate, LocalDate endDate) {
        String cacheKey = String.format("shows:city:%s:range:%s:%s", city, startDate, endDate);
        
        List<ShowDTO> cachedResult = cacheService.get(cacheKey, List.class).orElse(null);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        log.info("Searching shows from database for city: {}, date range: {} to {}", city, startDate, endDate);
        List<Show> shows = showRepository.findByCityAndDateRange(city, startDate, endDate, ShowStatus.ACTIVE);
        
        List<ShowDTO> result = shows.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        cacheService.set(cacheKey, result, Duration.ofSeconds(showListTtlSeconds));
        
        return result;
    }
    
    private ShowDTO convertToDTO(Show show) {
        return ShowDTO.builder()
            .showId(show.getId())
            .movieId(show.getMovieId())
            .movieTitle(show.getMovie() != null ? show.getMovie().getTitle() : null)
            .theatreId(show.getTheatreId())
            .theatreName(show.getTheatre() != null ? show.getTheatre().getName() : null)
            .showDate(show.getShowDate())
            .showTime(show.getShowTime())
            .availableSeats(show.getAvailableSeats())
            .basePrice(show.getBasePrice())
            .language(show.getMovie() != null ? show.getMovie().getLanguage() : null)
            .genre(show.getMovie() != null ? show.getMovie().getGenre() : null)
            .city(show.getTheatre() != null ? show.getTheatre().getCity() : null)
            .build();
    }
}
