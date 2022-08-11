package org.tensorflow.lite.codelabs.digitclassifier

import android.content.res.AssetManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.divyanshu.draw.widget.DrawView
import org.tensorflow.lite.Interpreter
import java.io.*
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class SubActivity : AppCompatActivity() {

    private var drawView: DrawView? = null

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
        var strokeNum = 0
        drawView?.setOnTouchListener { _, event ->
            // As we have interrupted DrawView's touch event,
            // we first need to pass touch events through to the instance for the drawing to show up.
            drawView?.onTouchEvent(event)
            posX.add(event.x)
            posY.add(event.y)

            // Then if user finished a touch event, run classification
            if (event.action == MotionEvent.ACTION_UP) {
                // 그림 크기 "width: ${drawView?.width}"
                // 터치가 떼어질때 stroke 증가
                strokeNum ++

            }

            true
        }

        testButton.setOnClickListener {
            // interpolate 실행
            var posXY = convertPos(posX, posY)

            // input size 구하기 (4* 100)
            val inputSize = FLOAT_TYPE_SIZE * posXY.size * posXY[0].size

            // byteBuffer 선언
            val byteBuffer = ByteBuffer.allocateDirect(inputSize)
            byteBuffer.order(ByteOrder.nativeOrder())

            // byteBuffer 값 추가
            posXY.forEach{
                byteBuffer.putFloat(it[0])
                byteBuffer.putFloat(it[1])
            }

            // output array 준비
            val output = Array(1) { FloatArray(1) }

            // 검사할 글자
            var dicMap = mapOf("ㄱ" to "a","ㄴ" to "b","ㄷ" to "c","ㄹ" to "d","ㅁ" to "e","ㅂ" to "f","ㅅ" to "g","ㅇ" to "h","ㅈ" to "i","ㅊ" to "j","ㅋ" to "k","ㅌ" to "l","ㅍ" to "m","ㅎ" to "n","ㅏ" to "o","ㅓ" to "p","ㅗ" to "q","ㅜ" to "r","ㅡ" to "s","ㅣ" to "t","ㅑ" to "u","ㅕ" to "v","ㅛ" to "w","ㅠ" to "x","ㅐ" to "y","ㅒ" to "z","ㅔ" to "a1","ㅖ" to "b1")
            var strokeMap = mapOf("ㄱ" to "1","ㄴ" to "1","ㄷ" to "2","ㄹ" to "3","ㅁ" to "3","ㅂ" to "4","ㅅ" to "2","ㅇ" to "1","ㅈ" to "2","ㅊ" to "3","ㅋ" to "2","ㅌ" to "3","ㅍ" to "4","ㅎ" to "3","ㅏ" to "2","ㅓ" to "2","ㅗ" to "2","ㅜ" to "2","ㅡ" to "1","ㅣ" to "1","ㅑ" to "3","ㅕ" to "3","ㅛ" to "3","ㅠ" to "3","ㅐ" to "3","ㅒ" to "4","ㅔ" to "3","ㅖ" to "4")
            var strokeModel = mapOf("ㄱ" to "a", "ㄴ" to "b", "ㄷ" to "ca", "ㄹ" to "acb", "ㅁ" to "dac", "ㅂ" to "ddcc", "ㅅ" to "ef", "ㅇ" to "g", "ㅈ" to "hf", "ㅊ" to "ihf", "ㅋ" to "ac", "ㅌ" to "ccb", "ㅍ" to "cddc", "ㅎ" to "icg", "ㅏ" to "kj", "ㅓ" to "jk", "ㅗ" to "kj", "ㅜ" to "jk", "ㅡ" to "j", "ㅣ" to "k", "ㅑ" to "kjj", "ㅕ" to "jjk", "ㅛ" to "kkj", "ㅠ" to "jkk")
            val checkChar = editText.text[0].toString()

            // stroke 비교
            if (strokeNum != strokeMap[checkChar]!!.toInt()){
                subTextView.text = "획수가 다름!\n실제 획수: " + strokeMap[checkChar] + "\n입력된 획수: "+ strokeNum.toString()
            } else {
                // 모델 불러오기
                var realStroke = strokeMap[checkChar]!!.toInt()
                Log.d("윤도현", realStroke.toString())
                for (i in 0 until realStroke){
                    var modelName = "stroke_" + strokeModel[checkChar]!![i] + ".tflite"
                    Log.d("윤도현", modelName)
                    var model = loadModelFile(resources.assets, modelName)
                    Log.d("윤도현", "load succ")

                    val interpreter = Interpreter(model)

                    Log.d("윤도현", "interpreter")

                    // 추론 결과
                    interpreter.run(byteBuffer, output)
                    val result = output[0][0]
                    Log.d("윤도현", result.toString())

                    // 결과 띄우기
                    if (result > 0.5){
                        subTextView.text = "옳은 획순,\nconfidence: "+ result.toString()
                    } else {
                        subTextView.text = "잘못된 획순 or 다른 글자,\nconfidence: " + result.toString()
                    }


                }

            }



        }

        returnButton.setOnClickListener {
            drawView?.clearCanvas()
            posX = ArrayList<Float>()
            posY = ArrayList<Float>()
            strokeNum = 0
            finish()
        }

        clearButton.setOnClickListener {
            drawView?.clearCanvas()
            posX = ArrayList<Float>()
            posY = ArrayList<Float>()
            strokeNum = 0
            subTextView.text = getString(R.string.sub_text)
        }

    }

    // model 불러오는 함수
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // 좌표 변환
    fun convertPos(posX: ArrayList<Float>, posY: ArrayList<Float>): ArrayList<ArrayList<Float>>{
        var maxX = posX.maxOrNull()
        var minX = posX.minOrNull()
        var cenX = (maxX!! + minX!!) / 2
        var lenX = (maxX!! - minX!!)

        var maxY = posY.maxOrNull()
        var minY = posY.minOrNull()
        var cenY = (maxY!! + minY!!) / 2
        var lenY = (maxY!! - minY!!)

        var len = if (lenX > lenY) lenX else lenY

        var resizeXY = ArrayList<ArrayList<Float>>()

        for (i in 0 until posX.size){
            var resizeX = (posX[i] - cenX) / len
            var resizeY = (cenY - posY[i]) / len
            resizeXY.add(arrayListOf(resizeX, resizeY))
        }

        // interpolate 50개로
        var interpolateXY = ArrayList<ArrayList<Float>>()
        for (i in 0 .. 48) {
            var newt = (resizeXY.size-1) * i / 49F
            // Log.d("윤도현", newt.toString())
            interpolateXY.add(interpolateArray(resizeXY, newt))

        }
        interpolateXY.add(resizeXY[resizeXY.size - 1])

        return interpolateXY
    }

    // linear interpolate 계산
    fun interpolate(a:Float, b:Float, f:Float):Float{
        return a + (b-a) * f
    }

    // posXY 받으면 t에 해당하는 lineer interpolate 좌표값 반환
    fun interpolateArray(posXY: ArrayList<ArrayList<Float>>, f: Float): ArrayList<Float>{
        var fInt = f.toInt()
        var a = posXY[fInt]
        var b = posXY[fInt + 1]

        var newX = interpolate(a[0], b[0], f-fInt)
        var newY = interpolate(a[1], b[1], f-fInt)

        return arrayListOf(newX, newY)
    }

//    fun readTextFile(directory: String):String{
//        Log.d("윤도현", directory)
//        val file = File("$directory/data.txt")
//
//        if(!file.exists())
//            return "no data"
//
//        return try{
//            val read = FileReader(file)
//            val dataText = read.readText()
//            Log.d("윤도현",dataText)
//            dataText
//        } catch (e: Exception){
//            println(e.message)
//            "Exception!"
//        }
//
//    }
    companion object {

        private const val FLOAT_TYPE_SIZE = 4

        private const val OUTPUT_CLASSES_COUNT = 2350
    }

}