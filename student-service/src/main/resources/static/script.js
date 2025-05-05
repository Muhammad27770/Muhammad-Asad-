const BASE_API = 'http://localhost:8080/student';
let activeStudent = null;

document.addEventListener('DOMContentLoaded', async () => {
    const savedStudent = localStorage.getItem('activeStudent');

    if (savedStudent) {
        try {
            activeStudent = JSON.parse(savedStudent);
            const response = await fetch(`${BASE_API}/verify?studentId=${activeStudent.studentId}`);
            if (response.ok) {
                initializeDashboard();
            } else {
                localStorage.removeItem('activeStudent');
                activeStudent = null;
                alert('Session expired. Please sign in again.');
                toggleLoginModal();
            }
        } catch (error) {
            toggleLoginModal();
        }
    } else {
        toggleLoginModal();
    }
});

function toggleLoginModal() {
    document.getElementById('auth-login-modal').classList.remove('hidden');
    document.getElementById('auth-register-modal').classList.add('hidden');
    document.getElementById('main-dashboard').classList.add('hidden');
    document.getElementById('login-error').classList.add('hidden');
    document.getElementById('auth-email').value = '';
    document.getElementById('auth-password').value = '';
}

function toggleRegisterModal() {
    document.getElementById('auth-register-modal').classList.remove('hidden');
    document.getElementById('auth-login-modal').classList.add('hidden');
    document.getElementById('main-dashboard').classList.add('hidden');
    document.getElementById('register-error').classList.add('hidden');
    document.getElementById('register-form').reset();
}

async function initializeDashboard() {
    document.getElementById('auth-login-modal').classList.add('hidden');
    document.getElementById('auth-register-modal').classList.add('hidden');
    document.getElementById('main-dashboard').classList.remove('hidden');
    
    // Updated to use correct field names
    document.getElementById('profile-name').textContent = 
        `${activeStudent.firstName} ${activeStudent.lastName}` || 'N/A';
    document.getElementById('profile-id').textContent = activeStudent.studentId || 'N/A';
    
    displaySection('profile');
    await Promise.all([fetchCourses(), loadEnrollments(), loadInvoices()]);
}

function displaySection(section) {
    const sections = ['profile', 'courses', 'enrollments', 'invoices', 'graduation'];
    sections.forEach(s => {
        document.getElementById(`${s}-section`).classList.add('hidden');
    });
    document.getElementById(`${section}-section`).classList.remove('hidden');
}

async function performLogin() {
    const email = document.getElementById('auth-email').value;
    const password = document.getElementById('auth-password').value;
    
    if (!email || !password) {
        alert('Email and password are required');
        return;
    }

    try {
        const response = await fetch(`${BASE_API}/login`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ 
                emailAddress: email,  // Match backend expectation
                loginPassword: password  // Match backend expectation
            })
        });

        if (response.ok) {
            activeStudent = await response.json();
            localStorage.setItem('activeStudent', JSON.stringify(activeStudent));
            initializeDashboard();
        } else {
            const error = await response.text();
            document.getElementById('login-error').textContent = error || 'Login failed';
            document.getElementById('login-error').classList.remove('hidden');
        }
    } catch (error) {
        document.getElementById('login-error').textContent = `Network error: ${error.message}`;
        document.getElementById('login-error').classList.remove('hidden');
    }
}

async function performRegister() {
    const firstName = document.getElementById('reg-firstname').value;
    const lastName = document.getElementById('reg-lastname').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    const hasGraduated = document.getElementById('reg-graduated').checked;

    if (!firstName || !lastName || !email || !password) {
        showError('All fields are required');
        return;
    }

    const studentData = {
        firstName: firstName,
        lastName: lastName,
        emailAddress: email,
        loginPassword: password,
        hasGraduated: hasGraduated
    };

    try {
        const response = await fetch(`${BASE_API}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(studentData)
        });

        if (response.ok) {
            activeStudent = await response.json();
            localStorage.setItem('activeStudent', JSON.stringify(activeStudent));
            initializeDashboard();
        } else {
            const error = await response.text();
            showError(error || 'Registration failed');
        }
    } catch (error) {
        showError(`Network error: ${error.message}`);
    }
}

function showError(message) {
    const errorElement = document.getElementById('register-error');
    errorElement.textContent = message;
    errorElement.classList.remove('hidden');
}

async function fetchCourses() {
    const coursesList = document.getElementById('courses-list');
    
    try {
        // Show loading state
        coursesList.innerHTML = '<li class="p-4 text-gray-500">Loading available courses...</li>';

        // Fetch both courses and enrollments in parallel
        const [coursesResponse, enrollmentsResponse] = await Promise.all([
            fetch(`${BASE_API}`),
            fetch(`${BASE_API}/courses/enrollments?studentId=${activeStudent.studentId}`, {
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include'
            })
        ]);

        if (!coursesResponse.ok) throw new Error('Failed to load courses');
        if (!enrollmentsResponse.ok) throw new Error('Failed to check enrollments');

        const [courses, enrollments] = await Promise.all([
            coursesResponse.json(),
            enrollmentsResponse.json()
        ]);

        renderCourses(courses, enrollments);
    } catch (error) {
        coursesList.innerHTML = `<li class="p-4 text-red-500">${error.message}</li>`;
    }
}

function renderCourses(courses, enrollments) {
    const coursesList = document.getElementById('courses-list');
    coursesList.innerHTML = '';

    if (!courses?.length) {
        coursesList.innerHTML = '<li class="p-4 text-gray-500">No courses available</li>';
        return;
    }

    // Get enrolled course IDs
    const enrolledIds = enrollments.map(e => e.courseId?.toString()).filter(Boolean);

    courses.forEach(course => {
        const isEnrolled = enrolledIds.includes(course.courseId?.toString());
        const li = document.createElement('li');
        li.className = 'flex justify-between items-center p-4 bg-gray-50 rounded-lg mb-2';
        li.innerHTML = `
            <div class="flex-1">
                <span class="font-medium">${course.courseTitle || 'Untitled Course'}</span>
                <p class="text-sm text-gray-600">ID: ${course.courseId || 'N/A'}</p>
            </div>
            <button class="enroll-btn ${isEnrolled ? 'bg-gray-400 cursor-not-allowed' : 'bg-green-600 hover:bg-green-700'} 
                    text-white px-4 py-2 rounded-lg transition"
                    data-course-id="${course.courseId}"
                    ${isEnrolled ? 'disabled' : ''}>
                ${isEnrolled ? 'Already Enrolled' : 'Enroll'}
            </button>
        `;
        coursesList.appendChild(li);
    });

    // Add event listeners to active buttons only
    document.querySelectorAll('.enroll-btn:not([disabled])').forEach(btn => {
        btn.addEventListener('click', () => enrollCourse(btn.dataset.courseId));
    });
}

async function enrollCourse(courseId) {

    try {
        // Check if the student is already enrolled
        const enrollmentsResponse = await fetch(`${BASE_API}/courses/enrollments?studentId=${activeStudent.studentId}`);
        if (!enrollmentsResponse.ok) {
            alert('Error fetching current enrollments. Please try again.');
            return;
        }

        const enrollments = await enrollmentsResponse.json();
        const alreadyEnrolled = enrollments.some(course => {
            try {
                const [label, id] = course.split(' - ')[0].split(': ');
                return label === 'Course ID' && id === courseId.toString();
            } catch {
                return false;
            }
        });

        if (alreadyEnrolled) {
            alert('You are already enrolled in this course.');
            return;
        }

        // Enroll in the course
        const enrollResponse = await fetch(`${BASE_API}/courses/enroll?studentId=${activeStudent.studentId}&courseId=${courseId}`, {
            method: 'POST'
        });

        if (enrollResponse.ok) {
            alert('Successfully enrolled in the course!');
            await loadEnrollments?.();
            await fetchCourses?.();
        } else {
            const errorText = await enrollResponse.text();
            alert(errorText || 'Enrollment failed. Please try again.');
        }
    } catch (error) {
        alert('Error during enrollment process: ' + error.message);
    }
}

// Helper function if using JWT
function getAuthToken() {
    return localStorage.getItem('jwtToken'); // Or your token storage method
}

async function loadEnrollments() {
    const enrollmentsList = document.getElementById('enrollments-list');
    
    try {
        enrollmentsList.innerHTML = '<li class="p-4 text-gray-500">Loading your enrollments...</li>';
        
        const response = await fetch(
            `${BASE_API}/courses/enrollments?studentId=${activeStudent.studentId}`,
            { headers: { 'Content-Type': 'application/json' } }
        );
        
        if (!response.ok) throw new Error('Failed to load enrollments');
        
        const enrollments = await response.json();
        
        enrollmentsList.innerHTML = '';
        enrollments.forEach(enrollment => {
            const li = document.createElement('li');
            li.className = 'flex justify-between items-center p-4 bg-gray-50 rounded-lg mb-2';
            li.innerHTML = `
                <div>
                    <span class="font-medium">${enrollment.courseTitle || 'Untitled Course'}</span>
                    <p class="text-sm text-gray-600">ID: ${enrollment.courseId || 'N/A'}</p>
                </div>
                <button onclick="withdrawCourse(${enrollment.courseId})"
                        class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition">
                    Withdraw
                </button>
            `;
            enrollmentsList.appendChild(li);
        });
    } catch (error) {
        enrollmentsList.innerHTML = `<li class="p-4 text-red-500">${error.message}</li>`;
    }
}

async function withdrawCourse(courseId) {
    if (!confirm('Are you sure you want to withdraw from this course?')) return;
    
    try {
        const response = await fetch(
            `${BASE_API}/courses/withdraw?studentId=${activeStudent.studentId}&courseId=${courseId}`,
            {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' }
            }
        );

        const text = await response.text();
        
        if (response.ok) {
            // Show success message
            alert(text || 'Successfully withdrawn from course');
            // Refresh enrollments, courses, and invoices
            await Promise.all([loadEnrollments(), fetchCourses(), loadInvoices()]);
        } else {
            // Show error message
            throw new Error(text || 'Withdrawal failed');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function loadInvoices() {
    try {
        const status = document.getElementById('filter-status').value;
        const type = document.getElementById('filter-type').value;
        
        const response = await fetch(
            `${BASE_API}/courses/${activeStudent.studentId}/bills?${new URLSearchParams({
                ...(status && {status}),
                ...(type && {type})
            })}`
        );

        const invoices = await response.json();
        renderInvoices(invoices);
        
    } catch (error) {
        console.error("Failed to load invoices:", error);
        document.getElementById('invoices-list').innerHTML = `
            <li class="error">Error loading invoices</li>
        `;
    }
}

function renderInvoices(invoices) {
    const container = document.getElementById('invoices-list');
    container.innerHTML = '';

    invoices.forEach(invoice => {
        const statusColor = invoice.status === 'CANCELLED' ? 'text-red-500' :
                          invoice.status === 'PAID' ? 'text-green-500' :
                          'text-yellow-500';
        
        const div = document.createElement('div');
        div.className = 'invoice-item p-4 border rounded-lg mb-2';
        div.innerHTML = `
            <div class="grid grid-cols-3 gap-4 items-center">
                <div>
                    <p class="invoice-id font-medium">ID: ${invoice.id || 'N/A'}</p>
                    ${invoice.courseId ? `<p class="text-sm text-gray-600">Course ID: ${invoice.courseId}</p>` : ''}
                </div>
                <div>
                    <p>Amount: $${invoice.amount?.toFixed(2) || '0.00'}</p>
                </div>
                <div class="${statusColor} invoice-status">
                    <p>Status: ${invoice.status || 'Unknown'}</p>
                </div>
            </div>
        `;
        container.appendChild(div);
    });
}

async function saveProfileChanges() {
    const name = document.getElementById('edit-firstname').value;
    const surname = document.getElementById('edit-lastname').value;
    
    if (!name && !surname) {
        alert('Please enter at least one field to update.');
        return;
    }

    try {
        const response = await fetch(`${BASE_API}/${activeStudent.studentId}?name=${encodeURIComponent(name)}&surname=${encodeURIComponent(surname)}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });

        if (response.ok) {
            activeStudent = await response.json();
            localStorage.setItem('activeStudent', JSON.stringify(activeStudent));
            document.getElementById('profile-name').textContent = 
                `${activeStudent.firstName} ${activeStudent.lastName}`;
            document.getElementById('edit-firstname').value = '';
            document.getElementById('edit-lastname').value = '';
            alert('Profile updated successfully');
        } else {
            const error = await response.text();
            alert(error || 'Profile update failed');
        }
    } catch (error) {
        alert('Error updating profile');
    }
}

async function verifyGraduation() {
    try {
        const response = await fetch(`${BASE_API}/graduation/${activeStudent.studentId}`);
        if (response.ok) {
            const isEligible = await response.json();
            document.getElementById('graduation-result').textContent = isEligible
                ? 'Congratulations! You are eligible to graduate.'
                : 'You are not eligible to graduate due to active courses or non-cancelled invoices.';
        } else {
            const error = await response.text();
            alert(error || 'Failed to check eligibility');
        }
    } catch (error) {
        alert('Error checking graduation eligibility');
    }
}

async function removeAccount() {
    if (!confirm('Are you sure you want to delete your account? This is permanent.')) return;
    
    try {
        const response = await fetch(`${BASE_API}/remove/${activeStudent.studentId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('Account deleted successfully');
            performLogout();
        } else {
            const error = await response.text();
            alert(error || 'Account deletion failed');
        }
    } catch (error) {
        alert('Error deleting account');
    }
}

function performLogout() {
    localStorage.removeItem('activeStudent');
    activeStudent = null;
    toggleLoginModal();
}