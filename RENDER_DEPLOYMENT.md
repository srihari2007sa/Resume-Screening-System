# 🆓 Deploy to Render (100% FREE)

## ✅ Why Render?
- **Completely FREE** for personal projects
- **512MB RAM** included in free tier  
- **Auto-deploy** from GitHub
- **Custom domains** supported
- **PostgreSQL database** free tier available
- **No credit card required**
- **Sleeps after 15min inactivity** (wakes up automatically)

## 🚀 Quick Deploy Steps

### 1. Create Render Account
1. Go to [render.com](https://render.com)
2. Sign up with your **GitHub account**
3. Grant access to repositories

### 2. Deploy from GitHub
1. Click **"New +"** → **"Web Service"**
2. Connect your **`Resume-Screening-System`** repository
3. Configure:
   - **Name:** `resume-screening-system`
   - **Environment:** `Docker`
   - **Branch:** `main`
   - **Dockerfile Path:** `./Dockerfile`

### 3. Set Environment Variables
In the deployment form, add:

```env
GROQ_API_KEY=your_groq_api_key_here
SPRING_PROFILES_ACTIVE=production
PORT=10000
```

### 4. Deploy!
1. Click **"Create Web Service"**
2. Wait ~5-7 minutes for build
3. Get your free URL: `https://resume-screening-system-abc123.onrender.com`

## 🎯 Features on Render FREE
- ✅ **AI Resume Screening** with Groq
- ✅ **Career DNA Analysis** 
- ✅ **Batch Processing**
- ✅ **AI Coach**
- ✅ **Auto HTTPS**
- ✅ **Custom domains**
- ⚠️ **Sleeps after 15min** (wakes up in ~10 seconds)

## 🔄 Auto-Deploy Setup
Render automatically redeploys when you push to `main`:

```bash
git add .
git commit -m "New feature"  
git push origin main
# 🚀 Render builds & deploys automatically!
```

## 💡 Pro Tips
- **First request after sleep** takes ~10 seconds
- **Upgrade to paid** ($7/month) to prevent sleeping
- **Add PostgreSQL** for production database (also free tier)
- **Custom domain** available on free tier

---
**🎉 Total Cost: $0/month forever!**