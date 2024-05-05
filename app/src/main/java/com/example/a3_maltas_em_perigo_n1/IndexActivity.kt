package com.example.a3_maltas_em_perigo_n1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.a3_maltas_em_perigo_n1.ml.ModelUnquant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("DEPRECATION")
class IndexActivity : AppCompatActivity() {

    private lateinit var result: TextView
    private lateinit var confidence: TextView
    private lateinit var imageView: ImageView
    private lateinit var picture: Button
    private lateinit var model: ModelUnquant
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val imageSize = 224

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.index)

        // Inicialize o Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Inicialize o Firestore
        db = FirebaseFirestore.getInstance()

        result = findViewById(R.id.result)
        confidence = findViewById(R.id.confidence)
        imageView = findViewById(R.id.imageView)
        picture = findViewById(R.id.button)

        // Create model instance
        model = ModelUnquant.newInstance(applicationContext)

        picture.setOnClickListener {
            // Launch camera if we have permission
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 1)
            } else {
                // Request camera permission if we don't have it.
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            }
        }

        // Botão para ir para a PerfilActivity
        val txtPerfil = findViewById<TextView>(R.id.textPerfil)
        txtPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, 1)
        } else {
            // Handle permission denial
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            // Create inputs for reference
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, imageSize, imageSize, 3), DataType.FLOAT32)
            val byteBuffer = image.toByteBuffer(imageSize)
            inputFeature0.loadBuffer(byteBuffer)

            // Run model inference and get result
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.getOutputFeature0AsTensorBuffer()

            val confidences = outputFeature0.floatArray
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }
            // parte para colocar mais objetos para a IA ver (caso tenham treinado ela para mais)
            val classes = arrayOf("Peluche", "Teclado")
            result.text = classes[maxPos]

            var s = ""
            for (i in classes.indices) {
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100)
            }

            confidence.text = s

            // Verificar se o usuário está autenticado e mostrar o nome de usuário no logcat
            val user = auth.currentUser
            if (user != null) {
                val nomeUsuario = user.displayName
                if (nomeUsuario != null) {
                    Log.d("IndexActivity", "Nome do usuário: $nomeUsuario")
                } else {
                    Log.e("IndexActivity", "Nome do usuário não está definido.")
                }
            }

        } catch (e: IOException) {
            // Handle exception
            Toast.makeText(this, "Error running inference: $e", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val scaledImage = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, false)
            imageView.setImageBitmap(scaledImage)
            classifyImage(scaledImage)
        }
    }
}

fun Bitmap.toByteBuffer(imageSize: Int): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
    byteBuffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(imageSize * imageSize)
    getPixels(intValues, 0, width, 0, 0, width, height)
    var pixel = 0
    for (i in 0 until imageSize) {
        for (j in 0 until imageSize) {
            val value = intValues[pixel++]
            byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f))
            byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))
            byteBuffer.putFloat((value and 0xFF) * (1f / 255f))
        }
    }

    return byteBuffer
}
