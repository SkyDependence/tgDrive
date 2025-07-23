# tgDrive - Unlimited Capacity and Speed Cloud Storage

<div align="center">

![GitHub release (latest by date)](https://img.shields.io/github/v/release/SkyDependence/tgDrive)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/SkyDependence/tgDrive/docker-publish.yml)
![Docker Image Size](https://img.shields.io/docker/image-size/nanyangzesi/tgdrive/latest)
![GitHub stars](https://img.shields.io/github/stars/SkyDependence/tgDrive)
![GitHub forks](https://img.shields.io/github/forks/SkyDependence/tgDrive)
![GitHub issues](https://img.shields.io/github/issues/SkyDependence/tgDrive)
![GitHub license](https://img.shields.io/github/license/SkyDependence/tgDrive)
[![tg-group](https://img.shields.io/static/v1?label=TG%20Group&amp;message=TgDrive&amp;color=blue)](https://t.me/+nhHtap9IYbVhOTM1)

</div>

**tgDrive** is a cloud storage application based on Telegram Bot, developed using Java, supporting unlimited capacity and speed for file storage. Utilizing multi-threading technology and optimized transfer strategies, it provides users with an efficient and reliable cloud storage solution.

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
- [Deployment Methods](#deployment-methods)
  - [Docker Compose Deployment](#docker-compose-deployment)
  - [Docker Deployment](#docker-deployment)
  - [Self-Hosting](#self-hosting)
  - [Render Deployment](#render-deployment)
- [Usage Instructions](#usage-instructions)
- [Advanced Configuration](#advanced-configuration)
  - [WebDAV Configuration](#webdav-configuration)
  - [PicGo Configuration](#picgo-configuration)
  - [Reverse Proxy](#reverse-proxy)
- [Support and Feedback](#support-and-feedback)

## Features

### Core Advantages

- 🚀 **Break Limits**: Completely bypasses the 20MB file size limit of the Telegram Bot API.
- 📈 **Multi-threaded Transfer**: Uses multi-threading for uploads and downloads to maximize bandwidth utilization.
- 🔗 **External Link Support**: Supports external links for images, allowing direct access and preview in the browser.
- 🖼️ **Image Hosting Integration**: Perfectly supports the PicGo image uploader tool, providing convenient image hosting services.
- 🎯 **GIF Optimization**: Addresses the issue of Telegram automatically converting GIFs to MP4s.
- 🌐 **WebDAV Support**: Provides a WebDAV interface, allowing third-party applications to manage and operate files, enhancing file interaction flexibility.

### Technical Features

- ⚡ **High Performance**: Developed based on Java 17+, ensuring stability and performance.
- 🐳 **Containerization**: Offers Docker support, simplifying deployment and maintenance processes.
- 💾 **Data Persistence**: Supports persistent data storage to ensure data safety.
- 🔄 **API Support**: Provides a complete RESTful API interface.
- 🌐 **WebDAV Support**: Provides a basic WebDAV interface.

## Quick Start

### Online Demo

- [Render Deployed Site (Recommended)](https://render.skydevs.link)
- [Demo Site](https://server.skydevs.link)

### Related Resources

- Frontend Code: [tgDriveFront](https://github.com/SkyDependence/tgDrive-front)
- Latest Version: [Releases](https://github.com/SkyDependence/tgDrive/releases)

## Deployment Methods

### Docker Compose Deployment

>[!TIP]
>📌 **Note**: If the server has limited memory (RAM ≤ 512MB), it is recommended to use the `nanyangzesi/tgdrive:server-latest` image.

1. Create a `docker-compose.yml` file:

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

#### Update Image

After mounting the data volume, you only need to pull the latest image and restart the container to update. Database data will not be lost:

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

#### Migrate Previous Data
> [!TIP]
> Starting from v0.0.9, you can directly download the database and restore file data from the database in the admin interface.

If you have already run the project and generated database files inside the container, you can manually migrate this data to a persistent directory on the host:

1. Find the old container's ID or name:

   ```bash
   docker ps -a
   ```

2. Copy the database files from the container to the host:

   ```bash
   docker cp <container_name_or_id>:/app/db ./db
   ```

   - Replace `<container_name_or_id>` with the actual container identifier.
   - This copies the contents of the `/app/db` folder inside the container to the `db` folder in the current directory on the host.

3. Restart the project:

   Using the updated `docker-compose.yml`, restart the project:

   ```bash
   docker compose up -d
   ```

4. Verify data:

   After starting, the project should be able to read the data from the host's `./db` folder.

### Self-Hosting

Prerequisites:

- Java 17 or higher

Deployment Steps:

1. Go to the [release page](https://github.com/SkyDependence/tgDrive/releases) to download the latest binary package.
2. Navigate to the directory where the downloaded binary package is located.
3. Run the following command:

   ```bash
   java -jar [latest_binary_package_name.jar]
   ```

   For example:

   ```bash
   java -jar tgDrive-0.0.2-SNAPSHOT.jar
   ```

4. After successful execution, access `localhost:8085` in your browser to start using it.

### Render Deployment

> [!TIP]
> Render free tier deployment requires bank card verification.

### Steps

1. Create a Web Service.

   ![Create Web Service](https://github.com/user-attachments/assets/543abbd1-0b2e-4892-8e46-265539159831)

2. Choose Docker image and enter `nanyangzesi/tgdrive:latest`.

   ![Enter Image](https://github.com/user-attachments/assets/09f212c1-886b-424e-8015-a8f96f7e48ee)

3. Select the free instance type.

   ![Select Free Instance](https://github.com/user-attachments/assets/18506bfa-9dda-4c41-a1eb-6cd7206c6f4b)

4. Scroll to the bottom of the page and click **Deploy Web Service** to complete the deployment.

## Usage Instructions

After accessing the URL where you deployed the project, you will see the following page:
> [!TIP]
> Starting from v0.0.9, all pages require login. There are two types of accounts: admin and visitor. Visitors can only access the upload page. The visitor account is `visitor` with the password `111111`.

![Home Page](https://github.com/user-attachments/assets/ede633bb-053a-49e4-ab2b-faff3c688c77)

Click on the Admin Interface and fill in the bot token:

![image](https://github.com/user-attachments/assets/83d05394-caf1-46ce-acdf-9b9c5611294e)

Don't know how to get the bot token and chatID? See [this article](https://skydevs.link/posts/tech/telegram_bot)

After filling it in, click Submit Configuration. Then, scroll down, select the configuration file you just added to load it, and you can start uploading:

![image](https://github.com/user-attachments/assets/25d1fd3d-d390-4674-9d77-d0d9bc1153fa)

## Advanced Configuration

### WebDAV Configuration

> [!TIP]
> WebDAV support started from v0.0.8.

#### Using [AList](https://alist.nn.ci) as an example

1. Click on Admin on the homepage:

![07d536381c29ac316f077743eab9c6ff](https://github.com/user-attachments/assets/eecd80be-3ec7-4916-ae73-779aaf09fc58)

2. Storage, Add:

![309722142e517cb0398d2c7a44976317](https://github.com/user-attachments/assets/7834972b-be4c-4307-9baa-8647216c6a42)

3. Select WebDAV as the driver:

![a94e203172604e571bd069f27fa15b9b](https://github.com/user-attachments/assets/419e7f96-e310-4edf-8698-c079dbb4215b)

4. Fill in the configuration:

![e7d28472622282b771914ecb5094386c](https://github.com/user-attachments/assets/fe6efd87-a584-46da-949c-ea8d6dfd1afb)

Address: `https://your.server.com/webdav`

> [!TIP]
> If both tgDrive and AList are running in local Docker containers, use the address `http://host.docker.internal:8085/webdav/`

Username and Password: These are the tgDrive admin username and password, default is `admin` `123456`. You can (and should) change the password in the tgDrive admin interface (Recommended).

After filling in, click Add. Go back to the homepage, enter the mount path you just configured, and start using it!

### PicGo Configuration

> [!TIP]
> PicGo support started from v0.0.4.

This project supports quick image uploads using [PicGo](https://github.com/Molunerfinn/PicGo).

#### Prerequisites

Ensure the PicGo plugin `web-uploader` is installed.

![PicGo Configuration Page](https://github.com/user-attachments/assets/fe52f47e-b2ab-4751-bb65-7ead9ebce2c0)

#### Parameter Description

- **API Address**: Default for local is `http://localhost:8085/api/upload`. If deployed on a server, change to `http://<server_address>:8085/api/upload`.
- **POST Parameter Name**: Defaults to `file`.
- **JSON Path**: Defaults to `data.downloadLink`.

![PicGo Configuration Example](https://github.com/user-attachments/assets/dffeeb23-8f63-4bdb-a676-0bd693a2bede)

### Reverse Proxy

#### Caddy Configuration

```caddyfile
example.com {
    # Enable HTTPS (Caddy automatically obtains and manages SSL certificates)
    reverse_proxy / http://localhost:8085 {
        # Set proxy headers
        header_up Host {host}                     # Preserve the original Host header from the client
        header_up X-Real-IP {remote}              # Client's real IP
        header_up X-Forwarded-For {remote}        # X-Forwarded-For header, identifying the client IP
        header_up X-Forwarded-Proto {scheme}      # Client's protocol (http or https)
        header_up X-Forwarded-Port {port}         # Client's port number
    }
}
```

#### NGINX Configuration

```nginx
server {
    listen 443 ssl;
    server_name example.com;
    # ssl_certificate /path/to/your/fullchain.pem; # Add your SSL cert path
    # ssl_certificate_key /path/to/your/privkey.pem; # Add your SSL key path

    location / {
        proxy_pass http://localhost:8085;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        client_max_body_size 100M ; # Can be set to the maximum size of the files you need to upload
    }
}
```

## Support and Feedback

If you find this project helpful, feel free to:

- ⭐ Star the project
- 🔄 Share it with more friends
- 🐛 Submit an Issue or Pull Request

Your support is the driving force behind the project's continued development!
