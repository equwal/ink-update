# Privacy

Ink Update collects nothing.

There is no account, no identifier, no advertisement and no analytics. The app
sends nothing to me.

## The two hosts it contacts

The app makes one kind of request to each host.

**f-droid.org**

    https://f-droid.org/api/v1/packages/<package name>

**api.github.com**

    https://api.github.com/repos/<owner>/<name>/releases?per_page=10

Each host sees what any web server sees: your IP address and the name in the
address. The name is the package of a watched app, or the repository of a
watched app. The request carries a User-Agent that says "InkUpdate" and the
version. It carries no cookie and no account.

The app makes these requests when you tap "Check now", and once a day while the
daily check is on. It makes none of them for an app that Google Play installed.

## What the app stores

The app stores the result of the last check on your device: the version it
found, the address of the page, and the time. It also stores whether the daily
check is on, and which version it last told you about. Android deletes all of
it when you uninstall the app.

## The permissions

- **INTERNET** and **ACCESS_NETWORK_STATE** make the two requests above.
- **POST_NOTIFICATIONS** tells you that an update is ready.
- **RECEIVE_BOOT_COMPLETED** keeps the daily check after you restart the
  device.

The app cannot install an app. It opens a page, and you decide.

## Contact

truex@equwal.com
