<img src="https://img.xda-cdn.com/1alP20gvsfJQN9MXLIcblm7aWSo=/https%3A%2F%2Fi.ibb.co%2FScPXnxz%2FFG-2.png" alt="Aurora Logo"><br/><img src="https://www.gnu.org/graphics/gplv3-88x31.png" alt="GPL v3 Logo"> 

# Aurora Store :  A Google Playstore Client

Aurora Store is an `UnOfficial` FOSS client to Google's Play Store, with an elegant design, using Aurora you can download apps, <br/> 
update existing apps, search for apps, get details about in-app trackers and much more.

You can also Spoof your Device Information, Language and Region to get access to the apps that are not yet available<br/> 
or restricted in your Country|Device.<br/>

Aurora Store does not require Google's Proprietary Framework (Spyware ?) to operate, it works perfectly fine with<br/>
or without GooglePlayService or MicroG. Thereby avoding the various concerned *userdata & privacy issues.<br/>

Earlier based on Sergei Yeriomin's [Yalp store](https://github.com/yeriomin/YalpStore), v3.0 is a clean & complete rewrite from scratch that follows<br/>
Material Design and runs on all devices running Android 5.0+.<br/>

# Features

* Free | Libre software
  -- Has GPLv3 licence

* Beautiful Design
  -- Built upon latest Material Design Guidelines

* Anonymous Accounts
  -- You can log in and download with anonymous accounts so you don't have to use your own account

* Personal Accounts
  -- You can download purchased apps or access your wishlist by using your own Google account

* Exodus Integration
  -- To tell you about trackers and other harmful components in the app instantly

# What's new in v3 ?
  
  * Inbuilt Download Manager
  * Improved Notification Manager
  * Improved Blacklist Manager & Filters
  * New UI based on latest Material Design guidelines
  * Support for Split | Bundled APK Installations 
  

# Downloads

  * Aurora OSS : [Nightlies](http://auroraoss.com/Nightly/)
  * XDA Forum : [Thread](https://forum.xda-developers.com/android/apps-games/galaxy-playstore-alternative-t3739733)
  * F-Droid : [Link](https://f-droid.org/en/packages/com.dragons.aurora/) (Links to v2 of Aurora Store, will update once v3 makes it to FDroid)
  
  `Aurora Store v3 is still in development ! Bugs are to be expected any bug reports are appreciated.`

# Frequently Asked Questions 

* <b>What is the difference between Aurora Store and Google PlayStore ?</b>

  Unlike Google Play Store, Aurora doesn't track your downloads or the apps you use. We respect your privacy. Aurora is also uneffected<br/>
  by Google marking your device as uncertified or a lack of Google Apps.''

* <b>Is Aurora Store a fork of YalpStore ?</b>

  Technically No, the Aurora Store v3 is written from scratch, but it does share some code from YalpStore

* <b>How can I report a bug or suggest something?</b>

  You can open issues here, or you can join our Telegram Developement Group [Join Now](https://t.me/AuroraSupport)

* <b>How to Join|Contribute to Aurora Store ?</b>

  I am open to any kind of suggestions/feature request, you can either mail me at <b>whyorean@gmail.com</b> or ping me on [Telegram](https://t.me/whyorean). 
  
* <b> Why Aurora store ?</b>

  Because PlayServices have always disappointed and are proven to be Spyware & Malicious to the end user [link](https://www.gnu.org/proprietary/malware-google.html).<br/> 
  Unfortunately, we cannot always get-by with just FOSS Apps. Aurora store helps us retrieve apps from the PlayStore repository,<br/>
  without having to trouble you and your device with Google Apps.

* <b> If I don't need GApps, Do I need MicroG ?</b>

  No. Aurora Store was built to access PlayStore without any kind of google services. It doesn't care if you use it with or without GApps or MicroG.

* <b> What is the FakeStore ?</b>

  Badly designed apps detect if PlayStore is missing and start misbehaving. The FakeStore is a stub that pretends to be as PlayStore and<br/>
  preventing the apps from crashing, as the package name for FakeStore is same as PlayStore (com.android.vending).

* <b> Is it safe to use Aurora store ?</b>

  Aurora is fully open source and verified by FDroid. If you're talking about the safety of the apps in the store, it just gets them from the same place<br/>
  that the PlayStore would, and are verified by Google. A lot of dangerous stuff seems to sneak past them though, so as a rule of thumb, don't download anything<br/>
  which you're not sure about.

* <b> What data does Aurora store send to Google ?</b>

  Aurora Store does its best to send the least identifiable information possible. It does send list of package names of your installed apps (for fetching updates), <br/>
  your search querries and your downloads for obvious reasons.

* <b> Do I need to use my own Google account to log in ?</b>

  Nope, Aurora Store can log you in with a dummy account so that nothing gets linked to your own account.

* <b> Why would I use my own account? Is it safe ?</b>

  You can use your own account to download apps purchased by you or to access your wishlist. Although you may want to be careful, as Google retains <br/>
  the right to block any account, so probably use a dummy account.

* <b> How do I login if I have 2-factor-authentication enabled ?</b>

  You just need to get an app password from the Google dashboard and use that to login into Aurora Store.

* <b> How do I purchase paid apps ?</b>

  Purchase the apps from the PlayStore [website](https://play.google.com/store), and login using your own account in Aurora Store to download them.

* <b> Can Aurora store verify licences ?</b>

  Not yet. All you can do at this point is pester the dev of the licenced app to give you an alternative method for verification.

* <b> Can I use Aurora store to get paid apps for free ?</b>

  No. Get out.

* <b> What is the FDroid filter ?</b>

  Since F-Droid signs APKs with its own keys, the PlayStore variants of apps cannot be installed over them. The FDroid filter excludes all the apps <br/>
  it finds with FDroid signatures on your device to prevent such conflicts.

* <b> What is the spoofing feature ?</b>

  Spoofing allows you to pretend to be any other device at any other location in the world, to download geo-restricted apps. You can use your own custom device<br/>
  configs by dropping the .properties file in the Downloads directory (Settings -> Downloads -> Download Path).

* <b> How does Aurora install apps ?</b>

  Aurora Store can install apps in 3 ways
    * Manual - Whenever an app is downloaded, it will open the manual installation screen. This doesn't require root or system perms.
    * Root | System - By giving Aurora Store root or system permissions, it will automatically install apps in the background as soon as they are downloaded.
    * Aurora Services - By installing Aurora Services as system app, Aurora Store can automatically install app upon download completion in background.

* <b> How do I use Aurora services ?</b>

  Install Aurora Services (to system preferred), open it, do everything it says, open Aurora settings and choose Aurora Services it as install method.<br/>
  You don't need to give Aurora Store system or root grants. Aurora Services handle all install & uninstall requests in background.
  
  `NOTE: Aurora Services support has yet to be implemented in v3.`

* <b> How to give Aurora Store|Services system permissions ?</b>

  You need to either manually push the APKs to /system/priv-app, or install the magisk module from the group.

* <b> Can Aurora Download and install Split or Bundled APKs ?</b>

  Yes, with or without root.

* <b> How can I submit/improve translations ?</b>

  Go to [POEditor](https://poeditor.com/join/project/54swaCpFXJ), and inform me when done.<br/>
  (Although I keep track of translations progress, but in case I miss, let me know)

* <b> Why do I have two Aurora Stores after installing the new builds ?</b>

  Because the v3 is a completely new rewrite, it comes with a new package name. You can uninstall the older one or keep it if you want.

* <b> Why are the versions on FDroid and XDA labs outdated? When will they be updated ?</b>

  Aurora Store is still in a development phase right now, and only infrequent stable builds will be uploaded there (Also, FDroid's review & build process is quite lengthy). <br/>
  You can always grab the latest tests builds either from the [Telegram Group](https://t.me/AuroraSupport) or from [AuroraOSS](http://auroraoss.com/Nightly/)

* <b>"Please add FDroid | Amazon App Store| Yada support"</b>

  No, this is a PlayStore client. Which means it's for the Play store only. Different clients for different services (^_~) 
 
 
 `If you are an Android App Developer and find something that can be improved|fixed|added, feel free to make a pull request.`
 
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
