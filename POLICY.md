# Aurora Store Privacy Policy

Last Updated: **16.10.2023**

## User Data sent to Google

The following data is mandatory to make the Service function, all of which Google needs for **Aurora Store** to be able to receive required data:

- IP Address, bound to network request
- Timezone, MCC & MNC are stripped & replaced with a random value. (see [here](https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/providers/NativeDeviceInfoProvider.kt?ref_type=heads#L104-105))
- List of install apps, search query & packagename of any new app that you install.
   - List of install apps sent to Google can be filter from within the App.

If you choose to use your own account, the following are added to the above:

- Account details for Google sign-in verification

We do not store, process or sell any of the collected data above. 
The data is sent to and processed by Google's servers directly untouched.

All accounts being used within **Aurora Store** are saved onto the device as AAS Tokens & email pair for account verification. 

**No passwords** are saved to Aurora Store or  Aurora Dispenser.

## Device Data sent to Google

### Build Properties
- Build.RADIO
- Build.BOOTLOADER
- Build.BRAND
- Build.ID
- Build.HARDWARE
- Build.VERSION.RELEASE
- Build.VERSION.SDK_INT
- Build.MODEL
- Build.FINGERPRINT
- Build.MANUFACTURER
- Build.DEVICE
- Build.PRODUCT

### Device Display Info (Required to decide what resources device supports. i.e. hdpi, xhdpi..so on)
- Screen.Density
- Screen.Width
- Screen.Height
- TouchScreen
- ScreenLayout

### Device Navigation Capabilities
- Keyboard
- HasHardKeyboard
- Navigation
- HasFiveWayNavigation

### CPU Architecture (Required to decide what binaries device supports. i.e. armeabi-v7a, arm64-v8a)
- Platforms

### List of all supported languages on device
- Locales

### Graphics Library (Required to decide GPU capabilities, mostly required to fing supported Games)

- GL.Extensions
- GL.Version

### PlayStore (if installed, otherwise whaterver works, see [here](https://gitlab.com/AuroraOSS/AuroraStore/-/blame/master/app/src/main/java/com/aurora/store/data/providers/NativeDeviceInfoProvider.kt?ref_type=heads#L94-97))
- GSF.version
- Vending.version
- Vending.versionString

### List of all device features & available libraries
- Features
- SharedLibraries

### Cellular Network Info (random value, see [here](https://gitlab.com/AuroraOSS/AuroraStore/-/blob/master/app/src/main/java/com/aurora/store/data/providers/NativeDeviceInfoProvider.kt?ref_type=heads#L104-105))
- MCC: Mobile Country Code 
- MNC: Mobile Network Code

Above fields are sent to Google as encoded-protobuf, you can find a sample device config used in Aurora Store [here](https://gitlab.com/AuroraOSS/gplayapi/-/blob/master/lib/src/main/res/raw/gplayapi_px_7a.properties).

------------------------------------------------------------------------

## User data sent to our server by default

The only data being sent to our server are **IP addresses**. This is to ensure the functionality and stability of our server, which detects the amount of GET requests from an IP address and if deemed them as spamming, will be banned from our server, otherwise rate-limited.

Rate-limiting lasts for an hour. If you keep going over the 20 request per hour limit, an additional hour to the rate-limiting will be added for every addtional 20 requests.

## Changes To This Privacy Policy

We keep updating our Privacy Policy from time to time. Depending upon new developements or API changes on Google side. You are thus advised to review this Privacy Policy periodically for any changes. 

## Contact Us

If you have any questions about this Privacy Policy, please contact us at **aurora.dev@gmail.com** or via our **[support group](https://t.me/AuroraSupport)** on Telegram.
