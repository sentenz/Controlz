package com.sentenz.controlz.ui.component.paternoster

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.hanks.htextview.base.HTextView
import com.ohoussein.playpause.PlayPauseView
import com.sentenz.controlz.NfcActivity
import com.sentenz.controlz.R
import com.victor.loading.rotate.RotateLoading
import kotlinx.android.synthetic.main.activity_multi_sample.*
import java.util.*

class PaternosterActivity : AppCompatActivity() {

    companion object {
        init {
            /** Load JNI */
            System.loadLibrary("native-lib")
        }
        private const val  REQUEST_CODE_STT = 1
    }

    /** Declare and initialize variables */
    private var isOpcuaConnection : Boolean = false
    private lateinit var handler: Handler
    private lateinit var rotateLoading: RotateLoading
    private lateinit var textView: HTextView
    private lateinit var editText: EditText
    private lateinit var playPauseView: PlayPauseView
    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(this,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeechEngine.language = Locale.getDefault()
                    }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paternoster)

        setupToolbar()
        setupProgress()
        setupTextView()
        setupTextEdit()
        setupButton()

        /** Initialize handler */
        handler = Handler(Looper.getMainLooper())
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        /* add the values which need to be saved from the drawer to the bundle */
        //outState = slider.saveInstanceState(outState)
        /* add the values which need to be saved from the drawer to the bundle */
        //outState = slider_end.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tool, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_opcua)?.isVisible = isOpcuaConnection
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /** Handle item selection */
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_nfc_read -> {
                val intent = Intent(this, NfcActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_nfc_write -> {
                true
            }
            R.id.menu_opcua -> {
                Toast.makeText(this, R.string.menu_opcua_description, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_stt -> {
                onSpeechToText()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        /** Connect OPC UA client */
        jniOpcuaConnect()
        /** Update JNI callback handler */
        handler.post(opcuaUpdateTask)
        /** Check STT permission */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        }
    }

    override fun onPause() {
        /** TTS */
        textToSpeechEngine.stop()
        /** Remove JNI callback from handler */
        handler.removeCallbacks(opcuaUpdateTask)
        /** Disconnect OPC UA client */
        jniOpcuaDisconnect()
        super.onPause()
    }

    override fun onDestroy() {
        /** TTS */
        textToSpeechEngine.shutdown()
        super.onDestroy()
    }

    override fun onBackPressed() {
        when {
            //root.isDrawerOpen(slider) -> root.closeDrawer(slider)
            //root.isDrawerOpen(slider_end) -> root.closeDrawer(slider_end)
            else -> super.onBackPressed()
        }
    }

    /**
     * STT/TTS callbacks
     */

    private fun onSpeechToText() {
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.s_speech_prompt))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_STT)
        }

        try {
            startActivityForResult(sttIntent, REQUEST_CODE_STT)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "Your device does not support STT.", Toast.LENGTH_LONG).show()
        }
    }

    private fun onTextToSpeech(text: String) {
        if (text.isNotEmpty()) {
            textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
        } else {
            Toast.makeText(this, "Text cannot be empty", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_STT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    result?.let { it ->
                        val recognizedText = it[0].filter { it.isDigit() }
                        val speechText = "${getString(R.string.s_tts_prepend)} $recognizedText ${getString(R.string.s_tts_append)}"
                        onTextToSpeech(speechText)
                        textView.animateText(recognizedText)
                        Toast.makeText(this, recognizedText, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Components
     */

    private fun setupToolbar() {
        /** Set status bar color */
/*
        val window: Window = this@PaternosterActivity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this@PaternosterActivity, R.color.colorPrimaryDark)
*/
        /** Set toolbar */
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.list_title_paternoster)
        /** Set the back arrow in the toolbar */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)
    }

    private fun setupProgress() {
        /** Circular graph view */
        rotateLoading = findViewById<RotateLoading>(R.id.rotateloading)
    }

    private fun setupTextView() {
        /** Text view */
        textView = findViewById<HTextView>(R.id.textview)
        textView.animateText("SENTENZ")
    }

    private fun setupTextEdit() {
        /** Edit text view */
        editText = findViewById<EditText>(R.id.textedit)
        editText.doAfterTextChanged {
            if (it.toString().trim().isNotEmpty()) {
                textView.animateText(it?.toString())
            } else {
                textView.animateText("SENTENZ")
            }
        }
    }

    private fun setupButton() {
        /** Play & pause button view */
        val playPauseView = findViewById<PlayPauseView>(R.id.play_pause_view)
        playPauseView.setOnClickListener {
            playPauseView.toggle()
            if (!playPauseView.isPlay) {
                rotateLoading.start()
                var id : Int = 0
                if (editText.text.toString().trim().isNotEmpty()) {
                    id = editText.text.toString().toInt()
                }
                jniOpcuaTaskUp(id)
            } else {
                rotateLoading.stop()
                jniOpcuaTaskIdle()
            }
        }
    }

    /**
     * JNI callbacks
     */

    private val opcuaUpdateTask = object : Runnable {
        override fun run() {
            val string = jniOpcuaMessage()
            if (string.trim().isNotEmpty()) {
                textView.animateText(string)
            }

            handler.postDelayed(this, 1000)
        }
    }

    fun jniConnectCallback(connected: Boolean) {
        isOpcuaConnection = connected
        invalidateOptionsMenu()
    }

    fun jniMessageCallback(message: String) {
        Log.i("jniMessageCallback", message)
    }

    /**
     * JNI native methods
     * Native methods that are implemented by the 'native-lib'
     * native library, which is packaged with this application.
     */

    external fun jniOpcuaConnect()
    external fun jniOpcuaDisconnect()
    external fun jniOpcuaTaskUp(value: Int = 0) : Int
    external fun jniOpcuaTaskDown(value: Int = 0) : Int
    external fun jniOpcuaTaskIdle() : Int
    external fun jniOpcuaMessage() : String
}

