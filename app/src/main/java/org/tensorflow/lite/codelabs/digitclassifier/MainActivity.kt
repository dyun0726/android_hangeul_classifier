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
import androidx.appcompat.app.AppCompatActivity
import com.divyanshu.draw.widget.DrawView
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

  private var drawView: DrawView? = null
  private var clearButton: Button? = null
  private var predictedTextView: TextView? = null
  private var digitClassifier = DigitClassifier(this)
  private var Utilities = Utilities()

  // 추가 변수
  private var testTextView: TextView? = null
  private var deleteButton: Button? = null
  private var checkButton: Button? = null
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
    checkButton = findViewById(R.id.stroke_check_button)
    compareButton = findViewById(R.id.compare_button)
    generateButton = findViewById(R.id.data_generate_button)

    var posX = ArrayList<Float>()
    var posY = ArrayList<Float>()
    var allStrokePosXY = ArrayList<ArrayList<ArrayList<Float>>>()
    var strokeNum = 0

    // delete 버튼 누르면 test.txt 파일 삭제
//    deleteButton?.setOnClickListener {
//      deleteTextFile(filesDir.absolutePath)
//    }

    // show 버튼 누르면 새창 나와서 저장된 값들 볼수 있게
    // SubActivity 실행
    checkButton?.setOnClickListener {
      val intent = Intent(this, SubActivity::class.java)
      startActivity(intent)
    }

    // Setup clear drawing button.
    clearButton?.setOnClickListener {
      drawView?.clearCanvas()
      predictedTextView?.text = getString(R.string.prediction_text_placeholder)
      testTextView?.text = getString(R.string.test_message)

      // posList 초기화
      allStrokePosXY = ArrayList<ArrayList<ArrayList<Float>>>()
      strokeNum = 0
    }

    // 문자 판별 버튼
    compareButton?.setOnClickListener {
      Log.d("윤도현","글자 인식 진행")
      val compare = compareText()
      if (compare) {
        Log.d("윤도현","글자 인식 통과, 획순 체크 진행")
        val editText: EditText = findViewById(R.id.edit_text)
        testTextView?.text = "글자를 올바르게 썼습니다.\n"+checkStroke(allStrokePosXY, editText.text[0])
      } else {
        Log.d("윤도현","글자 인식 실패")
        testTextView?.text = "글자를 올바르게 써주세요"
      }
    }

    // 생성 버튼
    generateButton?.setOnClickListener {
      val intent = Intent(this, GenerateActivity::class.java)
      startActivity(intent)
    }


    // Setup classification trigger so that it classify after every stroke drew.
    drawView?.setOnTouchListener { _, event ->
      // As we have interrupted DrawView's touch event,
      // we first need to pass touch events through to the instance for the drawing to show up.
      drawView?.onTouchEvent(event)
      posX.add(event.x)
      posY.add(event.y)

      // Then if user finished a touch event, run classification
      if (event.action == MotionEvent.ACTION_UP) {
        classifyDrawing()
        allStrokePosXY.add(Utilities.convertPos(posX, posY))
        strokeNum ++
        posX = ArrayList<Float>()
        posY = ArrayList<Float>()

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
  fun compareText(): Boolean{
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

    return compare
  }

  // 획순 체크
  fun checkStroke(allStrokePosXY: ArrayList<ArrayList<ArrayList<Float>>>, ch:Char): String{
    var result = ""

    val strokeMap = mapOf("ㄱ" to 1,"ㄴ" to 1,"ㄷ" to 2,"ㄹ" to 3,"ㅁ" to 3,"ㅂ" to 4,"ㅅ" to 2,"ㅇ" to 1,"ㅈ" to 2,"ㅊ" to 3,"ㅋ" to 2,"ㅌ" to 3,"ㅍ" to 4,"ㅎ" to 3,"ㅏ" to 2,"ㅓ" to 2,"ㅗ" to 2,"ㅜ" to 2,"ㅡ" to 1,"ㅣ" to 1,"ㅑ" to 3,"ㅕ" to 3,"ㅛ" to 3,"ㅠ" to 3,"ㅐ" to 3,"ㅒ" to 4,"ㅔ" to 3,"ㅖ" to 4, " " to 0)
//    val strokeModel = mapOf("ㄱ" to "a", "ㄴ" to "b", "ㄷ" to "cb", "ㄹ" to "acb", "ㅁ" to "kac", "ㅂ" to "kkcc", "ㅅ" to "ef", "ㅇ" to "g", "ㅈ" to "hf", "ㅊ" to "ihf", "ㅋ" to "ac", "ㅌ" to "ccb", "ㅍ" to "ckkc", "ㅎ" to "icg",
//      "ㅏ" to "kj", "ㅓ" to "jk", "ㅗ" to "kj", "ㅜ" to "jk", "ㅡ" to "j", "ㅣ" to "k",
//      "ㅑ" to "kjj", "ㅕ" to "jjk", "ㅛ" to "kkj", "ㅠ" to "jkk","ㅐ" to "jkj", "ㅒ" to "kjjk", "ㅔ" to "jkk", "ㅖ" to "jjkk", " " to "")
    // input size 구하기 (float size * data length * 2(x, y))
    val strokeModel = mapOf("ㄱ" to "a", "ㄴ" to "b", "ㄷ" to "cb", "ㄹ" to "acb", "ㅁ" to "dac", "ㅂ" to "ddcc", "ㅅ" to "ef", "ㅇ" to "g", "ㅈ" to "hf", "ㅊ" to "ihf", "ㅋ" to "ac", "ㅌ" to "ccb", "ㅍ" to "cddc", "ㅎ" to "icg",
      "ㅏ" to "kj", "ㅓ" to "jk", "ㅗ" to "kj", "ㅜ" to "jk", "ㅡ" to "j", "ㅣ" to "k",
      "ㅑ" to "kjj", "ㅕ" to "jjk", "ㅛ" to "kkj", "ㅠ" to "jkk","ㅐ" to "jkj", "ㅒ" to "kjjk", "ㅔ" to "jkk", "ㅖ" to "jjkk", " " to "")
    val inputSize = 4 * 50 * 2

    // 검사할 글자
    val listChar:String = Utilities.divideHangeul(ch)

    // 전체 획순이 맞는지 확인
    var correctStrokeNum = 0
    var modeltoCharArray: ArrayList<String> = ArrayList()
    listChar.forEach {
      correctStrokeNum += strokeMap[it.toString()]!!
      for (i in 0 until strokeMap[it.toString()]!!) {
        modeltoCharArray.add(it + (i+1).toString())
      }
    }
    Log.d("윤도현", modeltoCharArray.toString())

    val enteredStrokeNum = allStrokePosXY.size
    if (enteredStrokeNum == correctStrokeNum) {

      var falseText = ""
      var modelSequence = ""

      listChar.forEach {
        modelSequence += strokeModel[it.toString()]
      }

      for (i in 0 until modelSequence.length) {
        // 입력 데이터 준비
        val byteBuffer = ByteBuffer.allocateDirect(inputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        allStrokePosXY[i].forEach{
          byteBuffer.putFloat(it[0])
          byteBuffer.putFloat(it[1])
        }
        // output array 준비
        val output = Array(1) { FloatArray(1) }

        // model 불러오기
        var modelName = "stroke_" + modelSequence[i]!! + ".tflite"
        var model = Utilities.loadModelFile(resources.assets, modelName)
        val interpreter = Interpreter(model)

        // 추론 결과
        interpreter.run(byteBuffer, output)
        val result = output[0][0]
        Log.d("윤도현", result.toString())

        // 결과 띄우기
        if (result < 0.5) {
          falseText += (modeltoCharArray[i][0] + "의 " + modeltoCharArray[i][1]+ "번째 획순이 올바르지 않음\n")
        }
      }

      // 올바르게 썼는지 화면에 표시
      if (falseText == ""){
        result = "모든 획순을 올바르게 썼습니다"
      } else {
        result = falseText
      }

    } else {
      Log.d("윤도현", allStrokePosXY.toString() +": stroke num wrong")
      result = "입력한 획수가 올바르지 않습니다\n입력한 획수: " + enteredStrokeNum + "\n올바른 획수: " + correctStrokeNum
    }
    return result
  }



  companion object {
    private const val TAG = "MainActivity"
  }
}
