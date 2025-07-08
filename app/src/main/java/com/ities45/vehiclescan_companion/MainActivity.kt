package com.ities45.vehiclescan_companion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.SmsManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ities45.vehiclescan_companion.adapter.DtcAdapter
import com.ities45.vehiclescan_companion.model.DtcItem
import com.ities45.vehiclescan_companion.util.DtcMapper
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SERVER_PORT = 8888
        private const val SMS_PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var statusText: TextView
    private lateinit var hotspotInfoText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var dtcAdapter: DtcAdapter
    private val dtcList = mutableListOf<DtcItem>()

    private var serverSocket: ServerSocket? = null
    private var serverRunning = false
    private var serverJob: Job? = null
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        hotspotInfoText = findViewById(R.id.hotspot_info_text)
        recyclerView = findViewById(R.id.dtcRecyclerView)

        dtcAdapter = DtcAdapter(dtcList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = dtcAdapter

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }

        startLocalHotspot()
    }

    @RequiresPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
    private fun startLocalHotspot() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                2
            )
            return
        }

        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                hotspotReservation = reservation

                val config = reservation.wifiConfiguration
                val ssid = config?.SSID ?: "Unknown SSID"
                val password = config?.preSharedKey ?: "No password"
                hotspotInfoText.text = "SSID: $ssid\nPassword: $password"

                statusText.text = "Hotspot started. Starting server..."
                startServer()
            }

            override fun onStopped() {
                super.onStopped()
                statusText.text = "Hotspot stopped. Server offline."
                stopServer()
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                statusText.text = "Failed to start hotspot"
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun startServer() {
        if (serverRunning) return

        serverRunning = true
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                withContext(Dispatchers.Main) {
                    statusText.text = "Server running"
                }

                while (serverRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        withContext(Dispatchers.Main) {
                            statusText.text = "Client connected"
                        }
                        startReceivingHeaderAndRoute(clientSocket)
                        clientSocket.close()
                    } catch (_: Exception) {}
                }
            } catch (e: BindException) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Server error: ${e.message}"
                    Toast.makeText(this@MainActivity, "Server error: ${e.message}", Toast.LENGTH_LONG).show()
                }
                serverRunning = false
            }
        }
    }

    private fun stopServer() {
        serverRunning = false
        serverJob?.cancel()
        serverSocket?.close()
        serverSocket = null
    }

    private suspend fun startReceivingHeaderAndRoute(clientSocket: Socket) {
        val bis = clientSocket.getInputStream().buffered()
        val headerBytes = mutableListOf<Byte>()
        while (true) {
            val byte = bis.read()
            if (byte == -1 || byte == '\n'.code) break
            headerBytes.add(byte.toByte())
        }
        val header = String(headerBytes.toByteArray()).trim()

        if (header.startsWith("FILE_TRANSFER:")) {
            handleFileTransfer(bis)
        } else {
            sendSms(header)
        }
    }

    private suspend fun handleFileTransfer(inputStream: InputStream) {
        try {
            val content = inputStream.bufferedReader().readText().trim()
            val codes = content.split(",").map { it.trim() }.filter { it.matches(Regex("[A-Z][0-9]{4}")) }
            val extractedList = codes.mapNotNull { DtcMapper.getDtcItem(it) }

            withContext(Dispatchers.Main) {
                dtcList.clear()
                dtcList.addAll(extractedList)
                dtcAdapter.notifyDataSetChanged()
                Toast.makeText(this@MainActivity, "DTCs updated (${dtcList.size})", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                statusText.text = "Error handling DTC file: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun sendSms(message: String) {
        try {
            val parts = message.split(":", limit = 2)
            if (parts.size != 2) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Error: Invalid SMS format"
                    Toast.makeText(this@MainActivity, "Invalid SMS message format", Toast.LENGTH_LONG).show()
                }
                return
            }

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                withContext(Dispatchers.Main) {
                    statusText.text = "SMS permission not granted"
                    Toast.makeText(this@MainActivity, "Please grant SMS permission", Toast.LENGTH_LONG).show()
                }
                return
            }

            val phoneNumber = parts[0]
            val smsMessage = parts[1]
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, smsMessage, null, null)
            withContext(Dispatchers.Main) {
                statusText.text = "SMS sent to $phoneNumber"
                Toast.makeText(this@MainActivity, "SMS sent successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                statusText.text = "Failed to send SMS: ${e.message}"
                Toast.makeText(this@MainActivity, "SMS error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        hotspotReservation?.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_LONG).show()
                statusText.text = "Error: SMS permission denied"
            }
        }
    }
}
