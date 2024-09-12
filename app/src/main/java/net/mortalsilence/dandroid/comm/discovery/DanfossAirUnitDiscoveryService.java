package net.mortalsilence.dandroid.comm.discovery;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Enumeration;

public class DanfossAirUnitDiscoveryService {

    private static final String TAG = "DanfossAirUnitDiscoveryService";
    private static final int BROADCAST_PORT = 30045;
    private static final byte[] DISCOVER_SEND = { 0x0c, 0x00, 0x30, 0x00, 0x11, 0x00, 0x12, 0x00, 0x13 };
    private static final byte[] DISCOVER_RECEIVE = { 0x0d, 0x00, 0x07, 0x00, 0x02, 0x02, 0x00 };

    public void scanForDevice() {
        Log.d(TAG, "Start Danfoss Air CCM scan");
        discover();
    }

    private synchronized void discover() {
        Log.d(TAG, "Try to discover all Danfoss Air CCM devices");

        try (DatagramSocket socket = new DatagramSocket()) {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    if (interfaceAddress.getBroadcast() == null) {
                        continue;
                    }
                    Log.d(TAG, "Sending broadcast on interface " + interfaceAddress.getAddress() + " to discover Danfoss Air CCM device...");
                    sendBroadcastToDiscoverThing(socket, interfaceAddress.getBroadcast());
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "No Danfoss Air CCM device found. Diagnostic: " + e.getMessage());
        }
    }

    private void sendBroadcastToDiscoverThing(DatagramSocket socket, InetAddress broadcastAddress) throws IOException {
        socket.setBroadcast(true);
        socket.setSoTimeout(500);
        // send discover
        byte[] sendBuffer = DISCOVER_SEND;
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, broadcastAddress, BROADCAST_PORT);
        socket.send(sendPacket);
        Log.d(TAG, "Discover message sent");

        // wait for responses
        while (true) {

            byte[] receiveBuffer = new byte[7];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            try {
                socket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "socket timeout");
                break; // leave the endless loop
            }

            byte[] data = receivePacket.getData();
            if (Arrays.equals(data, DISCOVER_RECEIVE)) {
                Log.d(TAG, "Discover received correct response");
                String host = receivePacket.getAddress().getHostName();
                Log.d(TAG, "unit hostname " + host);
                Log.d(TAG, "unit address " + receivePacket.getAddress().hashCode());
                DiscoveryCache.DISCOVERY_CACHE_INSTANCE.setHost(host);
            }
        }
        Log.d(TAG, "after endless loop");
    }
}
