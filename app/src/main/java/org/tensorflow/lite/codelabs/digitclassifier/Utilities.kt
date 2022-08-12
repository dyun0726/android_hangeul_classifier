package org.tensorflow.lite.codelabs.digitclassifier

import android.content.res.AssetManager
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Utilities(){
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

    // 한글 나누는 함수
    fun divideHangeul(ch: Char): String{
        val uniBase: Int = 0xAC00
        val uniLast: Int = 0xD7AF
        val mOnsetTbl = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        val mNucleusTbl = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
        val mCodaTbl = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
        var mOnset = " "
        var mNucleus = " "
        var mCoda = " "
        var fullString = ""

        val intCh = ch.toInt()

        // 들어온 문자 한글 아닐시에
        if (intCh < uniBase  || intCh > uniLast) {
            fullString = mOnset + mNucleus + mCoda
            return fullString
        }

        var iUniCode = intCh - uniBase
        val iOnsetIdx = iUniCode / (21 * 28)
        iUniCode = iUniCode % (21 * 28)
        val iNucleusIdx = iUniCode / 28
        val iCodaIdx = iUniCode % 28

        mOnset = mOnsetTbl[iOnsetIdx].toString()
        mNucleus = mNucleusTbl[iNucleusIdx].toString()
        mCoda = mCodaTbl[iCodaIdx].toString()


        //쌍자음 등 다시 구분
        when (mOnset){
            "ㄲ" -> { mOnset = "ㄱㄱ" }
            "ㄸ" -> { mOnset = "ㄷㄷ" }
            "ㅆ" -> { mOnset = "ㅅㅅ" }
            "ㅃ" -> { mOnset = "ㅂㅂ" }
            "ㅉ" -> { mOnset = "ㅈㅈ"}
        }
        when (mNucleus) {
            "ㅘ" -> { mNucleus = "ㅗㅏ" }
            "ㅙ" -> { mNucleus = "ㅗㅐ" }
            "ㅚ" -> { mNucleus = "ㅗㅣ" }
            "ㅝ" -> { mNucleus = "ㅜㅓ" }
            "ㅞ" -> { mNucleus = "ㅜㅔ" }
            "ㅟ" -> { mNucleus = "ㅜㅣ" }
            "ㅢ" -> { mNucleus = "ㅡㅣ" }
        }
        when (mCoda){
            "ㄲ" -> { mCoda = "ㄱㄱ" }
            "ㄳ" -> { mCoda = "ㄱㅅ" }
            "ㄵ" -> { mCoda = "ㄴㅈ" }
            "ㄶ" -> { mCoda = "ㄴㅎ" }
            "ㄺ" -> { mCoda = "ㄹㄱ" }
            "ㄻ" -> { mCoda = "ㄹㅁ" }
            "ㄽ" -> { mCoda = "ㄹㅅ" }
            "ㄾ" -> { mCoda = "ㄹㅌ" }
            "ㄿ" -> { mCoda = "ㄹㅍ" }
            "ㅀ" -> { mCoda = "ㄹㅎ" }
            "ㅄ" -> { mCoda = "ㅂㅅ" }
            "ㅆ" -> { mCoda = "ㅅㅅ" }
        }

        fullString = mOnset + mNucleus + mCoda
        return fullString

    }


    fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }




}