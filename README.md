# Bluetooth Devices Helper

Bluetooth Devices Helper, also known as BtHelper, is an application that enhances the<br>
functionality of Bluetooth devices by providing<br>
features such as battery level display and device icons on Android Open Source Project (AOSP).<br>
These features are not currently supported by AOSP.<br>

## Project Status

Currently, Only Apple AirPods series are supported by this project.<br>
Media play/pause, OnePod mode features are highly experimental.

## Requirements

This project depends on AOSP components.<br>
You need to add these commits to corresponding repositories.

* packages/apps/Settings: [[HACK] Settings: Bluetooth: Don't check persistable uri permission for BtHelper](https://github.com/TheParasiteProject/packages_apps_Settings/commit/189dcd3b6c62cbc99e170a3b4c898ab3d039c542)
* vendor repository (e.g. vendor/aosp): [vendor: config: common: Build BtHelper](https://github.com/TheParasiteProject/vendor_aosp/commit/424bca6b12a9f1d5fd56374ba6ae4310ea98cde3)

After that clone this repo:<br>
```bash
git clone https://github.com/TheParasiteProject/packages_apps_BtHelper packages/apps/BtHelper
```

Finally, build your AOSP.<br>
BtHelper will be installed as system application.

## Acknowledgements

We would like to express our gratitude to the following projects:
* [OpenPods](https://github.com/adolfintel/OpenPods): Code base of this project.
* [CAPod](https://github.com/d4rken-org/capod): Media play/pause, OnePod mode, and many other functionalities.
* [xingrz](https://github.com/xingrz): Android System Settings integration.

## Trademark

<b>Trademark Notice:</b><br>
The term "AirPods" is a registered trademark of Apple Inc.<br>

<b>Ownership of Image Assets:</b><br>
The image assets located in the "res-apple" folder within this repository are the property of Apple Inc.<br>

## License

Bluetooth Devices Helper is licensed under the [GNU General Public License v3.0](LICENSE.md).
<br>
Copyright (C) 2023 someone5678
