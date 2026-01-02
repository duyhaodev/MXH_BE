<h1 align="center">Threads Clone Backend 🚀</h1>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?style=flat-square&logo=springboot" />
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql" />
  <img alt="Socket.IO" src="https://img.shields.io/badge/Socket.IO-Realtime-black?style=flat-square&logo=socket.io" />
  <img alt="Cloudinary" src="https://img.shields.io/badge/Cloudinary-Media-blueviolet?style=flat-square&logo=cloudinary" />
</p>

> **Project Description**
> 
> This is the **Backend** component of the **Threads Clone** project. It serves as the core engine of the system, handling business logic, data persistence, and providing a robust set of RESTful APIs for the frontend application. The system is designed to handle social media interactions, real-time messaging, and notifications.

## 🔗 Related Repository

*   **Frontend (React):** [MXH_FE Repository](https://github.com/duyhaodev/MXH_FE)

## ✨ Key Features

*   **🔐 Authentication & Security:**
    *   JWT-based stateless authentication (Access Token & Refresh Token).
    *   Email verification via OTP (JavaMailSender).
    *   Secure password hashing with BCrypt.
*   **📝 Post Management:**
    *   Create text posts with multimedia support (Images/Videos via Cloudinary).
    *   Repost, Quote, and Delete functionality.
    *   News Feed generation.
*   **💬 Interaction:**
    *   Comment system with multi-level replies.
    *   Like/Unlike posts and comments.
*   **⚡ Real-time Features (Socket.IO):**
    *   Instant push notifications (Likes, Comments, Follows).
    *   Real-time 1-on-1 private messaging/chat.
    *   Online/Offline user status.
*   **👥 Social Graph:**
    *   Follow/Unfollow users.
    *   User search and profile management.

## 🛠 Tech Stack

*   **Core:** Java 21, Spring Boot 3.5.x
*   **Database:** PostgreSQL, Spring Data JPA
*   **Security:** Spring Security, OAuth2 Resource Server, Nimbus JOSE + JWT
*   **Real-time:** Netty-SocketIO
*   **Storage:** Cloudinary SDK (for image/video storage)
*   **Utilities:** MapStruct (DTO Mapping), Lombok, Maven

## 🚀 Getting Started

### Prerequisites

*   Java Development Kit (JDK) 21
*   Maven 3.x
*   PostgreSQL
*   A Cloudinary Account (for media upload)
*   A Gmail Account (for sending OTPs via SMTP)

### Installation

To keep the project organized, it is recommended to create a parent folder for both Backend and Frontend:

1.  **Create a parent folder and clone the project**
    ```bash
    mkdir ThreadsClone
    cd ThreadsClone

    # Clone Backend
    git clone https://github.com/duyhaodev/backend-threads-clone.git Backend

    # Clone Frontend (optional but recommended)
    git clone https://github.com/duyhaodev/MXH_FE.git Frontend
    ```

2.  **Navigate to Backend**
    ```bash
    cd Backend
    ```

3.  **Configure Environment Variables**
    Open `src/main/resources/application.properties` and update the configuration with your own credentials:

    ```properties
    # Database Configuration
    spring.datasource.url=jdbc:postgresql://localhost:5432/YOUR_DB_NAME
    spring.datasource.username=YOUR_DB_USERNAME
    spring.datasource.password=YOUR_DB_PASSWORD

    # JWT Configuration
    jwt.signerKey=YOUR_VERY_LONG_SECRET_KEY_HERE

    # Cloudinary Configuration
    cloudinary.cloud_name=YOUR_CLOUD_NAME
    cloudinary.api_key=YOUR_API_KEY
    cloudinary.api_secret=YOUR_API_SECRET

    # Mail Configuration
    spring.mail.username=your-email@gmail.com
    spring.mail.password=your-app-password
    ```

4.  **Run the Application**
    ```bash
    ./mvnw spring-boot:run
    ```
    The server will start at `http://localhost:8080`.
    The Socket.IO server will start at `port 8099`.

## 📚 API Endpoints Overview

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/auth/token` | Login & get JWT |
| **POST** | `/users` | Register new user |
| **GET** | `/feed` | Get news feed |
| **POST** | `/posts` | Create a new post |
| **POST** | `/messages/create` | Send a private message |
| **GET** | `/api/notifications`| Get user notifications |

## 🤝 Contributing

Contributions, issues and feature requests are welcome!
Feel free to check [issues page](https://github.com/duyhaodev/issues).

## 👤 Author

**DuyHao**

*   Github: [@duyhaodev](https://github.com/duyhaodev)
*   LinkedIn: [@duyhaodev](https://linkedin.com/in/duyhaodev)

## ⭐️ Show your support

Give a ⭐️ if this project helped you!

***