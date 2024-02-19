# Aurora Store

**Aurora Store** is an unofficial, FOSS client to Google Play with bare minimum features. Aurora Store
allows users to download, update, and search for apps like the Play Store. It works perfectly fine
with or without Google Play Services or MicroG.


[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/en/packages/com.aurora.store/)

## Active Development on GitLab

**The source code for Aurora Store is actively developed on GitLab. You can find the latest updates, contribute to development, and track the progress on the [Aurora Store GitLab repository](https://gitlab.com/AuroraOSS/AuroraStore).**


## Features

- FOSS: Has GPLv3 licence
- Beautiful design: Built upon latest Material 3 guidelines
- Account login: You can login with either personal or an anonymous account
- Device & Locale spoofing: Change your device and/or locale to access geo locked apps
- [Exodus Privacy](https://exodus-privacy.eu.org/) integration: Instantly see trackers in app
- Updates blacklisting: Ignore updates for specific apps
- Download manager
- Manual downloads: allows you to download older version of apps, provided
  - The APKs are available with Google
  - You know the version codes for older versions 

## Limitations

- Underlying API used is reversed engineered from PlayStore, changes on side may break it.
- Provides only base minimum features
  - Can not download or update paid apps.
  - Can not update apps/games with [Play Asset Delivery](https://developer.android.com/guide/playcore/asset-delivery)
- Multiple in-app features are not available if logged-in as Anonymous.
  - Library
  - Purchase History
  - Editor's Choise
  - Beta Programs
  - Review Add/Update
- Token Dispenser Server is not super reliable, downtimes are expected.  

## Downloads

Please only download the latest stable releases from one of these sources:

- [F-Droid](https://f-droid.org/en/packages/com.aurora.store/) (Recommended)
- [AuroraOSS](https://auroraoss.com/AuroraStore/)
- [GitLab Releases](https://gitlab.com/AuroraOSS/AuroraStore/-/releases)

## Certificate Fingerprints

- SHA1: `94:42:75:D7:59:8B:C0:3E:48:85:06:06:42:25:A7:19:90:A2:22:02`
- SHA256: `4C:62:61:57:AD:02:BD:A3:40:1A:72:63:55:5F:68:A7:96:63:FC:3E:13:A4:D4:36:9A:12:57:09:41:AA:28:0F`

## Support

Aurora Store v4 is still in on-going development! Bugs are to be expected! Any bug reports are appreciated.
Please visit [Aurora Wiki](https://gitlab.com/AuroraOSS/AuroraStore/-/wikis/home) for FAQs.

- [Telegram](https://t.me/AuroraSupport)
- [XDA Developers](https://forum.xda-developers.com/t/app-5-0-aurora-store-open-source-google-play-client.3739733/)

## Screenshots
<img src="screenshot-01.png" height="400"><img src="screenshot-03.png" height="400"><img src="screenshot-07.png" height="400"><img src="screenshot-08.png" height="400">

## Translations

Don't see your preferred language? Click on the widget below to help translate Aurora Store!

<a href="https://hosted.weblate.org/engage/aurora-store/">
  <img src="https://hosted.weblate.org/widgets/aurora-store/-/287x66-grey.png" alt="Translation status" />
</a>

## Project references

Aurora Store is based on these projects

- [YalpStore](https://github.com/yeriomin/YalpStore)
- [AppCrawler](https://github.com/Akdeniz/google-play-crawler)
- [Raccoon](https://github.com/onyxbits/raccoon4)
- [SAI](https://github.com/Aefyr/SAI)
