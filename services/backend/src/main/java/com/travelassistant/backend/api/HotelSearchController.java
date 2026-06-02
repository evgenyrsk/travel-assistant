package com.travelassistant.backend.api;

import com.travelassistant.backend.api.model.ApiModels.HotelOffersResponse;
import com.travelassistant.backend.api.model.ApiModels.HotelSearchRequest;
import com.travelassistant.backend.api.model.ApiModels.HotelSearchResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel-searches")
class HotelSearchController {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    HotelSearchResponse createHotelSearch(@Valid @RequestBody HotelSearchRequest request) {
        return SkeletonResponses.hotelSearch(request);
    }

    @GetMapping("/{searchId}/offers")
    HotelOffersResponse getHotelOffers(@PathVariable String searchId) {
        return SkeletonResponses.hotelOffers(searchId);
    }
}

