// Dashboard JavaScript for SAP IDoc Capture Monitor

let eventSource = null;
let recentEvents = [];

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
    connectToEventStream();
    fetchInitialMetrics();
    fetchIdocTypes();
    fetchFilteredActivity();

    // Setup filter event listeners
    document.getElementById('applyFiltersBtn').addEventListener('click', fetchFilteredActivity);
    document.getElementById('resetFiltersBtn').addEventListener('click', resetFilters);
});

// Connect to Server-Sent Events stream
function connectToEventStream() {
    eventSource = new EventSource('/api/sse');

    eventSource.addEventListener('metric_update', (e) => {
        const metrics = JSON.parse(e.data);
        updateDashboard(metrics);
    });

    eventSource.addEventListener('idoc_received', (e) => {
        const data = JSON.parse(e.data);
        addToRecentActivity(data);
    });

    eventSource.onopen = () => {
        document.getElementById('connectionStatus').textContent = '● Connected';
        document.getElementById('connectionStatus').className = 'status connected';
    };

    eventSource.onerror = () => {
        document.getElementById('connectionStatus').textContent = '● Disconnected';
        document.getElementById('connectionStatus').className = 'status disconnected';
    };
}

// Fetch initial metrics
async function fetchInitialMetrics() {
    try {
        const response = await fetch('/api/metrics');
        const metrics = await response.json();
        updateDashboard(metrics);
    } catch (error) {
        console.error('Failed to fetch metrics:', error);
    }
}

// Fetch recent activity from database
async function fetchRecentActivity() {
    try {
        const response = await fetch('/api/history/recent?limit=10');
        const events = await response.json();

        // Convert to internal format and populate recentEvents array
        recentEvents = events.map(event => ({
            time: new Date(event.time),
            type: event.type,
            docNum: event.docNum,
            status: event.status
        }));

        updateRecentActivityTable();
    } catch (error) {
        console.error('Failed to fetch recent activity:', error);
    }
}

// Fetch IDoc types for filter dropdown
async function fetchIdocTypes() {
    try {
        const response = await fetch('/api/history/idoc-types');
        const types = await response.json();

        const select = document.getElementById('idocTypeFilter');
        types.forEach(type => {
            const option = document.createElement('option');
            option.value = type;
            option.textContent = type;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Failed to fetch IDoc types:', error);
    }
}

// Fetch filtered activity from database
async function fetchFilteredActivity() {
    try {
        const timeRange = document.getElementById('timeRangeFilter').value;
        const idocType = document.getElementById('idocTypeFilter').value;
        const eventType = document.getElementById('eventTypeFilter').value;
        const status = document.getElementById('statusFilter').value;
        const limit = document.getElementById('limitFilter').value;

        const params = new URLSearchParams({
            timeRange: timeRange,
            idocType: idocType,
            eventType: eventType,
            status: status,
            limit: limit
        });

        console.log('Fetching filtered activity with params:', params.toString());

        const response = await fetch(`/api/history/filtered?${params}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        console.log('Filtered activity response:', data);

        // Check if data has the expected structure
        if (!data || !data.events) {
            console.error('Unexpected response format:', data);
            recentEvents = [];
            updateFilteredActivityTable();
            return;
        }

        // Update recentEvents with filtered data
        recentEvents = data.events.map(event => ({
            time: new Date(event.time),
            eventType: event.eventType,
            idocType: event.idocType,
            docNum: event.docNum,
            status: event.status,
            processingTime: event.processingTime
        }));

        console.log('Processed events:', recentEvents);
        updateFilteredActivityTable();
    } catch (error) {
        console.error('Failed to fetch filtered activity:', error);
        // Show error in table
        const tbody = document.getElementById('recentActivity');
        tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Error loading activity: ${error.message}</td></tr>`;
    }
}

// Reset filters to default values
function resetFilters() {
    document.getElementById('timeRangeFilter').value = '24h';
    document.getElementById('idocTypeFilter').value = 'all';
    document.getElementById('eventTypeFilter').value = 'all';
    document.getElementById('statusFilter').value = 'all';
    document.getElementById('limitFilter').value = '10';
    fetchFilteredActivity();
}

// Update dashboard with new metrics
function updateDashboard(metrics) {
    // Update stats
    document.getElementById('totalIDocs').textContent = metrics.totalIdocsReceived.toLocaleString();

    const successCount = metrics.totalIdocsReceived - (metrics.errors.xml + metrics.errors.json + metrics.errors.kafka);
    document.getElementById('successCount').textContent = successCount.toLocaleString();
    document.getElementById('successRate').textContent = `(${metrics.successRate.toFixed(1)}%)`;

    const totalErrors = metrics.errors.xml + metrics.errors.json + metrics.errors.kafka;
    document.getElementById('errorCount').textContent = totalErrors.toLocaleString();
    const errorRate = metrics.totalIdocsReceived > 0 ? ((totalErrors / metrics.totalIdocsReceived) * 100) : 0;
    document.getElementById('errorRate').textContent = `(${errorRate.toFixed(1)}%)`;

    document.getElementById('receptionRate').textContent = metrics.receptionRatePer5Min.toFixed(1);

    // Update IDoc types
    updateIdocTypes(metrics.idocsByType, metrics.totalIdocsReceived);

    // Update pipeline status
    document.getElementById('xmlStorage').textContent = `${metrics.totalXmlWritten.toLocaleString()} files`;
    document.getElementById('jsonConversion').textContent = `${metrics.totalJsonConverted.toLocaleString()} files`;
    document.getElementById('kafkaPublished').textContent = `${metrics.totalKafkaPublished.toLocaleString()} messages`;

    const kafkaStatusElem = document.getElementById('kafkaStatus');
    if (metrics.kafkaStatus === 'CONNECTED') {
        kafkaStatusElem.textContent = '● Connected';
        kafkaStatusElem.style.color = '#10b981';
    } else if (metrics.kafkaStatus === 'DISCONNECTED') {
        kafkaStatusElem.textContent = '● Disconnected';
        kafkaStatusElem.style.color = '#ef4444';
    } else {
        kafkaStatusElem.textContent = '● Unknown';
        kafkaStatusElem.style.color = '#9ca3af';
    }

    // Update last received
    if (metrics.lastIdocType) {
        document.getElementById('lastIdocType').textContent = metrics.lastIdocType;
        document.getElementById('lastDocNum').textContent = metrics.lastDocNum || '--';
        document.getElementById('lastTime').textContent = metrics.lastIdocReceived ?
            new Date(metrics.lastIdocReceived).toLocaleString() : '--';
    }

    // Update uptime
    if (metrics.applicationStartTime) {
        const startTime = new Date(metrics.applicationStartTime);
        const uptime = Math.floor((Date.now() - startTime.getTime()) / 1000);
        document.getElementById('uptime').textContent = `Uptime: ${formatUptime(uptime)}`;
    }

    // Update last update time
    document.getElementById('lastUpdate').textContent = `Updated: ${new Date().toLocaleTimeString()}`;
}

// Update IDoc types list
function updateIdocTypes(idocsByType, total) {
    const container = document.getElementById('idocTypesList');

    if (Object.keys(idocsByType).length === 0) {
        container.innerHTML = '<div class="empty-state">No IDocs received yet</div>';
        return;
    }

    // Sort by count descending
    const sorted = Object.entries(idocsByType).sort((a, b) => b[1] - a[1]);

    container.innerHTML = sorted.map(([type, count]) => {
        const percent = total > 0 ? ((count / total) * 100) : 0;
        return `
            <div class="type-item">
                <div class="type-name">${type} <span class="type-count">(${count.toLocaleString()})</span></div>
                <div class="type-bar-container">
                    <div class="type-bar" style="width: ${percent}%"></div>
                </div>
                <div class="type-percent">${percent.toFixed(1)}%</div>
            </div>
        `;
    }).join('');
}

// Add to recent activity
function addToRecentActivity(data) {
    recentEvents.unshift({
        time: new Date(data.time),
        type: data.type,
        docNum: data.docNum,
        status: 'success'
    });

    // Keep only last 10
    recentEvents = recentEvents.slice(0, 10);

    updateRecentActivityTable();
}

// Update recent activity table
function updateRecentActivityTable() {
    const tbody = document.getElementById('recentActivity');

    if (recentEvents.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No recent activity</td></tr>';
        return;
    }

    tbody.innerHTML = recentEvents.map(event => `
        <tr>
            <td>${event.time.toLocaleTimeString()}</td>
            <td>${event.type}</td>
            <td>${event.docNum}</td>
            <td><span class="status-badge status-${event.status}">${event.status.toUpperCase()}</span></td>
        </tr>
    `).join('');
}

// Update filtered activity table (with more columns)
function updateFilteredActivityTable() {
    const tbody = document.getElementById('recentActivity');

    if (recentEvents.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No activity found matching the filters</td></tr>';
        return;
    }

    tbody.innerHTML = recentEvents.map(event => {
        const time = new Date(event.time);
        const timeStr = time.toLocaleString();
        const processingTimeStr = event.processingTime > 0 ? `${event.processingTime}ms` : '-';

        // Map database status to CSS class
        const statusClass = event.status === 'SUCCESS' ? 'success' :
                           event.status === 'FAILED' ? 'error' :
                           event.status.toLowerCase();
        const statusText = event.status === 'SUCCESS' ? 'Success' :
                          event.status === 'FAILED' ? 'Failed' :
                          event.status;

        return `
            <tr>
                <td>${timeStr}</td>
                <td>${event.eventType || '-'}</td>
                <td>${event.idocType || '-'}</td>
                <td>${event.docNum || '-'}</td>
                <td><span class="status-badge status-${statusClass}">${statusText}</span></td>
                <td>${processingTimeStr}</td>
            </tr>
        `;
    }).join('');
}

// Format uptime
function formatUptime(seconds) {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);

    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    if (hours > 0) parts.push(`${hours}h`);
    if (minutes > 0) parts.push(`${minutes}m`);

    return parts.length > 0 ? parts.join(' ') : '< 1m';
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (eventSource) {
        eventSource.close();
    }
});
