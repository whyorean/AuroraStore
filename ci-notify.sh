#!/bin/bash

MSG_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
DOC_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendDocument"
# COMMIT_URL="https://gitlab.com/AuroraOSS/AuroraStore/-/commit/$CI_COMMIT_SHA"
# TEXT="Project:$CI_PROJECT_NAME<a href=$CI_PIPELINE_URL><b>Building<b><a> with HEAD at <a href=$COMMIT_URL><b>$CI_COMMIT_SHORT_SHA<b><a> by $CI_COMMIT_AUTHOR.%0ABranch:+$CI_COMMIT_REF_SLUG"
#TEXT="Build status: $1%0A%0AProject:+$CI_PROJECT_NAME%0AURL:+$CI_PROJECT_URL/pipelines/$CI_PIPELINE_ID/%0ABranch:+$CI_COMMIT_REF_SLUG"

#curl -s --max-time $TIME -d "chat_id=$TELEGRAM_CHAT_ID&disable_web_page_preview=1&text=$TEXT" $MSG_URL > /dev/null

# curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID \
#     -d "disable_web_page_preview=true" \
#     -d "parse_mode=html" \
#     -d text=$TEXT

if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/debug/AuroraStore_$DATE.apk
    cd app/build/outputs/apk/debug/
    APK=$(echo AuroraStore_*.apk)
    curl -F document=@"$APK" $DOC_URL \
        -F chat_id=$TELEGRAM_CHAT_ID \
        -F "disable_web_page_preview=true" \
        -F "parse_mode=html" \
        -F caption="✅ <b>CI build completed successfully!</b>"
    rm -rf app/build/outputs/apk
else
    curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID \
        -d "disable_web_page_preview=true" \
        -d "parse_mode=html" \
        -d text="❌ <b>Build error, exiting now!</b>"
fi
