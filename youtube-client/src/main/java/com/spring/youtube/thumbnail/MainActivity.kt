package com.spring.youtube.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import com.spring.youtube.thumbnail.ml.MobilenetV2
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private val tag = "spring-main"
    private lateinit var model: MobilenetV2
    private val targetImagePath = "waffle-preview-image.png"
    private val videoPath = "waffle-preview-video.mp4"
    private lateinit var bestFrameView: ImageView
    private lateinit var bestFrameTextView: TextView

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        model = MobilenetV2.newInstance(this, Model.Options.Builder().setDevice(Model.Device.GPU).build())

        // Load target image
        val targetImageView = findViewById<ImageView>(R.id.targetImageView)
        val targetBitmap = loadAssetImage()
        targetImageView.setImageBitmap(targetBitmap)

        // Load video
        bestFrameView = findViewById(R.id.bestFrameView)
        bestFrameTextView = findViewById(R.id.bestFrameTimeView)

        // Start processing video
        GlobalScope.launch(Dispatchers.IO) {
            findMostSimilarFrame(targetBitmap)
        }
    }

    private fun findMostSimilarFrame(targetBitmap: Bitmap) {
        val targetImageBuffer = preprocessImage(targetBitmap)
        val targetFeatures = extractFeatures(targetImageBuffer)

        val retriever = loadAssetVideo()

        val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 30.0f
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        val totalFrames = (duration / 1000) * frameRate
        Log.d(tag, "Video frame rate: $frameRate fps, duration: $duration ms, total frames: $totalFrames")

        var bestSimilarity = -1f
        var bestFrame: Bitmap? = null
        var bestFrameTime = 0f

        for (i in 0 until totalFrames.toInt() step frameRate.toInt()) {
            val frameTime = (i / frameRate).toLong() * 1000000
//            val frameBitmap = retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST)
            val frameBitmap = retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: break

            val frameImageBuffer = preprocessImage(frameBitmap)
            val frameFeatures = extractFeatures(frameImageBuffer)
            val similarity = calculateSimilarity(targetFeatures, frameFeatures)

            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestFrame = frameBitmap
                bestFrameTime = i / frameRate
            }

            val progress = ((i + frameRate.toInt()).toFloat() / totalFrames * 100).coerceAtMost(100.0f)
            if (i % (2 * frameRate.toInt()) == 0 || i >= totalFrames.toInt() - frameRate.toInt()) {
                Log.d(tag, "Progress: $progress%, Best Similarity: $bestSimilarity, Best Frame Time: $bestFrameTime s")
                runOnUiThread {
                    if (bestFrame != null) {
                        bestFrameView.setImageBitmap(bestFrame)
                    }
                    bestFrameTextView.text = "Progress: $progress% \nBest Similarity: $bestSimilarity \nBest Frame Time: $bestFrameTime s"
                }
            }
        }

        retriever.release()
    }

    private fun preprocessImage(bitmap: Bitmap):  TensorBuffer {
        val resize = 224

        // 비트맵을 ARGB_8888 형식으로 변환
        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // TensorImage 객체 생성
        val tensorImage = TensorImage(DataType.FLOAT32)

        // bitmap 을 TensorImage 에 로드
        tensorImage.load(argbBitmap)

        // 이미지 처리기 생성
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(resize, resize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))         // Normalize image
            .build()

        // 이미지 처리
        val processedImage = imageProcessor.process(tensorImage)

        // TensorBuffer 생성
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, resize, resize, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(processedImage.buffer) // Load image buffer

        return inputFeature0
    }

    private fun extractFeatures(inputFeature0: TensorBuffer): FloatArray {
        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        return outputFeature0.floatArray
    }


    // 썸네일 피쳐와 동영상 1프레임의 피쳐를 비교
    private fun calculateSimilarity(features1: FloatArray, features2: FloatArray): Float {
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        for (i in features1.indices) {
            dotProduct += features1[i] * features2[i]
            norm1 += features1[i] * features1[i]
            norm2 += features2[i] * features2[i]
        }
        return (dotProduct / (sqrt(norm1) * sqrt(norm2))).toFloat()
    }

    // 썸네일과 동영상을 assets 에서 가져옴
    private fun loadAssetImage(): Bitmap {
        val assetManager = assets
        val inputStream = assetManager.open(targetImagePath)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun loadAssetVideo(): MediaMetadataRetriever {
        val retriever = MediaMetadataRetriever()
        val assetManager = assets
        val afd = assetManager.openFd(videoPath)
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        return retriever
    }
}