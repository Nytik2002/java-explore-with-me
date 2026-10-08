package ru.practicum.ewm.compilation;

import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    public static Compilation toCompilation(
            NewCompilationDto dto,
            Set<Event> events
    ) {
        return Compilation.builder()
                .events(events)
                .pinned(dto.isPinned())
                .title(dto.getTitle())
                .build();
    }

    public static CompilationDto toDto(
            Compilation compilation,
            Map<Long, Long> confirmedCounts,
            Map<Long, Long> views
    ) {
        List<EventShortDto> events =
                compilation.getEvents()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        Event::getId
                                )
                        )
                        .map(event ->
                                EventMapper.toEventShortDto(
                                        event,
                                        confirmedCounts
                                                .getOrDefault(
                                                        event.getId(),
                                                        0L
                                                ),
                                        views.getOrDefault(
                                                event.getId(),
                                                0L
                                        )
                                )
                        )
                        .toList();

        return CompilationDto.builder()
                .id(compilation.getId())
                .events(events)
                .pinned(compilation.isPinned())
                .title(compilation.getTitle())
                .build();
    }
}