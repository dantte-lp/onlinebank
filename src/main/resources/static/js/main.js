// main.js - Основной JavaScript файл для Online Bank

// Theme Management
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.classList.toggle('dark', savedTheme === 'dark');
}

function toggleTheme() {
    const currentTheme = document.documentElement.classList.contains('dark') ? 'dark' : 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    document.documentElement.classList.toggle('dark', newTheme === 'dark');
    localStorage.setItem('theme', newTheme);
}

// Mobile Menu
function toggleMobileMenu() {
    const mobileMenu = document.getElementById('mobile-menu');
    if (mobileMenu) {
        mobileMenu.classList.toggle('hidden');
    }
}

// Create Client Modal
function openCreateModal() {
    const modal = document.getElementById('createModal');
    if (modal) {
        // Load form content
        fetch('/clients/new')
            .then(response => response.text())
            .then(html => {
                document.getElementById('createClientForm').innerHTML = html;
                modal.classList.remove('hidden');

                // Initialize date picker
                initDatePicker();

                // Initialize phone mask
                initPhoneMask();
            })
            .catch(error => {
                console.error('Error loading form:', error);
                alert('Ошибка при загрузке формы');
            });
    }
}

function closeCreateModal() {
    const modal = document.getElementById('createModal');
    if (modal) {
        modal.classList.add('hidden');
    }
}

// Date Picker Initialization
function initDatePicker() {
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => {
        // Set max date to today
        const today = new Date().toISOString().split('T')[0];
        input.setAttribute('max', today);

        // Set min date (100 years ago)
        const minDate = new Date();
        minDate.setFullYear(minDate.getFullYear() - 100);
        input.setAttribute('min', minDate.toISOString().split('T')[0]);
    });
}

// Phone Number Mask
function initPhoneMask() {
    const phoneInputs = document.querySelectorAll('input[name="phoneNumber"]');
    phoneInputs.forEach(input => {
        input.addEventListener('input', function(e) {
            let value = e.target.value;

            // Keep only digits and +
            value = value.replace(/[^\d+]/g, '');

            // Ensure it starts with +
            if (value && !value.startsWith('+')) {
                value = '+' + value;
            }

            // Limit to 16 characters (+ and 15 digits max)
            if (value.length > 16) {
                value = value.substring(0, 16);
            }

            e.target.value = value;
        });
    });
}

// Form Validation
function validateClientForm(formElement) {
    const errors = [];

    // Validate last name
    const lastName = formElement.querySelector('input[name="lastName"]');
    if (!lastName.value.trim()) {
        errors.push('Фамилия обязательна для заполнения');
    } else if (!/^[А-Яа-яЁё\s-]+$/.test(lastName.value)) {
        errors.push('Фамилия должна содержать только русские буквы');
    }

    // Validate first name
    const firstName = formElement.querySelector('input[name="firstName"]');
    if (!firstName.value.trim()) {
        errors.push('Имя обязательно для заполнения');
    } else if (!/^[А-Яа-яЁё\s-]+$/.test(firstName.value)) {
        errors.push('Имя должно содержать только русские буквы');
    }

    // Validate middle name (optional)
    const middleName = formElement.querySelector('input[name="middleName"]');
    if (middleName.value && !/^[А-Яа-яЁё\s-]*$/.test(middleName.value)) {
        errors.push('Отчество должно содержать только русские буквы');
    }

    // Validate birth date
    const birthDate = formElement.querySelector('input[name="birthDate"]');
    if (!birthDate.value) {
        errors.push('Дата рождения обязательна для заполнения');
    } else {
        const birth = new Date(birthDate.value);
        const today = new Date();
        const age = today.getFullYear() - birth.getFullYear();
        if (age < 18) {
            errors.push('Клиент должен быть старше 18 лет');
        }
    }

    // Validate phone number
    const phoneNumber = formElement.querySelector('input[name="phoneNumber"]');
    if (!phoneNumber.value.trim()) {
        errors.push('Номер телефона обязателен для заполнения');
    } else if (!/^\+[1-9]\d{1,14}$/.test(phoneNumber.value)) {
        errors.push('Номер телефона должен быть в международном формате (например: +79001234567)');
    }

    // Validate currency
    const currency = formElement.querySelector('select[name="currency"]');
    if (!currency.value) {
        errors.push('Выберите валюту счета');
    }

    // Validate nationality
    const nationality = formElement.querySelector('select[name="nationality"]');
    if (!nationality.value) {
        errors.push('Выберите гражданство');
    }

    return errors;
}

// Show validation errors
function showValidationErrors(errors) {
    const errorContainer = document.getElementById('validationErrors');
    if (errorContainer) {
        if (errors.length > 0) {
            const errorHtml = errors.map(error => `<li>${error}</li>`).join('');
            errorContainer.innerHTML = `
                <div class="bg-red-100 dark:bg-red-900/20 border border-red-400 dark:border-red-600 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg mb-4">
                    <p class="font-medium mb-2">Пожалуйста, исправьте следующие ошибки:</p>
                    <ul class="list-disc list-inside text-sm">
                        ${errorHtml}
                    </ul>
                </div>
            `;
            errorContainer.classList.remove('hidden');
        } else {
            errorContainer.classList.add('hidden');
        }
    }
}

// Format account number display
function formatAccountNumber(accountNumber) {
    if (!accountNumber || accountNumber.length !== 20) return accountNumber;
    return accountNumber.replace(/(.{4})/g, '$1 ').trim();
}

// Copy to clipboard functionality
function copyToClipboard(text, button) {
    navigator.clipboard.writeText(text).then(() => {
        const originalText = button.innerHTML;
        button.innerHTML = '<i class="fas fa-check"></i> Скопировано';
        button.classList.add('text-green-600');

        setTimeout(() => {
            button.innerHTML = originalText;
            button.classList.remove('text-green-600');
        }, 2000);
    }).catch(err => {
        console.error('Failed to copy:', err);
    });
}

// Debounce function for search
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Live search functionality
const searchInput = document.querySelector('input[name="search"]');
if (searchInput) {
    const debouncedSearch = debounce((value) => {
        if (value.length >= 3 || value.length === 0) {
            const form = searchInput.closest('form');
            if (form) {
                form.submit();
            }
        }
    }, 500);

    searchInput.addEventListener('input', (e) => {
        debouncedSearch(e.target.value);
    });
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', function() {
    // Initialize theme
    initTheme();

    // Initialize tooltips
    const tooltips = document.querySelectorAll('[title]');
    tooltips.forEach(element => {
        element.addEventListener('mouseenter', function() {
            this.classList.add('relative');
        });
    });

    // Close modal on outside click
    const modal = document.getElementById('createModal');
    if (modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeCreateModal();
            }
        });
    }

    // Form submission handler
    const createForm = document.getElementById('createClientForm');
    if (createForm) {
        createForm.addEventListener('submit', function(e) {
            const errors = validateClientForm(this);
            if (errors.length > 0) {
                e.preventDefault();
                showValidationErrors(errors);
            }
        });
    }

    // Auto-hide alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-auto-hide');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });
});

// Export functions for global use
window.toggleTheme = toggleTheme;
window.toggleMobileMenu = toggleMobileMenu;
window.openCreateModal = openCreateModal;
window.closeCreateModal = closeCreateModal;
window.copyToClipboard = copyToClipboard;