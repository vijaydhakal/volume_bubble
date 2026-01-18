package com.doriit.volumebubble
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class VolumeOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var trashView: View

    private lateinit var params: WindowManager.LayoutParams
    private lateinit var trashParams: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager

    override fun onBind(intent: Intent?): IBinder? = null



    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(1, createNotification())

        setupTrashView()

        setupFloatingView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Volume Bubble Service"
            val descriptionText = "Keeps the volume bubble active in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("vol_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        setupTouchListener()
        windowManager.addView(floatingView, params)
    }

    private fun setupTrashView() {
        trashView = LayoutInflater.from(this).inflate(R.layout.trash_layout, null)
        trashParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        windowManager.addView(trashView, trashParams)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val bubble = floatingView.findViewById<ImageView>(R.id.bubble_visual)
        val trashIcon = trashView.findViewById<ImageView>(R.id.trash_icon)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val touchSlop = 10

        trashView.visibility = View.GONE

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    bubble.alpha = 1.0f

                    trashView.visibility = View.VISIBLE
                    trashIcon.alpha = 0f
                    trashIcon.animate().alpha(0.6f).setDuration(200).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)

                    if (isOverTrash()) {
                        trashIcon.animate().scaleX(1.4f).scaleY(1.4f).alpha(1.0f).setDuration(100).start()
                        trashIcon.setColorFilter(android.graphics.Color.RED)
                    } else {
                        trashIcon.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.6f).setDuration(100).start()
                        trashIcon.clearColorFilter()
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    bubble.alpha = 0.4f

                    trashIcon.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(200).withEndAction {
                        trashView.visibility = View.GONE
                    }.start()

                    if (isOverTrash()) {
                        stopSelf()
                    } else {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)

                        if (diffX < touchSlop && diffY < touchSlop) {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_SAME,
                                AudioManager.FLAG_SHOW_UI
                            )
                        } else {
                            snapToEdge()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val middle = screenWidth / 2
        val viewWidth = floatingView.width

        val targetX = if (params.x + (viewWidth / 2) < middle) {
            0
        } else {
            screenWidth - viewWidth
        }

        params.x = targetX
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun isOverTrash(): Boolean {
        val bubbleLocation = IntArray(2)
        floatingView.getLocationOnScreen(bubbleLocation)
        val trashLocation = IntArray(2)
        trashView.getLocationOnScreen(trashLocation)

        val distance = sqrt(
            Math.pow((bubbleLocation[0] - trashLocation[0]).toDouble(), 2.0) +
                    Math.pow((bubbleLocation[1] - trashLocation[1]).toDouble(), 2.0)
        )
        return distance < 300
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        if (::trashView.isInitialized) windowManager.removeView(trashView)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "vol_channel")
            .setContentTitle("Volume Bubble")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}