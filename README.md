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
3. Configure the MongoDB connection in the application configuration.
4. Keep database credentials in environment variables; do not commit secrets.
5. Run the Spring Boot application from your IDE or using Maven.
6. Open `http://localhost:8080/join.html` in your browser.

## Public Access Using Cloudflare Tunnel

Cloudflare Tunnel allows QueueNow to be accessed through a public URL without requiring customers to connect to the same Wi-Fi network as the host computer.

### Steps to Start the Tunnel

1. Start the Spring Boot application on port `8080`.
2. Open a separate terminal window.
3. Run the following command:

   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

4. Wait for Cloudflare to generate a public `trycloudflare.com` URL.
5. Open the generated URL to access QueueNow from another internet connection.

### Important Notes

- The Spring Boot application and Cloudflare Tunnel must remain running on the host computer.
- Quick Tunnel URLs are temporary and may change when the tunnel is restarted.
- Public access should be tested from another network to confirm that customers can join the queue.
- Cloudflare provides public connectivity; it does not itself manage queue data or make the application permanently hosted.

## QR Code

QueueNow uses the ZXing library to generate QR codes containing the supplied text, such as customer token details.

## Future Improvements

- Display live queue updates.
- Add an administrator dashboard.
- Allow customers to check their current queue position.
- Deploy the application online using a persistent hosting solution.

## Author

Developed as a QueueNow project.