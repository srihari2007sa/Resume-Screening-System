# 🗄️ MySQL Database Setup Guide

## Local Development Setup

### 1. Install MySQL
**Windows:**
```powershell
# Using Chocolatey
choco install mysql

# Or download from https://dev.mysql.com/downloads/mysql/
```

**Alternative: Using XAMPP (Easier)**
1. Download XAMPP from https://www.apachefriends.org/
2. Install and start Apache + MySQL services
3. Access phpMyAdmin at http://localhost/phpmyadmin

### 2. Create Database
```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE resume_screening_db;

-- Create user (optional)
CREATE USER 'resumeapp'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON resume_screening_db.* TO 'resumeapp'@'localhost';
FLUSH PRIVILEGES;

-- Exit
EXIT;
```

### 3. Update Local Configuration
Your password is already configured in `application-mysql.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=srihari2007sa
```

### 4. Run with MySQL Profile
```powershell
# Option 1: Run with Maven
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# Option 2: Update run.ps1
# Edit run.ps1 and add: -Dspring.profiles.active=mysql
```

---

## 🚀 Render Deployment with MySQL

### 1. Create MySQL Database on Render
1. Go to your Render dashboard
2. Click **"New +"** → **"MySQL"**
3. Configure:
   - **Name:** `resume-db`
   - **Database:** `resume_screening_db` 
   - **User:** `resume_user`
   - **Region:** Same as your web service
4. **Deploy Database** (takes 2-3 minutes)
5. **Copy the connection details**

### 2. Get Database Connection String
Render provides these details:
- **Host:** `xxx.oregon-postgres.render.com`
- **Port:** `3306`
- **Database:** `resume_screening_db`
- **Username:** `resume_user`
- **Password:** `auto-generated`
- **Connection String:** `mysql://user:pass@host:port/database`

### 3. Configure Web Service Environment Variables
In your Render web service, set these variables:

```env
GROQ_API_KEY=your_groq_api_key_here
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=mysql://resume_user:generated_password@xxx.oregon-postgres.render.com:3306/resume_screening_db
DB_USERNAME=resume_user
DB_PASSWORD=generated_password
```

### 4. Deploy Web Service
1. **New Web Service** → Connect GitHub repo
2. Add environment variables above
3. **Deploy!**

---

## 💰 Render MySQL Pricing

### Free Development Options:
1. **PlanetScale (FREE):** 1GB storage, 1 billion reads/month
2. **Aiven (FREE):** 1 month free trial
3. **Local MySQL:** Completely free for development

### Render MySQL Pricing:
- **Starter:** $7/month (256MB RAM, 1GB storage)
- **Standard:** $25/month (1GB RAM, 10GB storage)

---

## 🔄 Database Migration (H2 → MySQL)

If you have existing H2 data to migrate:

### 1. Export H2 Data
```sql
-- Connect to H2 console at http://localhost:8081/h2-console
-- Export data as INSERT statements
SCRIPT TO 'backup.sql';
```

### 2. Import to MySQL
```sql
-- Connect to MySQL
mysql -u root -p resume_screening_db

-- Import data (may need manual adjustment)
source backup.sql;
```

---

## 🧪 Test Your MySQL Connection

### 1. Local Test
```powershell
# Run with MySQL profile
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# Check logs for successful connection:
# "HikariPool-1 - Start completed"
```

### 2. Test Database Operations
1. Upload a resume → Check MySQL tables created
2. Run screening → Verify data saved
3. Use AI Coach → Confirm conversations stored

### 3. Verify Tables Created
```sql
USE resume_screening_db;
SHOW TABLES;

-- Should show:
-- candidates
-- job_descriptions  
-- screening_results
```

---

## 🐛 Troubleshooting

### Connection Failed
```properties
# Add to application-mysql.properties if needed
spring.datasource.url=jdbc:mysql://localhost:3306/resume_screening_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
```

### Charset Issues
```sql
-- Set UTF-8 charset
ALTER DATABASE resume_screening_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Port Already in Use
```sql
-- Check if MySQL is running on different port
SHOW VARIABLES LIKE 'port';
```

---

## 📋 Quick Commands Reference

```powershell
# Local MySQL Development
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# Check MySQL status
mysql -u root -p -e "SELECT 1"

# Create database quickly
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS resume_screening_db;"

# View application logs
tail -f logs/spring.log
```

---

**🎯 Your Resume Screening System will work perfectly with MySQL on both local development and Render production!**