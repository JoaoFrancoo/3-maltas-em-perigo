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

class AtualizarActivity
    : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser

        val editTextFirstName = findViewById<EditText>(R.id.txtNomeUser)
        val editTextPassUser = findViewById<EditText>(R.id.txtPassUser)
        val editTextEmail = findViewById<EditText>(R.id.txtEmailUser)
        val mensagem = findViewById<TextView>(R.id.txterro)
        val submit = findViewById<Button>(R.id.btnsubmit)
        val textViewIrParaLogin = findViewById<TextView>(R.id.IrParaLogin)
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
            val userEmail = editTextEmail.text.toString()
            val userPass = editTextPassUser.text.toString()

            if (userName.isEmpty() || userEmail.isEmpty() || userPass.isEmpty()) {
                val mensagemErro = "Por favor, preencha todos os campos."
                mensagem.text = mensagemErro
                return@setOnClickListener
            }

            currentUser?.let { user ->
                if (userEmail != user.email) {
                    user.updateEmail(userEmail).addOnFailureListener { exception ->
                        Log.e(TAG, "Falha ao atualizar email: $exception")
                        return@addOnFailureListener
                    }
                }

                if (userPass.isNotEmpty()) {
                    user.updatePassword(userPass).addOnFailureListener { exception ->
                        Log.e(TAG, "Falha ao atualizar senha: $exception")
                        return@addOnFailureListener
                    }
                }

                if (imageUri != null) {
                    uploadImageToStorage(user, userName, userEmail)
                } else {
                    updateFirestoreUserData(user, userName, userEmail, user.photoUrl.toString())
                }
            }
        }

        textViewIrParaLogin.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
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

        storageRef.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    user.updateProfile(UserProfileChangeRequest.Builder()
                        .setDisplayName(userName)
                        .setPhotoUri(uri)
                        .build()).addOnSuccessListener {
                        updateFirestoreUserData(user, userName, userEmail, imageUrl)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Falha ao atualizar perfil de usuário: $exception")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao fazer upload da imagem: $exception")
                user.updateProfile(UserProfileChangeRequest.Builder()
                    .setDisplayName(userName)
                    .build()).addOnSuccessListener {
                    updateFirestoreUserData(user, userName, userEmail, user.photoUrl.toString())
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Falha ao atualizar perfil de usuário: $exception")
                }
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
                val intent = Intent(this, MainActivity2::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Falha ao atualizar dados do usuário no Firestore: $exception")
            }
    }
}
