# 🗄️ Deploy to Render with MySQL (FREE Database Options)

## ✅ Why Render + MySQL?
- **Render Web Service:** FREE (with sleep after 15min)
- **MySQL Options:** Multiple free tiers available
- **Auto-deploy** from GitHub
- **Production-ready** database
- **Better performance** than H2

## 🆓 FREE MySQL Database Options

### Option 1: PlanetScale (Recommended FREE)
- ✅ **1GB storage FREE forever**
- ✅ **1 billion row reads/month**
- ✅ **10 million row writes/month** 
- ✅ **No credit card required**
- ✅ **Serverless MySQL**

### Option 2: Railway MySQL
- ✅ **512MB FREE** 
- ✅ **Perfect for development**

### Option 3: Render MySQL
- ⚠️ **$7/month** (not free)
- ✅ **Integrated with Render**
- ✅ **Easy setup**

---

## 🚀 Deploy Steps (Using PlanetScale FREE)

### 1. Setup PlanetScale Database (FREE)
1. Go to [planetscale.com](https://planetscale.com)
2. Sign up **FREE** (no credit card needed)
3. **Create database:** `resume-screening`
4. **Get connection string:**
   ```
   mysql://user:pass@aws.connect.psdb.cloud/resume-screening?sslaccept=strict
   ```

### 2. Deploy Web Service on Render
1. Go to [render.com](https://render.com)
2. **New Web Service** → Connect your GitHub repo
3. Configure:
   - **Name:** `resume-screening-system`
   - **Environment:** `Docker`
   - **Branch:** `main`

### 3. Set Environment Variables
```env
GROQ_API_KEY=your_groq_api_key_here
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=mysql://user:pass@aws.connect.psdb.cloud/resume-screening?sslaccept=strict
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

### 4. Deploy!
1. Click **"Create Web Service"**
2. Wait ~5-7 minutes for build
3. Get your URL: `https://resume-screening-system.onrender.com`

---

## 🔄 Alternative: Use Render MySQL ($7/month)

### 1. Create MySQL Database on Render
1. In Render dashboard: **"New +"** → **"MySQL"**
2. Configure:
   - **Name:** `resume-mysql`
   - **Database:** `resume_screening_db`
   - **User:** `resume_user`
3. **Deploy Database** (2-3 minutes)

### 2. Connect Web Service
Render automatically provides connection details:
```env
DATABASE_URL=mysql://user:pass@dpg-xxx-a.oregon-postgres.render.com/resume_screening_db
```

### 3. Environment Variables
```env
GROQ_API_KEY=your_groq_api_key_here
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=mysql://user:generated_pass@host/resume_screening_db
```

---

## 🧪 Test Your Deployment

### 1. Check Database Connection
Visit: `https://your-app.onrender.com/actuator/health`

Should show:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

### 2. Test Features
1. **Upload resume** → MySQL stores candidate data
2. **Run screening** → Results saved to MySQL
3. **Use AI Coach** → Conversations in database
4. **Batch processing** → All data persisted

### 3. Verify Data Persistence
1. Upload data and use app
2. **Restart the app** (or wait for sleep/wake)
3. **Data should still be there** → MySQL working!

---

## 💰 Total Cost Comparison

| Option | Web Service | Database | Total |
|--------|-------------|----------|-------|
| **Render + PlanetScale** | FREE | FREE | **$0/month** |
| **Render + Railway MySQL** | FREE | FREE | **$0/month** |
| **Render + Render MySQL** | FREE | $7 | **$7/month** |
| **Railway (All-in-one)** | $5 | $0 | **$5/month** |

**🎯 Recommendation: Render + PlanetScale = $0/month!**

---

## 🔄 Local Development with MySQL

### 1. Install MySQL Locally
```powershell
# Using Chocolatey
choco install mysql

# Or use XAMPP (includes phpMyAdmin)
```

### 2. Create Local Database
```sql
CREATE DATABASE resume_screening_db;
```

### 3. Run with MySQL Profile
```powershell
# Update your run.ps1 to include:
java -Dspring.profiles.active=mysql -jar target/ai-resume-screener-2.1.0.jar
```

---

## 🛠️ Environment Variables Reference

| Variable | Description | Example |
|----------|-------------|---------|
| `GROQ_API_KEY` | Groq AI API key | `gsk_xxx...` |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `production` |
| `DATABASE_URL` | MySQL connection string | `mysql://user:pass@host/db` |
| `DB_USERNAME` | Database username | `resume_user` |
| `DB_PASSWORD` | Database password | `generated_pass` |

---

**🎉 Your Resume Screening System will have a production-ready MySQL database with 99.9% uptime!**