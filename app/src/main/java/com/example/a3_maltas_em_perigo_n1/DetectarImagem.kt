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
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private lateinit var bottomNavigationHandler: BottomNavigationHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detectarimagem)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationHandler = BottomNavigationHandler(this, bottomNavigationView)
        bottomNavigationHandler.setupWithNavigation(
            R.id.navigation_camera,
            R.id.navigation_profile,
            R.id.navigation_feed,
            R.id.navigation_notification
        )

        // Atualizar item selecionado
        bottomNavigationHandler.updateSelectedItem(R.id.navigation_camera)

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
            val resultText = classes.getOrNull(maxPos) ?: "Unknown"
            result.text = resultText

            confidence.text = classes.indices.joinToString("\n") { i ->
                String.format("%s: %.1f%%", classes[i], confidences[i] * 100)
            }

            auth.currentUser?.let { user ->
                user.displayName?.let { nomeUsuario ->
                    Log.d("IndexActivity", "Nome do usuário: $nomeUsuario")
                } ?: Log.e("IndexActivity", "Nome do usuário não está definido.")

                showSaveImageDialog(user, image, resultText)
            }

        } catch (e: IOException) {
            Toast.makeText(this, "Error running inference: $e", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveImageDialog(user: FirebaseUser, image: Bitmap, resultText: String) {
        AlertDialog.Builder(this)
            .setTitle("Guardar Imagem")
            .setMessage("Deseja guardar esta imagem no seu perfil?")
            .setPositiveButton("Sim") { _, _ ->
                saveImageToProfile(user, image, resultText)
            }
            .setNegativeButton("Não", null)
            .create()
            .show()
    }

    private fun askForAdditionalInfo(user: FirebaseUser, resultText: String, photoUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Informações Adicionais")
            .setMessage("Você gostaria de fornecer mais informações sobre o $resultText?")
            .setPositiveButton("Sim") { _, _ ->
                val intent = Intent(this, infosadicionais::class.java)
                intent.putExtra("RESULT_TEXT", resultText)
                intent.putExtra("PHOTO_URL", photoUrl)
                startActivity(intent)
            }
            .setNegativeButton("Não", null)
            .create()
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            // Exibir a imagem original no ImageView
            imageView.setImageBitmap(imageBitmap)

            // Redimensionar com qualidade para classificação
            val scaledImage = Bitmap.createScaledBitmap(imageBitmap, imageSize, imageSize, true)
            classifyImage(scaledImage)

            numFotosTiradas++
            auth.currentUser?.let { concederConquistaTirarFoto(it, numFotosTiradas) }
        }
    }

    private fun saveImageToProfile(user: FirebaseUser, image: Bitmap, resultText: String) {
        val imageFileName = "${user.uid}_${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child("imagensUser/$imageFileName")

        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        storageRef.putBytes(imageData)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfileWithImageUrl(user, uri.toString())
                    askForAdditionalInfo(user, resultText, uri.toString())

                    // Enviar o broadcast para atualizar o feed
                    val intent = Intent("com.example.ACTION_IMAGE_UPLOADED")
                    sendBroadcast(intent)
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
                Toast.makeText(this, "Erro ao salvar nova imagem do perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveNotificationToFollowers(user: FirebaseUser, message: String) {
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val followers = documentSnapshot.get("followers") as? List<String> ?: emptyList()

                followers.forEach { followerId ->
                    db.collection("users").document(followerId)
                        .update("notifications", FieldValue.arrayUnion(message))
                        .addOnSuccessListener {
                            Log.d(TAG, "Notificação salva para o seguidor $followerId.")
                            showNotification(message)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Falha ao salvar notificação para o seguidor $followerId: $exception")
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao obter seguidores do usuário: $exception")
            }
    }

    private fun showNotification(message: String) {
        val intent = Intent(this, IndexActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, "CONQUISTA_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Nova notificação")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(this@DetectarImagem, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            notify(1, builder.build())
        }
    }

    private fun concederConquistaTirarFoto(user: FirebaseUser, numFotosTiradas: Int) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val conquistas = document.get("conquistas") as? ArrayList<String> ?: ArrayList()
                    if (numFotosTiradas >= 1 && !conquistas.contains("Tirou a Primeira Foto")) {
                        conquistas.add("Tirou a Primeira Foto")
                        userRef.update("conquistas", conquistas)
                        showNotification("Parabéns! Você conquistou: Tirou a Primeira Foto")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro ao conceder conquista: $exception")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    private fun Bitmap.toByteBuffer(imageSize: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(imageSize * imageSize)
        this.getPixels(intValues, 0, this.width, 0, 0, this.width, this.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) * (1f / 255f))
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) * (1f / 255f))
            byteBuffer.putFloat((pixelValue and 0xFF) * (1f / 255f))
        }
        return byteBuffer
    }
}
