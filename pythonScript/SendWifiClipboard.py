import os
import socket
import multiprocessing
import subprocess
import os
import re
import socket
import threading
from tkinter import Tk
import threading
import time
import sys

FORMAT = "utf-8"
PORT = 55051

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

try:  # tkinter comes with python on Windows by default
    msg = Tk().clipboard_get()  # tkinter has clipboard option builtin
except:
    print("Clipboard empty")
    exit(0)


Done = 0
threads = []

# Attempt to connect to the give IP
def sendMsg(HOST_IP):
    global Done
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.settimeout(1)  # at this point, server should be on already
        client.connect((HOST_IP, PORT))
        response = client.recv(2048).decode(FORMAT)
        # We expect this exact response, otherwise this connection may have been a coincidence
        if response == "Wi-fi Clipboard Connected":
            print("Sent", HOST_IP + ":" + str(PORT))
            client.send(str(msg if msg else input("Give input:")).encode(FORMAT))
            client.close()
            Done = len(threads)
        else:
            client.close()
    except ConnectionRefusedError:
        pass
    except socket.timeout:
        pass
    except OSError:
        pass
    Done += 1


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

while Done < len(threads):
    pass