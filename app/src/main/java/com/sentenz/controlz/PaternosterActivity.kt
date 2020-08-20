package com.sentenz.controlz

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
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.github.glomadrian.grav.GravView
import com.hanks.htextview.base.HTextView
import jp.co.recruit_lifestyle.android.widget.PlayPauseButton
import kotlinx.android.synthetic.main.activity_multi_sample.*
import java.util.*

class PaternosterActivity : AppCompatActivity() {

    companion object {
        init {
            /* Load JNI */
            System.loadLibrary("native-lib")
        }
        private const val  REQUEST_CODE_STT = 1
    }

    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(this,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeechEngine.language = Locale.getDefault()
                    }
                })
    }

    var opcua_connected : Boolean = false
    lateinit var handler: Handler
    lateinit var textView: HTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paternoster)

        /* Toolbar */
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_paternoster_title)
        /* Set the back arrow in the toolbar */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)

        /* UI composition */
        uiComposition()

        /* Update handler */
        handler = Handler(Looper.getMainLooper())
    }

    private fun uiComposition() {
        /* Circular graph */
        val gravView = findViewById<GravView>(R.id.grav).apply { visibility = View.INVISIBLE }

        /* Text view */
        textView = findViewById<HTextView>(R.id.textview)
        textView.animateText("SENTENZ")

        /* Edit text */
        val editText = findViewById<EditText>(R.id.textedit)
        editText.doAfterTextChanged {
            if (it.toString().trim().isNotEmpty()) {
                textView.animateText(it?.toString())
            } else {
                textView.animateText("SENTENZ")
            }
        }

        /* Play & pause button */
        val playPauseButton = findViewById<PlayPauseButton>(R.id.play_pause_button).apply {
            setColor(resources.getColor(R.color.md_grey_300))
        }

        playPauseButton.setOnControlStatusChangeListener { view, state ->
            if (state) {
                gravView.visibility = View.VISIBLE
                var id : Int = 0
                if (editText.text.toString().trim().isNotEmpty()) {
                    id = editText.text.toString().toInt()
                }
                jniOpcUaTaskUp(id)
                Toast.makeText(this, "Play", Toast.LENGTH_SHORT).show()
            } else {
                gravView.visibility = View.INVISIBLE
                jniOpcUaTaskIdle()
                Toast.makeText(this, "Pause", Toast.LENGTH_SHORT).show()
            }
        }
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
        menuInflater.inflate(R.menu.nfc_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.opcua)?.isVisible = opcua_connected
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /* Handle item selection */
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                Toast.makeText(this, "Back", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.nfc_read -> {
                val intent = Intent(this, NfcActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Menu 1", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.nfc_write -> {
                Toast.makeText(this, "Menu 2", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.opcua -> {
                Toast.makeText(this, "Menu 2", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.stt -> {
                uiSpeechToText()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * Callbacks
     */

    override fun onResume() {
        super.onResume()
        /* OPC UA */
        jniOpcUaConnect()
        handler.post(opcuaUpdateTask)
        /* STT */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        }
    }

    override fun onPause() {
        /* TTS */
        textToSpeechEngine.stop()
        /* JNI */
        handler.removeCallbacks(opcuaUpdateTask)
        /* OPC UA */
        jniOpcUaCleanup()
        super.onPause()
    }

    override fun onDestroy() {
        /* TTS */
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
     * STT
     */

    private fun uiSpeechToText() {
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
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

    private fun uiTextToSpeech(text: String) {
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
                        val speechText = "${getString(R.string.tts_prepend)} $recognizedText ${getString(R.string.tts_append)}"
                        uiTextToSpeech(speechText)
                        textView.animateText(recognizedText)
                        Toast.makeText(this, recognizedText, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * JNI
     */

    private val opcuaUpdateTask = object : Runnable {
        override fun run() {
            val string = jniOpcUaMessage()
            if (string.trim().isNotEmpty()) {
                textView.animateText(string)
            }

            handler.postDelayed(this, 1000)
        }
    }

    fun jniConnectCallback(connected: Boolean) {
        opcua_connected = connected
        invalidateOptionsMenu()
    }

    fun jniMessageCallback(message: String) {
        Log.i("jniMessageCallback", message)
    }

    /* A JNI native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application. */
    external fun jniOpcUaConnect()
    external fun jniOpcUaCleanup()
    external fun jniOpcUaTaskUp(value: Int = 0) : Int
    external fun jniOpcUaTaskDown(value: Int = 0) : Int
    external fun jniOpcUaTaskIdle() : Int
    external fun jniOpcUaMessage() : String
}

