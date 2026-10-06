package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.repository.HitRepository;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final HitRepository hitRepository;

    public void saveHit(EndpointHit endpointHit) {
        Hit hit = new Hit();

        hit.setApp(endpointHit.getApp());
        hit.setUri(endpointHit.getUri());
        hit.setIp(endpointHit.getIp());
        hit.setTimestamp(endpointHit.getTimestamp());

        hitRepository.save(hit);
    }
}