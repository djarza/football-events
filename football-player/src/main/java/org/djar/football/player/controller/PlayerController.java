package org.djar.football.player.controller;

import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.stream.EventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/command", produces = MediaType.APPLICATION_JSON_VALUE)
public class PlayerController {

    private final EventPublisher publisher;

    public PlayerController(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> startCareer(@RequestBody NewPlayerRequest player) {
        return publisher.fire(new PlayerStartedCareer(player.getId(), player.getName()));
    }
}
