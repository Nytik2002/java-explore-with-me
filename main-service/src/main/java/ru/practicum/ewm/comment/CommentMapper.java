package ru.practicum.ewm.comment;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.UserMapper;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class CommentMapper {

    public Comment toComment(
            NewCommentDto dto,
            User author,
            Event event
    ) {
        return Comment.builder()
                .text(dto.getText())
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

    public CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .build();
    }
}