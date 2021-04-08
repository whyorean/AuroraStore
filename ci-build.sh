#!/bin/bash

MSG_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
DOC_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendDocument"
COMMIT_URL="https://gitlab.com/AuroraOSS/AuroraStore/-/commit/$CI_COMMIT_SHA"
#TEXT="Project:+[${CI_PROJECT_NAME}](${CI_PROJECT_URL})%0A[*⚒️+Building*](${CI_JOB_URL})+with+HEAD+at+[*${CI_COMMIT_SHORT_SHA}*](${COMMIT_URL})+by+${CI_COMMIT_AUTHOR}"


curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID \
    -d "disable_web_page_preview=true" \
    -d "parse_mode=HTML" \
    -d text="Project:+%3Ca+href=${CI_PROJECT_URL}>${CI_PROJECT_NAME}%3C/a%3E%0A%3Ca+href=%22${CI_JOB_URL}%22%3E%3Cb%3E⚒️+Building%3C/b%3E%3C/a%3E+with+HEAD+at+%3Ca+href=%22${COMMIT_URL}%22%3E%3Cb%3E${CI_COMMIT_SHORT_SHA}%3C/b%3E%3C/a%3E+by+${CI_COMMIT_AUTHOR}"