package ru.practicum.ewm.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@Validated
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable long userId,
            @RequestParam long eventId,
            @Valid @RequestBody NewCommentDto dto
    ) {
        return commentService.createComment(
                userId,
                eventId,
                dto
        );
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(
            @PathVariable long userId,
            @PathVariable long commentId,
            @Valid @RequestBody UpdateCommentDto dto
    ) {
        return commentService.updateComment(
                userId,
                commentId,
                dto
        );
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable long userId,
            @PathVariable long commentId
    ) {
        commentService.deleteComment(
                userId,
                commentId
        );
    }

    @GetMapping
    public List<CommentDto> getUserComments(
            @PathVariable long userId,
            @RequestParam(defaultValue = "0")
            @PositiveOrZero
            int from,
            @RequestParam(defaultValue = "10")
            @Positive
            int size
    ) {
        return commentService.getUserComments(
                userId,
                from,
                size
        );
    }
}