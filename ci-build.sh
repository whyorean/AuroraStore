#!/bin/bash

MSG_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
DOC_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendDocument"
COMMIT_URL="https://gitlab.com/AuroraOSS/AuroraStore/-/commit/$CI_COMMIT_SHA"
TEXT="Project:+$CI_PROJECT_NAME%0A<a+href=%22$CI_PIPELINE_URL%22><b>Building<b><a>+with+HEAD+at+<a+href=%22$COMMIT_URL%22><b>$CI_COMMIT_SHORT_SHA<b><a>+by+$CI_COMMIT_AUTHOR.%0ABranch:+$CI_COMMIT_BRANCH"

curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID \
    -d "disable_web_page_preview=true" \
    -d "parse_mode=HTML" \
    -d text=$TEXT