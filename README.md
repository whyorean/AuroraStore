Aurora - a Google Playstore alternative
==============================================================

The Aurora App is a fork of **YalpStore** by **Sergey Yeriomin** [@yeriomin](https://github.com/yeriomin), all credits to him.

**YalpStore** is a very well coded app, but it lacks Material Design, **Sergey Yeriomin** has clearly mentioned  
he will not redo UI/UX changes as his intention is to support devices from Android 2.x to 8.x which is perfectly fine.  
Adding up new Material UI Libraries will increase the app size, which would defeat its purpose in context of older devices.   

Also in an attempt to have wider device support the **YalpStore** uses some depreciated Android Libraries/Methods which have   
(minor) Security/MemoryLeak issues.

Some of us try to avoid **GooglePlayServices** as much as possible, because of the MemoryUsage/StorageSpace/Privacy/UnwantedUpdates.   
But not all of us use Devices with Lower RAM and InternalMemory, so I thought of redesigning **YalpStore**   

I renamed the **YalpStore** to **Aurora** to avoid any ambiguity that may arise, later I would refactor packagename too.

In **Aurora** I will try my best to solve this issues, and provided a clean material design.

**Aurora is derived from the following projects:**
* [YalpStore](https://github.com/yeriomin/YalpStore)
* [AppCrawler](https://github.com/Akdeniz/google-play-crawler)
* [Raccoon](https://github.com/onyxbits/raccoon4/)

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.dragons.aurora/)