package com.fghilmany.acedemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap
    private lateinit var tfLiteHelper: TfLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tfLiteHelper = TfLiteHelper(this)
        tfLiteHelper.init()

        image.setOnClickListener {
            val SELECT_TYPE = "image/*"
            val SELECT_PICTURE = "Select Picture"
            val intent = Intent()
            intent.type = SELECT_TYPE
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, SELECT_PICTURE), 12)
        }

        classify.setOnClickListener {
            tfLiteHelper.classifyImage(bitmap)
            setLabel(tfLiteHelper.showResult())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12 && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                image.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun setLabel(entries: List<String>?) {
        classifytext.text = ""
        if (entries != null) {
            for (entry in entries) {
                classifytext.append(entry)
            }
        }
    }
}
