package com.example.a3_maltas_em_perigo_n1

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class PerfilActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicialize o Firebase Authentication
        auth = Firebase.auth

        // Exibir nome e e-mail do usuário logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val nomeUsuario = currentUser.displayName
            val emailUsuario = currentUser.email
            findViewById<TextView>(R.id.txtNome).text = "Nome: $nomeUsuario"
            findViewById<TextView>(R.id.txtEmail).text = "Email: $emailUsuario"

            // Carregar e exibir a imagem do perfil, se disponível
            val photoUrl = currentUser.photoUrl
            if (photoUrl != null) {
                val imageView = findViewById<ImageView>(R.id.imgPerfil)
                Picasso.get().load(photoUrl).into(imageView)
            }
        }

        // Configurar o botão "Terminar Sessão"
        val btnTerminarSessao = findViewById<Button>(R.id.btnTerminarSessao)
        btnTerminarSessao.setOnClickListener {
            terminarSessao()
        }
    }

    // Método para terminar a sessão
    private fun terminarSessao() {
        auth.signOut()
        // Após terminar a sessão, redirecione para a tela de login ou outra tela desejada
        startActivity(Intent(this, IndexActivity::class.java))
        // Finalize a atividade atual
        finish()
    }
}
