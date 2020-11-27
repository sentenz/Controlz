package com.sentenz.controlz.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.sentenz.controlz.NfcActivity
import com.sentenz.controlz.R
import com.sentenz.controlz.base.BaseActivity
import com.sentenz.controlz.base.BaseViewModel
import com.sentenz.controlz.databinding.ActivityControlBinding
import com.sentenz.controlz.databinding.ActivityDrawerBinding
import com.sentenz.controlz.vm.ControlViewModel
import com.sentenz.controlz.vm.DrawerViewModel
import kotlinx.android.synthetic.main.activity_control.*
import kotlinx.android.synthetic.main.activity_multi_sample.toolbar

/**
 * A V for [res.layout.activity_control]
 * A binding to VM [com.sentenz.controlz.vm.ControlViewModel]
 *
 * MVVM usage from: https://gist.github.com/BapNesS/3125b3f2aa6317a7486ee9c11fdc4017
 */
class ControlActivity : AppCompatActivity() {

    companion object {
        init {
            /* Load JNI */
            System.loadLibrary("native-lib")
        }
    }

    var opcua_connected : Boolean = false
    lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        /* Toolbar */
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.s_title_control)
        /* Set the back arrow in the toolbar */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(false)

        /* UI composition */
        uiComposition()

        /* Update handler */
        handler = Handler(Looper.getMainLooper())
    }

    private fun uiComposition() {

        /* DPadView events */
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

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        //add the values which need to be saved from the drawer to the bundle
//        outState = slider.saveInstanceState(outState)
        //add the values which need to be saved from the drawer to the bundle
//        outState = slider_end.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.nfc_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.opcua)?.isVisible = opcua_connected
        menu?.findItem(R.id.stt)?.isVisible = false
        menu?.findItem(R.id.nfc_write)?.isVisible = false
        menu?.findItem(R.id.nfc_read)?.isVisible = false
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
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * External callbacks
     */

    override fun onResume() {
        super.onResume()
        /* OPC UA */
        jniOpcuaConnect()
        /* Update handler */
        handler.post(opcuaUpdateTask)
    }

    override fun onPause() {
        super.onPause()
        /* Update handler */
        handler.removeCallbacks(opcuaUpdateTask)
        /* OPC UA */
        jniOpcuaCleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        when {
//            root.isDrawerOpen(slider) -> root.closeDrawer(slider)
//            root.isDrawerOpen(slider_end) -> root.closeDrawer(slider_end)
            else -> super.onBackPressed()
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
        opcua_connected = connected
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
    external fun jniOpcuaCleanup()
    external fun jniOpcuaTaskUp() : Int
    external fun jniOpcuaTaskDown() : Int
    external fun jniOpcuaTaskIdle() : Int
    external fun jniOpcuaMessage() : String
}
