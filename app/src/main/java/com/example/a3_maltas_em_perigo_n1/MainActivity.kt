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

                                // Guarda o URI da imagem no Firestore
                                user?.let { it1 ->
                                    // Salva o URI da imagem como uma string
                                    val imageUriString = imageUri.toString()

                                    // Salva os dados do usuário, incluindo o URI da imagem, no Firestore
                                    val userData = hashMapOf(
                                        "name" to userName,
                                        "email" to userEmail,
                                        "imageUri" to imageUriString // Salva o URI da imagem como uma string
                                    )

                                    db.collection("users")
                                        .document(it1.uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "URI da imagem adicionado com sucesso ao Firestore.")

                                            // Após salvar os dados do usuário, faça o upload da imagem para o Firebase Storage
                                            uploadImageToStorage(user, userName, userEmail)
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e(TAG, "Falha ao adicionar URI da imagem ao Firestore: $exception")
                                        }

                                    // Adiciona informações adicionais do usuário no Firestore
                                    val userAdditionalData = hashMapOf(
                                        "fotos_tiradas" to 0, // Inicializa a quantidade de fotos tiradas como 0
                                        "visualizacoes_perfil" to 0 // Inicializa o número de visualizações do perfil como 0
                                    )

                                    db.collection("user_additional_data")
                                        .document(it1.uid)
                                        .set(userAdditionalData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Informações adicionais do usuário adicionadas com sucesso ao Firestore.")
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e(TAG, "Falha ao adicionar informações adicionais do usuário ao Firestore: $exception")
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

    // Função para fazer upload da imagem para o Firebase Storage
    private fun uploadImageToStorage(user: FirebaseUser?, userName: String, userEmail: String) {
        if (imageUri != null) {
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
                            it.updateProfile(
                                UserProfileChangeRequest.Builder()
                                .setDisplayName(userName)
                                .setPhotoUri(uri)
                                .build())
                                .addOnSuccessListener {
                                    Log.d(TAG, "Perfil de usuário atualizado com sucesso com URL da imagem.")

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
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Falha ao fazer upload da imagem: $exception")
                }
        } else {
            // Se o URI da imagem for nulo, apenas atualize os dados do usuário sem a imagem
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
