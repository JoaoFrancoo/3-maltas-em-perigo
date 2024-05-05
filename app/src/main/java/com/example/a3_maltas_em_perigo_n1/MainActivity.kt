package com.example.a3_maltas_em_perigo_n1

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialize o Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Inicialize o Firestore
        db = FirebaseFirestore.getInstance()

        val editTextFirstName = findViewById<EditText>(R.id.txtNomeUser)
        val editTextPassUser = findViewById<EditText>(R.id.txtPassUser)
        val editTextEmail = findViewById<EditText>(R.id.txtEmailUser)
        val mensagem = findViewById<TextView>(R.id.txterro)
        val submit = findViewById<Button>(R.id.btnsubmit)
        val textViewIrParaLogin = findViewById<TextView>(R.id.IrParaLogin)

        submit.setOnClickListener {
            val userName = editTextFirstName.text.toString()
            val userEmail = editTextEmail.text.toString()
            val userPasse = editTextPassUser.text.toString()

            if (userName.isEmpty() || userEmail.isEmpty() || userPasse.isEmpty()) {
                // Lidar com campos vazios
                val mensagemErro = "Por favor, preencha todos os campos."
                mensagem.text = mensagemErro
                return@setOnClickListener
            }

            // Consulta na coleção "users" para verificar se já existe um documento com os mesmos dados
            db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Nenhum documento com o mesmo email foi encontrado, então podemos adicionar
                        auth.createUserWithEmailAndPassword(userEmail, userPasse)
                            .addOnSuccessListener { authResult ->
                                // Registro bem-sucedido
                                val user = authResult.user
                                // Agora, adicionamos o nome ao perfil do usuário
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName)
                                    .build()

                                user?.updateProfile(profileUpdates)
                                    ?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d(TAG, "Nome de usuário atualizado com sucesso.")
                                        }
                                    }

                                user?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        // E-mail de verificação enviado com sucesso
                                        val intent = Intent(this, MainActivity2::class.java)
                                        startActivity(intent)
                                    }
                                    ?.addOnFailureListener { exception ->
                                        // Lidar com falha no envio do e-mail de verificação
                                        Log.e("TAG", "Falha no envio do e-mail de verificação: $exception")
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // Lidar com falha no registro
                                Log.e("TAG", "Falha no registro: $exception")
                            }
                    } else {
                        // Já existe um documento com o mesmo email na base de dados
                        val msgCriado = findViewById<TextView>(R.id.msgCriado)
                        msgCriado.setTextColor(resources.getColor(R.color.red))
                        val msg = getString(R.string.dadosExistem)
                        msgCriado.text = msg
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("TAG", "Erro ao obter documentos: ", exception)
                    // Lidar com falhas na consulta
                }
        }
        val user = FirebaseAuth.getInstance().currentUser

// Verifica se o usuário não é nulo e se o nome está definido

        textViewIrParaLogin.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
    }

}
