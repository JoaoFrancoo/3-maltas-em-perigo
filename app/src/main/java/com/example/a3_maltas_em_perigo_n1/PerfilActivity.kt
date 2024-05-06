package com.example.a3_maltas_em_perigo_n1

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class PerfilActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var fotosAdapter: FotosAdapter
    private lateinit var conquistasAdapter: ConquistasAdapter
    private lateinit var buttonFotos: Button
    private lateinit var buttonConquistas: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser!!

        recyclerView = findViewById(R.id.recyclerViewFotos)
        fotosAdapter = FotosAdapter(emptyList())
        conquistasAdapter = ConquistasAdapter(emptyList())

        val layoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = layoutManager

        buttonFotos = findViewById(R.id.btnFotos)
        buttonConquistas = findViewById(R.id.btnConquistas)
        buttonFotos.setOnClickListener {
            Log.d("PerfilActivity", "Clicou no botão Fotos")
            exibirFotosUsuario()
        }
        buttonConquistas.setOnClickListener {
            Log.d("PerfilActivity", "Clicou no botão Conquistas")
            exibirConquistasUsuario()
        }

        exibirFotosUsuario()
    }

    override fun onResume() {
        super.onResume()
        atualizarVisualizacoesPerfil()
        atualizarNumeroFotosTiradas()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                terminarSessao()
                true
            }
            R.id.action_edit_profile -> {
                // Abra a tela para editar o perfil
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exibirInformacoesUsuario() {
        val userDocRef = db.collection("users").document(currentUser.uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val nomeUsuario = document.getString("name")
                    val emailUsuario = document.getString("email")
                    findViewById<TextView>(R.id.txtNome).text = "Nome: $nomeUsuario"
                    findViewById<TextView>(R.id.txtEmail).text = "Email: $emailUsuario"

                    val imageUri = document.getString("imageUri")
                    Picasso.get().load(imageUri).into(findViewById<ImageView>(R.id.imgPerfil))

                    val visualizacoesPerfil = document.getLong("visualizacoes_perfil")
                    findViewById<TextView>(R.id.txtVisualizacoes).text =
                        "$visualizacoesPerfil"
                }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao buscar informações do usuário: $e")
            }
    }

    private fun atualizarVisualizacoesPerfil() {
        val userDocRef = db.collection("users").document(currentUser.uid)

        userDocRef.update("visualizacoes_perfil", FieldValue.increment(1))
            .addOnSuccessListener {
                Log.d("PerfilActivity", "Visualizações do perfil atualizadas com sucesso")
                exibirInformacoesUsuario()
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao atualizar as visualizações do perfil: $e")
            }
    }

    private fun atualizarNumeroFotosTiradas() {
        val userDocRef = db.collection("users").document(currentUser.uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val photoUrls = document.get("photoUrl") as? List<String>
                    val numeroFotosTiradas = photoUrls?.size ?: 0
                    findViewById<TextView>(R.id.txtFotosTiradas).text =
                        "$numeroFotosTiradas"
                }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao buscar as URLs das imagens: $e")
            }
    }

    private fun terminarSessao() {
        auth.signOut()
        startActivity(Intent(this, IndexActivity::class.java))
        finish()
    }

    private fun exibirFotosUsuario() {
        val userDocRef = db.collection("users").document(currentUser.uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val photoUrls = document.get("photoUrl") as? List<String>
                    photoUrls?.let {
                        exibirFotos(it)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao buscar as URLs das imagens: $e")
            }
    }

    private fun exibirFotos(fotos: List<String>) {
        recyclerView.adapter = FotosAdapter(fotos)
    }

    private fun exibirConquistasUsuario() {
        val userDocRef = db.collection("users").document(currentUser.uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val conquistas = document.get("conquistas") as? List<String>
                    conquistas?.let {
                        exibirConquistas(it)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao buscar conquistas do usuário: $e")
            }
    }

    private fun exibirConquistas(conquistas: List<String>) {
        recyclerView.adapter = ConquistasAdapter(conquistas)
    }

    class FotosAdapter(private val fotos: List<String>) :
        RecyclerView.Adapter<FotosAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_foto, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val fotoUrl = fotos[position]
            Picasso.get().load(fotoUrl).into(holder.imageView)
        }

        override fun getItemCount(): Int {
            return fotos.size
        }
    }

    class ConquistasAdapter(private val conquistas: List<String>) :
        RecyclerView.Adapter<ConquistasAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.textView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_conquista, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val conquista = conquistas[position]
            holder.textView.text = conquista
        }

        override fun getItemCount(): Int {
            return conquistas.size
        }
    }
}
