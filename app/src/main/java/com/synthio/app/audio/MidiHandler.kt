package com.synthio.app.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles MIDI device connections and message routing.
 * 
 * Supports:
 * - USB MIDI keyboards
 * - Bluetooth MIDI devices
 * - Virtual MIDI devices
 * 
 * All incoming MIDI notes are routed to the synthesizer engine,
 * supporting the full 128-note MIDI range regardless of the UI keyboard display.
 */
class MidiHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "MidiHandler"
        
        // MIDI message types (status byte high nibble)
        const val NOTE_OFF = 0x80
        const val NOTE_ON = 0x90
        const val POLY_PRESSURE = 0xA0
        const val CONTROL_CHANGE = 0xB0
        const val PROGRAM_CHANGE = 0xC0
        const val CHANNEL_PRESSURE = 0xD0
        const val PITCH_BEND = 0xE0
        
        // Common MIDI CC numbers
        const val CC_MODULATION = 1
        const val CC_VOLUME = 7
        const val CC_SUSTAIN_PEDAL = 64
        const val CC_FILTER_CUTOFF = 74
        
        // Sustain pedal threshold (values >= 64 are "on")
        const val SUSTAIN_THRESHOLD = 64
    }
    
    private var midiManager: MidiManager? = null
    private val openDevices = mutableMapOf<MidiDeviceInfo, MidiDevice>()
    private val openPorts = mutableListOf<MidiOutputPort>()
    
    // Callback for MIDI note events
    var onNoteOn: ((Int, Int) -> Unit)? = null  // (midiNote, velocity)
    var onNoteOff: ((Int) -> Unit)? = null      // (midiNote)
    var onControlChange: ((Int, Int) -> Unit)? = null  // (controller, value)
    var onSustainPedal: ((Boolean) -> Unit)? = null  // (isPressed)
    
    // State flows for UI
    private val _connectedDevices = MutableStateFlow<List<MidiDeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<MidiDeviceInfo>> = _connectedDevices.asStateFlow()
    
    private val _isMidiAvailable = MutableStateFlow(false)
    val isMidiAvailable: StateFlow<Boolean> = _isMidiAvailable.asStateFlow()
    
    private val _isDeviceConnected = MutableStateFlow(false)
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()
    
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * MIDI receiver that processes incoming MIDI messages
     */
    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            // Process all MIDI messages in the buffer
            var i = offset
            while (i < offset + count) {
                val statusByte = data[i].toInt() and 0xFF
                val messageType = statusByte and 0xF0
                
                when (messageType) {
                    NOTE_ON -> {
                        if (i + 2 < offset + count) {
                            val note = data[i + 1].toInt() and 0x7F
                            val velocity = data[i + 2].toInt() and 0x7F
                            
                            // Note On with velocity 0 is equivalent to Note Off
                            if (velocity > 0) {
                                Log.d(TAG, "MIDI Note On: $note, velocity: $velocity")
                                onNoteOn?.invoke(note, velocity)
                            } else {
                                Log.d(TAG, "MIDI Note Off (vel=0): $note")
                                onNoteOff?.invoke(note)
                            }
                            i += 3
                        } else {
                            i++
                        }
                    }
                    NOTE_OFF -> {
                        if (i + 2 < offset + count) {
                            val note = data[i + 1].toInt() and 0x7F
                            Log.d(TAG, "MIDI Note Off: $note")
                            onNoteOff?.invoke(note)
                            i += 3
                        } else {
                            i++
                        }
                    }
                    CONTROL_CHANGE -> {
                        if (i + 2 < offset + count) {
                            val controller = data[i + 1].toInt() and 0x7F
                            val value = data[i + 2].toInt() and 0x7F
                            Log.d(TAG, "MIDI CC: $controller = $value")
                            
                            // Handle sustain pedal specially
                            if (controller == CC_SUSTAIN_PEDAL) {
                                val isPressed = value >= SUSTAIN_THRESHOLD
                                Log.d(TAG, "Sustain pedal: ${if (isPressed) "DOWN" else "UP"}")
                                onSustainPedal?.invoke(isPressed)
                            }
                            
                            // Also pass to generic CC handler
                            onControlChange?.invoke(controller, value)
                            i += 3
                        } else {
                            i++
                        }
                    }
                    POLY_PRESSURE, PITCH_BEND -> {
                        // 3-byte messages we're not handling yet
                        i += 3
                    }
                    PROGRAM_CHANGE, CHANNEL_PRESSURE -> {
                        // 2-byte messages we're not handling yet
                        i += 2
                    }
                    else -> {
                        // System messages or unknown - skip
                        i++
                    }
                }
            }
        }
    }
    
    /**
     * Initialize MIDI support
     */
    fun initialize(): Boolean {
        // Check if MIDI is supported
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            Log.w(TAG, "MIDI not supported on this device")
            _isMidiAvailable.value = false
            return false
        }
        
        midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
        if (midiManager == null) {
            Log.e(TAG, "Could not get MidiManager")
            _isMidiAvailable.value = false
            return false
        }
        
        _isMidiAvailable.value = true
        Log.i(TAG, "MIDI support initialized")
        
        // Register device callback to detect connections/disconnections
        midiManager?.registerDeviceCallback(deviceCallback, handler)
        
        // Scan for already-connected devices
        scanForDevices()
        
        return true
    }
    
    /**
     * Scan for available MIDI devices and connect to any input devices
     */
    fun scanForDevices() {
        val devices = midiManager?.devices ?: return
        
        val deviceList = devices.toList()
        _connectedDevices.value = deviceList
        
        Log.d(TAG, "Found ${deviceList.size} MIDI devices")
        
        // Auto-connect to devices with output ports (i.e., keyboards that send MIDI)
        for (info in deviceList) {
            if (info.outputPortCount > 0) {
                connectToDevice(info)
            }
        }
    }
    
    /**
     * Connect to a MIDI device
     */
    fun connectToDevice(deviceInfo: MidiDeviceInfo) {
        if (openDevices.containsKey(deviceInfo)) {
            Log.d(TAG, "Already connected to device: ${getDeviceName(deviceInfo)}")
            return
        }
        
        Log.d(TAG, "Connecting to MIDI device: ${getDeviceName(deviceInfo)}")
        
        midiManager?.openDevice(deviceInfo, { device ->
            if (device != null) {
                openDevices[deviceInfo] = device
                
                // Open all output ports (MIDI out from device = MIDI input to us)
                for (i in 0 until deviceInfo.outputPortCount) {
                    val port = device.openOutputPort(i)
                    if (port != null) {
                        port.connect(midiReceiver)
                        openPorts.add(port)
                        Log.i(TAG, "Connected to output port $i of ${getDeviceName(deviceInfo)}")
                    }
                }
                
                _isDeviceConnected.value = openDevices.isNotEmpty()
            } else {
                Log.e(TAG, "Failed to open MIDI device: ${getDeviceName(deviceInfo)}")
            }
        }, handler)
    }
    
    /**
     * Disconnect from a specific MIDI device
     */
    fun disconnectFromDevice(deviceInfo: MidiDeviceInfo) {
        val device = openDevices.remove(deviceInfo) ?: return
        
        try {
            device.close()
            Log.i(TAG, "Disconnected from: ${getDeviceName(deviceInfo)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing device: ${e.message}")
        }
        
        _isDeviceConnected.value = openDevices.isNotEmpty()
    }
    
    /**
     * Get human-readable device name
     */
    fun getDeviceName(info: MidiDeviceInfo): String {
        val properties = info.properties
        return properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
            ?: "Unknown MIDI Device"
    }
    
    /**
     * Device callback for monitoring connections/disconnections
     */
    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Log.i(TAG, "MIDI device connected: ${getDeviceName(device)}")
            
            _connectedDevices.value = _connectedDevices.value + device
            
            // Auto-connect if it has output ports
            if (device.outputPortCount > 0) {
                connectToDevice(device)
            }
        }
        
        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Log.i(TAG, "MIDI device disconnected: ${getDeviceName(device)}")
            
            _connectedDevices.value = _connectedDevices.value - device
            disconnectFromDevice(device)
        }
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        Log.d(TAG, "Releasing MIDI resources")
        
        // Close all open ports
        for (port in openPorts) {
            try {
                port.disconnect(midiReceiver)
                port.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing port: ${e.message}")
            }
        }
        openPorts.clear()
        
        // Close all open devices
        for ((_, device) in openDevices) {
            try {
                device.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing device: ${e.message}")
            }
        }
        openDevices.clear()
        
        // Unregister device callback
        try {
            midiManager?.unregisterDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering callback: ${e.message}")
        }
        
        _isDeviceConnected.value = false
    }
}
