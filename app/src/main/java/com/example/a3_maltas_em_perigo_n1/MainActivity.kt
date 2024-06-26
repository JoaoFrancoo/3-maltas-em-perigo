package com.example.a3_maltas_em_perigo_n1

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editTextFirstName = findViewById<EditText>(R.id.txtNomeUser)
        val editTextPassUser = findViewById<EditText>(R.id.txtPassUser)
        val editTextEmail = findViewById<EditText>(R.id.txtEmailUser)
        val mensagem = findViewById<TextView>(R.id.txterro)
        val submit = findViewById<Button>(R.id.btnsubmit)
        val textViewIrParaLogin = findViewById<TextView>(R.id.IrParaLogin)
        val imagePreview = findViewById<ImageView>(R.id.imagePreview)

        Glide.with(this).load(R.drawable.perfilgen).into(imagePreview)

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            findViewById<ImageView>(R.id.imagePreview).apply {
                setImageURI(uri)
                visibility = View.VISIBLE
            }

            // Atualiza a variável imageUri com a URI da imagem selecionada
            imageUri = uri
        }

        imagePreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        submit.setOnClickListener {
            val userName = editTextFirstName.text.toString()
            val userEmail = editTextEmail.text.toString()
            val userPass = editTextPassUser.text.toString()

            if (userName.isEmpty() || userEmail.isEmpty() || userPass.isEmpty()) {
                val mensagemErro = "Por favor, preencha todos os campos."
                mensagem.text = mensagemErro
                return@setOnClickListener
            }

            if (userPass.length < 6) {
                mensagem.text = "A senha precisa ter no mínimo 6 caracteres."
                return@setOnClickListener
            }

            // Verifica se o utilizador já existe no Firestore
            db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Cria um novo utilizador no Firebase Authentication
                        auth.createUserWithEmailAndPassword(userEmail, userPass)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user

                                // Envia email de verificação
                                user?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        Log.d(TAG, "Email de verificação enviado para $userEmail")
                                    }
                                    ?.addOnFailureListener { exception ->
                                        Log.e(TAG, "Falha ao enviar email de verificação: $exception")
                                    }

                                // Guarda o URI da imagem no Firestore
                                user?.let { it1 ->
                                    // Salva o URI da imagem como uma string
                                    val imageUriString = imageUri.toString()

                                    // Salva os dados do usuário, incluindo o URI da imagem, no Firestore
                                    db.collection("users")
                                        .document(it1.uid)
                                        .set(mapOf(
                                            "name" to userName,
                                            "email" to userEmail,
                                            "imageUri" to imageUriString // Salva o URI da imagem como uma string
                                        ))
                                        .addOnSuccessListener {
                                            Log.d(TAG, "URI da imagem adicionado com sucesso ao Firestore.")

                                            // Após salvar o URI da imagem, faça o upload da imagem para o Firebase Storage
                                            uploadImageToStorage(user, userName, userEmail, it1)

                                            // Salva o token do dispositivo
                                            saveDeviceToken(it1.uid)
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e(TAG, "Falha ao adicionar URI da imagem ao Firestore: $exception")
                                        }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Falha no registo: $exception")
                            }
                    } else {
                        val msgCriado = findViewById<TextView>(R.id.msgCriado)
                        msgCriado.setTextColor(resources.getColor(R.color.red))
                        val msg = getString(R.string.dadosExistem)
                        msgCriado.text = msg
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Erro ao obter documentos: ", exception)
                }
        }

        textViewIrParaLogin.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

    private fun saveDeviceToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val tokenInfo = hashMapOf("token" to token)
                db.collection("Tokens").document(userId).set(tokenInfo)
                    .addOnSuccessListener {
                        Log.d(TAG, "Token de dispositivo salvo com sucesso.")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Erro ao salvar token de dispositivo: ", e)
                    }
            } else {
                Log.w(TAG, "Falha ao obter o token de dispositivo.", task.exception)
            }
        }
    }

    // Função para fazer upload da imagem para o Firebase Storage
    private fun uploadImageToStorage(user: FirebaseUser?, userName: String, userEmail: String, firebaseUser: FirebaseUser) {
        // Gera um nome único para a imagem
        val imageFileName = UUID.randomUUID().toString()

        // Referência ao Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$imageFileName")

        // Faz o upload da imagem para o Firebase Storage
        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                // Obtém o URL da imagem após o upload
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // URL da imagem
                    val imageUrl = uri.toString()

                    // Atualiza os dados do usuário no Firestore com o URL da imagem
                    user?.let {
                        it.updateProfile(UserProfileChangeRequest.Builder()
                            .setDisplayName(userName)
                            .setPhotoUri(uri)
                            .build())
                            .addOnSuccessListener {
                                Log.d(TAG, "Perfil de usuário atualizado com sucesso com URL da imagem.")

                                // Salva o URL da imagem no Firestore
                                db.collection("users")
                                    .document(firebaseUser.uid)
                                    .update("imageUri", imageUrl)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "URL da imagem adicionado com sucesso ao Firestore.")

                                        // Navega para a próxima atividade
                                        val intent = Intent(this, MainActivity2::class.java)
                                        startActivity(intent)
                                        finish() // Finaliza a atividade atual
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e(TAG, "Falha ao adicionar URL da imagem ao Firestore: $exception")
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Falha ao atualizar perfil de usuário: $exception")
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao fazer upload da imagem: $exception")

                // Se o upload falhar, continue atualizando os dados do usuário sem a imagem
                user?.let {
                    it.updateProfile(UserProfileChangeRequest.Builder()
                        .setDisplayName(userName)
                        .build())
                        .addOnSuccessListener {
                            Log.d(TAG, "Perfil de usuário atualizado com sucesso.")

                            // Navega para a próxima atividade
                            val intent = Intent(this, MainActivity2::class.java)
                            startActivity(intent)
                            finish() // Finaliza a atividade atual
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Falha ao atualizar perfil de usuário: $exception")
                        }
                }
            }
    }
}
