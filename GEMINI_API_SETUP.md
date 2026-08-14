# Gemini API Setup Guide

## Error 429 - Rate Limit Exceeded

If you're seeing "Error 429" in the AI Coach, it means your Gemini API key has exceeded its request quota.

---

## Quick Fix Steps

### 1. Get a Valid API Key

1. Visit: https://aistudio.google.com/app/apikey
2. Sign in with your Google account
3. Click **"Create API Key"**
4. Copy the generated key (starts with `AIzaSy...`)

### 2. Update Your `.env` File

Replace the `GEMINI_API_KEY` in your `.env` file:

```env
GEMINI_API_KEY=AIzaSy... (your actual key here)
DB_USERNAME=root
DB_PASSWORD=srihari2007sa
```

### 3. Restart Your Application

```powershell
# Stop the current application (Ctrl+C)
# Then restart:
./run.ps1
```

---

## Gemini API Rate Limits

### Free Tier
- **15 requests per minute**
- **1,500 requests per day**
- **32,000 tokens per minute**

### If You Need More
- Upgrade to **Gemini API Pro** at: https://ai.google.dev/pricing
- Or use **Google Cloud Vertex AI** for higher quotas

---

## Common Issues

### Issue: "Invalid API key"
**Solution:** Make sure your key starts with `AIzaSy` and is exactly 39 characters

### Issue: "Rate limit exceeded" (429)
**Solutions:**
- Wait 1 minute before trying again
- Check your usage at: https://aistudio.google.com/app/apikey
- Upgrade to a paid plan if needed

### Issue: "API access forbidden" (403)
**Solutions:**
- Check if your API key has IP restrictions
- Ensure Generative Language API is enabled
- Verify billing is set up (for paid tiers)

---

## Testing Your API Key

You can test your API key directly using curl:

```bash
curl -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent" \
  -H "Content-Type: application/json" \
  -H "x-goog-api-key: YOUR_API_KEY_HERE" \
  -d '{
    "contents": [{
      "parts": [{"text": "Hello, test message"}]
    }]
  }'
```

If this works, your key is valid!

---

## What Changed in the Code

I've enhanced the error handling in `ResumeCoachService.java`:

1. **Better error messages** - Now shows specific guidance for each error type
2. **Retry logic** - Automatically retries on temporary server errors (5xx)
3. **No retry on rate limits** - Won't waste attempts when quota is exceeded
4. **Exponential backoff** - Waits progressively longer between retries

---

## Alternative: Use Claude or OpenAI

If Gemini isn't working, you can modify the service to use:
- **Claude API** (Anthropic)
- **OpenAI GPT-4**
- **Azure OpenAI**

Let me know if you'd like help switching to a different AI provider!
