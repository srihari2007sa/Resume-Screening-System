# 🚀 Deploy Resume Screening System to Railway

## Quick Deploy Steps

### 1. Login to Railway
1. Go to [railway.app](https://railway.app)
2. Sign in with GitHub account

### 2. Create New Project
1. Click **"New Project"**
2. Select **"Deploy from GitHub repo"**
3. Choose your **`Resume-Screening-System`** repository

### 3. Configure Environment Variables
In Railway project settings, add these environment variables:

```env
GROQ_API_KEY=your_groq_api_key_here
SPRING_PROFILES_ACTIVE=railway
PORT=8080
```

### 4. Deploy Configuration 
Railway will automatically detect the `Dockerfile` and use these settings from `railway.toml`:

- **Build:** Uses Dockerfile
- **Start Command:** `java -Djava.security.egd=file:/dev/./urandom -jar app.jar` 
- **Health Check:** `/actuator/health`

### 5. Monitor Deployment
1. Watch build logs in Railway dashboard
2. Build takes ~3-5 minutes (Maven dependencies + Docker build)
3. App starts on Railway-assigned URL (e.g., `https://resume-screening-system-production.up.railway.app`)

## 🔧 Production Features

### ✅ What Works in Production
- **Groq AI Integration** - Working with API key from environment
- **File Upload & Processing** - PDF resume parsing 
- **Resume Screening** - AI-powered candidate matching
- **Career DNA Analysis** - Personality profiling
- **Batch Processing** - Multiple resume screening
- **Candidate Comparison** - Side-by-side analysis
- **AI Resume Coach** - Conversational improvement suggestions

### 🗄️ Database
- **Development:** H2 in-memory database
- **Production:** H2 file database (persistent in Railway volumes)
- **Future:** Can be upgraded to PostgreSQL via Railway addons

### 🔐 Security
- **API Keys:** Configured via Railway environment variables
- **H2 Console:** Disabled in production
- **HTTPS:** Automatically provided by Railway
- **CORS:** Configured for production domains

### 📊 Monitoring
- **Health Check:** `/actuator/health` - Railway monitors app health
- **Logs:** Available in Railway dashboard
- **Metrics:** Basic app metrics via Spring Actuator

## 🌐 Access Your Deployed App

After successful deployment:
1. **Main App:** `https://your-app-name.up.railway.app`
2. **Health Check:** `https://your-app-name.up.railway.app/actuator/health`
3. **API Docs:** All endpoints available at `/api/*`

## 🔄 Auto-Deploy Setup

Railway automatically redeploys when you push to the `main` branch:

```bash
git add .
git commit -m "Update feature"
git push origin main
# 🚀 Railway automatically builds and deploys!
```

## 💰 Railway Pricing
- **Hobby Plan:** $5/month - Perfect for this app
- **Free Trial:** Available for testing
- **Resource Usage:** App uses ~256MB RAM, minimal CPU

## 🐛 Troubleshooting

### Build Fails
- Check build logs in Railway dashboard
- Ensure `Dockerfile` is in repository root
- Verify Maven dependencies resolve

### App Won't Start  
- Check environment variables are set correctly
- Verify `GROQ_API_KEY` is valid
- Check start command in railway.toml

### 404 Errors
- Ensure app is responding at health check endpoint
- Check Railway-assigned PORT is being used
- Verify Spring profile is set to `railway`

## 📝 Environment Variables Reference

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `GROQ_API_KEY` | Groq AI API Key for resume screening | Yes | `gsk_xxx...` |
| `SPRING_PROFILES_ACTIVE` | Spring profile to use | Yes | `railway` |
| `PORT` | Port for Railway (auto-assigned) | Yes | `8080` |
| `GEMINI_API_KEY` | Fallback AI provider (optional) | No | `AQ.Ab8...` |
| `DATABASE_URL` | Database connection (auto if using addon) | No | `jdbc:postgresql://...` |

## 🎯 Next Steps After Deploy
1. Test resume upload and screening
2. Verify AI Coach functionality 
3. Test batch processing
4. Check Career DNA analysis
5. Set up custom domain (optional)
6. Add database addon for production scale (optional)

---
**🚀 Your Resume Screening System is now live on Railway!**