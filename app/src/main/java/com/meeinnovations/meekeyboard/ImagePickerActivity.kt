package com.meeinnovations.meekeyboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class ImagePickerActivity : Activity() {

    private val REQ_PICK = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No UI — silently launch gallery
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, REQ_PICK)
        } catch (e: Throwable) {
            // Fallback to GET_CONTENT
            val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            try {
                startActivityForResult(Intent.createChooser(fallback, "Pick background"), REQ_PICK)
            } catch (e2: Throwable) {
                Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK) { finish(); return }
        if (resultCode != RESULT_OK) { finish(); return }

        val uri: Uri? = data?.data
        if (uri == null) { finish(); return }

        try {
            // Copy to internal storage so we always have access
            val dest = File(filesDir, "custom_bg.png")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            // Save path in the same SharedPreferences the keyboard uses
            val prefs = getSharedPreferences("meekeyboard", MODE_PRIVATE)
            prefs.edit()
                .putString("custom_bg_path", dest.absolutePath)
                .putString("background", "custom")
                .apply()

            Toast.makeText(this, "Background set", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(this, "Failed to set background", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
