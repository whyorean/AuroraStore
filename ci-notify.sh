#!/bin/bash

# Declare variables
DATE="`date +%d%m%Y-%H%M%S`"
MSG_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
DOC_URL="https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendDocument"
COMMIT_URL="https://gitlab.com/AuroraOSS/AuroraStore/-/commit/$CI_COMMIT_SHA"
TEXT="New+commit+to+[*${CI_PROJECT_NAME}*](${CI_PROJECT_URL})%0A[*⚒️+Building*](${CI_JOB_URL})+with+HEAD+at+[*${CI_COMMIT_SHORT_SHA}*](${COMMIT_URL})"

# send message to Telegram channel for first job

function tg_build_start()
{
    echo -e "*****************************************************"
    echo "           APK build script & Telegram notify           "
    echo -e "*****************************************************"
    echo "Sending build text to Telegram channel..."
    BUILD_START=$(date +"%s")
    curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID \
        -d "disable_web_page_preview=true" \
        -d "parse_mode=MarkdownV2" \
        -d text=$TEXT
    BUILD_END=$(date +"%s")
    DIFF=$(($BUILD_END - $BUILD_START))
    echo "Message sent!"
}                    

# NOTES: $CI_COMMIT_AUTHOR and $CI_COMMIT_BRANCH won't work due to Telegram's awful Markdown flavour. 

function tg_build_data()
{   
    echo "Sending build data to Telegram channel..."
    if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
        cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/debug/AuroraStore_CI_$DATE.apk
        cd app/build/outputs/apk/debug/
        APK=$(echo AuroraStore_CI_*.apk)
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
    echo "Data sent!"
}

function tg_push_error()
{
	curl -s -X POST $MSG_URL -d chat_id=$TELEGRAM_CHAT_ID -d "disable_web_page_preview=true" -d "parse_mode=html&text=<b>❌ Build failed after $(($DIFF / 60)) minute(s) and $(($DIFF % 60)) second(s).</b>"
}

function tg_push_log()
{
   	LOG=ci-logs.log
	curl -F document=@"$LOG" $MSG_URL \
			-F chat_id="$TELEGRAM_CHAT_ID" \
			-F "disable_web_page_preview=true" \
			-F "parse_mode=html" \
            -F caption="Build logs for $CI_PROJECT_NAME - $DATE, took $(($DIFF / 60)) minute(s) and $(($DIFF % 60)) second(s). @austinhornhead_12"
}


for i in "$@"
do
case $i in
    --build)
    tg_build_start()
    shift
    ;;
    --notify)
    tg_build_data()
    shift
    ;;
    --logs)
    tg_push_error()
    tg_push_log()
    shift
    ;;
    *)
    echo "Use available commands: --build, --notify & --logs"
    exit    
esac
done

