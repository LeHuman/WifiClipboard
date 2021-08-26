import os
import re
import sys
import time
import socket
import threading
import subprocess
import multiprocessing

FORMAT = "utf-8"
PORT = 55051

def getLocalIPFormat():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        sName = s.getsockname()[0]
        s.close()
        return re.search("\\d+\\.\\d+\\.\\d+\\.", sName)[0] + "%i"
    except:
        print("Failed to get local ip")
        return "192.168.1.%i"


msg = ""

if len(sys.argv) > 1:
    arg = sys.argv[1]
    try:
        _PORT = int(arg)
        if _PORT > 65535 or _PORT < 0:
            raise OverflowError
        PORT = _PORT
    except:
        print("Bad argument")

try:
    import pyperclip  # default to faster, but required install

    msg = pyperclip.paste()

    if not msg:
        raise ValueError
except ModuleNotFoundError:
    # tkinter comes with python on Windows by default, and is common
    from tkinter import Tk

    msg = Tk().clipboard_get()  # tkinter has clipboard option builtin
except:
    print("Failed to read from Clipboard")
    sys.exit(0)

Connected = False
threads = []

# Attempt to connect to the give IP
def sendMsg(HOST_IP):
    global Connected
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.settimeout(1)  # at this point, server should be on already
        client.connect((HOST_IP, PORT))
        response = client.recv(2048).decode(FORMAT)
        # We expect this exact response, otherwise this connection may have been a coincidence
        if response == "Wi-fi Clipboard Connected":
            Connected = True
            print("Sent", HOST_IP + ":" + str(PORT))
            client.send(str(msg if msg else input("Give input:")).encode(FORMAT))
            client.close()
            sys.exit(0)
        else:
            client.close()
    except ConnectionRefusedError:
        pass
    except socket.timeout:
        pass
    except OSError:
        pass

devices = ""

for device in os.popen("arp -a"):
    devices += device

# TODO: Be smarter about which IPs we check in the first place
devices = re.findall("^\\W+([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+).*$", devices, re.MULTILINE)

# Attempt to connect to every avaliable host using the specific port
for device in devices:
    thread = threading.Thread(target=sendMsg, args=(device,))
    threads.append(thread)

for thread in threads:
    thread.start()

BASE_IP = getLocalIPFormat()

for i in range(255):
    ip = BASE_IP%i
    if ip not in devices:
        thread = threading.Thread(target=sendMsg, args=(ip,))
        thread.start()