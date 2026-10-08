package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

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

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        boolean hasUris = uris != null && !uris.isEmpty();

        if (unique) {
            if (hasUris) {
                return hitRepository.findUniqueStatsByUris(start, end, uris);
            }

            return hitRepository.findUniqueStats(start, end);
        }

        if (hasUris) {
            return hitRepository.findStatsByUris(start, end, uris);
        }

        return hitRepository.findStats(start, end);
    }
}