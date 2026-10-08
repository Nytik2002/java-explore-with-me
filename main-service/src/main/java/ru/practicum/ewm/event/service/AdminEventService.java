package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.dto.AdminEventStateAction;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.EventSearchRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final EventSearchRepository eventSearchRepository;
    private final CategoryService categoryService;
    private final ParticipationRequestService requestService;

    @Transactional(readOnly = true)
    public List<EventFullDto> getEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        validateDateRange(rangeStart, rangeEnd);

        List<Event> events =
                eventSearchRepository.findAdminEvents(users, states, categories, rangeStart, rangeEnd, from, size);

        Map<Long, Long> confirmedCounts = requestService.getConfirmedCounts(events.stream().map(Event::getId).toList());

        return events.stream()
                .map(event -> EventMapper.toEventFullDto(event, confirmedCounts.getOrDefault(event.getId(), 0L), 0L))
                .toList();
    }

    @Transactional
    public EventFullDto updateEvent(long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        updateFields(event, request);

        changeState(event, request.getStateAction());

        Event savedEvent = eventRepository.save(event);

        long confirmedRequests = requestService.getConfirmedCount(eventId);

        return EventMapper.toEventFullDto(savedEvent, confirmedRequests, 0L);
    }

    private void updateFields(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getCategory() != null) {
            Category category = categoryService.getCategoryEntity(request.getCategory());

            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventDate() != null) {
            validateAdminEventDate(event, request.getEventDate());

            event.setEventDate(request.getEventDate());
        }

        if (request.getLocation() != null) {
            event.setLat(request.getLocation().getLat());

            event.setLon(request.getLocation().getLon());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    private void changeState(Event event, AdminEventStateAction stateAction) {
        if (stateAction == null) {
            return;
        }

        if (stateAction == AdminEventStateAction.PUBLISH_EVENT) {
            publishEvent(event);
        }

        if (stateAction == AdminEventStateAction.REJECT_EVENT) {
            rejectEvent(event);
        }
    }

    private void publishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException(
                    "Cannot publish the event because " + "it's not in the right state: " + event.getState());
        }

        LocalDateTime publicationTime = LocalDateTime.now();

        if (event.getEventDate().isBefore(publicationTime.plusHours(1))) {
            throw new ConflictException("Event date must be at least " + "1 hour after publication");
        }

        event.setState(EventState.PUBLISHED);

        event.setPublishedOn(publicationTime);
    }

    private void rejectEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Published event cannot be rejected");
        }

        event.setState(EventState.CANCELED);
    }

    private void validateAdminEventDate(Event event, LocalDateTime eventDate) {
        LocalDateTime minimumDate;

        if (event.getPublishedOn() != null) {
            minimumDate = event.getPublishedOn().plusHours(1);
        } else {
            minimumDate = LocalDateTime.now().plusHours(1);
        }

        if (eventDate.isBefore(minimumDate)) {
            throw new IllegalArgumentException("Event date must be at least " + "1 hour after publication");
        }
    }

    private void validateDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Range start must be before range end");
        }
    }
}