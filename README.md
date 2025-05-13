# Hyperledger Fabric Basil Management System

This project is a full-stack application for managing basil records using Hyperledger Fabric blockchain technology. It consists of a Spring Boot backend and an Angular frontend.

## Project Structure

```
.
├── application-template/     # Spring Boot Backend
│   ├── src/
│   └── build.gradle
└── fabric-frontend/         # Angular Frontend
    ├── src/
    └── package.json
```

## Prerequisites

- Java 11 or later
- Node.js 14 or later
- Angular CLI
- Hyperledger Fabric test network running locally

## Backend Setup

1. Navigate to the backend directory:
```bash
cd application-template
```

2. Build and run the Spring Boot application:
```bash
./gradlew bootRun
```

The backend will start on `http://localhost:8080`

## Frontend Setup

1. Navigate to the frontend directory:
```bash
cd fabric-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
ng serve
```

The frontend will be available at `http://localhost:4200`

## API Endpoints

### Create Basil
```
POST /api/basil?id={id}&country={country}
```

### Read Basil
```
GET /api/basil/{id}
```

## Features

- Create new basil records
- View existing basil records
- Real-time blockchain integration
- Modern, responsive UI
- CORS enabled for local development

## Development

### Backend
- Spring Boot 2.7.0
- Hyperledger Fabric Gateway SDK
- Gradle build system

### Frontend
- Angular 17
- SCSS styling
- Responsive design

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
