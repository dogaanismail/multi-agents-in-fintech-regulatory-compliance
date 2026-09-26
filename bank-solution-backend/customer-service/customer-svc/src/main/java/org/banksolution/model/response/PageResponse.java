package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(description = "Paged list of results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    @Schema(description = "Items on this page")
    private List<T> content;

    @Schema(description = "Pagination details")
    private PageMetadata page;

    @Schema(description = "Pagination details of a page")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageMetadata {

        @Schema(description = "Requested page size", example = "20")
        private int size;

        @Schema(description = "Zero-based page index", example = "0")
        private int number;

        @Schema(description = "Total number of items", example = "125")
        private long totalElements;

        @Schema(description = "Total number of pages", example = "7")
        private int totalPages;

        @Schema(description = "Whether this is the first page")
        private boolean first;

        @Schema(description = "Whether this is the last page")
        private boolean last;

        @Schema(description = "Whether this page has no items")
        private boolean empty;

    }
}

