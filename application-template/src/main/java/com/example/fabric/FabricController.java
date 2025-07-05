package com.example.fabric;

import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.CommitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class FabricController {

    @Autowired
    private FabricService fabricService;

    @PostMapping("/basil")
    public ResponseEntity<String> createBasil(@RequestParam String id, @RequestParam String country) {
        try {
            String result = fabricService.createBasil(id, country);
            return ResponseEntity.ok(result);
        } catch (GatewayException | CommitException e) {
            return ResponseEntity.internalServerError().body("Error creating basil: " + e.getMessage());
        }
    }

    @GetMapping("/basil/{id}")
    public ResponseEntity<String> getBasil(@PathVariable String id) {
        try {
            String result = fabricService.readBasil(id);
            return ResponseEntity.ok(result);
        } catch (GatewayException e) {
            return ResponseEntity.internalServerError().body("Error getting basil: " + e.getMessage());
        }
    }


    @DeleteMapping("/basil/{id}")
    public ResponseEntity<String> deleteBasil(@PathVariable String id) {
        try {
            String result = fabricService.deleteBasil(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (GatewayException | CommitException e) {
            return ResponseEntity.internalServerError().body("Error deleting basil: " + e.getMessage());
        }
    }

    @PutMapping("/basil/{id}/state")
    public ResponseEntity<String> updateBasilState(
            @PathVariable String id,
            @RequestParam String gps,
            @RequestParam Long timestamp,
            @RequestParam String temp,
            @RequestParam String humidity,
            @RequestParam String status) {
        try {
            fabricService.updateBasilState(id, gps, timestamp, temp, humidity, status);
            return ResponseEntity.ok("Basil state updated successfully");
        } catch (GatewayException | CommitException e) {
            return ResponseEntity.internalServerError().body("Error updating basil state: " + e.getMessage());
        }
    }

    @GetMapping("/basil/{id}/history")
    public ResponseEntity<String> getBasilHistory(@PathVariable String id) {
        try {
            String result = fabricService.getBasilHistory(id);
            return ResponseEntity.ok(result);
        } catch (GatewayException e) {
            return ResponseEntity.internalServerError().body("Error getting basil history: " + e.getMessage());
        }
    }

    @PutMapping("/basil/{id}/transfer")
    public ResponseEntity<String> transferBasilOwnership(
            @PathVariable String id,
            @RequestParam String newOrgId,
            @RequestParam String newName) {
        try {
            fabricService.transferBasilOwnership(id, newOrgId, newName);
            return ResponseEntity.ok("Basil ownership transferred successfully");
        } catch (GatewayException | CommitException e) {
            return ResponseEntity.internalServerError().body("Error transferring basil ownership: " + e.getMessage());
        }
    }
} 