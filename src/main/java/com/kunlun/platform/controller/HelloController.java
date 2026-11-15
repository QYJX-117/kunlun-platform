package com.kunlun.platform.controller;

import com.kunlun.platform.entity.Station;
import com.kunlun.platform.repository.StationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HelloController {

    private final StationRepository stationRepository;

    public HelloController(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping("/hello")
    public List<Station> hello() {
        return stationRepository.findAll();
    }
}
