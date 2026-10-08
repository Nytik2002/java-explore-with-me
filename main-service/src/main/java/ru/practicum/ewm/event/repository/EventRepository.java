package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository
        extends JpaRepository<Event, Long> {

    @Override
    @EntityGraph(
            attributePaths = {
                    "category",
                    "initiator"
            }
    )
    Optional<Event> findById(Long eventId);

    @EntityGraph(
            attributePaths = {
                    "category",
                    "initiator"
            }
    )
    List<Event> findAllByIdIn(
            Collection<Long> ids
    );

    @EntityGraph(
            attributePaths = {
                    "category",
                    "initiator"
            }
    )
    List<Event> findAllByInitiatorId(
            long initiatorId,
            Pageable pageable
    );

    @EntityGraph(
            attributePaths = {
                    "category",
                    "initiator"
            }
    )
    Optional<Event> findByIdAndInitiatorId(
            long eventId,
            long initiatorId
    );

    @EntityGraph(
            attributePaths = {
                    "category",
                    "initiator"
            }
    )
    Optional<Event> findByIdAndState(
            long eventId,
            EventState state
    );

    boolean existsByCategoryId(long categoryId);
}