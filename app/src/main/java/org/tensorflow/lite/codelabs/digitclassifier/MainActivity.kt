/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.codelabs.digitclassifier

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.divyanshu.draw.widget.DrawView
import java.io.*

class MainActivity : AppCompatActivity() {

  private var drawView: DrawView? = null
  private var clearButton: Button? = null
  private var predictedTextView: TextView? = null
  private var digitClassifier = DigitClassifier(this)

  // 추가 변수
  private var testTextView: TextView? = null
  private var deleteButton: Button? = null
  private var showButton: Button? = null
  private var compareButton: Button? = null
  private var generateButton: Button? = null

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Setup view instances.
    drawView = findViewById(R.id.draw_view)
    drawView?.setStrokeWidth(70.0f)
    drawView?.setColor(Color.WHITE)
    drawView?.setBackgroundColor(Color.BLACK)
    clearButton = findViewById(R.id.clear_button)
    predictedTextView = findViewById(R.id.predicted_text)
    // 추가 test string
    testTextView = findViewById(R.id.test_text)
    // test.txt 관련 버튼
    deleteButton = findViewById(R.id.text_delete_button)
    showButton = findViewById(R.id.text_show_button)
    //
    compareButton = findViewById(R.id.compare_button)
    generateButton = findViewById(R.id.data_generate_button)


    var tmp = ""
    var posList = ArrayList<String>()

    // delete 버튼 누르면 test.txt 파일 삭제
    deleteButton?.setOnClickListener {
      deleteTextFile(filesDir.absolutePath)
    }

    // show 버튼 누르면 새창 나와서 저장된 값들 볼수 있게
    // SubActivity 실행
    showButton?.setOnClickListener {
      val intent = Intent(this, SubActivity::class.java)
      startActivity(intent)
    }

    // Setup clear drawing button.
    clearButton?.setOnClickListener {
      drawView?.clearCanvas()
      predictedTextView?.text = getString(R.string.prediction_text_placeholder)
      testTextView?.text = getString(R.string.test_message)
      tmp = ""

      // 지금까지의 좌표값 test.txt에 저장
//      writeTextFile(filesDir.absolutePath, "clear")
//      posList.forEach{ writeTextFile(filesDir.absolutePath, it)}

      // posList 초기화
      posList = ArrayList<String>()
    }

    // 문자 판별 버튼
    compareButton?.setOnClickListener {
      compareText()
    }

    generateButton?.setOnClickListener {
      val intent = Intent(this, GenerateActivity::class.java)
      startActivity(intent)
    }


    // Setup classification trigger so that it classify after every stroke drew.
    drawView?.setOnTouchListener { _, event ->
      // As we have interrupted DrawView's touch event,
      // we first need to pass touch events through to the instance for the drawing to show up.

      drawView?.onTouchEvent(event)
      // Toast.makeText(this, "touch: ${event.x}, ${event.y}", Toast.LENGTH_SHORT).show()
      tmp += "(${event.x}:${event.y}) "

      // Then if user finished a touch event, run classification
      if (event.action == MotionEvent.ACTION_UP) {
        // Toast.makeText(this, tmp, Toast.LENGTH_SHORT).show()
        // testTextView?.text = tmp
        Log.d("윤도현 좌표값",tmp)

        // 그림 크기 "width: ${drawView?.width}"
        // 터치가 떼어질때 좌표값들 저장 (stroke 한개당)
        testTextView?.text = posList.size.toString()
        posList.add(tmp)
        classifyDrawing()
        tmp = ""
      }

      true
    }
    // Setup digit classifier.
    digitClassifier
      .initialize()
      .addOnFailureListener { e -> Log.e(TAG, "Error to setting up digit classifier.", e) }
  }

  override fun onDestroy() {
    // Sync DigitClassifier instance lifecycle with MainActivity lifecycle,
    // and free up resources (e.g. TF Lite instance) once the activity is destroyed.
    digitClassifier.close()
    super.onDestroy()
  }

  private fun classifyDrawing() {
    val bitmap = drawView?.getBitmap()

    if ((bitmap != null) && (digitClassifier.isInitialized)) {
      digitClassifier
        .classifyAsync(bitmap)
        .addOnSuccessListener { resultText -> predictedTextView?.text = resultText }
        .addOnFailureListener { e ->
          predictedTextView?.text = getString(
            R.string.classification_error_message,
            e.localizedMessage
          )
          Log.e(TAG, "Error classifying drawing.", e)
        }
    }
  }

  // 문자 비교
  fun compareText(){
    // edit 문자 확인
    var editText: EditText = findViewById(R.id.edit_text)
    var editTextString = editText?.text.toString()

    var editTextChar = ' '
    if (editTextString != ""){
      editTextChar = editTextString[0]
    }
    Log.d("윤도현", editTextChar.toString())

    // predict 문자 확인
    var predictText = predictedTextView?.text.toString()
    Log.d("윤도현", predictText)

    var compare = false
    if (predictText != "2. 캔버스에 한글을 써주세요"){
      var label = predictText[19]
      compare = (editTextChar == label)

      Log.d("윤도현", label.toString())
    }

    // 화면에 true or false 표시
    if (compare){
      testTextView?.text = "맞는 글자를 씀"
    } else {
      if (editTextChar == ' '){
        testTextView?.text = "채점할 글자를 입력하지 않음"
      } else {
        testTextView?.text = "틀린 글자를 씀"
      }
    }

  }

  // 파일 삭제
  fun deleteTextFile(directory: String) {
    Log.d("윤도현", "deleteTextFile")
    val file = File("$directory/data.txt")

    if (file.exists()){
      Log.d("윤도현", "isFile")
      file.delete()
    }
  }


  companion object {
    private const val TAG = "MainActivity"
  }
}
