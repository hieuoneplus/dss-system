package com.example.nsga;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nsga")
public class Nsga2Controller {
    private final Nsga2Service service;
    public Nsga2Controller(Nsga2Service service){ this.service = service; }
    @PostMapping("/run")
    public ResponseEntity<Nsga2Result> run(@RequestBody Nsga2Request req){
        return ResponseEntity.ok(service.run(req));
    }
}
