# Live Classes Service - Technical Implementation Report

## Executive Summary

The Live Classes Service is a comprehensive Spring Boot microservice designed to facilitate real-time video conferencing, live class sessions, and conference management. The system integrates with VideoSDK for video streaming capabilities, AWS DynamoDB for data persistence, and AWS S3 for recording storage. This service is part of a larger microservices architecture and communicates with other services through Feign clients and Eureka service discovery.

**Version:** 0.0.1-SNAPSHOT  
**Framework:** Spring Boot 3.2.0  
**Java Version:** 17  
**Build Tool:** Maven

---

## 1. System Architecture

### 1.1 Technology Stack

**Core Framework:**
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Java 17

**Cloud & Infrastructure:**
- Netflix Eureka Client (Service Discovery)
- OpenFeign (Inter-service Communication)
- AWS DynamoDB (NoSQL Database)
- AWS S3 (Object Storage)

**Third-Party Integrations:**
- VideoSDK API (Video Conferencing Platform)
- JWT (JSON Web Tokens) for authentication

**Development Tools:**
- Lombok (Code Generation)
- Spring DevTools (Development Support)

### 1.2 Architectural Pattern

The service follows a layered architecture pattern:

```
Controller Layer → Service Layer → Repository Layer → External Services
```

- **Controllers:** Handle HTTP requests and responses
- **Services:** Implement business logic and orchestration
- **Repositories:** Manage data persistence with DynamoDB
- **External Services:** VideoSDK API, S3 Storage, Enrollment Service

---

## 2. Core Features & Capabilities

### 2.1 Live Class Management

**Purpose:** Enable trainers to conduct live classes for enrolled students in specific courses.

**Key Features:**
- Create live class sessions with course association
- Start and end live classes
- Join live classes with enrollment verification
- Recording management (start/stop)
- Course-based session listing

**Access Control:**
- Only trainers can create and manage live classes
- Students must be enrolled in the course to join
- Role-based authorization through enrollment service

### 2.2 Generic Session Management

**Purpose:** Provide flexible session creation for general-purpose meetings.

**Key Features:**
- Create standalone sessions without course association
- Session lifecycle management (create, start, end)
- Recording capabilities
- Participant management

**Access Control:**
- Session organizer has full control
- Token-based authentication for participants

### 2.3 Conference Management

**Purpose:** Support large-scale conference and webinar scenarios.

**Key Features:**
- Conference creation and scheduling
- Multi-participant support
- Recording functionality
- Conference lifecycle management

**Access Control:**
- Organizer-controlled conference management
- Token-based participant authentication

### 2.4 Recording Management

**Automated Recording Workflow:**
1. Recording initiated by session organizer
2. VideoSDK captures video stream
3. Recording stored temporarily by VideoSDK
4. Webhook notification on recording completion
5. Automatic upload to AWS S3
6. S3 URL stored in DynamoDB
7. Recording accessible through session metadata

**Storage Configuration:**
- Bucket: kodemilabs-recordings
- Region: ap-south-1
- Format: MP4
- Path Structure: recordings/{sessionId}.mp4

---

## 3. Data Model

### 3.1 LiveClassEntity

**DynamoDB Table:** LiveClass

**Attributes:**
- `sessionId` (Hash Key): Unique session identifier
- `courseId` (GSI): Associated course identifier
- `trainerId` (GSI): Trainer/instructor identifier
- `roomId` (GSI): VideoSDK room identifier
- `title`: Session title
- `description`: Session description
- `sessionType`: Type classification (LIVE_CLASS)
- `status`: Current status (CREATED, STARTED, ENDED)
- `scheduledAt`: Scheduled start time
- `startedAt`: Actual start timestamp
- `endedAt`: End timestamp
- `maxParticipants`: Maximum allowed participants
- `isRecording`: Recording status flag
- `recordingUrl`: S3 URL of recorded session
- `createdAt`: Creation timestamp

**Global Secondary Indexes:**
- course-index: Query by courseId
- trainer-index: Query by trainerId
- room-index: Query by roomId

### 3.2 SessionEntity

**DynamoDB Table:** Session

**Attributes:**
- `sessionId` (Hash Key): Unique session identifier
- `organizerId` (GSI): Session organizer identifier
- `roomId` (GSI): VideoSDK room identifier
- `title`: Session title
- `description`: Session description
- `sessionType`: Type classification (SESSION)
- `status`: Current status
- `scheduledAt`: Scheduled start time
- `startedAt`: Actual start timestamp
- `endedAt`: End timestamp
- `maxParticipants`: Maximum allowed participants
- `isRecording`: Recording status flag
- `recordingId`: VideoSDK recording identifier
- `recordingUrl`: S3 URL of recorded session
- `createdAt`: Creation timestamp

**Global Secondary Indexes:**
- organizer-index: Query by organizerId
- room-index: Query by roomId

### 3.3 ConferenceEntity

**DynamoDB Table:** Conference

**Attributes:**
- `conferenceId` (Hash Key): Unique conference identifier
- `organizerId` (GSI): Conference organizer identifier
- `roomId` (GSI): VideoSDK room identifier
- `title`: Conference title
- `description`: Conference description
- `sessionType`: Type classification (CONFERENCE)
- `status`: Current status
- `scheduledAt`: Scheduled start time
- `startedAt`: Actual start timestamp
- `endedAt`: End timestamp
- `maxParticipants`: Maximum allowed participants
- `isRecording`: Recording status flag
- `recordingUrl`: S3 URL of recorded conference
- `createdAt`: Creation timestamp

**Global Secondary Indexes:**
- organizer-index: Query by organizerId
- room-index: Query by roomId

---

## 4. API Endpoints

### 4.1 Live Class API (`/api/v1/live-class`)

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/create` | Create new live class | Trainer only |
| POST | `/start/{sessionId}` | Start live class | Creator only |
| GET | `/join/{sessionId}` | Join live class | Enrolled users |
| GET | `/course/{courseId}` | List classes by course | Public |
| POST | `/recording/start/{sessionId}` | Start recording | Creator only |
| POST | `/recording/stop/{sessionId}` | Stop recording | Creator only |

### 4.2 Session API (`/api/v1/session`)

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/create` | Create new session | Authenticated |
| POST | `/start/{sessionId}` | Start session | Organizer only |
| GET | `/join/{sessionId}` | Join session | Authenticated |
| POST | `/recording/start/{sessionId}` | Start recording | Organizer only |
| POST | `/recording/stop/{sessionId}` | Stop recording | Organizer only |

### 4.3 Conference API (`/api/v1/conference`)

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/create` | Create new conference | Authenticated |
| POST | `/start/{conferenceId}` | Start conference | Organizer only |
| GET | `/join/{conferenceId}` | Join conference | Authenticated |
| POST | `/recording/start/{conferenceId}` | Start recording | Organizer only |
| POST | `/recording/stop/{conferenceId}` | Stop recording | Organizer only |

### 4.4 Webhook API (`/webhooks`)

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/videosdk` | VideoSDK webhook handler | VideoSDK signature |

---

## 5. External Service Integrations

### 5.1 VideoSDK Integration

**Purpose:** Real-time video conferencing infrastructure

**Implemented Features:**
- JWT token generation for VideoSDK authentication
- Room creation and management
- Room validation
- Recording start/stop operations
- Room deactivation

**API Configuration:**
- Endpoint: https://api.videosdk.live/v2
- Authentication: JWT with API key and secret
- Token Expiry: 24 hours
- Permissions: allow_join, allow_mod

**Key Operations:**

**Token Generation:**
```
Claims: apikey, permissions, version
Algorithm: HS256
Expiry: 24 hours
```

**Room Management:**
- Create Room: POST /rooms
- Validate Room: GET /rooms/validate/{roomId}
- Deactivate Room: POST /rooms/deactivate

**Recording Management:**
- Start Recording: POST /recordings/start
- Stop Recording: POST /recordings/stop
- Storage: Direct to S3 with credentials

### 5.2 AWS DynamoDB Integration

**Configuration:**
- Region: eu-north-1
- SDK Version: AWS SDK v1 (1.12.700)
- Mapper: DynamoDBMapper
- Credentials: DefaultAWSCredentialsProviderChain (IAM Role)

**Tables:**
- LiveClass
- Session
- Conference

**Features:**
- Global Secondary Indexes for efficient querying
- Automatic credential management via IAM roles
- DynamoDBMapper for object-relational mapping

### 5.3 AWS S3 Integration

**Configuration:**
- Bucket: kodemilabs-recordings
- Region: ap-south-1
- SDK Version: AWS SDK v2 (2.25.0)
- Credentials: Default credential chain

**Usage:**
- Recording storage
- Automatic upload from VideoSDK webhook
- Public URL generation for playback

**Upload Process:**
1. Receive recording URL from VideoSDK webhook
2. Download recording stream
3. Upload to S3 with content type video/mp4
4. Generate and store S3 URL
5. Update session entity with recording URL

### 5.4 Enrollment Service Integration

**Purpose:** Verify user enrollment and role in courses

**Implementation:** OpenFeign Client

**Endpoints:**
- `GET /api/v1/enrollment/status/{courseId}` - Check enrollment status
- `GET /api/v1/enrollments/session/{sessionId}/status` - Session enrollment status

**Service Discovery:** Via Eureka (service name: enrollment-progress-service)

**Response Model:**
```java
EnrollmentStatusResponse {
    boolean enrolled
    String role (TRAINER/STUDENT)
}
```

---

## 6. Security Implementation

### 6.1 JWT Authentication

**Configuration:**
- Secret Key: Base64 encoded symmetric key
- Algorithm: HS256
- Token Format: Bearer token

**JWT Claims Extraction:**
- `userId`: User identifier
- `name`: User display name
- `role`: User role

**Utility Class:** JwtUtil
- Token parsing and validation
- Claims extraction
- Bearer prefix handling

### 6.2 Authorization Model

**Role-Based Access Control:**

**Live Classes:**
- Create: TRAINER role required
- Start/End: Creator only
- Join: Enrolled users only
- Recording: Creator only

**Sessions:**
- Create: Any authenticated user
- Start/End: Organizer only
- Join: Any authenticated user
- Recording: Organizer only

**Conferences:**
- Create: Any authenticated user
- Start/End: Organizer only
- Join: Any authenticated user
- Recording: Organizer only

### 6.3 Service-to-Service Security

**Feign Client:**
- Token propagation via Authorization header
- Service discovery through Eureka
- Circuit breaker patterns (implicit via Spring Cloud)

---

## 7. Configuration Management

### 7.1 Application Configuration (application.yaml)

**Server Configuration:**
```yaml
server.port: 8095
```

**Service Registration:**
```yaml
spring.application.name: live-classes-service
eureka.client.service-url.defaultZone: http://localhost:8761/eureka/
```

**DynamoDB Configuration:**
```yaml
dynamodb.region: eu-north-1
dynamodb.bucket-name: kodemilabs-recordings
```

**VideoSDK Configuration:**
```yaml
videosdk.api.key: [API_KEY]
videosdk.api.secret: [API_SECRET]
videosdk.api.endpoint: https://api.videosdk.live/v2
```

**AWS S3 Configuration:**
```yaml
aws.s3.bucket: kodemilabs-recordings
aws.s3.region: ap-south-1
aws.s3.accessKey: [ACCESS_KEY]
aws.s3.secretKey: [SECRET_KEY]
```

**JWT Configuration:**
```yaml
jwt.secret: [BASE64_SECRET]
```

### 7.2 Bean Configuration

**DynamoDBConfig:**
- AmazonDynamoDB client with IAM credentials
- DynamoDBMapper for ORM operations
- Region-specific configuration

**S3Config:**
- S3Client with region configuration
- Default credential provider chain
- Apache HTTP client for connections

---

## 8. Webhook Processing

### 8.1 VideoSDK Webhook Handler

**Endpoint:** POST /webhooks/videosdk

**Supported Events:**
- `recording-stopped`: Triggered when recording completes

**Processing Flow:**
1. Receive webhook payload
2. Extract webhook type and data
3. For recording-stopped events:
   - Extract roomId and fileUrl
   - Find associated session by roomId
   - Download recording from VideoSDK
   - Upload to S3
   - Update session with S3 URL
   - Set isRecording to false

**Error Handling:**
- Comprehensive logging
- Graceful failure handling
- HTTP 500 response on errors
- Session not found warnings

---

## 9. Data Transfer Objects (DTOs)

### 9.1 Request DTOs

**CreateLiveClassRequest:**
- title: String
- description: String
- courseId: String
- scheduledAt: String
- maxParticipants: Integer

**CreateSessionRequest:**
- title: String
- description: String
- scheduledAt: String
- maxParticipants: int

**CreateConferenceRequest:**
- title: String
- description: String
- scheduledAt: String
- maxParticipants: Integer

### 9.2 Response DTOs

**SessionResponseDTO:**
- sessionId: String
- courseId: String
- roomId: String
- title: String
- status: String
- scheduledAt: String
- createdAt: String
- maxParticipants: Integer
- isRecording: Boolean
- conferenceId: String (for conferences)
- organizerId: String (for conferences)

**JoinResponseDTO:**
- token: String (VideoSDK token)
- roomId: String
- sessionId: String
- role: String

**EnrollmentStatusResponse:**
- enrolled: boolean
- role: String

---

## 10. Service Layer Implementation

### 10.1 LiveClassServiceImpl

**Dependencies:**
- LiveClassRepository
- EnrollmentClient
- JwtUtil
- VideoSDKService

**Key Methods:**
- `createLiveClass()`: Validates trainer role, creates VideoSDK room, persists entity
- `startLiveClass()`: Validates creator, updates status to STARTED
- `joinLiveClass()`: Validates enrollment, generates VideoSDK token
- `getLiveClassesByCourse()`: Retrieves all classes for a course
- `startRecording()`: Initiates VideoSDK recording with S3 storage
- `stopRecording()`: Stops active recording
- `endLiveClass()`: Deactivates room, updates status to ENDED

### 10.2 SessionServiceImpl

**Dependencies:**
- SessionRepository
- JwtUtil
- VideoSDKService

**Key Methods:**
- `createSession()`: Creates VideoSDK room, persists session entity
- `startSession()`: Validates organizer, updates status
- `joinSession()`: Generates VideoSDK token for participant
- `startRecording()`: Initiates recording
- `stopRecording()`: Stops recording
- `endSession()`: Deactivates room, ends session

### 10.3 ConferenceServiceImpl

**Dependencies:**
- ConferenceRepository
- JwtUtil
- VideoSDKService

**Key Methods:**
- `createConference()`: Creates VideoSDK room, persists conference entity
- `startConference()`: Validates organizer, updates status
- `joinConference()`: Generates VideoSDK token
- `startRecording()`: Initiates recording with S3 storage
- `stopRecording()`: Stops recording
- `endConference()`: Deactivates room, ends conference

### 10.4 VideoSDKService

**Responsibilities:**
- JWT token generation for VideoSDK API
- Room lifecycle management
- Recording operations
- API communication

**Key Methods:**
- `generateToken()`: Creates JWT with VideoSDK claims
- `createRoom()`: Creates new VideoSDK room
- `validateRoom()`: Validates room existence
- `startRecording()`: Starts recording with S3 configuration
- `stopRecording()`: Stops active recording
- `endRoom()`: Deactivates VideoSDK room

### 10.5 S3Service

**Responsibilities:**
- Recording upload to S3
- Stream handling from VideoSDK URLs

**Key Methods:**
- `uploadRecording()`: Downloads from VideoSDK URL, uploads to S3

---

## 11. Repository Layer

### 11.1 LiveClassRepository

**Operations:**
- `save()`: Persist or update entity
- `findById()`: Retrieve by sessionId
- `findByCourseId()`: Query by course (uses GSI)
- `findByRoomId()`: Query by VideoSDK room (uses GSI)

### 11.2 SessionRepository

**Operations:**
- `save()`: Persist or update entity
- `findById()`: Retrieve by sessionId

### 11.3 ConferenceRepository

**Operations:**
- `save()`: Persist or update entity
- `findById()`: Retrieve by conferenceId

---

## 12. Enumerations

### 12.1 SessionStatus
- SCHEDULED
- LIVE
- ENDED

### 12.2 SessionType
- LIVE_CLASS
- CONFERENCE

---

## 13. Error Handling & Validation

### 13.1 Authorization Errors

**Scenarios:**
- Non-trainer attempting to create live class
- Non-creator attempting to start/end session
- Non-enrolled user attempting to join live class
- Non-organizer attempting recording operations

**Response:** RuntimeException with descriptive message

### 13.2 Webhook Processing Errors

**Handling:**
- Comprehensive logging of errors
- Graceful degradation
- HTTP 500 response
- Session not found warnings

### 13.3 External Service Failures

**VideoSDK API:**
- RestTemplate exception handling
- Validation failures logged
- Room validation returns boolean

**S3 Upload:**
- Exception wrapped in RuntimeException
- Descriptive error messages

---

## 14. Deployment Configuration

### 14.1 Maven Build

**Artifact Name:** LIVE SERVICE
**Packaging:** JAR
**Compiler:** Java 17

**Plugins:**
- maven-compiler-plugin (Lombok annotation processing)
- spring-boot-maven-plugin (Executable JAR creation)

### 14.2 Service Discovery

**Eureka Client Configuration:**
- Service Name: live-classes-service
- Eureka Server: http://localhost:8761/eureka/
- IP Address Preference: Enabled

### 14.3 AWS Deployment Considerations

**IAM Roles Required:**
- DynamoDB read/write access
- S3 read/write access to recordings bucket
- CloudWatch logs (implicit)

**Environment Variables:**
- JWT_SECRET
- VIDEOSDK_API_KEY
- VIDEOSDK_API_SECRET
- AWS_REGION (for DynamoDB and S3)

---

## 15. Monitoring & Logging

### 15.1 Logging Implementation

**Framework:** SLF4J with Lombok @Slf4j

**Log Levels:**
- INFO: Room creation, recording operations, webhook processing
- WARN: Session not found, validation failures
- ERROR: Webhook processing failures, external service errors

**Key Log Points:**
- VideoSDK room creation
- Recording start/stop
- Webhook event processing
- S3 upload completion
- Room validation failures

---

## 16. Testing

### 16.1 Test Structure

**Test Package:** com.example.live_classes_service

**Test Class:** LiveClassesDemoApplicationTests

**Framework:** Spring Boot Test

---

## 17. Dependencies Summary

### 17.1 Spring Framework
- spring-boot-starter-web
- spring-boot-starter-test
- spring-boot-devtools

### 17.2 Spring Cloud
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-openfeign

### 17.3 AWS SDK
- aws-java-sdk-dynamodb (v1.12.700)
- aws-java-sdk-s3 (v1.12.700)
- AWS SDK v2 S3 (v2.25.0)
- AWS SDK v2 DynamoDB (v2.25.0)

### 17.4 Security & JWT
- jjwt-api (v0.11.5)
- jjwt-impl (v0.11.5)
- jjwt-jackson (v0.11.5)

### 17.5 Utilities
- lombok (annotation processing)

---

## 18. Future Enhancement Opportunities

### 18.1 Potential Improvements

1. **Participant Management:**
   - Track active participants
   - Participant limit enforcement
   - Waiting room functionality

2. **Analytics & Reporting:**
   - Session duration tracking
   - Participant engagement metrics
   - Recording view statistics

3. **Advanced Recording Features:**
   - Multiple quality options
   - Automatic transcription
   - Recording highlights/chapters

4. **Notification System:**
   - Session start notifications
   - Recording ready notifications
   - Reminder notifications

5. **Enhanced Security:**
   - Rate limiting
   - IP whitelisting
   - Enhanced webhook signature verification

6. **Performance Optimization:**
   - Caching layer (Redis)
   - Async processing for webhooks
   - Connection pooling optimization

7. **Resilience Patterns:**
   - Circuit breakers for external services
   - Retry mechanisms
   - Fallback strategies

---

## 19. Conclusion

The Live Classes Service provides a robust, scalable solution for managing real-time video sessions across multiple use cases (live classes, generic sessions, and conferences). The service successfully integrates with VideoSDK for video infrastructure, AWS DynamoDB for data persistence, and AWS S3 for recording storage. The implementation follows Spring Boot best practices, maintains clear separation of concerns, and provides comprehensive API endpoints for all session management operations.

The architecture supports future scalability through microservices patterns, service discovery, and cloud-native AWS services. The webhook-based recording workflow ensures automated processing and storage of session recordings without manual intervention.

---

**Document Version:** 1.0  
**Last Updated:** March 17, 2026  
**Prepared By:** Technical Documentation Team