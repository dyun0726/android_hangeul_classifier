package org.tensorflow.lite.codelabs.digitclassifier

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.*
import java.lang.Exception

class SubActivity : AppCompatActivity() {

    private var returnButton: Button? = null
    private var subTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        returnButton = findViewById(R.id.return_button)
        subTextView = findViewById(R.id.sub_text)

        val data = readTextFile(filesDir.absolutePath)

        subTextView?.text = data


        returnButton?.setOnClickListener {
            finish()
        }
    }

    fun readTextFile(directory: String):String{
        Log.d("윤도현", directory)
        val file = File("$directory/data.txt")

        if(!file.exists())
            return "no data"

        return try{
            val read = FileReader(file)
            val dataText = read.readText()
            Log.d("윤도현",dataText)
            dataText
        } catch (e: Exception){
            println(e.message)
            "Exception!"
        }

    }

}