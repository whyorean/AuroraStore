# Aurora Privacy Policy
### Data collection using bug report tracking system.
We implemented an automated bug reporting system that is transparent to
the user. When Aurora crashes on an Android device, the user is shown a
dialog containing a summary of the information.

The user may now opt to send the full details to the us in order to
allow us to identify and hopefully fix the bug. When a user clicks the
send button, Aurora composes an e-mail containing the details, allowing
users to read the data and check exactly what data could be sent to us.

Users can remove or anonymize data which they do not want to be shared.
Users can optionally attach logcat in the email.

The generated data contains the following information:

* BUILD_CONFIG (Includes the app details - package name,
  version-name,version-code )
* BUILD (Includes your device details)
* USER_EMAIL (Sender's Email)
* ENVIRONMENT (Your device's storage paths)
* STACK TRACE (Stack trace of the issue/exception)
* LOGCAT (Optional can be externally sent, covers last 200 lines)

Aurora Bug hunter is implemented to not embed any personal data in the
blob. However, we cannot guarantee that there won't be any personal data
contained by accident. Also, we cannot control whether the user will add
any personal data to the generated data, nor can we control whether
users add personal data outside it.

Therefore we tried to implement our bug report system as transparently
as possible, allowing the user to entirely control what data is sent.

We assume that by sending an actual e-mail to us, any personal data is
sent on behalf and with consent of the user. We preserve the right to
process this data and store the data as long as it is needed for
analysis.

The e-mails are currently stored on a google mail server (buy me a
better one), and won't be deleted before processing takes place.

Any additional text in the e-mail that is not part of the report is
considered as user comment and will be processed and stored by us as
well.
