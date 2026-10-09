# Wavely

Online music player (Jamendo) with background playback.

1. Get a free client ID at https://devportal.jamendo.com
2. In your GitHub repo: Settings > Secrets and variables > Actions > New repository secret
   Name: JAMENDO_CLIENT_ID   Value: your client id
3. Actions tab > "Build APK" > Run workflow
4. When it finishes, open the run and download the "Wavely-APK" artifact (a zip containing app-debug.apk)
5. Unzip, install the APK on your Android phone (allow "install unknown apps").

Local build: put JAMENDO_CLIENT_ID=... in local.properties.
Music via Jamendo. Non-commercial use only without a Jamendo commercial agreement.
