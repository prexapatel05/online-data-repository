package com.onlinedatatepo.data_repository.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinedatatepo.data_repository.entity.Comment;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.CommentRepository;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final DatasetService datasetService;
    private final NotificationService notificationService;
    private final MailService mailService;

    public CommentService(CommentRepository commentRepository,
                          DatasetService datasetService,
                          NotificationService notificationService,
                          MailService mailService) {
        this.commentRepository = commentRepository;
        this.datasetService = datasetService;
        this.notificationService = notificationService;
        this.mailService = mailService;
    }

    @Transactional(readOnly = true)
    public List<Comment> getTopLevelComments(Integer datasetId) {
        return commentRepository.findTopLevelCommentsByDatasetIdOrderByCreatedAtDesc(datasetId);
    }

    @Transactional(readOnly = true)
    public long countComments(Integer datasetId) {
        return commentRepository.countByDataset_DatasetId(datasetId);
    }

    @Transactional
    public Comment addComment(User actor, Integer datasetId, Integer parentId, String content) {
        Dataset dataset = datasetService.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found."));

        if (!datasetService.canAccessDataset(actor, dataset)) {
            throw new IllegalArgumentException("You do not have permission to comment on this dataset.");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found."));
            if (parent.getDataset() == null || !datasetId.equals(parent.getDataset().getDatasetId())) {
                throw new IllegalArgumentException("Invalid reply target.");
            }
        }

        Comment comment = new Comment();
        comment.setUser(actor);
        comment.setDataset(dataset);
        comment.setParent(parent);
        comment.setContent(content.trim());
        Comment saved = commentRepository.save(comment);

        User datasetOwner = dataset.getUser();
        if (parent == null) {
            notificationService.notifyComment(datasetOwner, actor, dataset, saved);
            mailService.sendCommentNotification(datasetOwner, actor, dataset, saved, false);
        } else {
            notificationService.notifyComment(datasetOwner, actor, dataset, saved);
            mailService.sendCommentNotification(datasetOwner, actor, dataset, saved, true);

            User parentAuthor = parent.getUser();
            if (parentAuthor != null && (datasetOwner == null || !parentAuthor.getUserId().equals(datasetOwner.getUserId()))) {
                notificationService.notifyReply(parentAuthor, actor, dataset, saved);
                mailService.sendReplyNotification(parentAuthor, actor, dataset, saved);
            }
        }

        return saved;
    }
}
