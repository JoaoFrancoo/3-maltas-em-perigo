package com.example.a3_maltas_em_perigo_n1

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

class AtualizarActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.atualizar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser

        val editTextFirstName = findViewById<EditText>(R.id.txtNomeUser)
        val editTextPassUser = findViewById<EditText>(R.id.txtPassUser)
        val etEmail = findViewById<EditText>(R.id.txtEmailUser)
        val mensagem = findViewById<TextView>(R.id.txterro)
        val submit = findViewById<Button>(R.id.btnsubmit)
        val imagePreview = findViewById<ImageView>(R.id.imagePreview)

        if (currentUser != null) {
            loadUserData()
        }

        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            findViewById<ImageView>(R.id.imagePreview).apply {
                setImageURI(uri)
                visibility = View.VISIBLE
            }
            imageUri = uri
        }

        imagePreview.setOnClickListener {
            pickImage.launch("image/*")
        }

        submit.setOnClickListener {
            val userName = editTextFirstName.text.toString()
            val userEmail = etEmail.text.toString()
            val userPass = editTextPassUser.text.toString()

            if (userName.isEmpty() || userEmail.isEmpty() || userPass.isEmpty()) {
                mensagem.text = "Por favor, preencha todos os campos."
                return@setOnClickListener
            }

            if (userPass.length < 6) {
                mensagem.text = "A senha precisa ter no mínimo 6 caracteres."
                return@setOnClickListener
            }

            db.collection("users")
                .whereEqualTo("name", userName)
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        if (documents.first().data["name"] == currentUser?.displayName) {
                            // Nome de usuário do Firebase é igual ao do editText
                            // Permitir
                            currentUser?.let { user ->
                                if (userEmail != user.email) {
                                    user.updateEmail(userEmail).addOnSuccessListener {
                                        uploadImageToStorage(user, userName, userEmail)
                                    }.addOnFailureListener { exception ->
                                        mensagem.text = "O email já está em uso"
                                    }
                                } else {
                                    uploadImageToStorage(user, userName, userEmail)
                                }
                            }
                        } else {
                            // Nome de usuário do Firebase é diferente do do editText
                            mensagem.text = "Este nome de utilizador já está em uso."
                        }
                    } else {
                        // Nome de usuário não existe
                        currentUser?.let { user ->
                            if (userEmail != user.email) {
                                user.updateEmail(userEmail).addOnSuccessListener {
                                    uploadImageToStorage(user, userName, userEmail)
                                }.addOnFailureListener { exception ->
                                    mensagem.text = "Falha ao atualizar email: ${exception.message}"
                                }
                            } else {
                                uploadImageToStorage(user, userName, userEmail)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Falha ao verificar nome de usuário existente: $exception")
                    mensagem.text = "Ocorreu um erro ao verificar o nome de usuário."
                }
        }
    }

    private fun loadUserData() {
        currentUser?.let { user ->
            findViewById<EditText>(R.id.txtNomeUser).setText(user.displayName)
            findViewById<EditText>(R.id.txtEmailUser).setText(user.email)
            val imagePreview = findViewById<ImageView>(R.id.imagePreview)

            Glide.with(this).load(user.photoUrl).into(imagePreview)

            db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageUrl = document.getString("imageUri")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl).into(imagePreview)
                    }
                }
            }
        }
    }

    private fun uploadImageToStorage(user: FirebaseUser, userName: String, userEmail: String) {
        val imageFileName = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$imageFileName")

        imageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()
                        updateUserProfile(user, userName, imageUrl)
                        updateFirestoreUserData(user, userName, userEmail, imageUrl)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Falha ao fazer upload da imagem: $exception")
                    showMessage("Falha ao fazer upload da imagem.")
                    updateUserProfile(user, userName, null)
                    updateFirestoreUserData(user, userName, userEmail, user.photoUrl.toString())
                }
        } ?: run {
            // Caso o usuário não tenha selecionado uma imagem
            updateUserProfile(user, userName, null)
            updateFirestoreUserData(user, userName, userEmail, user.photoUrl.toString())
        }
    }

    private fun updateUserProfile(user: FirebaseUser, userName: String, imageUrl: String?) {
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(userName)
            .apply {
                imageUrl?.let { setPhotoUri(Uri.parse(it)) }
            }
            .build()

        user.updateProfile(profileUpdate)
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao atualizar perfil de usuário: $exception")
                showMessage("Falha ao atualizar perfil de usuário.")
            }
    }

    private fun updateFirestoreUserData(user: FirebaseUser, userName: String, userEmail: String, imageUrl: String) {
        db.collection("users").document(user.uid)
            .update(mapOf(
                "name" to userName,
                "email" to userEmail,
                "imageUri" to imageUrl
            ))
            .addOnSuccessListener {
                Log.d(TAG, "Dados do usuário atualizados com sucesso no Firestore.")
                val intent = Intent(this, PerfilActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao atualizar dados do usuário no Firestore: $exception")
            }
    }

    private fun showMessage(message: String) {
        findViewById<TextView>(R.id.txterro).text = message
    }

    companion object {
        private const val TAG = "AtualizarActivity"
    }
}
