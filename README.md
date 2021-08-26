# Wi-fi Clipboard

![App-Icon](https://github.com/LeHuman/WifiClipboard/raw/main/pythonScript/images/app.png)

## What is this

This very simple android app creates a widget on the homescreen which, when enabled, allows it to receive a string over a TCP socket to then copy it onto the clipboard.

In other words, this app is Copy-Paste over Wi-fi.

## Why

I thought that this method of just sending a string to my phone would be faster than the alternatives.

## Usage

Place the widget associated with this app on your homescreen.

Next, use the included python script / executable to send data from a locally connected device to this app. Make sure you have copied the text you want to send.

Any received text will then be place on your device's clipboard.

Tap the icon twice to cancel.

## Requirements

The arp command is required for the python script / executable.

I believe Windows and Mac come with this command.

Linux needs [net-tools](https://wiki.linuxfoundation.org/networking/net-tools) to be installed, or at least just the arp command.

If using the python script, [python3](https://www.python.org/) is needed along with the module [pyperclip](https://pypi.org/project/pyperclip/) or [tkinter](https://docs.python.org/3/library/tkinter.html)

Android device with SDK 23+.

Local network for everything to connect through.

## Notes

When the widget is enabled, the device sending the string only really needs to connect to the address listed in the app and send a string over the appropriate TCP port.

My current implementation in python is a bit brute forced as it attempts to connect with nearly every host available.

Security was not a concern as this probably shouldn't be used publicly.Regardless, encrypting the string being sent might help with this.

## Compatibility

Android app is built for SDK 21+, but only tested on 30

Python script / executable has only really been tested on Win10. However, I believe implementing the sender is easier than the receiver.

Sender does not necessarily have to be another computer or phone.

## Improvements

Originally this app was going to use bluetooth, however, I rarely use bluetooth, but I am almost always connected to Wi-fi.

Find a way so that the clipboard is constantly in sync. One issue, however, is whether this is actually worth the background process.