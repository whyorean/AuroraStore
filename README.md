<img src="https://img.xda-cdn.com/1alP20gvsfJQN9MXLIcblm7aWSo=/https%3A%2F%2Fi.ibb.co%2FScPXnxz%2FFG-2.png" alt="Aurora Logo"><br/><img src="https://www.gnu.org/graphics/gplv3-88x31.png" alt="GPL v3 Logo">

# Aurora Store: A Google Playstore Client

*Aurora Store* is an **unofficial**, FOSS client to Google's Play Store with an elegant design. Not only does Aurora Store download, update, and search for apps like the Play Store, it also empowers the user with new features.

For those concerned with privacy, *Aurora Store* does not require Google's proprietary framework (spyware?) to operate; It works perfectly fine with or without GooglePlayService or [MicroG](https://microg.org/). However, those still reliant on those services are welcome to use *Aurora Store* as well!

While *Aurora Store* was originally based on Sergei Yeriomin's [Yalp store](https://github.com/yeriomin/YalpStore), v3.0 is a clean & complete rewrite from scratch that follows Material Design and runs on all devices running Android 5.0+.

# Screenshots

<img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss001.png" height="400"><img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss002.png" height="400">
<img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss003.png" height="400"><img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss004.png" height="400">
<img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss005.png" height="400"><img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss006.png" height="400">
<img src="https://gitlab.com/AuroraOSS/AuroraStore/raw/master/fastlane/metadata/android/en-US/phoneScreenshots/ss007.png" height="400">

# Features

* Free/Libre software
  -- Has GPLv3 licence

* Beautiful design
  -- Built upon latest Material Design guidelines

* Anonymous accounts
  -- You can log in and download with anonymous accounts so you don't have to use your own account

* Personal Accounts
  -- You can download purchased apps or access your wishlist by using your own Google account

* [Exodus](https://exodus-privacy.eu.org/) integration
  -- Instantly see trackers an app is hiding in its code

# What's new in v3?

  * Built-in download manager
  * Improved notification manager
  * Improved blacklist manager & filters
  * New UI based on latest Material Design guidelines
  * Support for split/bundled APK installations


# Downloads

  * Aurora OSS: [Nightlies](http://auroraoss.com/Nightly/)
  * XDA Forum: [Thread](https://forum.xda-developers.com/android/apps-games/galaxy-playstore-alternative-t3739733)
  * F-Droid: [Link](https://f-droid.org/en/packages/com.aurora.store/)

  `Aurora Store v3 is still in development! Bugs are to be expected! Any bug reports are appreciated.`

# Frequently Asked Questions

* <b>What is the difference between Aurora Store and Google's Play store?</b>

  Unlike Google's Play Store, *Aurora Store* doesn't track your downloads or the apps you use. We respect your privacy. Aurora is also unaffected by Google marking your device as *uncertified* or lacking of necessary Google apps.

* <b>Is *Aurora Store* a fork of YalpStore?</b>

  Technically, no. the *Aurora Store* v3 is written from scratch, but it does share some code from YalpStore.

* <b>How can I report a bug or suggest something?</b>

  You can open issues [here](https://gitlab.com/AuroraOSS/AuroraStore/issues), or you can join our [Telegram Developement Group](https://t.me/AuroraSupport).

* <b>How do I join/contribute to *Aurora Store*?</b>

  I am open to any kind of suggestions/feature request! you can either mail me at [whyorean@gmail.com](mailto:whyorean@gmail.com) or ping me on [Telegram](https://t.me/whyorean).

* <b> Why create *Aurora Store*?</b>

  Because Google's Play store disappoints with a lack of features; Play is also proven to be [spyware/malicious](https://www.gnu.org/proprietary/malware-google.html).<br/>
  Unfortunately, we cannot always get by with just FOSS apps, so *Aurora Store* helps us retrieve apps from the Google Play repository without having to trouble you and your device with Google Apps.

* <b>Do I need Google Play Services to use *Aurora Store*?</b>

  No. *Aurora Store* was built to access the Google Play store without any kind of Google services. It doesn't care if you use it with or without Google Play Services/MicroG.

* <b>What is the FakeStore?</b>

  Some poorly-designed apps detect if Google Play is missing and punish the user by misbehaving. The FakeStore is a stub that disguises itself as the Play store: FakeStore shares the same package name as the Play store (`com.android.vending`). This prevents some apps from crashing.

* <b>Is it safe to use Aurora store?</b>

  Aurora is fully open source and verified by FDroid. If you're talking about the safety of the apps in the store, it just gets them from the same place<br/>
  that the PlayStore would, and are verified by Google. A lot of dangerous stuff seems to sneak past them though, so as a rule of thumb, don't download anything<br/>
  which you're not sure about.

* <b>What data does *Aurora Store* send to Google?</b>

  *Aurora Store* does its best to send the least identifiable information possible. It does send list of package names of your installed apps (for fetching updates).<br/>
  It also sends your search queries and your downloads for obvious reasons.

* <b>Do I need to use my own Google account to log in?</b>

  Nope. *Aurora Store* can log you in with a dummy account so that nothing gets linked to your own account.

* <b>Why would I use my own account? Is it safe?</b>

  You can use your own account to download apps purchased by you or to access your wishlist. However, you may want to be careful as Google retains <br/>
  the right to block any account. It might be worth using a dummy account for that reason.

* <b>How do I log in if I have two-factor authentication enabled?</b>

  You just need to get an app password from the Google dashboard and use that to login into *Aurora Store*.

* <b>How do I purchase paid apps?</b>

  Purchase the apps from the [Google Play website](https://play.google.com/store), then log in using your own account in *Aurora Store* to download them.

* <b>Can Aurora store verify licences?</b>

  Not yet. All you can do at this point is pester the dev of the licenced app to give you an alternative method for verification.

* <b>Can I use Aurora store to get paid apps for free?</b>

  No. Get out.

* <b>What is the FDroid filter?</b>

  Since F-Droid signs APKs with its own keys, the Play store variants of apps cannot be installed over them. The F-Droid filter excludes all the apps <br/>
  it finds with FDroid signatures on your device to prevent such conflicts.

* <b>What is the spoofing feature?</b>

  Spoofing allows you to pretend to be any other device at any other location in the world in order to download geo-restricted apps. You can use your own custom device<br/>
  configs by dropping the .properties file in the Downloads directory (Settings -> Downloads -> Download Path).

* <b>How does *Aurora* install apps?</b>

  *Aurora Store* can install apps in 3 ways:
    * Manual - Whenever an app is downloaded, it will open the manual installation screen. This doesn't require root or system perms.
    * Root/System - By giving Aurora Store root or system permissions, it will automatically install apps in the background as soon as they are downloaded.
    * *Aurora Services* - By installing *Aurora Services* as system app, *Aurora Store* can automatically install app upon download completion in background.

* <b>How do I use *Aurora Services*?</b>

  1. Install *Aurora Services* (preferably to the system).
  2. Open *Aurora Services* and follow the initial set-up instructions
  3. Open *Aurora Services*' settings and choose Aurora Services it as an install method.

  You don't need to give *Aurora Store* system or root grants; *Aurora Services* handles all install and uninstall requests in the background.
  
  Get Aurora Services from [here](https://gitlab.com/AuroraOSS/AuroraServices)

* <b>How to give *Aurora Services* system permissions?</b>

  You need to either manually push the APKs to `/system/priv-app`, or install the Magisk module from the [Telegram Group](https://t.me/AuroraSupport), will also attach the same in future release tags (>3.0.7)

* <b>Can Aurora Download and install Split or Bundled APKs?</b>

  Yes, with or without root.

* <b>How can I submit/improve translations ?</b>

  Go to [POEditor](https://poeditor.com/join/project/54swaCpFXJ), and inform me when done.<br/>
  (I typically keep track of translations progress, but it's possible that I missed a contribution. If you don't receive a reply, please remind me!)

* <b>Why do I have two *Aurora Store* installations after installing the new builds?</b>

  Because the v3 is a completely new rewrite, it comes with a new package name. You can uninstall the older one or keep it if you want.

* <b>Why are the versions on F-Droid and XDA labs outdate? When will they be updated ?</b>

  Aurora Store is still in a development phase right now; Only infrequent, stable builds will be uploaded there. F-Droid's review & build process is also quite lengthy. <br/>
  You can always grab the latest tests builds either from the [Telegram Group](https://t.me/AuroraSupport) or from [AuroraOSS](http://auroraoss.com/Nightly/)

* <b>"Please add support for F-Droid/Amazon/Yada repositories!"</b>

  No, this is a Play store client only. Different clients for different services (^_~)

* <b>Installation failes without warning after download on MIUI/OneUI/H2OS!</b>

  Apps can't be installed on MIUI/OneUI/H2OS unless you turn off vendor optimizations (like MIUI Optimizations)
and select "Enforce Native Installer" from Aurora->Settings->Installations.

 `If you are an Android App Developer and find something that can be improved/fixed/added, feel free to make a pull request.`

# Aurora Store uses the following Open Source libraries:

* [RX-Java](https://github.com/ReactiveX/RxJava)
* [ButterKnife](https://github.com/JakeWharton/butterknife)
* [OkHttp3](https://square.github.io/okhttp/)
* [Glide](https://github.com/bumptech/glide)
* [Fetch2](https://github.com/tonyofrancis/Fetch)
* [PlayStoreApi-v2](https://github.com/whyorean/playstore-api-v2)

# Aurora Store is based on following projects:

* [YalpStore](https://github.com/yeriomin/YalpStore)
* [AppCrawler](https://github.com/Akdeniz/google-play-crawler)
* [Raccoon](https://github.com/onyxbits/raccoon4)
* [SAI](https://github.com/Aefyr/SAI)

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60">](https://f-droid.org/packages/com.dragons.aurora/) [<img src="https://poeditor.com/public/images/logo_small.png" alt="Join POEditor">](https://poeditor.com/join/project/54swaCpFXJ)