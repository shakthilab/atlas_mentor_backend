# Student Registration API Documentation

## Overview
The student registration API allows students to register through a multi-step form process that captures personal details, academic preferences, and academic background information.

## API Endpoints

### 1. Student Registration
**POST** `/api/students/register`

Registers a new student with all their information from the multi-step form.

#### Request Body
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1234567890",
  "password": "securePassword123",
  
  // Academic Preferences (Step 2)
  "preferredCountry": "United States",
  "preferredUniversity": "Harvard University",
  "course": "Computer Science",
  "intake": "Fall 2024",
  
  // Academic Background (Step 3)
  "referralCode": "REF123",
  "basicAcademicDetails": "Completed high school with 3.8 GPA",
  "optionalNotes": "Interested in scholarship opportunities"
}
```

#### Response
```json
{
  "success": true,
  "message": "Student registration successful. We will contact you soon.",
  "data": {
    "id": 1234567890,
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "status": "LEAD",
    "createdAt": "2024-01-15T10:30:00",
    "academicInfo": {
      "preferredCountry": "United States",
      "preferredUniversity": "Harvard University",
      "course": "Computer Science",
      "intake": "Fall 2024",
      "referralCode": "REF123",
      "basicAcademicDetails": "Completed high school with 3.8 GPA",
      "optionalNotes": "Interested in scholarship opportunities"
    }
  }
}
```

#### Error Response
```json
{
  "success": false,
  "message": "Full name is required",
  "data": null
}
```

### 2. Get Student Details
**GET** `/api/students/{id}`

Retrieves student information by ID.

#### Response
```json
{
  "success": true,
  "message": "Student retrieved successfully",
  "data": {
    "id": 1234567890,
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "status": "LEAD"
  }
}
```

## Form Field Validation

### Required Fields
- `fullName`: Must be between 2-150 characters
- `email`: Valid email format, max 150 characters  
- `phone`: Max 20 characters
- `password`: Min 6 characters

### Optional Fields
- `preferredCountry`
- `preferredUniversity`
- `course`
- `intake`
- `referralCode`
- `basicAcademicDetails`
- `optionalNotes`

## Integration with Frontend

### Step 1: Personal Details
```javascript
const step1Data = {
  fullName: document.getElementById('fullName').value,
  email: document.getElementById('email').value,
  phone: document.getElementById('phone').value,
  password: document.getElementById('password').value
};
```

### Step 2: Academic Preferences
```javascript
const step2Data = {
  preferredCountry: document.getElementById('preferredCountry').value,
  preferredUniversity: document.getElementById('preferredUniversity').value,
  course: document.getElementById('course').value,
  intake: document.getElementById('intake').value
};
```

### Step 3: Academic Background
```javascript
const step3Data = {
  referralCode: document.getElementById('referralCode').value,
  basicAcademicDetails: document.getElementById('basicAcademicDetails').value,
  optionalNotes: document.getElementById('optionalNotes').value
};
```

### Complete Registration
```javascript
const completeRegistration = async () => {
  const registrationData = {
    ...step1Data,
    ...step2Data,
    ...step3Data
  };
  
  try {
    const response = await fetch('/api/students/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registrationData)
    });
    
    const result = await response.json();
    
    if (result.success) {
      // Show success message
      alert('Registration successful!');
      // Redirect to success page or login
    } else {
      // Show error message
      alert('Registration failed: ' + result.message);
    }
  } catch (error) {
    alert('Network error: ' + error.message);
  }
};
```

## Database Schema (Simplified)

### Students Table
```sql
CREATE TABLE students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150) NOT NULL,
  email VARCHAR(150),
  phone VARCHAR(20),
  status ENUM('LEAD', 'REGISTERED', 'LOST') DEFAULT 'LEAD',
  branch_id BIGINT NOT NULL,
  assigned_counsellor_id BIGINT,
  referral_id BIGINT,
  company_id BIGINT,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Student Notes Table
```sql
CREATE TABLE student_notes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  note TEXT,
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Testing the API

### Using curl
```bash
curl -X POST http://localhost:8080/api/students/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test Student",
    "email": "test@example.com",
    "phone": "+1234567890",
    "password": "password123",
    "preferredCountry": "Canada",
    "preferredUniversity": "University of Toronto",
    "course": "Engineering",
    "intake": "Winter 2024"
  }'
```

### Using Postman
1. Set method to POST
2. URL: `http://localhost:8080/api/students/register`
3. Headers: `Content-Type: application/json`
4. Body: Raw JSON with the registration data

## Security Considerations
- Password should be hashed before storing
- Email verification should be implemented
- Input validation is performed on the server side
- Consider implementing rate limiting for registration attempts

## Future Enhancements
- Email verification workflow
- File upload for documents
- Integration with CRM system
- SMS notifications
- Multi-language support
