package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventStatisticsService {

    private static final String APP_NAME = "ewm-main-service";

    private static final LocalDateTime STATS_START =
            LocalDateTime.of(
                    1970,
                    1,
                    1,
                    0,
                    0
            );

    private final StatsClient statsClient;

    public void saveHit(
            String uri,
            String ip
    ) {
        EndpointHit hit = EndpointHit.builder()
                .app(APP_NAME)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.saveHit(hit);
    }

    public long getViews(long eventId) {
        return getViews(List.of(eventId))
                .getOrDefault(
                        eventId,
                        0L
                );
    }

    public Map<Long, Long> getViews(
            Collection<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> uriToEventId =
                new HashMap<>();

        for (Long eventId : eventIds) {
            uriToEventId.put(
                    "/events/" + eventId,
                    eventId
            );
        }

        List<String> uris =
                new ArrayList<>(
                        uriToEventId.keySet()
                );

        List<ViewStats> statistics =
                statsClient.getStats(
                        STATS_START,
                        LocalDateTime.now(),
                        uris,
                        true
                );

        Map<Long, Long> result =
                new HashMap<>();

        if (statistics == null) {
            return result;
        }

        for (ViewStats viewStats : statistics) {
            Long eventId =
                    uriToEventId.get(
                            viewStats.getUri()
                    );

            if (eventId != null) {
                result.put(
                        eventId,
                        viewStats.getHits()
                );
            }
        }

        return result;
    }
}