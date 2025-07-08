// charts.js - Performance charts for monitoring page

// Initialize performance chart
document.addEventListener('DOMContentLoaded', function() {
    const ctx = document.getElementById('performanceChart');
    if (!ctx) return;

    // Generate demo data for the last 24 hours
    const labels = [];
    const responseTimeData = [];
    const requestCountData = [];

    const now = new Date();
    for (let i = 23; i >= 0; i--) {
        const time = new Date(now.getTime() - i * 60 * 60 * 1000);
        labels.push(time.getHours() + ':00');

        // Generate random data for demo
        responseTimeData.push(Math.floor(Math.random() * 50) + 30);
        requestCountData.push(Math.floor(Math.random() * 100) + 50);
    }

    // Chart configuration
    const chartConfig = {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Время ответа (ms)',
                    data: responseTimeData,
                    borderColor: 'rgb(59, 130, 246)',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    yAxisID: 'y',
                    tension: 0.3,
                    fill: true
                },
                {
                    label: 'Количество запросов',
                    data: requestCountData,
                    borderColor: 'rgb(16, 185, 129)',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    yAxisID: 'y1',
                    tension: 0.3,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                title: {
                    display: true,
                    text: 'Производительность API за последние 24 часа',
                    font: {
                        size: 16
                    }
                },
                legend: {
                    position: 'top',
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed.y !== null) {
                                if (context.datasetIndex === 0) {
                                    label += context.parsed.y + ' ms';
                                } else {
                                    label += context.parsed.y + ' запросов';
                                }
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                y: {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    title: {
                        display: true,
                        text: 'Время ответа (ms)'
                    },
                    min: 0,
                    max: 200
                },
                y1: {
                    type: 'linear',
                    display: true,
                    position: 'right',
                    title: {
                        display: true,
                        text: 'Количество запросов'
                    },
                    min: 0,
                    max: 200,
                    grid: {
                        drawOnChartArea: false,
                    },
                }
            }
        }
    };

    // Create chart
    const performanceChart = new Chart(ctx, chartConfig);

    // Update chart colors based on theme
    function updateChartTheme() {
        const isDark = document.documentElement.classList.contains('dark');

        if (isDark) {
            Chart.defaults.color = '#9ca3af';
            Chart.defaults.borderColor = '#374151';
            performanceChart.options.plugins.title.color = '#e5e7eb';
        } else {
            Chart.defaults.color = '#666';
            Chart.defaults.borderColor = '#e5e7eb';
            performanceChart.options.plugins.title.color = '#111827';
        }

        performanceChart.update();
    }

    // Listen for theme changes
    const observer = new MutationObserver(updateChartTheme);
    observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['class']
    });

    // Initial theme setup
    updateChartTheme();

    // Simulate real-time updates
    setInterval(() => {
        // Remove first element
        performanceChart.data.labels.shift();
        performanceChart.data.datasets[0].data.shift();
        performanceChart.data.datasets[1].data.shift();

        // Add new element
        const now = new Date();
        performanceChart.data.labels.push(now.getHours() + ':' + now.getMinutes().toString().padStart(2, '0'));
        performanceChart.data.datasets[0].data.push(Math.floor(Math.random() * 50) + 30);
        performanceChart.data.datasets[1].data.push(Math.floor(Math.random() * 100) + 50);

        performanceChart.update();
    }, 5000); // Update every 5 seconds

    // Memory usage pie chart (if exists)
    const memoryCtx = document.getElementById('memoryChart');
    if (memoryCtx) {
        const memoryData = {
            labels: ['Используется', 'Свободно'],
            datasets: [{
                data: [70, 30], // Example data
                backgroundColor: [
                    'rgba(239, 68, 68, 0.8)',
                    'rgba(34, 197, 94, 0.8)'
                ],
                borderWidth: 0
            }]
        };

        new Chart(memoryCtx, {
            type: 'doughnut',
            data: memoryData,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                    },
                    title: {
                        display: true,
                        text: 'Использование памяти JVM'
                    }
                }
            }
        });
    }

    // Connection pool gauge (if exists)
    const poolCtx = document.getElementById('connectionPoolChart');
    if (poolCtx) {
        const poolData = {
            labels: ['Активные', 'Свободные', 'В очереди'],
            datasets: [{
                data: [3, 7, 0], // Example data
                backgroundColor: [
                    'rgba(59, 130, 246, 0.8)',
                    'rgba(34, 197, 94, 0.8)',
                    'rgba(251, 191, 36, 0.8)'
                ],
                borderWidth: 0
            }]
        };

        new Chart(poolCtx, {
            type: 'pie',
            data: poolData,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'right',
                    },
                    title: {
                        display: true,
                        text: 'Пул соединений БД'
                    }
                }
            }
        });
    }
});

// Export chart update function for external use
window.updatePerformanceChart = function(newData) {
    const chart = Chart.getChart('performanceChart');
    if (chart && newData) {
        if (newData.labels) {
            chart.data.labels = newData.labels;
        }
        if (newData.responseTime) {
            chart.data.datasets[0].data = newData.responseTime;
        }
        if (newData.requestCount) {
            chart.data.datasets[1].data = newData.requestCount;
        }
        chart.update();
    }
};