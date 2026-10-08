package ru.practicum.ewm.compilation.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.compilation.model.Compilation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompilationRepository
        extends JpaRepository<Compilation, Long> {

    @Query("""
            select c.id
            from Compilation c
            order by c.id
            """)
    List<Long> findCompilationIds(
            Pageable pageable
    );

    @Query("""
            select c.id
            from Compilation c
            where c.pinned = :pinned
            order by c.id
            """)
    List<Long> findCompilationIdsByPinned(
            @Param("pinned") boolean pinned,
            Pageable pageable
    );

    @Query("""
            select distinct c
            from Compilation c
            left join fetch c.events e
            left join fetch e.category
            left join fetch e.initiator
            where c.id in :ids
            """)
    List<Compilation> findAllWithEventsByIdIn(
            @Param("ids") Collection<Long> ids
    );

    @Query("""
            select distinct c
            from Compilation c
            left join fetch c.events e
            left join fetch e.category
            left join fetch e.initiator
            where c.id = :id
            """)
    Optional<Compilation> findWithEventsById(
            @Param("id") long id
    );
}