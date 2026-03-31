package com.onlinedatatepo.data_repository.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DatasetTagCatalog {

    public static final List<String> TAGS = List.of(
            "Computer Science",
            "Healthcare",
            "Climate",
            "Economics",
            "Education",
            "Social Science"
    );

    private DatasetTagCatalog() {
    }

    public static String normalizeSelectedTags(List<String> selectedTags) {
        if (selectedTags == null || selectedTags.isEmpty()) {
            return null;
        }

        Set<String> allowed = new LinkedHashSet<>(TAGS);
        Set<String> normalized = selectedTags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .filter(allowed::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalized.isEmpty()) {
            return null;
        }
        return String.join(", ", normalized);
    }
}