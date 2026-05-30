# GhostPeek — Build Your APK for FREE (No Android Studio Needed)

## What is GhostPeek?
Reads your Messenger/Instagram messages silently — no read receipts sent.
Shows an iOS-style floating peek sheet when you long-press a chat.

---

## HOW TO GET YOUR APK (Step-by-Step, 100% Free)

### Step 1 — Upload to GitHub (free account needed)
1. Go to https://github.com and sign up / log in
2. Click **New repository** (the green button)
3. Name it `ghostpeek`, set to **Public**, click **Create repository**
4. Click **uploading an existing file**
5. Drag and drop ALL the files from this zip into the browser
6. Click **Commit changes**

### Step 2 — Run the Build
1. Click the **Actions** tab at the top of your repository
2. Click **Build GhostPeek APK** on the left
3. Click **Run workflow** → **Run workflow** (green button)
4. Wait ~3-5 minutes for the build to finish (green checkmark = success)

### Step 3 — Download Your APK
1. Click on the finished build (the green checkmark row)
2. Scroll down to **Artifacts**
3. Click **GhostPeek-debug** to download the APK zip
4. Unzip it — inside is `app-debug.apk`

### Step 4 — Install on Your Phone
1. Transfer `app-debug.apk` to your Android phone
2. On your phone go to **Settings → Security → Install unknown apps** → allow your file manager
3. Tap the APK file to install
4. Open GhostPeek and grant the 3 permissions it asks for

---

## Permissions Needed (why each one)
- **Notification Access** — reads message text from Messenger notifications (no read receipt)
- **Accessibility Service** — detects when you long-press a chat row
- **Display over other apps** — shows the floating peek sheet

---

## Supported Apps
- Facebook Messenger
- Messenger Lite
- Instagram DMs
- Facebook (messages)
