import PyInstaller.__main__

PyInstaller.__main__.run(
    [
        "SendWifiClipboard.py",
        "--onefile",
        "--icon=images/app.ico",
        "--version-file=version_file.txt"
    ]
)
