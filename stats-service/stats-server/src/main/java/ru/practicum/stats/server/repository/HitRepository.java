package ru.practicum.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.stats.server.model.Hit;

public interface HitRepository extends JpaRepository<Hit, Long> {
}