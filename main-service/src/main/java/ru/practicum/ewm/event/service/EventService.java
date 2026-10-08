package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.dto.UserEventStateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.service.ParticipationRequestService;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.service.UserService;
import ru.practicum.ewm.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ParticipationRequestService requestService;

    @Transactional
    public EventFullDto createEvent(
            long userId,
            NewEventDto dto
    ) {
        validateEventDate(dto.getEventDate());

        User user =
                userService.getUserEntity(userId);

        Category category =
                categoryService.getCategoryEntity(
                        dto.getCategory()
                );

        Event event = EventMapper.toEvent(
                dto,
                user,
                category
        );

        Event savedEvent =
                eventRepository.save(event);

        return EventMapper.toEventFullDto(
                savedEvent,
                0L,
                0L
        );
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(
            long userId,
            int from,
            int size
    ) {
        userService.getUserEntity(userId);

        OffsetPageRequest pageable =
                new OffsetPageRequest(
                        from,
                        size,
                        Sort.by("id").ascending()
                );

        List<Event> events =
                eventRepository.findAllByInitiatorId(
                        userId,
                        pageable
                );

        Map<Long, Long> confirmedCounts =
                requestService.getConfirmedCounts(
                        events.stream()
                                .map(Event::getId)
                                .toList()
                );

        return events.stream()
                .map(event ->
                        EventMapper.toEventShortDto(
                                event,
                                confirmedCounts
                                        .getOrDefault(
                                                event.getId(),
                                                0L
                                        ),
                                0L
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(
            long userId,
            long eventId
    ) {
        Event event =
                getUserEventEntity(
                        userId,
                        eventId
                );

        long confirmedRequests =
                requestService.getConfirmedCount(
                        eventId
                );

        return EventMapper.toEventFullDto(
                event,
                confirmedRequests,
                0L
        );
    }

    @Transactional
    public EventFullDto updateUserEvent(
            long userId,
            long eventId,
            UpdateEventUserRequest request
    ) {
        Event event =
                getUserEventEntity(
                        userId,
                        eventId
                );

        if (event.getState() != EventState.PENDING
                && event.getState()
                != EventState.CANCELED) {
            throw new ConflictException(
                    "Only pending or canceled "
                            + "events can be changed"
            );
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(
                    request.getAnnotation()
            );
        }

        if (request.getCategory() != null) {
            Category category =
                    categoryService.getCategoryEntity(
                            request.getCategory()
                    );

            event.setCategory(category);
        }

        if (request.getDescription() != null) {
            event.setDescription(
                    request.getDescription()
            );
        }

        if (request.getEventDate() != null) {
            validateEventDate(
                    request.getEventDate()
            );

            event.setEventDate(
                    request.getEventDate()
            );
        }

        if (request.getLocation() != null) {
            event.setLat(
                    request.getLocation().getLat()
            );

            event.setLon(
                    request.getLocation().getLon()
            );
        }

        if (request.getPaid() != null) {
            event.setPaid(
                    request.getPaid()
            );
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(
                    request.getParticipantLimit()
            );
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(
                    request.getRequestModeration()
            );
        }

        if (request.getTitle() != null) {
            event.setTitle(
                    request.getTitle()
            );
        }

        changeState(
                event,
                request.getStateAction()
        );

        long confirmedRequests =
                requestService.getConfirmedCount(
                        eventId
                );

        return EventMapper.toEventFullDto(
                event,
                confirmedRequests,
                0L
        );
    }

    @Transactional(readOnly = true)
    public Event getEventEntity(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Event with id="
                                        + eventId
                                        + " was not found"
                        )
                );
    }

    private Event getUserEventEntity(
            long userId,
            long eventId
    ) {
        return eventRepository
                .findByIdAndInitiatorId(
                        eventId,
                        userId
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Event with id="
                                        + eventId
                                        + " was not found"
                        )
                );
    }

    private void validateEventDate(
            LocalDateTime eventDate
    ) {
        LocalDateTime minimumDate =
                LocalDateTime.now()
                        .plusHours(2);

        if (eventDate.isBefore(minimumDate)) {
            throw new ConflictException(
                    "Event date must be at least "
                            + "2 hours from now"
            );
        }
    }

    private void changeState(
            Event event,
            UserEventStateAction stateAction
    ) {
        if (stateAction == null) {
            return;
        }

        if (stateAction
                == UserEventStateAction.SEND_TO_REVIEW) {
            event.setState(
                    EventState.PENDING
            );
        }

        if (stateAction
                == UserEventStateAction.CANCEL_REVIEW) {
            event.setState(
                    EventState.CANCELED
            );
        }
    }
}