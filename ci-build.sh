#!/bin/bash

# Declare variables
MSG_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
DOC_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendDocument"
COMMIT_URL="https://gitlab.com/AuroraOSS/AuroraStore/-/commit/$CI_COMMIT_SHA"
TEXT="Project:+[${CI_PROJECT_NAME}](${CI_PROJECT_URL})%0A[*⚒️+Building*](${CI_JOB_URL})+with+HEAD+at+[*${CI_COMMIT_SHORT_SHA}*](${COMMIT_URL})"

# send message to Telegram channel
curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID \
    -d "disable_web_page_preview=true" \
    -d "parse_mode=HTML" \
    -d text=$TEXT

# NOTES: $CI_COMMIT_AUTHOR and $CI_COMMIT_BRANCH won't work due to Telegram's awful Markdown flavour. 