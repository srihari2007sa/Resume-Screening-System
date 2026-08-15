# 🚀 GitHub Models Setup (FREE AI!)

## What is GitHub Models?

GitHub provides **FREE access to GPT-4o, Claude, and other AI models** for all GitHub users!

**Benefits:**
- ✅ Completely FREE (no credit card required)
- ✅ GPT-4o (better than GPT-4)
- ✅ No billing/deposits needed
- ✅ Much higher rate limits than Gemini free tier
- ✅ Works reliably 24/7

---

## Step 1: Get Your GitHub Personal Access Token

### Quick Steps:
1. Go to: **https://github.com/settings/tokens/new**
2. Login to your GitHub account
3. Fill in:
   - **Note:** `Resume Screener AI` (any name)
   - **Expiration:** 90 days (or longer)
   - **Scopes:** Leave all unchecked (no scopes needed!)
4. Click **"Generate token"**
5. **COPY THE TOKEN** (starts with `github_pat_...` or `ghp_...`)

### Important:
- You can use your existing GitHub account (free account works!)
- No special permissions needed
- Token is completely free
- Save it somewhere safe - you can't see it again!

---

## Step 2: Add Token to Your Application

### Option A: Using .env file (Local Development)
1. Open `d:\Resume-Screening-System\.env`
2. Find the line: `GITHUB_TOKEN=`
3. Paste your token: `GITHUB_TOKEN=github_pat_YOUR_TOKEN_HERE`
4. Save the file

### Option B: Using PowerShell (Quick Test)
```powershell
cd d:\Resume-Screening-System
$env:GITHUB_TOKEN="github_pat_YOUR_TOKEN_HERE"
./run.ps1
```

---

## Step 3: Rebuild and Restart

```powershell
cd d:\Resume-Screening-System

# Rebuild
./maven/bin/mvn clean package -DskipTests

# Restart
./run.ps1
```

---

## Step 4: Test AI Coach

1. Go to **http://localhost:8081**
2. Login with `admin` / `admin123`
3. Click on any screening result
4. Click **"AI Coach"** button
5. Ask: **"How can I improve?"**

**You should now get responses from GPT-4o!** ✅

---

## How It Works

Your application now tries AI providers in this order:

1. **GitHub Models (GPT-4o)** ⭐ FREE and reliable
2. Anthropic Claude (if you add key)
3. OpenAI GPT-4 (if you add key)
4. Gemini (your existing key - often gets 503 errors)
5. Simple rule-based coach (always works as final fallback)

---

## Troubleshooting

### "GitHub Models failed"
- Check your token is correct in `.env`
- Make sure token hasn't expired
- Generate a new token at https://github.com/settings/tokens/new

### "No response from AI Coach"
- Check the console logs - it will show which AI it tried
- The simple coach should always work as fallback

### Rate Limits
GitHub Models has generous free limits:
- **150 requests per minute**
- **15,000 requests per day**

Much better than Gemini's free tier!

---

## Security Note

Your GitHub token is:
- ✅ Stored in `.env` (not committed to Git)
- ✅ Only used locally on your machine
- ✅ Can be revoked anytime at: https://github.com/settings/tokens

---

## Need Help?

1. Generate token: https://github.com/settings/tokens/new
2. GitHub Models docs: https://github.com/marketplace/models
3. Just paste your token in `.env` and restart!

**That's it! You now have FREE GPT-4o access!** 🎉
