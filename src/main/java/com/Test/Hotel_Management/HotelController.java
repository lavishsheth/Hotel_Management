package com.Test.Hotel_Management;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HotelController {

    public record BookRequest(int count) {}

    private final HotelService service;

    public HotelController(HotelService service) {
        this.service = service;
    }

    @GetMapping("/rooms")
    public HotelService.State rooms() {
        return service.state();
    }

    @PostMapping("/book")
    public HotelService.BookingResult book(@RequestBody BookRequest request) {
        return service.book(request.count());
    }

    @PostMapping("/random")
    public HotelService.State random() {
        return service.randomOccupancy();
    }

    @PostMapping("/reset")
    public HotelService.State reset() {
        return service.reset();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handle(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
