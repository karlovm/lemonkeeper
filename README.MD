

# 🍋 LemonKeeper

**LemonKeeper** is a secure file sharing service with full client-side encryption using **AES-256-GCM**.  
Files are encrypted in the browser before being uploaded, and the server never sees the unencrypted content.

![LemonKeeper Screenshot](https://github.com/karlovm/lemonkeeper/blob/master/preview.png?raw=true)

---

## ✨ Features

- 🔐 Client-side AES-256 encryption (GCM mode)
- 🧠 Passwords never sent to the server
- 📁 File upload, download, and deletion
- 📊 Disk usage monitoring
- 🌍 Multilingual interface (Русский / English)
- 🚫 Fully zero-knowledge: only encrypted files are stored

---

## 🛠️ Technologies

- **Frontend:** HTML, CSS, JavaScript (vanilla)
- **Backend:** Java (Spring Boot)
- **Storage:** Local filesystem

---





## 🚀 Running the Project

1. Clone the repository:

```
git clone https://github.com/karlovm/lemonkeeper.git
cd lemonkeeper
```




2. Edit tokens and urls. By default, it runs on 8083 port:

Add the following to your `application.properties`:

```properties
# Upload directory
app.upload.dir=uploads

# API token for authentication
lemonkeeper.api.token=your-secure-token

```

3. Edit frontend endpoint

Open ```index.html``` and edit ```const API_BASE = 'http:///localhost:8083/files';``` to actual url.

4. Build JAR file (output will be located at target folder)

```
./mvnw clean install
```

5. Run jar file from terminal

```
java -jar lemonkeeper-0.0.1-SNAPSHOT.jar
```

JDK 17 required

---

## 🧪 Development Notes

* All files are encrypted before upload and decrypted after download.
* Encrypted files are stored with `.encrypted` extension.
* File metadata (like name and size) is managed on the server, but content is opaque.

---

## 📜 License

MIT License © 2025 [karlovm](https://github.com/karlovm)

```
