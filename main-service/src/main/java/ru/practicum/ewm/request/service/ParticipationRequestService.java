package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.ParticipationRequestMapper;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.RequestUpdateStatus;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(
            long userId
    ) {
        userService.getUserEntity(userId);

        return requestRepository
                .findAllByRequesterIdOrderByIdAsc(userId)
                .stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Transactional
    public ParticipationRequestDto createRequest(
            long userId,
            long eventId
    ) {
        User requester =
                userService.getUserEntity(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + eventId
                                + " was not found"
                ));

        if (event.getInitiator().getId() == userId) {
            throw new ConflictException(
                    "Event initiator cannot "
                            + "request participation "
                            + "in own event"
            );
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException(
                    "Cannot participate "
                            + "in an unpublished event"
            );
        }

        if (requestRepository
                .existsByRequesterIdAndEventId(
                        userId,
                        eventId
                )) {
            throw new ConflictException(
                    "Participation request already exists"
            );
        }

        long confirmedRequests =
                getConfirmedCount(eventId);

        if (event.getParticipantLimit() > 0
                && confirmedRequests
                >= event.getParticipantLimit()) {
            throw new ConflictException(
                    "The participant limit has been reached"
            );
        }

        RequestStatus status = RequestStatus.PENDING;

        if (event.getParticipantLimit() == 0
                || !event.isRequestModeration()) {
            status = RequestStatus.CONFIRMED;
        }

        ParticipationRequest request =
                ParticipationRequest.builder()
                        .created(LocalDateTime.now())
                        .event(event)
                        .requester(requester)
                        .status(status)
                        .build();

        ParticipationRequest savedRequest =
                requestRepository.save(request);

        return ParticipationRequestMapper.toDto(
                savedRequest
        );
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(
            long userId,
            long requestId
    ) {
        userService.getUserEntity(userId);

        ParticipationRequest request =
                requestRepository
                        .findByIdAndRequesterId(
                                requestId,
                                userId
                        )
                        .orElseThrow(
                                () -> new NotFoundException(
                                        "Request with id="
                                                + requestId
                                                + " was not found"
                                )
                        );

        request.setStatus(RequestStatus.CANCELED);

        ParticipationRequest savedRequest =
                requestRepository.save(request);

        return ParticipationRequestMapper.toDto(
                savedRequest
        );
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(
            long userId,
            long eventId
    ) {
        getOwnedEvent(
                userId,
                eventId
        );

        return requestRepository
                .findAllByEventIdOrderByIdAsc(eventId)
                .stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
            long userId,
            long eventId,
            EventRequestStatusUpdateRequest updateRequest
    ) {
        Event event = getOwnedEvent(
                userId,
                eventId
        );

        if (updateRequest == null
                || updateRequest.getStatus() == null
                || updateRequest.getRequestIds() == null) {
            throw new IllegalArgumentException(
                    "Request ids and status must be specified"
            );
        }

        if (updateRequest.getRequestIds().isEmpty()) {
            return emptyResult();
        }

        Set<Long> uniqueIds =
                updateRequest.getRequestIds()
                        .stream()
                        .collect(Collectors.toSet());

        List<ParticipationRequest> requests =
                requestRepository.findAllByEventIdAndIdIn(
                        eventId,
                        uniqueIds
                );

        if (requests.size() != uniqueIds.size()) {
            throw new NotFoundException(
                    "One or more participation "
                            + "requests were not found"
            );
        }

        for (ParticipationRequest request : requests) {
            if (request.getStatus()
                    != RequestStatus.PENDING) {
                throw new IllegalArgumentException(
                        "Request must have status PENDING"
                );
            }
        }

        if (updateRequest.getStatus()
                == RequestUpdateStatus.REJECTED) {
            return rejectRequests(requests);
        }

        return confirmRequests(
                event,
                requests
        );
    }

    @Transactional(readOnly = true)
    public long getConfirmedCount(long eventId) {
        return requestRepository.countByEventIdAndStatus(
                eventId,
                RequestStatus.CONFIRMED
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getConfirmedCounts(
            Collection<Long> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> rows =
                requestRepository.countByEventIdsAndStatus(
                        eventIds,
                        RequestStatus.CONFIRMED
                );

        Map<Long, Long> result = new HashMap<>();

        for (Object[] row : rows) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];

            result.put(eventId, count);
        }

        return result;
    }

    private EventRequestStatusUpdateResult confirmRequests(
            Event event,
            List<ParticipationRequest> requests
    ) {
        long confirmedCount =
                getConfirmedCount(event.getId());

        int participantLimit =
                event.getParticipantLimit();

        if (participantLimit > 0
                && confirmedCount + requests.size()
                > participantLimit) {
            throw new ConflictException(
                    "The participant limit has been reached"
            );
        }

        requests.forEach(
                request -> request.setStatus(
                        RequestStatus.CONFIRMED
                )
        );

        List<ParticipationRequest> confirmed =
                requestRepository.saveAll(requests);

        List<ParticipationRequest> rejected =
                new ArrayList<>();

        if (participantLimit > 0
                && confirmedCount + confirmed.size()
                == participantLimit) {
            rejected =
                    requestRepository
                            .findAllByEventIdAndStatus(
                                    event.getId(),
                                    RequestStatus.PENDING
                            );

            rejected.forEach(
                    request -> request.setStatus(
                            RequestStatus.REJECTED
                    )
            );

            requestRepository.saveAll(rejected);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(
                        confirmed.stream()
                                .map(
                                        ParticipationRequestMapper
                                                ::toDto
                                )
                                .toList()
                )
                .rejectedRequests(
                        rejected.stream()
                                .map(
                                        ParticipationRequestMapper
                                                ::toDto
                                )
                                .toList()
                )
                .build();
    }

    private EventRequestStatusUpdateResult rejectRequests(
            List<ParticipationRequest> requests
    ) {
        requests.forEach(
                request -> request.setStatus(
                        RequestStatus.REJECTED
                )
        );

        List<ParticipationRequest> rejected =
                requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of())
                .rejectedRequests(
                        rejected.stream()
                                .map(
                                        ParticipationRequestMapper
                                                ::toDto
                                )
                                .toList()
                )
                .build();
    }

    private EventRequestStatusUpdateResult emptyResult() {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of())
                .rejectedRequests(List.of())
                .build();
    }

    private Event getOwnedEvent(
            long userId,
            long eventId
    ) {
        return eventRepository
                .findByIdAndInitiatorId(
                        eventId,
                        userId
                )
                .orElseThrow(
                        () -> new NotFoundException(
                                "Event with id="
                                        + eventId
                                        + " was not found"
                        )
                );
    }
}