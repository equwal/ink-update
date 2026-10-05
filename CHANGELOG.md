# Changelog

## 0.1.2

- An app that a F-Droid client installed gets its updates from that client.
  Ink Update no longer offers a GitHub download for it.
- Rebind is checked on F-Droid.
- The description names the check at the first start and the daily check.

## 0.1.1

- The APK is smaller: R8 removes the code that the app does not use.

## 0.1.0

First release.

- Watches Rebind, Ink Recents, Ink Dim and Ink Update. It shows only the ones
  that are installed.
- Looks in F-Droid, then Google Play, then GitHub.
- Makes one notification for each app that has an update. Tap it for the page,
  or tap "Download" for the APK.
- A daily check that lives through a restart. You can turn it off.
- The intent action `dev.equwal.inkupdate.CHECK`.
