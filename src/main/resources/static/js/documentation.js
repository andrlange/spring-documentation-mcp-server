// Documentation page JavaScript

// Configure marked.js when it loads
document.addEventListener('DOMContentLoaded', function() {
    if (typeof marked !== 'undefined') {
        marked.setOptions({
            breaks: true,
            gfm: true,
            headerIds: true,
            mangle: false
        });
    }
});

// Toggle markdown content visibility using DOM traversal
// This function is global so it can be called from onclick attributes
function toggleMarkdown(button) {
    // Get the parent row (main documentation row)
    const docRow = button.closest('tr');
    if (!docRow) {
        console.error('Could not find parent row');
        return;
    }

    // Get the next sibling (markdown expandable row)
    const markdownRow = docRow.nextElementSibling;
    if (!markdownRow || !markdownRow.classList.contains('markdown-row')) {
        console.error('Could not find markdown row');
        return;
    }

    // Find the icon within the button
    const toggleIcon = button.querySelector('i');
    if (!toggleIcon) {
        console.error('Could not find toggle icon');
        return;
    }

    // Get doc ID from button's data attribute
    const docId = button.dataset.docId;
    if (!docId) {
        console.error('Could not find doc ID');
        return;
    }

    if (markdownRow.classList.contains('d-none')) {
        // Expand
        markdownRow.classList.remove('d-none');
        toggleIcon.classList.remove('bi-chevron-right');
        toggleIcon.classList.add('bi-chevron-down');

        // Load markdown if not already loaded
        const markdownContent = markdownRow.querySelector('.markdown-content');
        if (markdownContent && markdownContent.innerHTML.trim() === 'Content will be loaded here via JavaScript') {
            loadMarkdownContent(docId, markdownRow);
        }
    } else {
        // Collapse
        markdownRow.classList.add('d-none');
        toggleIcon.classList.remove('bi-chevron-down');
        toggleIcon.classList.add('bi-chevron-right');
    }
}

// Load markdown content from server
function loadMarkdownContent(docId, markdownRow) {
    const loadingDiv = markdownRow.querySelector('.markdown-loading');
    const markdownContent = markdownRow.querySelector('.markdown-content');
    const errorDiv = markdownRow.querySelector('.markdown-error');
    const errorMsg = markdownRow.querySelector('.markdown-error-msg');

    if (!markdownContent) {
        console.error('Could not find markdown content div');
        return;
    }

    // Show loading spinner if it exists
    if (loadingDiv) loadingDiv.classList.remove('d-none');
    markdownContent.classList.add('d-none');
    if (errorDiv) errorDiv.classList.add('d-none');

    // Fetch markdown content
    fetch(`/api/documentation/${docId}/content`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.content && data.content.trim() !== '') {
                // Render markdown
                const htmlContent = marked.parse(data.content);
                markdownContent.innerHTML = htmlContent;

                // Hide loading, show content
                if (loadingDiv) loadingDiv.classList.add('d-none');
                markdownContent.classList.remove('d-none');
            } else {
                // Show "no content" message
                if (errorMsg) {
                    errorMsg.textContent = 'No content available for this documentation';
                }
                if (loadingDiv) loadingDiv.classList.add('d-none');
                if (errorDiv) errorDiv.classList.remove('d-none');
            }

            // Also load GitHub docs count
            loadGitHubDocsCount(docId, markdownRow);
        })
        .catch(error => {
            console.error('Error loading markdown:', error);
            if (errorMsg) {
                errorMsg.textContent = `Failed to load content: ${error.message}`;
            }

            // Hide loading, show error
            if (loadingDiv) loadingDiv.classList.add('d-none');
            if (errorDiv) errorDiv.classList.remove('d-none');

            // Still try to load GitHub docs count
            loadGitHubDocsCount(docId, markdownRow);
        });
}

// Load GitHub docs count for display in the header
function loadGitHubDocsCount(docId, markdownRow) {
    const githubSection = markdownRow.querySelector('.github-docs-section');
    if (!githubSection) return;

    const countBadge = githubSection.querySelector('.github-docs-count');
    if (!countBadge) return;

    fetch(`/api/documentation/${docId}/github-docs`)
        .then(response => response.json())
        .then(data => {
            const count = data.count || 0;
            countBadge.textContent = count + ' doc' + (count !== 1 ? 's' : '');

            // Store docs data for later use
            githubSection.dataset.githubDocs = JSON.stringify(data.docs || []);
            githubSection.dataset.loaded = 'true';
        })
        .catch(error => {
            console.error('Error loading GitHub docs count:', error);
            countBadge.textContent = 'Error';
        });
}

// Toggle GitHub docs section visibility
function toggleGitHubDocs(headerElement) {
    const section = headerElement.closest('.github-docs-section');
    if (!section) return;

    const body = section.querySelector('.github-docs-body');
    const chevron = section.querySelector('.github-docs-chevron');
    const docId = section.dataset.docId;

    if (!body) return;

    // Toggle visibility
    if (body.classList.contains('collapse')) {
        body.classList.remove('collapse');
        body.classList.add('show');
        if (chevron) {
            chevron.classList.remove('bi-chevron-down');
            chevron.classList.add('bi-chevron-up');
        }

        // Load GitHub docs list if not already loaded
        const list = body.querySelector('.github-docs-list');
        if (list && list.children.length === 0) {
            loadGitHubDocsList(docId, section);
        }
    } else {
        body.classList.add('collapse');
        body.classList.remove('show');
        if (chevron) {
            chevron.classList.remove('bi-chevron-up');
            chevron.classList.add('bi-chevron-down');
        }
    }
}

// Load and render GitHub docs list
function loadGitHubDocsList(docId, section) {
    const body = section.querySelector('.github-docs-body');
    const loadingDiv = body.querySelector('.github-docs-loading');
    const listDiv = body.querySelector('.github-docs-list');
    const emptyDiv = body.querySelector('.github-docs-empty');

    // Check if we have cached data
    if (section.dataset.loaded === 'true' && section.dataset.githubDocs) {
        const docs = JSON.parse(section.dataset.githubDocs);
        renderGitHubDocsList(docs, loadingDiv, listDiv, emptyDiv);
        return;
    }

    // Fetch from API
    fetch(`/api/documentation/${docId}/github-docs`)
        .then(response => response.json())
        .then(data => {
            section.dataset.githubDocs = JSON.stringify(data.docs || []);
            section.dataset.loaded = 'true';
            renderGitHubDocsList(data.docs || [], loadingDiv, listDiv, emptyDiv);
        })
        .catch(error => {
            console.error('Error loading GitHub docs:', error);
            if (loadingDiv) loadingDiv.classList.add('d-none');
            if (emptyDiv) {
                emptyDiv.innerHTML = '<i class="bi bi-exclamation-triangle text-warning"></i><span class="text-muted ms-2">Error loading GitHub documentation</span>';
                emptyDiv.classList.remove('d-none');
            }
        });
}

// Render the GitHub docs list
function renderGitHubDocsList(docs, loadingDiv, listDiv, emptyDiv) {
    if (loadingDiv) loadingDiv.classList.add('d-none');

    if (!docs || docs.length === 0) {
        if (emptyDiv) emptyDiv.classList.remove('d-none');
        return;
    }

    // Build list items
    listDiv.innerHTML = docs.map(doc => `
        <div class="list-group-item list-group-item-action github-doc-item" data-doc-id="${doc.id}">
            <div class="d-flex w-100 justify-content-between align-items-center">
                <div class="d-flex align-items-center flex-grow-1">
                    <button type="button" class="btn btn-sm btn-link text-decoration-none p-0 me-2"
                            onclick="toggleGitHubDocContent(this, ${doc.id})">
                        <i class="bi bi-chevron-right github-item-chevron"></i>
                    </button>
                    <div>
                        <h6 class="mb-0">
                            <i class="bi bi-file-earmark-code text-secondary me-1"></i>
                            ${escapeHtml(doc.title)}
                        </h6>
                        <small class="text-muted">${doc.version || ''}</small>
                    </div>
                </div>
                <a href="${escapeHtml(doc.url)}" target="_blank" class="btn btn-sm btn-outline-secondary"
                   onclick="event.stopPropagation();">
                    <i class="bi bi-box-arrow-up-right"></i>
                </a>
            </div>
            <!-- Expandable content area -->
            <div class="github-doc-content mt-2 d-none">
                <div class="github-doc-loading text-center py-2">
                    <div class="spinner-border spinner-border-sm text-secondary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
                <div class="github-doc-markdown markdown-body d-none" style="max-height: 400px; overflow-y: auto; padding: 10px; border-radius: 4px;"></div>
                <div class="github-doc-error alert alert-warning py-2 mb-0 d-none">
                    <small><i class="bi bi-exclamation-triangle"></i> Failed to load content</small>
                </div>
            </div>
        </div>
    `).join('');

    listDiv.classList.remove('d-none');
}

// Toggle individual GitHub doc content
function toggleGitHubDocContent(button, docId) {
    const item = button.closest('.github-doc-item');
    if (!item) return;

    const contentDiv = item.querySelector('.github-doc-content');
    const chevron = button.querySelector('.github-item-chevron');

    if (!contentDiv) return;

    if (contentDiv.classList.contains('d-none')) {
        // Expand
        contentDiv.classList.remove('d-none');
        if (chevron) {
            chevron.classList.remove('bi-chevron-right');
            chevron.classList.add('bi-chevron-down');
        }

        // Load content if not loaded
        const markdown = contentDiv.querySelector('.github-doc-markdown');
        if (markdown && markdown.innerHTML.trim() === '') {
            loadGitHubDocContent(docId, contentDiv);
        }
    } else {
        // Collapse
        contentDiv.classList.add('d-none');
        if (chevron) {
            chevron.classList.remove('bi-chevron-down');
            chevron.classList.add('bi-chevron-right');
        }
    }
}

// Load individual GitHub doc content
function loadGitHubDocContent(docId, contentDiv) {
    const loadingDiv = contentDiv.querySelector('.github-doc-loading');
    const markdown = contentDiv.querySelector('.github-doc-markdown');
    const errorDiv = contentDiv.querySelector('.github-doc-error');

    if (loadingDiv) loadingDiv.classList.remove('d-none');
    if (markdown) markdown.classList.add('d-none');
    if (errorDiv) errorDiv.classList.add('d-none');

    fetch(`/api/documentation/${docId}/content`)
        .then(response => response.json())
        .then(data => {
            if (loadingDiv) loadingDiv.classList.add('d-none');

            if (data.content && data.content.trim() !== '') {
                const htmlContent = marked.parse(data.content);
                markdown.innerHTML = htmlContent;
                markdown.classList.remove('d-none');
            } else {
                if (errorDiv) {
                    errorDiv.innerHTML = '<small><i class="bi bi-info-circle"></i> No content available</small>';
                    errorDiv.classList.remove('d-none');
                }
            }
        })
        .catch(error => {
            console.error('Error loading GitHub doc content:', error);
            if (loadingDiv) loadingDiv.classList.add('d-none');
            if (errorDiv) errorDiv.classList.remove('d-none');
        });
}

// Helper function to escape HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
