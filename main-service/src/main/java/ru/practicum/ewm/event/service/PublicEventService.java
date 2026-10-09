package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.EventSort;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.EventSearchRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicEventService {

    private final EventRepository eventRepository;
    private final EventSearchRepository eventSearchRepository;
    private final ParticipationRequestService requestService;
    private final EventStatisticsService statisticsService;

    @Transactional(readOnly = true)
    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart,
                                         LocalDateTime rangeEnd, boolean onlyAvailable, EventSort sort, int from,
                                         int size, String uri, String ip) {
        LocalDateTime actualRangeStart = rangeStart;

        if (actualRangeStart == null) {
            actualRangeStart = LocalDateTime.now();
        }

        validateDateRange(actualRangeStart, rangeEnd);

        List<Event> events = eventSearchRepository.findPublicEvents(text, categories, paid, actualRangeStart, rangeEnd);

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedCounts = requestService.getConfirmedCounts(eventIds);

        Map<Long, Long> views = statisticsService.getViews(eventIds);

        List<EventShortDto> result = events.stream()
                .filter(event -> isAvailable(event, confirmedCounts, onlyAvailable))
                .map(event -> EventMapper.toEventShortDto(
                        event,
                        confirmedCounts.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)
                ))
                .sorted(getComparator(sort))
                .skip(from)
                .limit(size)
                .toList();

        statisticsService.saveHit(uri, ip);

        return result;
    }

    @Transactional(readOnly = true)
    public EventFullDto getEvent(long eventId, String uri, String ip) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        statisticsService.saveHit(uri, ip);

        long confirmedRequests = requestService.getConfirmedCount(eventId);

        long views = statisticsService.getViews(eventId);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    private boolean isAvailable(Event event, Map<Long, Long> confirmedCounts, boolean onlyAvailable) {
        if (!onlyAvailable) {
            return true;
        }

        if (event.getParticipantLimit() == 0) {
            return true;
        }

        long confirmed = confirmedCounts.getOrDefault(event.getId(), 0L);

        return confirmed < event.getParticipantLimit();
    }

    private Comparator<EventShortDto> getComparator(EventSort sort) {
        if (sort == EventSort.VIEWS) {
            return Comparator.comparing(EventShortDto::getViews).reversed().thenComparing(EventShortDto::getId);
        }

        if (sort == EventSort.EVENT_DATE) {
            return Comparator.comparing(EventShortDto::getEventDate).thenComparing(EventShortDto::getId);
        }

        return Comparator.comparing(EventShortDto::getId);
    }

    private void validateDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Range start must be before range end");
        }
    }
}