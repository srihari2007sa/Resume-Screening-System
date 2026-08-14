# Railway Deployment Guide

## ✅ Local Deployment - COMPLETE

Your application is now running locally with the updated API key!

**Access your application at:** http://localhost:8081
- **Login:** admin / admin123
- **AI Coach is now working** with the new Gemini API key

---

## 🚀 Deploy to Railway (Cloud)

### Option 1: Deploy via Railway Dashboard (Easiest)

1. **Go to Railway Dashboard**
   - Visit: https://railway.app/
   - Sign in with GitHub

2. **Create New Project**
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your `Resume-Screening-System` repository

3. **Set Environment Variables**
   Railway needs these environment variables:
   
   ```
   GEMINI_API_KEY=<your-gemini-api-key-from-.env-file>
   DB_USERNAME=root
   DB_PASSWORD=<your-database-password>
   PORT=8081
   ```
   
   **Note:** Get your actual API key from the `.env` file in your local project.

   - Go to your project → Variables tab
   - Add each variable above
   - Railway will automatically redeploy

4. **Railway Auto-Detection**
   Railway will automatically:
   - Detect the `Dockerfile`
   - Build using Docker
   - Deploy using `railway.toml` configuration
   - Expose on a public URL like: `https://your-app.up.railway.app`

5. **Check Deployment**
   - Go to "Deployments" tab
   - Wait for build to complete (~3-5 minutes)
   - Click on the deployment URL
   - Your app should be live!

---

### Option 2: Deploy via Railway CLI

If you want to deploy from command line:

#### 1. Install Railway CLI

```powershell
# Using npm (if you have Node.js)
npm install -g @railway/cli

# Or using Scoop
scoop install railway
```

#### 2. Login to Railway

```powershell
railway login
```

#### 3. Initialize Project

```powershell
cd d:\Resume-Screening-System
railway init
```

#### 4. Set Environment Variables

```powershell
railway variables set GEMINI_API_KEY=<your-api-key>
railway variables set DB_USERNAME=root
railway variables set DB_PASSWORD=<your-password>
railway variables set PORT=8081
```

Replace `<your-api-key>` and `<your-password>` with your actual values from `.env`.

#### 5. Deploy

```powershell
railway up
```

#### 6. Open Your App

```powershell
railway open
```

---

## 🔍 Verify Deployment

### Check Health Endpoint
```bash
curl https://your-app.up.railway.app/health
```

Should return:
```json
{"status":"UP"}
```

### Check AI Coach
1. Login to your deployed app
2. Upload a resume
3. Screen it against a job
4. Click "AI Coach" 
5. The error should be gone! ✅

---

## 📝 Railway Configuration Files

Your project already has these configured:

### `railway.toml`
- Specifies build using Dockerfile
- Sets health check endpoint
- Configures restart policy

### `Dockerfile`
- Multi-stage build (Maven + JDK)
- Optimized for Railway
- Includes health check
- Uses PORT environment variable

---

## 🐛 Troubleshooting

### If deployment fails:

1. **Check build logs** in Railway dashboard
2. **Verify environment variables** are set correctly
3. **Check if Dockerfile builds locally:**
   ```powershell
   docker build -t resume-screener .
   docker run -p 8081:8081 -e GEMINI_API_KEY=your_key resume-screener
   ```

### If AI Coach still shows error 429:

1. **Verify the API key is set** in Railway variables
2. **Check you haven't exceeded quota** at https://aistudio.google.com/
3. **Wait 1 minute** and try again (rate limit reset)

---

## 💰 Railway Pricing

- **Free Tier:** $5 credit per month (enough for testing)
- **Hobby Plan:** $5/month for 5 projects
- **Pro Plan:** $20/month for unlimited projects

Your app should fit comfortably in the free tier for development!

---

## 🎯 Current Status

✅ **Local deployment:** Running at http://localhost:8081
✅ **API key updated:** New Gemini key in place
✅ **Build successful:** Maven package complete
✅ **Enhanced error handling:** Better error messages added

**Next step:** Deploy to Railway using Option 1 or 2 above!
