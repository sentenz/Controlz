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
class ControlActivity : BaseActivity() {

    companion object {
        init {
            /* Load JNI */
            System.loadLibrary("native-lib")
        }
    }

    var opcua_connected : Boolean = false
    lateinit var handler: Handler

    /* MVVM */
    private lateinit var viewModel: ControlViewModel
    override val baseViewModel: BaseViewModel?
        get() = viewModel
    private lateinit var binding: ActivityControlBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* MVVM */
        binding = DataBindingUtil.setContentView(this@ControlActivity, R.layout.activity_control)
        initViewModelAndBinding {
            // Do other stuff if needed
        }

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

    /**
     * MVVM - Do multiple things:
     *  - Initialize the current [viewModel] ViewModel
     *  - Do the binding
     *  - Initialize the [viewModel] observers
     */
    private fun initViewModelAndBinding( after: () -> Unit ) {
        viewModel = provideViewModel()
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.executePendingBindings()
        initObservers()
        after()
    }

    /**
     * MVVM - Should be done after [baseViewModel] instantiation
     */
    override fun initObservers() {
        // Important : don't forget to call the super method
        super.initObservers()
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
                    MotionEvent.ACTION_UP -> Toast.makeText(this, jniOpcUaTaskUp(), Toast.LENGTH_SHORT).show()
                    MotionEvent.ACTION_DOWN -> Toast.makeText(this, jniOpcUaTaskDown(), Toast.LENGTH_SHORT).show()
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
                    MotionEvent.ACTION_UP -> jniOpcUaTaskIdle()
                    MotionEvent.ACTION_DOWN -> jniOpcUaTaskUp()
                }
            }
            if (direction?.name.equals("DOWN")) {
                when (action) {
                    MotionEvent.ACTION_UP -> jniOpcUaTaskIdle()
                    MotionEvent.ACTION_DOWN -> jniOpcUaTaskDown()
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
     * Callbacks
     */

    override fun onResume() {
        super.onResume()
        /* OPC UA */
        jniOpcUaConnect()
        /* Update handler */
        handler.post(opcuaUpdateTask)
    }

    override fun onPause() {
        super.onPause()
        /* Update handler */
        handler.removeCallbacks(opcuaUpdateTask)
        /* OPC UA */
        jniOpcUaCleanup()
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
     * JNI
     */

    private val opcuaUpdateTask = object : Runnable {
        override fun run() {
            dpad.centerText = jniOpcUaMessage()
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
     *  A JNI native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun jniOpcUaConnect()
    external fun jniOpcUaCleanup()
    external fun jniOpcUaTaskUp() : Int
    external fun jniOpcUaTaskDown() : Int
    external fun jniOpcUaTaskIdle() : Int
    external fun jniOpcUaMessage() : String
}
