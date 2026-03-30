package com.moviebooking.catalog.controller;

import com.moviebooking.catalog.service.OfferService;
import com.moviebooking.catalog.service.ShowCatalogService;
import com.moviebooking.common.dto.OfferDTO;
import com.moviebooking.common.dto.ShowDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Show Catalog", description = "APIs for browsing movies, theatres, and show timings")
public class ShowCatalogController {
    
    private final ShowCatalogService catalogService;
    private final OfferService offerService;
    
    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Get shows by movie", description = "Browse theatres showing a specific movie on a given date in a city")
    public ResponseEntity<List<ShowDTO>> getShowsByMovie(
        @Parameter(description = "Movie ID") @PathVariable Long movieId,
        @Parameter(description = "Show date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Parameter(description = "City name") @RequestParam String city
    ) {
        log.info("GET /shows/movie/{} - date: {}, city: {}", movieId, date, city);
        List<ShowDTO> shows = catalogService.findShowsByMovie(movieId, date, city);
        return ResponseEntity.ok(shows);
    }
    
    @GetMapping("/theatre/{theatreId}")
    @Operation(summary = "Get shows by theatre", description = "Browse all shows in a specific theatre on a given date")
    public ResponseEntity<List<ShowDTO>> getShowsByTheatre(
        @Parameter(description = "Theatre ID") @PathVariable Long theatreId,
        @Parameter(description = "Show date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /shows/theatre/{} - date: {}", theatreId, date);
        List<ShowDTO> shows = catalogService.findShowsByTheatre(theatreId, date);
        return ResponseEntity.ok(shows);
    }
    
    @GetMapping("/{showId}")
    @Operation(summary = "Get show details", description = "Get detailed information about a specific show")
    public ResponseEntity<ShowDTO> getShowDetails(
        @Parameter(description = "Show ID") @PathVariable Long showId
    ) {
        log.info("GET /shows/{}", showId);
        ShowDTO show = catalogService.getShowDetails(showId);
        return ResponseEntity.ok(show);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search shows", description = "Search shows by city and date range")
    public ResponseEntity<List<ShowDTO>> searchShows(
        @Parameter(description = "City name") @RequestParam String city,
        @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /shows/search - city: {}, range: {} to {}", city, startDate, endDate);
        List<ShowDTO> shows = catalogService.searchShows(city, startDate, endDate);
        return ResponseEntity.ok(shows);
    }
    
    @GetMapping("/offers")
    @Operation(summary = "Get applicable offers", description = "Get all active offers for a city and theatre")
    public ResponseEntity<List<OfferDTO>> getOffers(
        @Parameter(description = "City name") @RequestParam String city,
        @Parameter(description = "Theatre ID", required = false) @RequestParam(required = false) Long theatreId
    ) {
        log.info("GET /shows/offers - city: {}, theatreId: {}", city, theatreId);
        List<OfferDTO> offers = offerService.getApplicableOffers(city, theatreId);
        return ResponseEntity.ok(offers);
    }
}
