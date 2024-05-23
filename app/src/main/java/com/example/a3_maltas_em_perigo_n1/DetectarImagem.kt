package com.example.a3_maltas_em_perigo_n1

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
    private val REQUEST_CODE_POST_NOTIFICATIONS = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detectarimagem)

        // Verifique e solicite a permissão de notificações se necessário
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            }
        }

        // Cria o canal de notificação
        createNotificationChannel()

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        result = findViewById(R.id.result)
        confidence = findViewById(R.id.confidence)
        imageView = findViewById(R.id.imageView)
        picture = findViewById(R.id.button)

        model = ModelUnquant.newInstance(applicationContext)

        picture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            }
        }

        findViewById<TextView>(R.id.textPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ConquistaChannel"
            val descriptionText = "Canal para notificações de conquistas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CONQUISTA_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun launchCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permissão de notificações concedida, você pode continuar com o envio de notificações
        } else {
            Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun classifyImage(image: Bitmap) {
        try {
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, imageSize, imageSize, 3), DataType.FLOAT32)
            val byteBuffer = image.toByteBuffer(imageSize)
            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.getOutputFeature0AsTensorBuffer()

            val confidences = outputFeature0.floatArray
            val maxPos = confidences.indices.maxByOrNull { confidences[it] } ?: -1
            val classes = arrayOf("Peluche", "Teclado")
            result.text = classes.getOrNull(maxPos) ?: "Unknown"

            confidence.text = classes.indices.joinToString("\n") { i ->
                String.format("%s: %.1f%%", classes[i], confidences[i] * 100)
            }

            auth.currentUser?.let { user ->
                user.displayName?.let { nomeUsuario ->
                    Log.d("IndexActivity", "Nome do usuário: $nomeUsuario")
                } ?: Log.e("IndexActivity", "Nome do usuário não está definido.")

                showSaveImageDialog(user, image)
            }

        } catch (e: IOException) {
            Toast.makeText(this, "Error running inference: $e", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveImageDialog(user: FirebaseUser, image: Bitmap) {
        AlertDialog.Builder(this)
            .setTitle("Guardar Imagem")
            .setMessage("Deseja guardar esta imagem no seu perfil?")
            .setPositiveButton("Sim") { _, _ -> saveImageToProfile(user, image) }
            .setNegativeButton("Não", null)
            .create()
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val scaledImage = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, false)
            imageView.setImageBitmap(scaledImage)
            classifyImage(scaledImage)

            numFotosTiradas++
            auth.currentUser?.let { concederConquistaTirarFoto(it, numFotosTiradas) }
        }
    }

    private fun saveImageToProfile(user: FirebaseUser, image: Bitmap) {
        val imageFileName = "${user.uid}_${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child("imagensUser/$imageFileName")

        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        storageRef.putBytes(imageData)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfileWithImageUrl(user, uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao fazer upload da imagem: $exception")
                Toast.makeText(this, "Erro ao fazer upload da imagem.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileWithImageUrl(user: FirebaseUser, imageUrl: String) {
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val photoUrls = documentSnapshot.get("photoUrl") as? ArrayList<String> ?: ArrayList()
                photoUrls.add(imageUrl)
                db.collection("users").document(user.uid)
                    .update("photoUrl", photoUrls)
                    .addOnSuccessListener {
                        Log.d(TAG, "URL da nova imagem do perfil salva no Firestore com sucesso.")
                        Toast.makeText(this, "Nova imagem do perfil salva com sucesso.", Toast.LENGTH_SHORT).show()
                        saveNotificationToFollowers(user, "Seu amigo postou uma nova imagem.")
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

    private fun saveNotificationToFollowers(user: FirebaseUser, message: String) {
        val userDocRef = db.collection("users").document(user.uid)

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val seguidores = document.get("seguidores") as? List<String> ?: emptyList()

                seguidores.forEach { seguidorId ->
                    val notification = hashMapOf(
                        "message" to message,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("users").document(seguidorId).collection("notifications")
                        .add(notification)
                        .addOnSuccessListener {
                            Log.d(TAG, "Notificação enviada para o seguidor $seguidorId com sucesso.")
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Falha ao enviar notificação para o seguidor $seguidorId: $exception")
                        }
                }
            } else {
                Log.e(TAG, "Documento do usuário não encontrado.")
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Falha ao obter documento do usuário: $exception")
        }
    }

    private fun concederConquistaTirarFoto(user: FirebaseUser, numFotosTiradas: Int) {
        val conquistas = when (numFotosTiradas) {
            1 -> listOf("Conquista 1")
            5 -> listOf("Conquista 2")
            10 -> listOf("Conquista 3")
            else -> emptyList()
        }

        if (conquistas.isNotEmpty()) {
            val userDocRef = db.collection("users").document(user.uid)
            conquistas.forEach { conquista ->
                userDocRef.update("conquistas", FieldValue.arrayUnion(conquista))
                    .addOnSuccessListener {
                        Log.d(TAG, "Conquista concedida com sucesso: $conquista")
                        sendLocalNotification(user, "Você ganhou a conquista: $conquista")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao conceder conquista: $conquista, ${e.message}")
                    }
            }
        }
    }

    private fun sendLocalNotification(user: FirebaseUser, message: String) {
        val intent = Intent(this, DetectarImagem::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, "CONQUISTA_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Nova Conquista!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(2, builder.build())
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
