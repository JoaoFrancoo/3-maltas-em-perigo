package com.example.a3_maltas_em_perigo_n1

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

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
        auth = FirebaseAuth.getInstance()

        // Exibir nome e e-mail do usuário logado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val nomeUsuario = currentUser.displayName
            val emailUsuario = currentUser.email
            findViewById<TextView>(R.id.txtNome).text = nomeUsuario
            findViewById<TextView>(R.id.txtEmail).text = emailUsuario
        }
    }
}
