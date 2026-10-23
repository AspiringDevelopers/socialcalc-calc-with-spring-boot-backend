# SocialCalc with Spring Boot Backend

A modern Spring Boot implementation of the Aspiring Investments backend with comprehensive security, database integration, and REST API endpoints.

## Features

- **Authentication & Security**: JWT-based authentication with Spring Security
- **Database Integration**: PostgreSQL with JPA/Hibernate
- **File Management**: User-isolated file storage with version tracking
- **Email Integration**: SMTP email support for notifications
- **In-App Purchases**: Purchase tracking and validation system
- **REST API**: Clean REST API with comprehensive error handling
- **Security**: CORS support, password hashing with BCrypt

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Security**: Spring Security with JWT
- **Database**: PostgreSQL with JPA/Hibernate
- **Authentication**: JWT with custom user principal
- **Email**: Spring Mail with SMTP support
- **Validation**: Bean Validation (Jakarta)
- **Build Tool**: Maven

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 12+

### Environment Variables

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aspiring_investments
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# JWT (required: at least 32 bytes, e.g. `openssl rand -base64 48`;
# the app refuses to start without it unless SPRING_PROFILES_ACTIVE=dev)
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000

# Email (Gmail example)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password

# AWS (Optional)
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=us-east-1
S3_BUCKET=your-s3-bucket
```

### Installation

1. Clone the repository
2. Configure PostgreSQL database
3. Update `application.properties` or set environment variables
4. Install dependencies:
   ```bash
   mvn clean install
   ```
5. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The server will start on `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/logout` - User logout
- `POST /api/auth/forgot-password` - Password reset request
- `POST /api/auth/reset-password` - Password reset

### File Management
- `GET /api/files/save` - List user files
- `POST /api/files/save` - Save file
- `POST /api/files/insert` - Get file content
- `POST /api/files/usersheet` - User sheet operations
- `GET /api/files/runas` - Run applications

### Web App Operations
- `GET /api/webapp` - Web app operations (login check, in-app status)
- `POST /api/webapp` - Web app actions (save, delete, list files, purchases)

### Health & Monitoring
- `GET /api/health` - Application health check
- `GET /api/` - API information

## Database Schema

The application uses PostgreSQL with JPA/Hibernate for ORM:

### Tables
- `users` - User accounts and authentication
- `files` - File storage with user isolation
- `in_app_purchases` - Purchase tracking and limits

### Key Features
- UUID primary keys for security
- Automatic timestamps (created_at, updated_at)
- Foreign key constraints with cascade operations
- Unique constraints for data integrity

## Security Features

- **JWT Authentication**: Stateless authentication with configurable expiration
- **Password Security**: BCrypt hashing with salt
- **User Isolation**: All data operations are user-scoped
- **CORS Support**: Configurable cross-origin resource sharing
- **Input Validation**: Bean validation for request data
- **Custom Security**: Current user injection for controllers

## Configuration

### Security Configuration
- JWT token validation and extraction
- Protected endpoints with authentication requirements
- CORS configuration for frontend integration

### Database Configuration
- PostgreSQL connection with connection pooling
- JPA/Hibernate with automatic schema updates
- Transaction management for data consistency

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package -Pprod
```

### Profile Configuration
- `application.properties` - Default configuration
- `application-dev.properties` - Development environment
- `application-prod.properties` - Production environment

## API Response Format

All API endpoints return a consistent JSON response format:

```json
{
  "result": "ok|fail",
  "data": "response data (when applicable)",
  "message": "error message (when applicable)"
}
```

## Error Handling

The application includes comprehensive error handling:
- Input validation errors
- Authentication/authorization errors
- Database constraint violations
- Service-level business logic errors

## Monitoring & Logging

- Structured logging with different levels
- Health check endpoint for monitoring
- Request/response logging for debugging
- Security event logging

## Production Deployment

1. Set all required environment variables
2. Configure production database
3. Build production JAR:
   ```bash
   mvn clean package -Pprod
   ```
4. Deploy with proper JVM settings:
   ```bash
   java -jar -Xmx512m -Xms256m target/aspiring-investments-backend-0.0.1-SNAPSHOT.jar
   ```

## Architecture

The application follows a layered architecture:
- **Controller Layer**: REST endpoints and request handling
- **Service Layer**: Business logic and transaction management
- **Repository Layer**: Data access with Spring Data JPA
- **Security Layer**: Authentication and authorization
- **Configuration Layer**: Application and security configuration
