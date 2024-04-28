package com.example.a3_maltas_em_perigo_n1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
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
        val mensagem = findViewById<TextView>(R.id.txterro)
        val submit = findViewById<Button>(R.id.btnsubmit)

        submit.setOnClickListener {
            val userName = editTextFirstName.text.toString()
            val userPasse = editTextPassUser.text.toString()

            if (userName.isEmpty() || userPasse.isEmpty()) {
                // Lidar com campos vazios
                val mensagemErro = "Nome de utilizador ou palavra-passe vazios"
                mensagem.text = mensagemErro
                return@setOnClickListener
            }

            // Consulta na coleção "users" para verificar se já existe um documento com os mesmos dados
            db.collection("users")
                .whereEqualTo("first", userName)
                .whereEqualTo("pass", userPasse)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Nenhum documento com os mesmos dados foi encontrado, então podemos adicionar
                        val user = hashMapOf(
                            "first" to userName,
                            "pass" to userPasse
                        )

                        auth.createUserWithEmailAndPassword(userName, userPasse)
                            .addOnSuccessListener { authResult ->
                                // Registro bem-sucedido
                                val user = authResult.user
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
                        // Já existe um documento com os mesmos dados na base de dados
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
            val textViewIrParaLogin = findViewById<TextView>(R.id.IrParaLogin)

            textViewIrParaLogin.setOnClickListener {
                val intent = Intent(this, MainActivity2::class.java)
                startActivity(intent)
            }

        }
    }
}