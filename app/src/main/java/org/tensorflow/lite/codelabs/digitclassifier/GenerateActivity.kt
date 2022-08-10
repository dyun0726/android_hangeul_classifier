package org.tensorflow.lite.codelabs.digitclassifier

import android.graphics.Color
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.divyanshu.draw.widget.DrawView
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.math.max


class GenerateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate)

        val drawView: DrawView = findViewById(R.id.gen_draw_view)
        drawView.setStrokeWidth(70.0f)
        drawView.setColor(Color.WHITE)
        drawView.setBackgroundColor(Color.BLACK)

        val saveButton: Button = findViewById(R.id.gen_save_button)
        val clearButton: Button = findViewById(R.id.gen_clear_button)
        val returnButton: Button = findViewById(R.id.gen_return_button)

        // 좌표값 저장 변수들
        var stroke = 0
        var posX = ArrayList<Float>()
        var posY = ArrayList<Float>()
        var strokeInfo = ArrayList<Int>()
        drawView.setOnTouchListener { _, event ->
            // As we have interrupted DrawView's touch event,
            // we first need to pass touch events through to the instance for the drawing to show up.
            drawView.onTouchEvent(event)

            // Toast.makeText(this, "touch: ${event.x}, ${event.y}", Toast.LENGTH_SHORT).show()
            posX.add(event.x)
            posY.add(event.y)
            strokeInfo.add(stroke)

            // Then if user finished a touch event, run classification
            if (event.action == MotionEvent.ACTION_UP) {
                // 그림 크기 "width: ${drawView?.width}"
                // 터치가 떼어질때 stroke 증가
                stroke += 1
            }

            true
        }

        // save 버튼 누를 시 좌표 저장하고 캔버스 클리어
        saveButton.setOnClickListener {
            var editText: EditText = findViewById(R.id.gen_edit_text)
            var saveValue: String = editText.text.toString()

            // posX, posY 를 통해 길이 50이고 변환된 좌표 리스트 얻음
            var posXY = convertPos(posX, posY)

            var dicMap = mapOf("ㄱ" to "a","ㄴ" to "b","ㄷ" to "c","ㄹ" to "d","ㅁ" to "e","ㅂ" to "f","ㅅ" to "g","ㅇ" to "h","ㅈ" to "i","ㅊ" to "j","ㅋ" to "k","ㅌ" to "l","ㅍ" to "m","ㅎ" to "n","ㅏ" to "o","ㅓ" to "p","ㅗ" to "q","ㅜ" to "r","ㅡ" to "s","ㅣ" to "t","ㅑ" to "u","ㅕ" to "v","ㅛ" to "w","ㅠ" to "x","ㅐ" to "y","ㅒ" to "z","ㅔ" to "a1","ㅖ" to "b1")

            // 텍스트 파일 작성
            writeTextFile(filesDir.absolutePath, dicMap[saveValue]!!,saveValue + "\n")
            writeTextFile(filesDir.absolutePath, dicMap[saveValue]!!,posXY.toString() + "\n")
            writeTextFile(filesDir.absolutePath, dicMap[saveValue]!!,strokeInfo.toString() + "\n")


            // posList, canvas 초기화
            drawView.clearCanvas()
            posX = ArrayList<Float>()
            posY = ArrayList<Float>()
            strokeInfo = ArrayList<Int>()
            stroke = 0

        }

        clearButton.setOnClickListener {
            // posList, canvas 초기화
            drawView.clearCanvas()
            posX = ArrayList<Float>()
            posY = ArrayList<Float>()
            strokeInfo = ArrayList<Int>()
            stroke = 0
        }

        returnButton.setOnClickListener {
            finish()
        }

    }

    // 파일 쓰기
    fun writeTextFile(directory:String, filename:String, content:String){
        Log.d("윤도현", directory)
        val dir = File(directory)

        if(!dir.exists()){ //dir이 존재 하지 않을때
            dir.mkdirs() //mkdirs : 중간에 directory가 없어도 생성됨
        }


        val writer = FileWriter(directory + "/" + filename + "_right.txt",true)
        val buffer = BufferedWriter(writer)
        buffer.write(content)
        buffer.close()

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
        val arrlen: Float = 49F
        Log.d("윤도현", resizeXY.size.toString())

        var cnt = 0
        for (i in 0 .. 48) {
            var newt = (resizeXY.size-1) * i / arrlen
            Log.d("윤도현", newt.toString())
            interpolateXY.add(interpolateArray(resizeXY, newt))
            cnt ++

        }
        interpolateXY.add(resizeXY[resizeXY.size - 1])
        cnt ++
        Log.d("윤도현", cnt.toString())

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
}