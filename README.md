# AtlasMentor

A Spring Boot application for managing mentor-mentee relationships and hierarchical structures within an educational organization.

## Overview

AtlasMentor is a comprehensive management system designed to handle:
- Student registration and management
- Counselor hierarchy management
- Employee assignment and tracking
- JWT-based authentication and authorization
- Email notifications

## Tech Stack

- **Framework**: Spring Boot 4.0.5
- **Java Version**: 17
- **Database**: PostgreSQL
- **Authentication**: JWT (JSON Web Tokens)
- **Build Tool**: Maven
- **Security**: Spring Security
- **Email**: Spring Boot Mail Starter

## Features

- **User Management**: Secure user registration and authentication
- **Hierarchy Management**: Organize counselors and mentors in hierarchical structures
- **Student Assignment**: Assign students to counselors and mentors
- **Email Notifications**: Automated email communications
- **RESTful APIs**: Well-designed REST endpoints for all operations

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL database
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd atlasmentor
```

2. Configure the database in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/atlasmentor
spring.datasource.username=your-username
spring.datasource.password=your-password
```

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

## API Documentation

The application provides RESTful APIs for managing:
- User authentication and registration
- Student management
- Counselor hierarchy operations
- Employee assignments

For detailed API documentation, refer to the `STUDENT_REGISTRATION_API.md` file.

## Project Structure

```
src/
├── main/
│   ├── java/com/lab/atlasmentor/
│   │   ├── controller/     # REST controllers
│   │   ├── model/          # Entity classes
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── repository/     # JPA repositories
│   │   ├── service/        # Business logic
│   │   ├── config/         # Configuration classes
│   │   └── util/           # Utility classes
│   └── resources/
│       ├── application.properties
│       └── static/         # Static resources
└── test/                   # Test classes
```

## Configuration

Key configuration files:
- `application.properties` - Database and application settings
- `SecurityConfig.java` - Security and JWT configuration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For any questions or support, please contact the development team.
