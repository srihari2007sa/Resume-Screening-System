# ☁️ Deploy to Google Cloud Run (PAY-PER-USE)

## ✅ Why Google Cloud Run?
- **2 million requests/month FREE**
- **Auto-scales to zero** (no cost when idle)
- **Pay only when used** (~$0.001 per request after free tier)
- **No server management**
- **Supports any container**
- **Global CDN included**

## 🆓 Free Tier Limits
- **2 million requests/month**
- **360,000 GB-seconds/month**  
- **180,000 CPU-seconds/month**
- **5GB outbound data/month**

*Your app will likely stay within free limits!*

## 🚀 Deploy Steps

### 1. Setup Google Cloud
1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Create new project: `resume-screening-system`
3. Enable **Cloud Run API** and **Cloud Build API**
4. Install Google Cloud CLI locally

### 2. Deploy via Cloud Build
```bash
# Clone your repo locally
git clone https://github.com/yourusername/Resume-Screening-System.git
cd Resume-Screening-System

# Set your project ID
gcloud config set project your-project-id

# Build and deploy in one command
gcloud run deploy resume-screening-system \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GROQ_API_KEY=your_groq_api_key_here,SPRING_PROFILES_ACTIVE=production \
  --memory 512Mi \
  --cpu 1000m \
  --max-instances 10
```

### 3. Get Your URL
After deployment, you'll get a URL like:
`https://resume-screening-system-abc123-uc.a.run.app`

## 🎯 Features on Cloud Run
- ✅ **All app features working**
- ✅ **Instant auto-scaling** (0 to 1000+ instances)
- ✅ **Global CDN**
- ✅ **Custom domains** with SSL
- ✅ **99.95% uptime SLA**
- ✅ **No cold starts** under load

## 💰 Cost Estimation
For a personal project with moderate usage:
- **< 1000 requests/month:** $0 (free tier)
- **10,000 requests/month:** ~$0.50
- **100,000 requests/month:** ~$5.00

*Much cheaper than traditional hosting!*

## 🔄 CI/CD Setup
Set up automatic deployment with Cloud Build:

1. Connect your GitHub repo to Cloud Build
2. Use the included `cloudbuild.yaml` 
3. Every push to `main` triggers deployment

---
**🎉 Perfect for production apps with real traffic!**