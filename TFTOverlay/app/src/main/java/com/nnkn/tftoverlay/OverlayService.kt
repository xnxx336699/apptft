package com.nnkn.tftoverlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nnkn.tftoverlay.net.BlitzClient
import com.nnkn.tftoverlay.ui.CompAdapter
import kotlinx.coroutines.*

class OverlayService : Service(), CoroutineScope by MainScope() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private var popupView: View? = null
    private var isPopupVisible = false
    private val channelId = "tft_overlay_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(1, buildNotification())
        addBubble()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "TFT Overlay", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TFT Overlay đang chạy")
            .setContentText("Chạm để mở cấu hình")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun addBubble() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = inflater.inflate(R.layout.view_bubble, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200
        windowManager.addView(bubbleView, params)

        val bubble = bubbleView.findViewById<ImageView>(R.id.bubbleIcon)
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f; var click = false

        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { ix = params.x; iy = params.y; tx = e.rawX; ty = e.rawY; click = true; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) click = false
                    params.x = ix + dx; params.y = iy + dy
                    windowManager.updateViewLayout(bubbleView, params); true
                }
                MotionEvent.ACTION_UP -> { if (click) togglePopup(); true }
                else -> false
            }
        }
    }

    private fun togglePopup() {
        if (isPopupVisible) {
            popupView?.let { runCatching { windowManager.removeView(it) } }
            popupView = null
            isPopupVisible = false
        } else showPopup()
    }

    private fun showPopup() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.view_popup, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP; params.y = 120

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerComps)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = CompAdapter(emptyList())

        popupView = view
        windowManager.addView(view, params)
        isPopupVisible = true

        // Load data from Blitz
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val url = prefs.getString("data_url", "https://solomid-resources.s3.amazonaws.com/blitz/tft/data/champions.json")!!
        launch {
            val items = withContext(Dispatchers.IO) { BlitzClient(url).fetchComps() }
            recycler.adapter = CompAdapter(items)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        popupView?.let { runCatching { windowManager.removeView(it) } }
        runCatching { windowManager.removeView(bubbleView) }
        cancel()
    }
}
