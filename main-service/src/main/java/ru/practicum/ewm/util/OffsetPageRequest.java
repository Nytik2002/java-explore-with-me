package ru.practicum.ewm.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageRequest implements Pageable {

    private final int offset;
    private final int pageSize;
    private final Sort sort;

    public OffsetPageRequest(int offset, int pageSize, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }

        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }

        this.offset = offset;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return offset / pageSize;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(
                offset + pageSize,
                pageSize,
                sort
        );
    }

    @Override
    public Pageable previousOrFirst() {
        if (!hasPrevious()) {
            return first();
        }

        return new OffsetPageRequest(
                Math.max(offset - pageSize, 0),
                pageSize,
                sort
        );
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, pageSize, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(
                pageNumber * pageSize,
                pageSize,
                sort
        );
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}