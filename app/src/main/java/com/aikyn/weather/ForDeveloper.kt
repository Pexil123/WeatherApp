package com.aikyn.weather

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("StaticFieldLeak")

class ForDeveloper : AppCompatActivity() {

    private var info: TextView? = null
    private var backbtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.for_developer)

        info = findViewById(R.id.info)
        backbtn = findViewById(R.id.back)

        val inf = intent.getStringExtra("info")
        info?.text = inf

        backbtn?.setOnClickListener {
            finish()
        }

    }
}