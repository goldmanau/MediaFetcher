package com.example.mediafetcher

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Link XML elements to variables immediately
        val urlInput = findViewById<EditText>(R.id.urlEditText)
        val downloadBtn = findViewById<Button>(R.id.downloadButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val radioMp3 = findViewById<RadioButton>(R.id.radioMp3)

        // 2. Initialize the Engine once in the background when the app starts
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(application)
                com.yausername.ffmpeg.FFmpeg.getInstance().init(application)
                // Optional: Update the engine to avoid the "90 days old" warning
                YoutubeDL.getInstance().updateYoutubeDL(application)

                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Engine Ready!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Init Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 3. Setup the Download Button Logic
        downloadBtn.setOnClickListener {
            val url = urlInput.text.toString()
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = YoutubeDLRequest(url)

                    // Path to the public Downloads folder
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                    // Set saving options
                    request.addOption("-o", "${downloadsDir.absolutePath}/%(title)s.%(ext)s")

                    // Select format based on RadioButton
                    if (radioMp3.isChecked) {
                        request.addOption("-f", "bestaudio")
                        request.addOption("--extract-audio")
                        request.addOption("--audio-format", "mp3")
                    } else {
                        request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
                    }

                    // Identify as a browser to avoid YouTube blocks
                    request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")

                    // Start the download
                    YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                        // Update UI on the Main thread
                        launch(Dispatchers.Main) {
                            progressBar.progress = progress.toInt()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Download Complete!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}