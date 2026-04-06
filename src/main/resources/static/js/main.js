// ===== Drop Zone File Upload =====
document.addEventListener('DOMContentLoaded', function () {
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileSelected = document.getElementById('fileSelected');
    const nextBtn = document.getElementById('nextBtn');
    const uploadForm = document.getElementById('uploadForm');
    const editMode = uploadForm && uploadForm.dataset.editMode === 'true';

    if (dropZone && fileInput) {
        dropZone.addEventListener('click', function () {
            fileInput.click();
        });

        dropZone.addEventListener('dragover', function (e) {
            e.preventDefault();
            dropZone.classList.add('dragover');
        });

        dropZone.addEventListener('dragleave', function () {
            dropZone.classList.remove('dragover');
        });

        dropZone.addEventListener('drop', function (e) {
            e.preventDefault();
            dropZone.classList.remove('dragover');
            if (e.dataTransfer.files.length > 0) {
                fileInput.files = e.dataTransfer.files;
                showSelectedFiles(e.dataTransfer.files);
            }
        });

        fileInput.addEventListener('change', function () {
            if (fileInput.files.length > 0) {
                showSelectedFiles(fileInput.files);
            }
        });

        if (editMode && nextBtn) {
            nextBtn.disabled = false;
        }

        function showSelectedFiles(files) {
            if (fileSelected) {
                if (files.length === 1) {
                    const file = files[0];
                    fileSelected.textContent = 'Selected: ' + file.name + ' (' + formatSize(file.size) + ')';
                } else {
                    fileSelected.textContent = 'Selected ' + files.length + ' files';
                }
                fileSelected.style.display = 'block';
            }
            // Auto-set dataset name from filename
            var nameInput = document.getElementById('datasetName');
            if (nameInput && !editMode && nameInput.value === 'Untitled Dataset' && files.length > 0) {
                nameInput.value = files[0].name.replace(/\.[^/.]+$/, '');
            }
            if (nextBtn) {
                nextBtn.disabled = false;
            }
        }

        function formatSize(bytes) {
            if (bytes < 1024) return bytes + ' B';
            if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
            return (bytes / 1048576).toFixed(1) + ' MB';
        }
    }

    // ===== Additional Documents Drop Zone =====
    const docDropZone = document.getElementById('docDropZone');
    const docInput = document.getElementById('docInput');
    const docFileSelected = document.getElementById('docFileSelected');
    const docUploadBtn = document.getElementById('docUploadBtn');

    if (docDropZone && docInput) {
        docDropZone.addEventListener('click', function () {
            docInput.click();
        });

        docDropZone.addEventListener('dragover', function (e) {
            e.preventDefault();
            docDropZone.classList.add('dragover');
        });

        docDropZone.addEventListener('dragleave', function () {
            docDropZone.classList.remove('dragover');
        });

        docDropZone.addEventListener('drop', function (e) {
            e.preventDefault();
            docDropZone.classList.remove('dragover');
            if (e.dataTransfer.files.length > 0) {
                docInput.files = e.dataTransfer.files;
                showSelectedDocs(e.dataTransfer.files);
            }
        });

        docInput.addEventListener('change', function () {
            if (docInput.files.length > 0) {
                showSelectedDocs(docInput.files);
            }
        });

        function showSelectedDocs(files) {
            if (docFileSelected) {
                if (files.length === 1) {
                    docFileSelected.textContent = 'Selected: ' + files[0].name + ' (' + formatSize(files[0].size) + ')';
                } else {
                    docFileSelected.textContent = 'Selected ' + files.length + ' files';
                }
                docFileSelected.style.display = 'block';
            }
            if (docUploadBtn) {
                docUploadBtn.disabled = false;
            }
        }

        function formatSize(bytes) {
            if (bytes < 1024) return bytes + ' B';
            if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
            return (bytes / 1048576).toFixed(1) + ' MB';
        }
    }

    // ===== Category Tab Switching =====
    var tabs = document.querySelectorAll('.category-tab');
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            tabs.forEach(function (t) { t.classList.remove('active'); });
            tab.classList.add('active');
        });
    });

    // ===== Dataset Detail: Focus One Table At A Time =====
    var tableFocusContainers = document.querySelectorAll('.table-focus');
    tableFocusContainers.forEach(function (container) {
        var selector = container.querySelector('[data-table-selector]');
        var items = container.querySelectorAll('.table-focus-item');
        var counter = container.querySelector('[data-table-current]');
        var prevBtn = container.querySelector('[data-table-prev]');
        var nextBtn = container.querySelector('[data-table-next]');

        if (!items.length) {
            return;
        }

        function renderActive(index) {
            var safeIndex = Math.max(0, Math.min(index, items.length - 1));

            items.forEach(function (item) {
                var itemIndex = Number(item.dataset.tableIndex || 0);
                item.style.display = itemIndex === safeIndex ? 'flex' : 'none';
            });

            if (selector) {
                selector.value = String(safeIndex);
            }

            if (counter) {
                counter.textContent = String(safeIndex + 1);
            }

            if (prevBtn) {
                prevBtn.disabled = safeIndex === 0;
            }

            if (nextBtn) {
                nextBtn.disabled = safeIndex === items.length - 1;
            }
        }

        var initialIndex = selector ? Number(selector.value || 0) : 0;
        renderActive(initialIndex);

        if (selector) {
            selector.addEventListener('change', function () {
                renderActive(Number(selector.value || 0));
            });
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', function () {
                var current = selector ? Number(selector.value || 0) : 0;
                renderActive(current - 1);
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', function () {
                var current = selector ? Number(selector.value || 0) : 0;
                renderActive(current + 1);
            });
        }
    });

    // ===== Notification Bell =====
    var notificationShell = document.getElementById('notificationShell');
    var notificationBell = document.getElementById('notificationBell');
    var notificationDropdown = document.getElementById('notificationDropdown');
    var notificationBadge = document.getElementById('notificationBadge');
    var notificationList = document.getElementById('notificationList');
    var notificationMarkAll = document.getElementById('notificationMarkAll');

    if (notificationBell && notificationDropdown && notificationBadge && notificationList) {
        function escapeHtml(text) {
            return (text || '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        function refreshUnreadCount() {
            fetch('/notifications/unread-count', { credentials: 'same-origin' })
                .then(function (res) { return res.ok ? res.json() : { count: 0 }; })
                .then(function (data) {
                    var count = Number(data.count || 0);
                    if (count > 0) {
                        notificationBadge.textContent = String(count > 99 ? '99+' : count);
                        notificationBadge.style.display = 'inline-flex';
                    } else {
                        notificationBadge.style.display = 'none';
                    }
                })
                .catch(function () {
                    notificationBadge.style.display = 'none';
                });
        }

        function refreshNotifications() {
            fetch('/notifications/recent?limit=8', { credentials: 'same-origin' })
                .then(function (res) { return res.ok ? res.json() : []; })
                .then(function (items) {
                    if (!items.length) {
                        notificationList.innerHTML = '<div class="notification-empty">No notifications yet.</div>';
                        return;
                    }

                    notificationList.innerHTML = items.map(function (item) {
                        var message = escapeHtml(item.message);
                        var timestamp = escapeHtml(item.timestamp);
                        var targetUrl = encodeURIComponent(item.targetUrl || '/dashboard');
                        return '<a class="notification-item" href="/notifications/' + item.notificationId + '/read?redirect=' + targetUrl + '">' +
                            '<div class="notification-item-message">' + message + '</div>' +
                            '<div class="notification-item-time">' + timestamp + '</div>' +
                            '</a>';
                    }).join('');
                })
                .catch(function () {
                    notificationList.innerHTML = '<div class="notification-empty">Unable to load notifications.</div>';
                });
        }

        notificationBell.addEventListener('click', function () {
            var isOpen = notificationDropdown.style.display === 'block';
            notificationDropdown.style.display = isOpen ? 'none' : 'block';
            if (!isOpen) {
                refreshNotifications();
            }
        });

        if (notificationMarkAll) {
            notificationMarkAll.addEventListener('click', function () {
                fetch('/notifications/read-all', { credentials: 'same-origin' })
                    .then(function () {
                        refreshUnreadCount();
                        refreshNotifications();
                    });
            });
        }

        document.addEventListener('click', function (event) {
            if (notificationShell && !notificationShell.contains(event.target)) {
                notificationDropdown.style.display = 'none';
            }
        });

        refreshUnreadCount();
        setInterval(refreshUnreadCount, 30000);
    }

    // ===== Discussion Comments: Reply Button & Collapsible Replies (with event delegation) =====
    // Use event delegation for dynamically loaded content
    document.addEventListener('click', function (event) {
        // Reply button handler
        if (event.target.closest('.reply-btn')) {
            var btn = event.target.closest('.reply-btn');
            var commentId = btn.getAttribute('data-comment-id');
            var replyForm = document.querySelector('.reply-form[data-comment-id="' + commentId + '"]');
            
            if (replyForm) {
                var isHidden = replyForm.classList.contains('hidden-form');
                // Close all other reply forms
                document.querySelectorAll('.reply-form').forEach(function (form) {
                    form.classList.add('hidden-form');
                });
                // Toggle current form
                if (isHidden) {
                    replyForm.classList.remove('hidden-form');
                    // Focus on textarea
                    var textarea = replyForm.querySelector('textarea');
                    if (textarea) setTimeout(function () { textarea.focus(); }, 0);
                }
            }
        }
        
        // Toggle replies button handler
        if (event.target.closest('.toggle-replies')) {
            var btn = event.target.closest('.toggle-replies');
            var commentId = btn.getAttribute('data-comment-id');
            var repliesContainer = document.querySelector('.replies-container[data-comment-id="' + commentId + '"]');
            
            if (repliesContainer) {
                var isHidden = repliesContainer.classList.contains('hidden-form');
                repliesContainer.classList.toggle('hidden-form');
                
                // Update chevron rotation
                var icon = btn.querySelector('[data-lucide]');
                if (icon) {
                    btn.classList.toggle('expanded');
                }
            }
        }
        
        // Load nested replies button handler
        if (event.target.closest('.load-nested-replies')) {
            var btn = event.target.closest('.load-nested-replies');
            var parentId = btn.getAttribute('data-parent-id');
            var datasetId = btn.getAttribute('data-dataset-id');
            var container = document.querySelector('.nested-replies-container[data-parent-id="' + parentId + '"]');
            
            if (!container) return;

            // Check if already loaded
            if (container.hasAttribute('data-loaded')) {
                btn.classList.toggle('expanded');
                container.classList.toggle('hidden-form');
                return;
            }

            // Show loading state
            btn.disabled = true;
            var originalText = btn.innerHTML;
            btn.innerHTML = '<span style="display: inline-flex; align-items: center; gap: 4px;"><i data-lucide="loader"></i> Loading...</span>';

            // Fetch nested replies
            fetch('/datasets/' + datasetId + '/comments/' + parentId + '/nested?limit=100', {
                credentials: 'same-origin'
            })
            .then(function (res) { return res.ok ? res.json() : []; })
            .then(function (data) {
                // Extract CSRF token from page
                var csrfInput = document.querySelector('input[name="_csrf"]');
                var csrfToken = csrfInput ? csrfInput.value : '';
                
                if (Array.isArray(data) && data.length > 0) {
                    var html = data.map(function (comment) {
                        return '<div class="discussion-reply">' +
                            '<div class="discussion-comment" data-comment-id="' + comment.commentId + '">' +
                            '<div class="comment-header">' +
                            '<div class="comment-meta">' +
                            '<span class="comment-author">' + escapeHtml(comment.authorName) + '</span>' +
                            '<span class="comment-time">' + escapeHtml(comment.createdAt) + '</span>' +
                            '</div>' +
                            '</div>' +
                            '<div class="comment-body">' +
                            '<p class="comment-text">' + escapeHtml(comment.content) + '</p>' +
                            '</div>' +
                            '<div class="comment-actions">' +
                            '<button type="button" class="comment-action-btn reply-btn" data-comment-id="' + comment.commentId + '">' +
                            '<i data-lucide="reply"></i>' +
                            'Reply' +
                            '</button>' +
                            '</div>' +
                            '<form action="/datasets/' + datasetId + '/comments" method="post" class="reply-form hidden-form" data-comment-id="' + comment.commentId + '">' +
                            '<input type="hidden" name="_csrf" value="' + escapeHtml(csrfToken) + '">' +
                            '<input type="hidden" name="parentId" value="' + comment.commentId + '">' +
                            '<div class="form-group">' +
                            '<textarea name="content" rows="2" maxlength="2000" placeholder="Write a reply..." class="comment-input" required></textarea>' +
                            '</div>' +
                            '<div style="display: flex; gap: 8px; margin-top: 8px;">' +
                            '<button type="submit" class="btn-comment btn-small">Reply</button>' +
                            '<button type="button" class="btn-cancel btn-small">Cancel</button>' +
                            '</div>' +
                            '</form>' +
                            (comment.replyCount > 0 ? '<div style="margin-top: 12px;">' +
                            '<button type="button" class="comment-action-btn load-nested-replies" data-parent-id="' + comment.commentId + '" data-dataset-id="' + datasetId + '">' +
                            '<i data-lucide="chevron-down"></i> ' + comment.replyCount + ' nested ' + (comment.replyCount === 1 ? 'reply' : 'replies') +
                            '</button>' +
                            '<div class="nested-replies-container" data-parent-id="' + comment.commentId + '"></div>' +
                            '</div>' : '') +
                            '</div>' +
                            '</div>';
                    }).join('');
                    container.innerHTML = html;
                    container.classList.remove('hidden-form');
                    container.setAttribute('data-loaded', 'true');
                    btn.classList.add('expanded');
                    lucide.createIcons();
                } else {
                    container.innerHTML = '<div style="padding: 8px; color: #94a3b8; font-size: 12px;">No nested replies found.</div>';
                    container.setAttribute('data-loaded', 'true');
                }
                
                btn.disabled = false;
                btn.innerHTML = originalText;
            })
            .catch(function (err) {
                console.error('Error loading nested replies:', err);
                btn.disabled = false;
                btn.innerHTML = originalText;
                container.innerHTML = '<div style="padding: 8px; color: #e07060; font-size: 12px;">Failed to load replies. Try again.</div>';
            });
        }
        
        // Cancel button handler
        if (event.target.closest('.btn-cancel')) {
            event.preventDefault();
            var form = event.target.closest('.reply-form');
            if (form) {
                form.classList.add('hidden-form');
            }
        }
    });

    // ===== Confirm Password Validation =====
    var confirmField = document.querySelector('input[name="confirmPassword"]');
    var passwordField = document.querySelector('input[name="password"]');
    if (confirmField && passwordField) {
        confirmField.addEventListener('input', function () {
            if (confirmField.value !== passwordField.value) {
                confirmField.setCustomValidity('Passwords do not match');
            } else {
                confirmField.setCustomValidity('');
            }
        });
    }
});
