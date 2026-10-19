@echo off
echo ========================================================
echo   Starting QueueNow Public Cloudflare Tunnel (4G/5G)
echo ========================================================
echo.
echo Forwarding http://localhost:8080 to the public internet...
echo You will see your public HTTPS URL below in a few seconds.
echo Any phone on mobile data can scan QR codes using that URL!
echo.
.\cloudflared.exe tunnel --url http://localhost:8080
pause
