package ru.practicum.ewm.event.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class EventSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Event> findAdminEvents(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    ) {
        CriteriaBuilder builder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> query =
                builder.createQuery(Event.class);

        Root<Event> event =
                query.from(Event.class);

        event.fetch(
                "category",
                JoinType.INNER
        );

        event.fetch(
                "initiator",
                JoinType.INNER
        );

        List<Predicate> predicates =
                new ArrayList<>();

        if (users != null && !users.isEmpty()) {
            predicates.add(
                    event.get("initiator")
                            .get("id")
                            .in(users)
            );
        }

        if (states != null && !states.isEmpty()) {
            predicates.add(
                    event.get("state")
                            .in(states)
            );
        }

        if (categories != null
                && !categories.isEmpty()) {
            predicates.add(
                    event.get("category")
                            .get("id")
                            .in(categories)
            );
        }

        if (rangeStart != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(
                            event.<LocalDateTime>
                                    get("eventDate"),
                            rangeStart
                    )
            );
        }

        if (rangeEnd != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(
                            event.<LocalDateTime>
                                    get("eventDate"),
                            rangeEnd
                    )
            );
        }

        query.where(
                predicates.toArray(
                        Predicate[]::new
                )
        );

        query.orderBy(
                builder.asc(
                        event.get("id")
                )
        );

        TypedQuery<Event> typedQuery =
                entityManager.createQuery(query);

        typedQuery.setFirstResult(from);
        typedQuery.setMaxResults(size);

        return typedQuery.getResultList();
    }

    public List<Event> findPublicEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        CriteriaBuilder builder =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> query =
                builder.createQuery(Event.class);

        Root<Event> event =
                query.from(Event.class);

        event.fetch(
                "category",
                JoinType.INNER
        );

        event.fetch(
                "initiator",
                JoinType.INNER
        );

        List<Predicate> predicates =
                new ArrayList<>();

        predicates.add(
                builder.equal(
                        event.get("state"),
                        EventState.PUBLISHED
                )
        );

        if (text != null) {
            String searchText =
                    "%"
                            + text.toLowerCase(
                            Locale.ROOT
                    )
                            + "%";

            Predicate annotationContains =
                    builder.like(
                            builder.lower(
                                    event.<String>
                                            get("annotation")
                            ),
                            searchText
                    );

            Predicate descriptionContains =
                    builder.like(
                            builder.lower(
                                    event.<String>
                                            get("description")
                            ),
                            searchText
                    );

            predicates.add(
                    builder.or(
                            annotationContains,
                            descriptionContains
                    )
            );
        }

        if (categories != null
                && !categories.isEmpty()) {
            predicates.add(
                    event.get("category")
                            .get("id")
                            .in(categories)
            );
        }

        if (paid != null) {
            predicates.add(
                    builder.equal(
                            event.get("paid"),
                            paid
                    )
            );
        }

        if (rangeStart != null) {
            predicates.add(
                    builder.greaterThanOrEqualTo(
                            event.<LocalDateTime>
                                    get("eventDate"),
                            rangeStart
                    )
            );
        }

        if (rangeEnd != null) {
            predicates.add(
                    builder.lessThanOrEqualTo(
                            event.<LocalDateTime>
                                    get("eventDate"),
                            rangeEnd
                    )
            );
        }

        query.where(
                predicates.toArray(
                        Predicate[]::new
                )
        );

        query.orderBy(
                builder.asc(
                        event.get("id")
                )
        );

        return entityManager
                .createQuery(query)
                .getResultList();
    }
}