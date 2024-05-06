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

class MainActivity2 : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Inicialize o Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Inicialize o Firestore
        db = FirebaseFirestore.getInstance()

        val editTextFirstName = findViewById<EditText>(R.id.txtEmailUser)
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

            // Autenticar usuário com e-mail e senha
            auth.signInWithEmailAndPassword(userName, userPasse)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sucesso ao iniciar sessão, redirecionar para a próxima atividade
                        val intent = Intent(this, IndexActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Falha ao iniciar sessão, exibir mensagem de erro
                        mensagem.text = "Falha ao iniciar sessão. Verifique o nome de utilizador e a palavra-passe."
                        Log.e("TAG", "signInWithEmail:failure", task.exception)
                    }

                }
        }
    }
}