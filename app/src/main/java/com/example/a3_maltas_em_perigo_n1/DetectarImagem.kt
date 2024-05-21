package com.example.a3_maltas_em_perigo_n1

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.a3_maltas_em_perigo_n1.ml.ModelUnquant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("DEPRECATION")
class DetectarImagem : AppCompatActivity() {

    private lateinit var result: TextView
    private lateinit var confidence: TextView
    private lateinit var imageView: ImageView
    private lateinit var picture: Button
    private lateinit var model: ModelUnquant
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val imageSize = 224
    private var numFotosTiradas: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detectarimagem)

        // Inicialize o Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Inicialize o Firestore
        db = Firebase.firestore

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

    @SuppressLint("DefaultLocale")
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

                // Pergunta ao usuário se ele deseja guardar a imagem
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Guardar Imagem")
                    .setMessage("Deseja guardar esta imagem no seu perfil?")
                    .setPositiveButton("Sim") { _, _ ->
                        saveImageToProfile(user, image)
                    }
                    .setNegativeButton("Não", null)
                    .create()

                dialog.show()
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

            // Após classificar a imagem, incrementar o número de fotos tiradas
            numFotosTiradas++
            // Conceder conquista se o número de fotos tiradas corresponder a um marco
            concederConquistaTirarFoto(auth.currentUser!!, numFotosTiradas)
        }
    }

    private fun saveImageToProfile(user: FirebaseUser, image: Bitmap) {
        // Gera um nome único para a imagem
        val imageFileName = "${user.uid}_${System.currentTimeMillis()}.jpg"

        // Referência ao Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference.child("imagensUser/$imageFileName")

        // Converte a imagem em um byte array
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        // Faz o upload da imagem para o Firebase Storage
        storageRef.putBytes(imageData)
            .addOnSuccessListener { taskSnapshot ->
                // Obtém o URL da imagem após o upload
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Adiciona o novo URL à lista existente de photoUrl no documento do usuário
                    db.collection("users").document(user.uid)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            val photoUrls = documentSnapshot.get("photoUrl") as? ArrayList<String> ?: ArrayList()
                            photoUrls.add(uri.toString())
                            // Atualiza o documento do usuário com a nova lista de photoUrl
                            db.collection("users").document(user.uid)
                                .update("photoUrl", photoUrls)
                                .addOnSuccessListener {
                                    Log.d(TAG, "URL da nova imagem do perfil salva no Firestore com sucesso.")
                                    Toast.makeText(this, "Nova imagem do perfil salva com sucesso.", Toast.LENGTH_SHORT).show()
                                    // Atualiza a exibição das fotos do usuário
                                    exibirFotosUsuario()
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(TAG, "Falha ao salvar URL da nova imagem do perfil no Firestore: $exception")
                                    Toast.makeText(this, "Erro ao salvar nova imagem do perfil.", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Falha ao obter o documento do usuário: $exception")
                            Toast.makeText(this, "Erro ao obter documento do usuário.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao fazer upload da imagem: $exception")
                Toast.makeText(this, "Erro ao fazer upload da imagem.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun concederConquistaTirarFoto(user: FirebaseUser, numFotosTiradas: Int) {
        // Lista para armazenar as conquistas a serem adicionadas
        val conquistas = mutableListOf<String>()

        // Conceder conquista com base no número de fotos tiradas
        when (numFotosTiradas) {
            1 -> conquistas.add("Conquista 1")
            5 -> conquistas.add("Conquista 2")
            10 -> conquistas.add("Conquista 3")
            // Adicione mais casos conforme necessário
        }

        // Verifique se há conquistas para conceder
        if (conquistas.isNotEmpty()) {
            // Referência ao documento do usuário
            val userDocRef = db.collection("users").document(user.uid)

            // Iterar sobre cada conquista e adicioná-la ao documento do usuário
            conquistas.forEach { conquista ->
                // Adiciona a conquista ao array no documento do usuário
                userDocRef.update("conquistas", FieldValue.arrayUnion(conquista))
                    .addOnSuccessListener {
                        Log.d(TAG, "Conquista concedida com sucesso: $conquista")
                        // Adicione qualquer lógica de interface do usuário necessária para refletir as novas conquistas concedidas
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao conceder conquista: $conquista, ${e.message}")
                    }
            }
        }
    }
    private fun saveImageUrlToUserDocument(user: FirebaseUser, imageUrl: String) {
        // Atualiza o documento do usuário com a URL da imagem
        db.collection("users").document(user.uid)
            .update("photoUrl", imageUrl)
            .addOnSuccessListener {
                Log.d(TAG, "URL da imagem do perfil salva no Firestore com sucesso.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao salvar URL da imagem do perfil no Firestore: $exception")
            }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        return Uri.parse(MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null))
    }

    private fun exibirFotosUsuario() {
        // Implemente o código para exibir as fotos do usuário a partir dos URLs armazenados no Firestore
        // Você precisará recuperar a lista de URLs de photoUrl no documento do usuário e carregar as imagens correspondentes
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
