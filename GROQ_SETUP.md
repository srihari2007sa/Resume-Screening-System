# 🚀 Groq AI Setup (FREE & SUPER FAST!)

## What is Groq?

Groq provides **FREE access to Llama 3.3 70B** - one of the best open-source AI models!

**Benefits:**
- ✅ Completely FREE (no credit card, no deposits!)
- ✅ Lightning FAST (faster than GPT-4)
- ✅ Llama 3.3 70B (better than most paid models)
- ✅ 30 requests per minute (plenty for testing)
- ✅ 14,400 tokens per day FREE
- ✅ Works reliably 24/7

---

## Step 1: Get Your FREE Groq API Key (30 seconds!)

### Quick Steps:
1. Go to: **https://console.groq.com/keys**
2. Sign up with Google/GitHub/Email (takes 10 seconds)
3. Click **"Create API Key"**
4. Give it a name: `Resume Screener`
5. **COPY THE KEY** (starts with `gsk_...`)

**That's it! No billing, no verification needed!**

---

## Step 2: Add Key to Your Application

### Open `.env` file:
1. Go to `d:\Resume-Screening-System\.env`
2. Find the line: `GROQ_API_KEY=`
3. Paste your key: `GROQ_API_KEY=gsk_YOUR_KEY_HERE`
4. Save the file

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

**You should now get fast, intelligent responses from Llama 3.3!** ✅

---

## How It Works

Your application now tries AI providers in this order:

1. **Groq (Llama 3.3)** ⭐ FREE, fast, and reliable!
2. Anthropic Claude (if you add key)
3. OpenAI GPT-4 (if you add key)
4. Gemini (your existing key - often fails)
5. Simple rule-based coach (always works as final fallback)

---

## Why Groq is BEST:

### Speed Comparison:
- **Groq:** ~300 tokens/second ⚡
- GPT-4: ~40 tokens/second
- Gemini: ~60 tokens/second

### Cost:
- **Groq:** FREE (14,400 tokens/day)
- GPT-4: Requires billing
- Gemini free: Often gets 503 errors

### Model Quality:
- Llama 3.3 70B rivals GPT-4 in most tasks
- Excellent for resume coaching and analysis
- Great at following instructions

---

## Troubleshooting

### "Groq failed"
- Check your key is correct in `.env`
- Make sure you copied the full key (starts with `gsk_`)
- Generate a new key at https://console.groq.com/keys

### Rate Limits
Groq's free tier:
- **30 requests per minute**
- **14,400 tokens per day**

If you hit limits, wait a minute and try again!

---

## Security Note

Your Groq API key is:
- ✅ Stored in `.env` (not committed to Git)
- ✅ Only used locally on your machine
- ✅ Can be regenerated anytime at: https://console.groq.com/keys

---

## Need Help?

1. Get key: https://console.groq.com/keys
2. Groq docs: https://console.groq.com/docs
3. Just paste your key in `.env` and restart!

**Groq is the BEST free AI option available right now!** 🚀🔥
