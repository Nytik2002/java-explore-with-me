package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterIdOrderByIdAsc(long requesterId);

    List<ParticipationRequest> findAllByEventIdOrderByIdAsc(long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(long requestId, long requesterId);

    boolean existsByRequesterIdAndEventId(long requesterId, long eventId);

    long countByEventIdAndStatus(long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByEventIdAndIdIn(long eventId, Collection<Long> ids);

    List<ParticipationRequest> findAllByEventIdAndStatus(long eventId, RequestStatus status);

    @Query("""
            select r.event.id, count(r.id)
            from ParticipationRequest r
            where r.event.id in :eventIds
              and r.status = :status
            group by r.event.id
            """)
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") Collection<Long> eventIds,
                                            @Param("status") RequestStatus status);
}