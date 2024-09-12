package net.mortalsilence.dandroid.comm

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class DanfossAirUnitCommunicationController(private val inetAddr: InetAddress) :
    CommunicationController {

    companion object {
        private const val SOCKET_TIMEOUT_MILLISECONDS = 5000
        private const val TAG = "DanfossAirUnitCommunicationController"
    }

    private val port = 30046
    private var connected = false
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    @Synchronized
    @Throws(IOException::class)
    override fun connect() {
        if (connected) {
            return
        }
        val localSocket = Socket()
        Log.d(TAG, "Connecting socket using inetAddr $inetAddr and port $port")
        localSocket.connect(InetSocketAddress(inetAddr, port), 5000)
        Log.d(TAG, "... connected.")
        localSocket.soTimeout = SOCKET_TIMEOUT_MILLISECONDS
        this.outputStream = localSocket.getOutputStream()
        this.inputStream = localSocket.getInputStream()
        this.socket = localSocket
        connected = true
    }

    @Synchronized
    override fun disconnect() {
        if (!connected) {
            return
        }
        try {
            val localSocket = this.socket
            Log.d(TAG, "Closing socket...")
            localSocket?.close()
        } catch (ioe: IOException) {
            Log.d(TAG, "Connection to air unit could not be closed gracefully. " + ioe.message)
        } finally {
            this.socket = null
            this.inputStream = null
            this.outputStream = null
        }
        connected = false
    }

    @Throws(IOException::class)
    override fun sendRobustRequest(operation: ByteArray, register: ByteArray): ByteArray {
        return sendRobustRequest(operation, register, Commands.EMPTY)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun sendRobustRequest(operation: ByteArray, register: ByteArray, value: ByteArray):
            ByteArray {
        connect()
        val request = ByteArray(4 + value.size)
        System.arraycopy(operation, 0, request, 0, 2)
        System.arraycopy(register, 0, request, 2, 2)
        System.arraycopy(value, 0, request, 4, value.size)
        var result: ByteArray
        try {
            result = sendRequestInternal(request)
        } catch (ioe: IOException) {
            Log.d(TAG, ioe.message + " " + ioe.javaClass)
            // retry once if there was connection problem
            disconnect()
            connect()
            result = sendRequestInternal(request)
        }
        disconnect()
        return result
    }

    @Synchronized
    @Throws(IOException::class)
    private fun sendRequestInternal(request: ByteArray): ByteArray {
        val localOutputStream = this.outputStream
            ?: throw IOException(
                String.format(
                    "Output stream is null while sending request: %s",
                    request.contentToString()
                )
            )

        localOutputStream.write(request)
        localOutputStream.flush()

        val result = ByteArray(63)
        val localInputStream = this.inputStream
            ?: throw IOException(
                String.format(
                    "Input stream is null while sending request: %s",
                    request.contentToString()
                )
            )

        val bytesRead = localInputStream.read(result, 0, 63)
        if (bytesRead < 63) {
            throw IOException(
                String.format(
                    "Error reading from stream, read returned %d as number of bytes read into the buffer",
                    bytesRead
                )
            )
        }

        return result
    }
}
