package com.sentenz.controlz.ui.component.control

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sentenz.controlz.R
import kotlinx.android.synthetic.main.activity_control.*
import kotlinx.android.synthetic.main.activity_multi_sample.toolbar

/**
 * A Activity for I/O control over a DPad.
 */
class ControlActivity : AppCompatActivity() {

    companion object {
        init {
            /** Load JNI */
            System.loadLibrary("native-lib")
        }
    }

    /** Create and initialize variables */
    private var isOpcuaConnection: Boolean = false
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        setupToolbar()
        setupDPad()

        /** Initialize handler */
        handler = Handler(Looper.getMainLooper())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tool, menu)
        /** Disable items from fragment */
        menu.findItem(R.id.menu_stt)?.isVisible = false
        menu.findItem(R.id.menu_nfc_write)?.isVisible = false
        menu.findItem(R.id.menu_nfc_read)?.isVisible = false
/*
        val item: MenuItem = menu.findItem(R.id.menu_opcua)
        if(item.title == "Archive") {
            item.title = "Unarchive"
        }
*/
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        /** Enable/disable items in fragment */
        menu?.findItem(R.id.menu_opcua)?.isVisible = isOpcuaConnection
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /** Handle item selection */
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                Toast.makeText(this, R.string.menu_home, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_opcua -> {
                Toast.makeText(this, R.string.menu_opcua_description, Toast.LENGTH_SHORT).show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        //add the values which need to be saved from the drawer to the bundle
//        outState = slider.saveInstanceState(outState)
        //add the values which need to be saved from the drawer to the bundle
//        outState = slider_end.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        /** Connect OPC UA client */
        jniOpcuaConnect()
        /** Update JNI callback handler */
        handler.post(opcuaUpdateTask)
    }

    override fun onPause() {
        super.onPause()
        /** Remove JNI callback from handler */
        handler.removeCallbacks(opcuaUpdateTask)
        /** Disconnect OPC UA client */
        jniOpcuaDisconnect()
    }

    override fun onDestroy() {
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
     * Components
     */

    private fun setupToolbar() {
        /** Set status bar color */
/*
        val window: Window = this@ControlActivity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this@ControlActivity, R.color.colorPrimaryDark)
*/
        /** Set toolbar */
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.list_title_control)
        /** Set the back arrow in the toolbar */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)
    }

    private fun setupDPad() {
        /** DPadView events */
/*
        dpad.onDirectionPressListener = { direction, action ->
            val text = StringBuilder()
            val directionText = direction?.name ?: ""
            if (directionText.isNotEmpty()) {
                text.append("Direction:\t")
            }
            text.append(directionText)
            if (directionText.isNotEmpty()) {
                text.append("\nAction:\t")
                val actionText = when (action) {
                    MotionEvent.ACTION_UP -> Toast.makeText(this, jniOpcuaTaskUp(), Toast.LENGTH_SHORT).show()
                    MotionEvent.ACTION_DOWN -> Toast.makeText(this, jniOpcuaTaskDown(), Toast.LENGTH_SHORT).show()
                    MotionEvent.ACTION_MOVE -> "Move"
                    else -> action.toString()
                }
                text.append(actionText)
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
            }
        }
*/

        dpad.onDirectionPressListener = { direction, action ->
            if (direction?.name.equals("UP")) {
                when (action) {
                    MotionEvent.ACTION_UP -> jniOpcuaTaskIdle()
                    MotionEvent.ACTION_DOWN -> jniOpcuaTaskUp()
                }
            }
            if (direction?.name.equals("DOWN")) {
                when (action) {
                    MotionEvent.ACTION_UP -> jniOpcuaTaskIdle()
                    MotionEvent.ACTION_DOWN -> jniOpcuaTaskDown()
                }
            }
        }

        dpad.onDirectionClickListener = {
            it?.let {
                Log.i("directionPress", it.name)
                Toast.makeText(this, it.name, Toast.LENGTH_SHORT).show()
            }
        }

        dpad.setOnClickListener {
            Log.i("Click", "Done")
        }

        dpad.onCenterLongClick = {
            Log.i("center", "long click")
        }
    }

    /**
     * JNI callbacks
     */

    private val opcuaUpdateTask = object : Runnable {
        override fun run() {
            dpad.centerText = jniOpcuaMessage()
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
    external fun jniOpcuaTaskUp() : Int
    external fun jniOpcuaTaskDown() : Int
    external fun jniOpcuaTaskIdle() : Int
    external fun jniOpcuaMessage() : String
}
