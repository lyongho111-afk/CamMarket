@echo off

rem Copy this file to local.env.cmd and enter your own TEST keys.

set "GOOGLE_MAPS_API_KEY="

set "TOSS_CLIENT_KEY="

set "TOSS_SECRET_KEY="


rem v14 notifications: keep false until real provider settings are ready.
set "NOTIFICATIONS_ENABLED=false"
set "SOLAPI_API_KEY="
set "SOLAPI_API_SECRET="
set "SMS_FROM="
set "KAKAO_PF_ID="
set "KAKAO_TEMPLATE_ID="
set "MAIL_FROM="
set "SMTP_HOST="
set "SMTP_PORT=587"
set "SMTP_USERNAME="
set "SMTP_PASSWORD="
set "SMTP_AUTH=true"
set "SMTP_STARTTLS=true"
set "SMTP_SSL=false"
set "SMTP_SSL_CHECK_SERVER_IDENTITY=true"

rem Naver: SMTP_HOST=smtp.naver.com, SMTP_PORT=465, SMTP_SSL=true, SMTP_STARTTLS=false.
rem Use the sender's application password, not the ordinary account password.

rem Address-to-coordinate search (road and parcel address)
set "KAKAO_REST_API_KEY="
