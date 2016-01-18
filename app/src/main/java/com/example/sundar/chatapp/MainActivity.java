package com.example.sundar.chatapp;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MainActivity extends ActionBarActivity {

    String TAG="NsdHelper";
    NsdHelper1 mNsdHelper1 = null;

    static final int SocketServerPORT = 8080;


    LinearLayout loginPanel, chatPanel;

    TextView infoIp, infoPort, chatMsg;

    String msgLog = "";

    EditText editTextSay;
    EditText editTextUserName, editTextAddress;
    Button buttonConnect;


    Button buttonSend;
    Button buttonDisconnect;
    ChatClientThread chatClientThread = null;
    ChatClientThread1 chatClientThread1 = null;





    ServerSocket serverSocket;

    boolean flag = false;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextUserName = (EditText) findViewById(R.id.username);
        editTextAddress = (EditText) findViewById(R.id.address);
        buttonConnect = (Button) findViewById(R.id.connect);



        loginPanel = (LinearLayout) findViewById(R.id.loginpanel);
        chatPanel = (LinearLayout) findViewById(R.id.chatpanel);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);
        chatMsg = (TextView) findViewById(R.id.chatmsg);
        buttonSend = (Button) findViewById(R.id.send);
        buttonDisconnect = (Button) findViewById(R.id.disconnect);
        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        infoIp.setText(getIpAddress());

        editTextSay = (EditText) findViewById(R.id.say);

        buttonSend.setOnClickListener(buttonSendOnClickListener);
        buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);

        mNsdHelper1 = new NsdHelper1(this);
        mNsdHelper1.initializeNsd();





        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();




    }
    public void clickAdvertise(View v) {
        // Register service
        if(serverSocket.getLocalPort() > -1) {
            mNsdHelper1.registerService(serverSocket.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }
    }
    public void clickDiscover(View v) {
        mNsdHelper1.discoverServices();
    }

    @Override
    protected void onPause() {
        if (mNsdHelper1 != null) {
            mNsdHelper1.stopDiscovery();

        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper1 != null) {
            mNsdHelper1.discoverServices();
        }
    }











    View.OnClickListener buttonDisconnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (chatClientThread == null && chatClientThread1 == null) {
                return;
            }
            if (chatClientThread != null) {
                chatClientThread.disconnect();

            }
            if (chatClientThread1 != null) {
                chatClientThread1.disconnect();

            }

        }

    };

    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (editTextSay.getText().toString().equals("")) {
                return;
            }

            if (chatClientThread == null && chatClientThread1 == null) {
                return;
            }
            if (chatClientThread != null) {
                chatClientThread.sendMsg(editTextSay.getText().toString() + "\n");

            }
            if (chatClientThread1 != null) {
                chatClientThread1.sendMsg(editTextSay.getText().toString() + "\n");

            }


            editTextSay.setText("");
        }

    };

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String textUserName = editTextUserName.getText().toString();
            if (textUserName.equals("")) {
                Toast.makeText(MainActivity.this, "Enter User Name",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String textAddress = editTextAddress.getText().toString();
            if (textAddress.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Addresse",
                        Toast.LENGTH_LONG).show();
                return;
            }

            msgLog = "";
            chatMsg.setText(msgLog);
            loginPanel.setVisibility(View.GONE);
            chatPanel.setVisibility(View.VISIBLE);

            chatClientThread1 = new ChatClientThread1(
                    textUserName, textAddress, serverSocket.getLocalPort());
            chatClientThread1.start();
        }

    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNsdHelper1.tearDown();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;


            try {
                serverSocket = new ServerSocket(0);
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("Port No: "
                                + serverSocket.getLocalPort());
                    }

                });


                while (!flag) {
                    socket = serverSocket.accept();
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            loginPanel.setVisibility(View.GONE);
                            chatPanel.setVisibility(View.VISIBLE);
                        }

                    });

                    chatClientThread= new ChatClientThread(socket);
                    chatClientThread.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }


    }

    private class ChatClientThread extends Thread {


        String msgToSend = "";
        boolean goOut = false;
        Socket socket = null;

        ChatClientThread(Socket socket) {
            this.socket = socket;

        }

        @Override
        public void run() {

            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());


                while (!goOut) {
                    if (socket != null) {
                        if (dataInputStream.available() > 0) {
                            msgLog += dataInputStream.readUTF();

                            MainActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    chatMsg.setText(msgLog);
                                }
                            });
                        }

                        if (!msgToSend.equals("")) {
                            dataOutputStream.writeUTF(msgToSend);
                            dataOutputStream.flush();
                            msgToSend = "";
                        }
                    }
                    else{
                        Toast.makeText(MainActivity.this,
                                "Client left", Toast.LENGTH_LONG).show();
                        break;
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Client left", Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Client left", Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {


                        loginPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

        private void disconnect(){
            goOut = true;

        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "My Ip "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class ChatClientThread1 extends Thread {

        String name;
        String dstAddress;
        int dstPort;

        String msgToSend = "";
        boolean goOut = false;

        ChatClientThread1(String name, String address, int port) {
            this.name = name;
            dstAddress = address;
            dstPort = port;
        }

        @Override
        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream.writeUTF(name + "\n");
                dataOutputStream.flush();

                while (!goOut) {
                    if (socket != null) {
                        if (dataInputStream.available() > 0) {
                            msgLog += dataInputStream.readUTF();

                            MainActivity.this.runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    chatMsg.setText(msgLog);
                                }
                            });
                        }

                        if (!msgToSend.equals("")) {
                            dataOutputStream.writeUTF(msgToSend);
                            dataOutputStream.flush();
                            msgToSend = "";
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Client left", Toast.LENGTH_LONG).show();
                        break;

                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Client left", Toast.LENGTH_LONG).show();
                    }

                });
            } catch (IOException e) {
                e.printStackTrace();
                final String eString = e.toString();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Client left", Toast.LENGTH_LONG).show();
                    }

                });
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        loginPanel.setVisibility(View.VISIBLE);
                        chatPanel.setVisibility(View.GONE);
                    }

                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

        private void disconnect(){
            goOut = true;
        }
    }



}