package com.moviebooking.booking.service;

import com.moviebooking.booking.dto.BookingRequest;
import com.moviebooking.booking.dto.HoldSeatsRequest;
import com.moviebooking.booking.dto.HoldSeatsResponse;
import com.moviebooking.booking.exception.SeatUnavailableException;
import com.moviebooking.booking.model.Booking;
import com.moviebooking.booking.model.Seat;
import com.moviebooking.booking.repository.BookingRepository;
import com.moviebooking.booking.repository.SeatRepository;
import com.moviebooking.common.enums.BookingStatus;
import com.moviebooking.common.enums.SeatCategory;
import com.moviebooking.common.enums.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class BookingServiceConcurrencyTest {
    
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
    private BookingService bookingService;
    
    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    private Seat testSeat;
    
    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        
        testSeat = seatRepository.save(Seat.builder()
            .showId(1L)
            .seatNumber("A1")
            .rowLabel("A")
            .category(SeatCategory.NORMAL)
            .status(SeatStatus.AVAILABLE)
            .version(0)
            .createdAt(Instant.now())
            .build());
    }
    
    @Test
    void testHoldSeats_Success() {
        HoldSeatsRequest request = HoldSeatsRequest.builder()
            .sessionId(UUID.randomUUID().toString())
            .showId(1L)
            .seatIds(List.of(testSeat.getId()))
            .userId(1L)
            .build();
        
        HoldSeatsResponse response = bookingService.holdSeats(request);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSeatIds()).containsExactly(testSeat.getId());
        assertThat(response.getExpiresAt()).isAfter(Instant.now());
        
        Seat updatedSeat = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }
    
    @Test
    void testHoldSeats_AlreadyHeld_ShouldFail() {
        testSeat.setStatus(SeatStatus.HELD);
        seatRepository.save(testSeat);
        
        HoldSeatsRequest request = HoldSeatsRequest.builder()
            .sessionId(UUID.randomUUID().toString())
            .showId(1L)
            .seatIds(List.of(testSeat.getId()))
            .userId(1L)
            .build();
        
        assertThatThrownBy(() -> bookingService.holdSeats(request))
            .isInstanceOf(SeatUnavailableException.class);
    }
    
    @Test
    void testCreateBooking_Success() {
        String sessionId = UUID.randomUUID().toString();
        
        HoldSeatsRequest holdRequest = HoldSeatsRequest.builder()
            .sessionId(sessionId)
            .showId(1L)
            .seatIds(List.of(testSeat.getId()))
            .userId(1L)
            .build();
        
        bookingService.holdSeats(holdRequest);
        
        BookingRequest bookingRequest = BookingRequest.builder()
            .userId(1L)
            .showId(1L)
            .seatIds(List.of(testSeat.getId()))
            .sessionId(sessionId)
            .build();
        
        Booking booking = bookingService.createBooking(bookingRequest);
        
        assertThat(booking).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getBookingReference()).isNotNull();
        assertThat(booking.getSeats()).hasSize(1);
    }
    
    @Test
    void testConcurrentBooking_OnlyOneSucceeds() throws InterruptedException {
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    String sessionId = UUID.randomUUID().toString();
                    HoldSeatsRequest holdRequest = HoldSeatsRequest.builder()
                        .sessionId(sessionId)
                        .showId(1L)
                        .seatIds(List.of(testSeat.getId()))
                        .userId((long) Thread.currentThread().getId())
                        .build();
                    
                    bookingService.holdSeats(holdRequest);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        log.info("Concurrent test results - Success: {}, Failures: {}", successCount.get(), failureCount.get());
        
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(numThreads - 1);
    }
}
