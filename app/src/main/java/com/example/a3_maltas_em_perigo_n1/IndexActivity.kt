package com.example.a3_maltas_em_perigo_n1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.LinearLayout
import com.google.firebase.auth.FirebaseAuth

class IndexActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_index)

        // Referencie o LinearLayout pelo ID correto
        val linearLayout = findViewById<LinearLayout>(R.id.main)

        // Aplicar recuos da janela ao LinearLayout
        ViewCompat.setOnApplyWindowInsetsListener(linearLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicialize o Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Referenciar os botões
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnI = findViewById<Button>(R.id.btnVamosSo)

        // Verificar se o usuário está logado
        val usuarioLogado = auth.currentUser != null

        if (usuarioLogado) {
            // Se o usuário estiver logado, esconda o botão "Criar Conta"
            btnCadastrar.visibility = View.GONE
            // Altere o texto do botão "Iniciar Sessão" para "Ver Perfil"
            btnLogin.text = getString(R.string.verPerfil)
        } else {
            // Se o usuário não estiver logado, mantenha o botão "Criar Conta" visível
            btnCadastrar.visibility = View.VISIBLE
            // Mantenha o texto do botão "Iniciar Sessão" como está
            btnLogin.text = getString(R.string.inicio_sessao)
        }

        // Configurar cliques nos botões
        btnCadastrar.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnLogin.setOnClickListener {
            // Dependendo do estado do usuário, redirecione para diferentes atividades
            if (usuarioLogado) {
                startActivity(Intent(this, PerfilActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity2::class.java))
            }
        }
    }
}
