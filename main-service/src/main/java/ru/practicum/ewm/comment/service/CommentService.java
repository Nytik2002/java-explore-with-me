package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.CommentMapper;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.service.UserService;
import ru.practicum.ewm.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final EventService eventService;

    @Transactional
    public CommentDto createComment(
            long userId,
            long eventId,
            NewCommentDto dto
    ) {
        User author = userService.getUserEntity(userId);
        Event event = eventService.getEventEntity(eventId);

        Comment comment = CommentMapper.toComment(dto, author, event);
        Comment savedComment = commentRepository.save(comment);

        return CommentMapper.toCommentDto(savedComment);
    }

    @Transactional
    public CommentDto updateComment(
            long userId,
            long commentId,
            UpdateCommentDto dto
    ) {
        userService.getUserEntity(userId);

        Comment comment = getCommentEntity(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException(
                    "Only the author can update this comment"
            );
        }

        comment.setText(dto.getText());
        comment.setUpdatedOn(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );

        Comment savedComment = commentRepository.save(comment);

        return CommentMapper.toCommentDto(savedComment);
    }

    @Transactional
    public void deleteComment(
            long userId,
            long commentId
    ) {
        userService.getUserEntity(userId);

        Comment comment = getCommentEntity(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException(
                    "Only the author can delete this comment"
            );
        }

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public CommentDto getComment(long commentId) {
        Comment comment = getCommentEntity(commentId);

        return CommentMapper.toCommentDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getEventComments(
            long eventId,
            int from,
            int size
    ) {
        eventService.getEventEntity(eventId);

        OffsetPageRequest pageable = new OffsetPageRequest(
                from,
                size,
                Sort.by("createdOn").descending()
        );

        return commentRepository.findAllByEventId(eventId, pageable)
                .stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(
            long userId,
            int from,
            int size
    ) {
        userService.getUserEntity(userId);

        OffsetPageRequest pageable = new OffsetPageRequest(
                from,
                size,
                Sort.by("createdOn").descending()
        );

        return commentRepository.findAllByAuthorId(userId, pageable)
                .stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    private Comment getCommentEntity(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " was not found"
                ));
    }
}