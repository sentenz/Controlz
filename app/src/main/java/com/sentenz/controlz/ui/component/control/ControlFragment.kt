package com.sentenz.controlz.ui.component.control

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import com.sentenz.controlz.R
import com.sentenz.controlz.databinding.FragmentControlBinding
import com.sentenz.controlz.ui.base.BaseFragment
import kotlinx.android.synthetic.main.activity_control.*


/**
 * A Fragment for I/O control over a DPad.
 */
class ControlFragment : BaseFragment<FragmentControlBinding, ControlViewModel>() {

    companion object {
        fun newInstance() = ControlFragment()
        init {
            /** Load JNI */
            System.loadLibrary("native-lib")
        }
    }

    /** Initialize data binding */
    override val layoutId = R.layout.fragment_control
    override val viewModelClass = ControlViewModel::class

    /** Create and initialize variables */
    private var isOpcuaConnection: Boolean = false
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        /** Enable options menu in this fragment */
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDPad()

        /** Initialize handler */
        handler = Handler(Looper.getMainLooper())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_tool, menu)
        /** Disable items from this fragment */
        menu.findItem(R.id.menu_stt)?.isVisible = false
        menu.findItem(R.id.menu_nfc_write)?.isVisible = false
        menu.findItem(R.id.menu_nfc_read)?.isVisible = false
/*
        val item: MenuItem = menu.findItem(R.id.menu_opcua)
        if(item.title == "Archive") {
            item.title = "Unarchive"
        }
*/
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        /** Enable/disable items in fragment */
        menu.findItem(R.id.menu_opcua)?.isVisible = isOpcuaConnection
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /** Handle item selection */
        return when (item.itemId) {
            android.R.id.home -> {
                Toast.makeText(activity, R.string.menu_home, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_opcua -> {
                Toast.makeText(activity, R.string.menu_opcua_description, Toast.LENGTH_SHORT).show()
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

    /**
     * Components
     */

    private fun setupToolbar() {
        toolbar.setNavigationIcon(R.drawable.icon_back_white)
        toolbar.setTitle(R.string.list_title_control)
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
                    MotionEvent.ACTION_UP -> Toast.makeText(activity, jniOpcuaTaskUp(), Toast.LENGTH_SHORT).show()
                    MotionEvent.ACTION_DOWN -> Toast.makeText(activity, jniOpcuaTaskDown(), Toast.LENGTH_SHORT).show()
                    MotionEvent.ACTION_MOVE -> "Move"
                    else -> action.toString()
                }
                text.append(actionText)
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()
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
//        invalidateOptionsMenu()
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