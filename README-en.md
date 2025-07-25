# tgDrive - Unlimited Capacity and Speed Cloud Storage

<div align="center">

![GitHub release (latest by date)](https://img.shields.io/github/v/release/SkyDependence/tgDrive)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/SkyDependence/tgDrive/docker-publish.yml)
![Docker Image Size](https://img.shields.io/docker/image-size/nanyangzesi/tgdrive/latest)
![GitHub stars](https://img.shields.io/github/stars/SkyDependence/tgDrive)
![GitHub forks](https://img.shields.io/github/forks/SkyDependence/tgDrive)
![GitHub issues](https://img.shields.io/github/issues/SkyDependence/tgDrive)
![GitHub license](https://img.shields.io/github/license/SkyDependence/tgDrive)

</div>

**tgDrive** is a cloud storage application developed in Java based on Telegram Bot, supporting unlimited capacity and speed for file storage. Through multi-threading technology and optimized transfer strategies, it provides users with an efficient and reliable cloud storage solution.

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Deployment Methods](#deployment-methods)
  - [Docker Compose Deployment](#docker-compose-deployment)
  - [Docker Deployment](#docker-deployment)
  - [Self-Deployment](#self-deployment)
  - [Render Deployment](#render-deployment)
- [Usage Guide](#usage-guide)
- [Advanced Configuration](#advanced-configuration)
  - [PicGo Configuration](#picgo-configuration)
  - [Reverse Proxy](#reverse-proxy)
- [Support and Feedback](#support-and-feedback)

## Features

### Core Advantages

- 🚀 **Breaking Limits**: Completely breaks through the 20MB file size limit of Telegram Bot API
- 📈 **Multi-threaded Transfer**: Uses multi-threaded upload and download technology to maximize bandwidth utilization
- 🔗 **External Links**: Supports image external linking, allowing direct browser access and preview
- 🖼️ **Image Hosting Integration**: Perfect support for PicGo image hosting tool, providing convenient image hosting services
- 🎯 **GIF Optimization**: Solves the issue of Telegram automatically converting GIFs to MP4

### Technical Features

- ⚡ **High Performance**: Developed with Java 17+, ensuring stability and performance
- 🐳 **Containerization**: Provides Docker support, simplifying deployment and maintenance
- 💾 **Data Persistence**: Supports data persistence storage, ensuring data security
- 🔄 **API Support**: Provides complete RESTful API interfaces

## Quick Start

### Online Experience

- [Render Deployment Site (Recommended)](https://render.skydevs.link)
- [Demo Site](https://server.skydevs.link)

### Related Resources

- Frontend Code: [tgDriveFront](https://github.com/SkyDependence/tgDrive-front)
- Latest Version: [Releases](https://github.com/SkyDependence/tgDrive/releases)

## Deployment Methods

### Docker Compose Deployment

>[!TIP]
>📌 **Note**: If your server has limited memory (RAM ≤ 512MB), it's recommended to use the `nanyangzesi/tgdrive:server-latest` image

1. Create `docker-compose.yml` file:

   ```yaml
   services:
     tgdrive:
       image: nanyangzesi/tgdrive:latest
       container_name: tgdrive
       ports:
         - "8085:8085"
       volumes:
         - ./db:/app/db
       restart: always
   ```

2. Start the service:

```bash
docker-compose up -d
```

#### Updating the Image

When using volume mounting, after each image update, you only need to pull the image and restart the container. The database data will not be lost:

```bash
docker compose pull
docker compose up -d
```

### Docker Deployment

Basic deployment command:

```bash
docker pull nanyangzesi/tgdrive:latest
docker run -d -p 8085:8085 --name tgdrive --restart always nanyangzesi/tgdrive:latest
```

#### Migrating Previous Data

If you've run the project before and generated database files in the container, you can manually migrate this data to a persistent directory on the host:

1. Find the old container's ID or name:

   ```bash
   docker ps -a
   ```

2. Copy the database files from the container to the host:

   ```bash
   docker cp <container-name-or-ID>:/app/db ./db
   ```

   - Replace `<container-name-or-ID>` with the actual container identifier.
   - Copy the contents of the `/app/db` folder in the container to the `db` folder in the current directory on the host.

3. Restart the project:

   Use the updated `docker-compose.yml` and restart the project:

   ```bash
   docker compose up -d
   ```

4. Verify the data:

   After startup, the project should be able to read the data from the `./db` folder on the host.

### Self-Deployment

Prerequisites:

- Java 17 or higher version

Deployment steps:

1. Go to the [release page](https://github.com/SkyDependence/tgDrive/releases) to download the latest binary package.
2. Navigate to the directory containing the downloaded binary package.
3. Run the following command:

   ```bash
   java -jar [latest-binary-package-name]
   ```

   For example:

   ```bash
   java -jar tgDrive-0.0.2-SNAPSHOT.jar
   ```

4. After successful execution, visit `localhost:8085` in your browser to start using.

### Render Deployment

> [!TIP]
> Render free deployment requires bank card verification.

### Steps

1. Create a Web Service.

   ![Create Web Service](https://github.com/user-attachments/assets/543abbd1-0b2e-4892-8e46-265539159831)

2. Select Docker image and enter `nanyangzesi/tgdrive:latest`.

   ![Enter Image](https://github.com/user-attachments/assets/09f212c1-886b-424e-8015-a8f96f7e48ee)

3. Choose the free instance.

   ![Choose Free Instance](https://github.com/user-attachments/assets/18506bfa-9dda-4c41-a1eb-6cd7206c6f4b)

4. Scroll to the bottom of the page and click **Deploy Web Service** to complete deployment.

## Usage Guide

After accessing your deployed project URL, you'll see the following page:

![Homepage](https://github.com/user-attachments/assets/ede633bb-053a-49e4-ab2b-faff3c688c77)

Click on the management interface and fill in the bot token:

![image](https://github.com/user-attachments/assets/83d05394-caf1-46ce-acdf-9b9c5611294e)

Don't know how to get the bot token and chatID? Check out [this article](https://skydevs.link/posts/tech/telegram_bot)

After filling in, click submit configuration, scroll down, select the configuration file you just filled in to load, and you can start uploading:

![image](https://github.com/user-attachments/assets/25d1fd3d-d390-4674-9d77-d0d9bc1153fa)

## Advanced Configuration

### PicGo Configuration

> [!TIP]
> Supported from v0.0.4+ onwards.

This project supports quick image uploads in combination with [PicGo](https://github.com/Molunerfinn/PicGo).

#### Preparation

Ensure the PicGo plugin `web-uploader` is installed.

![PicGo Configuration Page](https://github.com/user-attachments/assets/fe52f47e-b2ab-4751-bb65-7ead9ebce2c0)

#### Parameter Description

- **API Address**: Local default is `http://localhost:8085/api/upload`. For server deployment, modify to `http://<server-address>:8085/api/upload`.
- **POST Parameter Name**: Default is `file`.
- **JSON Path**: Default is `data.downloadLink`.

![PicGo Configuration Example](https://github.com/user-attachments/assets/dffeeb23-8f63-4bdb-a676-0bd693a2bede)

### Reverse Proxy

#### Caddy Configuration

```caddyfile
example.com {
    reverse_proxy /api* localhost:8080 {
        header_up X-Forwarded-Proto {scheme}
        header_up X-Forwarded-Port {server_port}
    }
}
```

- `{scheme}`: Filled based on the actual request protocol (HTTP or HTTPS).
- `{server_port}`: Automatically obtains the port the client connects to (e.g., 443).

#### NGINX Configuration

```nginx
server {
    listen 443 ssl;
    server_name example.com;

    location / {
        proxy_pass http://localhost:8085;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Support and Feedback

If you find this project helpful, you're welcome to:

- ⭐ Star the project
- 🔄 Share it with more friends
- 🐛 Submit Issues or Pull Requests

Your support is the driving force behind the project's continued development!
