package ru.practicum.ewm.comment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.comment.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Override
    @EntityGraph(attributePaths = {"author", "event"})
    Optional<Comment> findById(Long commentId);

    @EntityGraph(attributePaths = {"author", "event"})
    List<Comment> findAllByEventId(long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "event"})
    List<Comment> findAllByAuthorId(long authorId, Pageable pageable);
}