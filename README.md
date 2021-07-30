# Wi-fi Clipboard

![App-Icon](https://github.com/LeHuman/WifiClipboard/raw/main/pythonScript/images/app.png)

## What is this

This very simple android app creates a widget on the homescreen which, when enabled, allows it to receive a string over a TCP socket to then copy it onto the clipboard.

In other words, this app is Copy-Paste over Wi-fi.

## Why

I thought that this method of just sending a string to my phone would be faster than the alternatives.

## Usage

Place the widget associated with this app on your homescreen.

Next, use the included python script / executable to send data from a locally connected device to this app.

Any received text will then be place on your device's clipboard.

Tap the icon twice to cancel.

When the widget is enabled, the device sending the string only really needs to connect to the address listed in the app and send a string over the appropriate TCP port.
