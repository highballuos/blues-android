package com.highballuos.blues.tfml.classifier

import android.content.Context
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Helper class to read TensorFlow model and labels from file
 */
internal object ModelInput {
    /*
     * Reads the compressed model as MappedByteBuffer from file.
     *
     */
    @Throws(IOException::class)
    fun loadModelFile(ctx: Context, modelFile: String?): MappedByteBuffer {
        val assetManager = ctx.assets
        val fileDescriptor = assetManager.openFd(modelFile!!)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}