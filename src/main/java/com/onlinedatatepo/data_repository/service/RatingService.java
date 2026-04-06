package com.onlinedatatepo.data_repository.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.Rating;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.RatingRepository;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final DatasetService datasetService;
    private final NotificationService notificationService;
    private final MailService mailService;

    public RatingService(RatingRepository ratingRepository,
                         DatasetService datasetService,
                         NotificationService notificationService,
                         MailService mailService) {
        this.ratingRepository = ratingRepository;
        this.datasetService = datasetService;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Transactional
    public Rating rateDataset(User actor, Integer datasetId, int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        Dataset dataset = datasetService.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found."));

        if (!datasetService.canAccessDataset(actor, dataset)) {
            throw new IllegalArgumentException("You do not have permission to rate this dataset.");
        }

        Rating rating = ratingRepository.findByUser_UserIdAndDataset_DatasetId(actor.getUserId(), datasetId)
                .orElseGet(() -> {
                    Rating newRating = new Rating();
                    newRating.setUser(actor);
                    newRating.setDataset(dataset);
                    return newRating;
                });

        boolean updated = rating.getRatingId() != null;
        rating.setRatingValue(value);
        Rating saved = ratingRepository.save(rating);

        User owner = dataset.getUser();
        notificationService.notifyRating(owner, actor, dataset, saved, updated);
        mailService.sendRatingNotification(owner, actor, dataset, saved, updated);

        return saved;
    }

    @Transactional(readOnly = true)
    public Double averageRating(Integer datasetId) {
        return ratingRepository.getAverageRatingForDataset(datasetId);
    }

    @Transactional(readOnly = true)
    public long ratingCount(Integer datasetId) {
        return ratingRepository.countByDataset_DatasetId(datasetId);
    }

    @Transactional(readOnly = true)
    public Integer currentUserRating(Integer userId, Integer datasetId) {
        return ratingRepository.findByUser_UserIdAndDataset_DatasetId(userId, datasetId)
                .map(Rating::getRatingValue)
                .orElse(null);
    }
}
