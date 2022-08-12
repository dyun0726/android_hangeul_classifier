package org.tensorflow.lite.codelabs.digitclassifier

import android.R.string
import android.content.res.AssetManager
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
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class SubActivity : AppCompatActivity() {

    private var drawView: DrawView? = null
    private var Utilities = Utilities()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        //drawView 설정
        drawView = findViewById(R.id.sub_draw_view)
        drawView?.setStrokeWidth(70.0f)
        drawView?.setColor(Color.WHITE)
        drawView?.setBackgroundColor(Color.BLACK)

        var testButton: Button = findViewById(R.id.sub_test_button)
        var clearButton: Button = findViewById(R.id.sub_clear_button)
        var returnButton: Button = findViewById(R.id.sub_return_button)
        var subTextView: TextView = findViewById(R.id.sub_guide_text)
        var editText: EditText = findViewById(R.id.sub_edit_text)

        var posX = ArrayList<Float>()
        var posY = ArrayList<Float>()
        var allStrokePosXY = ArrayList<ArrayList<ArrayList<Float>>>()
        var strokeNum = 0
        drawView?.setOnTouchListener { _, event ->
            // As we have interrupted DrawView's touch event,
            // we first need to pass touch events through to the instance for the drawing to show up.
            drawView?.onTouchEvent(event)
            posX.add(event.x)
            posY.add(event.y)

            // Then if user finished a touch event, run classification
            if (event.action == MotionEvent.ACTION_UP) {
                // 터치가 떼어질때 stroke 증가
                allStrokePosXY.add(Utilities.convertPos(posX, posY))
                strokeNum ++
                posX = ArrayList<Float>()
                posY = ArrayList<Float>()
            }

            true
        }

        testButton.setOnClickListener {
            // 필요한 정보들
            val strokeMap = mapOf("ㄱ" to 1,"ㄴ" to 1,"ㄷ" to 2,"ㄹ" to 3,"ㅁ" to 3,"ㅂ" to 4,"ㅅ" to 2,"ㅇ" to 1,"ㅈ" to 2,"ㅊ" to 3,"ㅋ" to 2,"ㅌ" to 3,"ㅍ" to 4,"ㅎ" to 3,"ㅏ" to 2,"ㅓ" to 2,"ㅗ" to 2,"ㅜ" to 2,"ㅡ" to 1,"ㅣ" to 1,"ㅑ" to 3,"ㅕ" to 3,"ㅛ" to 3,"ㅠ" to 3,"ㅐ" to 3,"ㅒ" to 4,"ㅔ" to 3,"ㅖ" to 4, " " to 0)
            val strokeModel = mapOf("ㄱ" to "a", "ㄴ" to "b", "ㄷ" to "cb", "ㄹ" to "acb", "ㅁ" to "dac", "ㅂ" to "ddcc", "ㅅ" to "ef", "ㅇ" to "g", "ㅈ" to "hf", "ㅊ" to "ihf", "ㅋ" to "ac", "ㅌ" to "ccb", "ㅍ" to "cddc", "ㅎ" to "icg",
                                    "ㅏ" to "kj", "ㅓ" to "jk", "ㅗ" to "kj", "ㅜ" to "jk", "ㅡ" to "j", "ㅣ" to "k",
                                    "ㅑ" to "kjj", "ㅕ" to "jjk", "ㅛ" to "kkj", "ㅠ" to "jkk","ㅐ" to "jkj", "ㅒ" to "kjjk", "ㅔ" to "jkk", "ㅖ" to "jjkk", " " to "")
            // input size 구하기 (float size * data length * 2(x, y))
            val inputSize = 4 * 50 * 2

            // 검사할 글자
            val listChar:String = Utilities.divideHangeul(editText.text[0])

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
                    subTextView.text = "모든 획순을 올바르게 썼습니다"
                } else {
                    subTextView.text = falseText
                }

            } else {
                Log.d("윤도현", allStrokePosXY.toString() +": stroke num wrong")
                subTextView.text = "입력한 획수가 올바르지 않음\n입력한 획수: " + enteredStrokeNum + "\n총 옳은 획수: " + correctStrokeNum
            }
        }

        returnButton.setOnClickListener {
            drawView?.clearCanvas()
            posX = ArrayList<Float>()
            posY = ArrayList<Float>()
            allStrokePosXY = ArrayList<ArrayList<ArrayList<Float>>>()
            strokeNum = 0
            finish()
        }

        clearButton.setOnClickListener {
            drawView?.clearCanvas()
            posX = ArrayList<Float>()
            posY = ArrayList<Float>()
            allStrokePosXY = ArrayList<ArrayList<ArrayList<Float>>>()
            strokeNum = 0
            subTextView.text = getString(R.string.sub_text)
        }

    }

}