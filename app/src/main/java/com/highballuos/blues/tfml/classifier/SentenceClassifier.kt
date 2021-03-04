package com.highballuos.blues.tfml.classifier

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import kotlin.properties.Delegates

class SentenceClassifier(ctx: Context) {
    private val output // depending on models architecture (possible multiple output)
            : Array<FloatArray> = Array(1){FloatArray(1)}
    private var input: ByteBuffer // models input format
    private val tflite: Interpreter // the model

    fun classify(bitmap: Bitmap): Any {
        convertBitmapToByteBuffer(bitmap) // flatten bitmap to byte array
        tflite.run(input, output) // classify task
        return output
    }

    /*
    fun classifyCategoryResult(bitmap: Bitmap, max: Int): List<Category> {
        convertBitmapToByteBuffer(bitmap) // flatten bitmap to byte array
        tflite.run(input, output) // classify task

        val out = mutableListOf<Category>()
        val result = CFResult(output[0], labels)

        for (index in result.topK) {
            val category = Category(getLabel(index), getProbability(index))
            out.add(category)
            if(expectedIndex == index && category.score > 0.6){
                find = true
            }
        }

        return out
    }
    */

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (input == null) {
            return
        }
        input!!.rewind()
        var pixel = 0
    }

    fun getProbability(index: Int): Float {
        return output[0][index]
    }

    companion object {
        const val DIM_IMG_SIZE_HEIGHT = 28
        const val DIM_IMG_SIZE_WIDTH = 28
        private const val MODEL_FILE = "10/model10.tflite"
        private const val LABELS_FILE = "10/labels.csv"
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 1
    }

    init {
        // load model
        val modelBuffered: MappedByteBuffer = ModelInput.loadModelFile(ctx, MODEL_FILE)
        Log.i("SentenceClassifier.kt", "load buffer: ${modelBuffered.isLoaded}")
        val option = Interpreter.Options()
        tflite = Interpreter(modelBuffered, option)

        // allocate memory for model input
        input = ByteBuffer.allocateDirect(4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_HEIGHT * DIM_IMG_SIZE_WIDTH * DIM_PIXEL_SIZE)
        input.order(ByteOrder.nativeOrder())
    }
}