package com.fghilmany.acedemo

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

class TfLiteHelper (private val context: Context){
    private var imageSizeX = 0
    private var imageSizeY = 0

    private lateinit var labels: List<String>
    private lateinit var tfLite: Interpreter

    private lateinit var tfLiteModel: MappedByteBuffer
    private lateinit var inputImageBuffer: TensorImage
    private lateinit var outputprobabilityBuffer: TensorBuffer
    private lateinit var probabilityProcessor: TensorProcessor

    companion object{
        private const val IMAGE_MEAN = 0.0f
        private const val IMAGE_STD = 0.0f
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 255.0f
    }

    fun init(){
        try{
            val opt = Interpreter.Options()
            tfLite = Interpreter(loadModelFile(context), opt)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val modelName = "converted.tflite"
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOfSet = fileDescriptor.startOffset
        val declarationLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOfSet, declarationLength)
    }

    private fun loadImage(bitmap: Bitmap): TensorImage{
        inputImageBuffer.load(bitmap)
        val cropSize = min(bitmap.width, bitmap.height)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(preProcessNormalizeOp())
            .build()
        return imageProcessor.process(inputImageBuffer)
    }

    fun classifyImage(bitmap: Bitmap){
        val imageTensorIndex = 0
        val imageShape = tfLite.getInputTensor(imageTensorIndex).shape()
        imageSizeX = imageShape[1]
        imageSizeY = imageShape[2]
        val imageDataType = tfLite.getInputTensor(imageTensorIndex).dataType()

        val probabilityTensorIndex = 0
        val probabilityShape = tfLite.getOutputTensor(probabilityTensorIndex).shape()
        val probabilityDataType = tfLite.getOutputTensor(probabilityTensorIndex).dataType()
        inputImageBuffer = TensorImage(imageDataType)
        outputprobabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
        probabilityProcessor = TensorProcessor.Builder()
            .add(portProcessNormalizeOp()).build()
        inputImageBuffer = loadImage(bitmap)
        tfLite.run(inputImageBuffer.buffer, outputprobabilityBuffer.buffer.rewind())
    }

    fun showResult(): List<String>? {
        try {
            labels = FileUtil.loadLabels(context, "labels.txt")
        }catch (e: Exception){
            e.printStackTrace()
            return null
        }

        val labeledProbability = TensorLabel(labels, probabilityProcessor.process(outputprobabilityBuffer)).mapWithFloatValue
        val maxValueInMap = Collections.max(labeledProbability.values)
        val result = ArrayList<String>()
        for ((key, value) in labeledProbability){
            if (value == maxValueInMap){
                result.add(key)
            }
        }
        return result
    }

    private fun portProcessNormalizeOp(): TensorOperator? =
        NormalizeOp(IMAGE_MEAN, IMAGE_STD)

    private fun preProcessNormalizeOp(): TensorOperator =
        NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

}