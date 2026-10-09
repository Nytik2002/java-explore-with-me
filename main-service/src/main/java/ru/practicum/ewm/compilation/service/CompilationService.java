package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.CompilationMapper;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.service.EventStatisticsService;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.service.ParticipationRequestService;
import ru.practicum.ewm.util.OffsetPageRequest;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestService requestService;
    private final EventStatisticsService statisticsService;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        Set<Event> events = getEvents(dto.getEvents());

        Compilation compilation = CompilationMapper.toCompilation(dto, events);

        Compilation savedCompilation = compilationRepository.save(compilation);

        return toDto(savedCompilation);
    }

    @Transactional
    public CompilationDto updateCompilation(long compilationId, UpdateCompilationRequest request) {
        Compilation compilation = getCompilationEntity(compilationId);

        if (request.getEvents() != null) {
            compilation.setEvents(getEvents(request.getEvents()));
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }

        Compilation savedCompilation = compilationRepository.save(compilation);

        return toDto(savedCompilation);
    }

    @Transactional
    public void deleteCompilation(long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));

        compilationRepository.delete(compilation);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        OffsetPageRequest pageable = new OffsetPageRequest(from, size, Sort.by("id").ascending());

        List<Long> ids;

        if (pinned == null) {
            ids = compilationRepository.findCompilationIds(pageable);
        } else {
            ids = compilationRepository.findCompilationIdsByPinned(pinned, pageable);
        }

        if (ids.isEmpty()) {
            return List.of();
        }

        List<Compilation> compilations = compilationRepository.findAllWithEventsByIdIn(ids);

        compilations.sort(Comparator.comparing(Compilation::getId));

        return toDtos(compilations);
    }

    @Transactional(readOnly = true)
    public CompilationDto getCompilation(long compilationId) {
        Compilation compilation = getCompilationEntity(compilationId);

        return toDto(compilation);
    }

    private Compilation getCompilationEntity(long compilationId) {
        return compilationRepository.findWithEventsById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilationId + " was not found"));
    }

    private Set<Event> getEvents(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        return new HashSet<>(eventRepository.findAllByIdIn(eventIds));
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Long> eventIds = compilation.getEvents().stream().map(Event::getId).toList();

        Map<Long, Long> confirmedCounts = requestService.getConfirmedCounts(eventIds);

        Map<Long, Long> views = statisticsService.getViews(eventIds);

        return CompilationMapper.toDto(compilation, confirmedCounts, views);
    }

    private List<CompilationDto> toDtos(List<Compilation> compilations) {
        List<Long> eventIds =
                compilations.stream().flatMap(compilation -> compilation.getEvents().stream())
                        .map(Event::getId).distinct().toList();

        Map<Long, Long> confirmedCounts = requestService.getConfirmedCounts(eventIds);

        Map<Long, Long> views = statisticsService.getViews(eventIds);

        return compilations.stream().map(compilation -> CompilationMapper.toDto(
                compilation, confirmedCounts, views)).toList();
    }
}