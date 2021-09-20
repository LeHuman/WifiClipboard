import os
import re
import sys
import socket
from multiprocessing import Process
import signal
import platform
from time import sleep

MSG_FORMAT = "utf-8"
DEFAULT_PORT = 55051


def stopScript(PID):
    if platform.system() != "Windows":
        os.killpg(PID, signal.SIGKILL)
    else:
        os.kill(PID, signal.SIGTERM)


def getLocalIPFormat() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        sName = s.getsockname()[0]
        s.close()
        return re.search("\\d+\\.\\d+\\.\\d+\\.", sName)[0] + "%i"
    except:
        print("Using default ipv4 format")
        return "192.168.1.%i"


def getPort() -> int:
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        try:
            _PORT = int(arg)
            if _PORT > 65535 or _PORT < 0:
                raise OverflowError
            return _PORT
        except:
            print("Bad argument")
    return DEFAULT_PORT


def getMessage() -> str:
    msg: str = None
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
    return msg


# Attempt to connect to the give IP
def sendMsg(HOST_IP, msg, PORT, PID):
    try:
        client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client.settimeout(1)  # at this point, server should be on already
        client.connect((HOST_IP, PORT))
        response = client.recv(2048).decode(MSG_FORMAT)
        # We expect this exact response, otherwise this connection may have been a coincidence
        if response == "Wi-fi Clipboard Connected":
            print("Sent", HOST_IP + ":" + str(PORT))
            client.send(str(msg if msg else input("Give input:")).encode(MSG_FORMAT))
            client.close()
            stopScript(PID)
            sys.exit(0)
        else:
            client.close()
    except ConnectionRefusedError:
        pass
    except socket.timeout:
        pass
    except OSError:
        pass


if __name__ == "__main__":

    msg = getMessage()
    devices = ""

    PORT = getPort()
    PID = os.getpid() if platform.system() == "Windows" else os.getpgid(os.getpid())

    for device in os.popen("arp -a"):
        devices += device

    # IMPROVE: Be smarter about which IPs we check in the first place
    devices = re.findall("^\\W+([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+).*$", devices, re.MULTILINE)

    threads: list[Process] = []

    # Attempt to connect to every avaliable host using the specific port
    for device in devices:
        thread = Process(target=sendMsg, args=(device, msg, PORT, PID))
        threads.append(thread)

    for thread in threads:
        thread.start()
        sleep(0.05)

    BASE_IP = getLocalIPFormat()

    print("Scanning all local addresses")

    for i in range(255):
        ip = BASE_IP % i
        thread = Process(target=sendMsg, args=(ip, msg, PORT, PID))
        thread.start()