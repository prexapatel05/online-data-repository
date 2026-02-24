package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bookmark entity for user-saved datasets.
 * 
 * Maps to the "bookmarks" table with composite primary key (user_id, dataset_id).
 * Allows users to bookmark datasets for quick access.
 */
@Entity
@Table(name = "bookmarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {

    @EmbeddedId
    private BookmarkId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("datasetId")
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;
}
