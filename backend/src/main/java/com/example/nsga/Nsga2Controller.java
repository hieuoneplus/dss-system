package com.example.nsga;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Nsga2Controller {

    private final CityRepository repo;
    private final Nsga2Service service;
    public Nsga2Controller(CityRepository repo, Nsga2Service service){
        this.repo = repo;
        this.service = service;
    }
    @PostMapping("/nsga/run")
    public ResponseEntity<Nsga2Result> run(@RequestBody Nsga2Request req){
        return ResponseEntity.ok(service.run(req));
    }

    @GetMapping("/city")
    public ResponseEntity<List<CityRepository.City>> list(){
        return ResponseEntity.ok(repo.findAll());
    }
}
