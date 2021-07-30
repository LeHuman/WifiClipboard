package com.lehuman.wificlipboard;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TCPServer {

    final int port;
    int job = 0;
    boolean done = true;
    volatile String response;
    String message = "Wi-fi Clipboard Connected";
    Thread messenger;
    TCPListener listener;
    ServerSocket serverSocket;

    public interface TCPListener {
        void run(String message);
    }

    public TCPServer(int port) {
        this.port = port;
    }

    public boolean available() {
        return serverSocket != null;
    }

    public void setTimeout(Context context, int timeout) {
        try {
            serverSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to set timeout", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean running() {
        return job != 0 || !done;
    }

    public void receive() {
        response = null;
        job = 1;
        done = false;
    }

    public void send(String msg) {
        message = msg;
        job = 2;
        done = false;
    }

    public void cancel() {
        messenger.interrupt();
        messenger = new Thread(new Messenger());
        job = 0;
        done = true;
        messenger.start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            messenger = new Thread(new Messenger());
            messenger.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (messenger != null)
            messenger.interrupt();
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setListener(TCPListener listener) {
        this.listener = listener;
    }

//    public String waitForResponse() {
//        while (response == null) {
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        return response;
//    }

    public String getResponse() {
        return response;
    }

    private class Messenger extends Thread {
        private void work(boolean sendOnly) {
            try {
                Socket socket = serverSocket.accept();

                OutputStream outputStream = socket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(message);

                if (sendOnly) {
                    printStream.close();
                    return;
                }

                InputStream inputStream = socket.getInputStream();
                String result = Utility.inputStreamString(inputStream);
                inputStream.close();

                response = result;

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (listener != null)
                listener.run(response);
        }

        @Override
        public void run() {
            while (job != -1) {
                if (job != 0) {
                    boolean sendOnly = job == 2;
                    job = 0;
                    work(sendOnly);
                    done = true;
                }
            }
        }
    }

}
