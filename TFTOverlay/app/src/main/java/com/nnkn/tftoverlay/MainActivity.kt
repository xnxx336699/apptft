package com.nnkn.tftoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edtUrl = findViewById<EditText>(R.id.edtUrl)
        edtUrl.setText("https://solomid-resources.s3.amazonaws.com/blitz/tft/data/champions.json")

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "Bật quyền 'Hiển thị trên ứng dụng khác'", Toast.LENGTH_LONG).show()
            } else {
                val i = Intent(this, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
                finish()
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val url = edtUrl.text.toString().trim()
            getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("data_url", url).apply()
            Toast.makeText(this, "Đã lưu URL", Toast.LENGTH_SHORT).show()
        }
    }
}
