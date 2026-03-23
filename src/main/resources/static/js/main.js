// ===== Drop Zone File Upload =====
document.addEventListener('DOMContentLoaded', function () {
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileSelected = document.getElementById('fileSelected');
    const nextBtn = document.getElementById('nextBtn');

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
                showSelectedFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener('change', function () {
            if (fileInput.files.length > 0) {
                showSelectedFile(fileInput.files[0]);
            }
        });

        function showSelectedFile(file) {
            if (fileSelected) {
                fileSelected.textContent = 'Selected: ' + file.name + ' (' + formatSize(file.size) + ')';
                fileSelected.style.display = 'block';
            }
            // Auto-set dataset name from filename
            var nameInput = document.getElementById('datasetName');
            if (nameInput && nameInput.value === 'Untitled Dataset') {
                nameInput.value = file.name.replace(/\.[^/.]+$/, '');
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

    // ===== Category Tab Switching =====
    var tabs = document.querySelectorAll('.category-tab');
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            tabs.forEach(function (t) { t.classList.remove('active'); });
            tab.classList.add('active');
        });
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
