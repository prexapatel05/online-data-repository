package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rating entity for user ratings on datasets.
 * 
 * Maps to the "rates" table in PostgreSQL.
 * Each user can rate each dataset only once (unique constraint on user_id, dataset_id).
 * Rating values are between 1 and 5.
 */
@Entity
@Table(
    name = "rates",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "dataset_id"},
        name = "uq_user_dataset_rating"
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Integer ratingId;

    @Column(name = "rating_value", nullable = false)
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    @NotNull(message = "Rating value cannot be null")
    private Integer ratingValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;
}
