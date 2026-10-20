# QueueNow - Smart Queue Management System

## Project Overview

QueueNow is a web-based queue management system that allows customers to join a queue digitally and receive a token number. It helps reduce physical waiting lines and makes queue handling easier.

## Features

- Customers can join the queue by entering their name.
- A unique token number is assigned to each customer.
- Customer details are stored using MongoDB.
- REST APIs manage queue operations.
- QR codes are generated for token details.
- Empty customer names and QR code inputs are validated.

## Technologies Used

- Java 17
- Spring Boot
- Maven
- MongoDB Atlas
- HTML, CSS and JavaScript
- ZXing QR Code Library
- Git and GitHub
- Cloudflare Tunnel for public access

## Project Structure

- `src/main/java` - Java application code
- `src/main/resources/static` - Frontend files
- `QueueController.java` - Queue and QR code API endpoints
- `QueueService.java` - Queue management logic
- `QRCodeService.java` - QR code generation
- `pom.xml` - Maven dependencies and configuration

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/queue/join` | Join the queue |
| GET | `/api/queue` | Retrieve queue entries |
| GET | `/api/queue/qr?text=QueueNow` | Generate a QR code |

### Join Queue Request

Send a POST request to `/api/queue/join` with JSON:

```json
{
  "name": "Mansi"
}
```

### Example Response

```json
{
  "message": "You have joined the queue.",
  "tokenNumber": 5,
  "customerName": "Mansi"
}
```

*The token number shown is an example.*

## Setup Instructions

1. Install Java 17 or a compatible version.
2. Install Git and Maven, or use the Maven Wrapper if included.
3. Set the `MONGODB_URI` environment variable to your MongoDB Atlas connection string.
4. Keep database credentials in environment variables; do not commit secrets.
5. Run the Spring Boot application from your IDE or using Maven.
6. Open `http://localhost:8080/join.html` in your browser.

## Run with Docker Compose

The frontend is served by the Spring Boot application from `src/main/resources/static`, so the project runs as a single container.

1. Create a `.env` file in the project root with:

   ```dotenv
   MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>/<database>?retryWrites=true&w=majority
   ```

   Generated QR codes use the current public host from the hosting proxy, or the machine's LAN address when running locally. For emergency access from mobile data through a local Cloudflare Quick Tunnel, optionally add `LOCAL_PUBLIC_URL` with the current tunnel URL. Proxy headers take precedence, so Render continues using its own public domain.
2. Start the app from the project root:

   ```bash
   docker compose --env-file .env -f docker/docker-compose.yml up --build
   ```
3. Open `http://localhost:8080`.

The root `.dockerignore` excludes local environment files and build output. Do not commit `.env`.

## Public Access Using Cloudflare Tunnel

Cloudflare Tunnel allows QueueNow to be accessed through a public URL without requiring customers to connect to the same Wi-Fi network as the host computer.

### Steps to Start the Tunnel

1. Start the Spring Boot application on port `8080`.
2. Open a separate terminal window.
3. Run the following command:

   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

4. Copy the generated `trycloudflare.com` URL. To embed it in locally generated QR codes for customers on mobile data, stop and restart the app with `LOCAL_PUBLIC_URL` set:

   ```powershell
   $env:LOCAL_PUBLIC_URL = "https://your-current-tunnel.trycloudflare.com"
   .\mvnw.cmd spring-boot:run
   ```

5. Open the generated URL to access QueueNow from another internet connection.

### Important Notes

- The Spring Boot application and Cloudflare Tunnel must remain running on the host computer.
- Quick Tunnel URLs are temporary and may change when the tunnel is restarted.
- `LOCAL_PUBLIC_URL` is optional and intended for local emergency access; do not set it on the Render service.
- Public access should be tested from another network to confirm that customers can join the queue.
- Cloudflare provides public connectivity; it does not itself manage queue data or make the application permanently hosted.

## QR Code

QueueNow uses the ZXing library to generate QR codes containing the supplied text, such as customer token details.


## Author

Developed as a QueueNow project.
