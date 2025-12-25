// Custom JavaScript for Spring MCP Server

// Auto-dismiss alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// CSRF token for HTMX requests
document.addEventListener('DOMContentLoaded', function() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');

    if (token && header) {
        document.body.addEventListener('htmx:configRequest', function(event) {
            event.detail.headers[header.content] = token.content;
        });
    }
});

// Sidebar collapse/expand functionality
document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('main');
    const toggleBtn = document.getElementById('sidebarToggle');
    const STORAGE_KEY = 'spring-mcp-sidebar-collapsed';

    if (!sidebar || !toggleBtn) return;

    // Set data-title attributes for tooltips when collapsed
    const navLinks = sidebar.querySelectorAll('.nav-link');
    navLinks.forEach(function(link) {
        const navText = link.querySelector('.nav-text');
        if (navText) {
            link.setAttribute('data-title', navText.textContent.trim());
        }
    });

    // Function to apply collapsed state (also updates html class for consistency)
    function applyCollapsedState(isCollapsed) {
        if (isCollapsed) {
            document.documentElement.classList.add('sidebar-is-collapsed');
            sidebar.classList.add('collapsed');
            if (mainContent) mainContent.classList.add('sidebar-collapsed');
        } else {
            document.documentElement.classList.remove('sidebar-is-collapsed');
            sidebar.classList.remove('collapsed');
            if (mainContent) mainContent.classList.remove('sidebar-collapsed');
        }
    }

    // Restore saved state from localStorage (re-apply to ensure element classes are set)
    const savedState = localStorage.getItem(STORAGE_KEY);
    applyCollapsedState(savedState === 'true');

    // Toggle sidebar on button click
    toggleBtn.addEventListener('click', function() {
        const isCollapsed = !sidebar.classList.contains('collapsed');
        applyCollapsedState(isCollapsed);

        // Save state to localStorage
        localStorage.setItem(STORAGE_KEY, isCollapsed);
    });
});
