package com.example.a3_maltas_em_perigo_n1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var fotosAdapter: FotosAdapter
    private lateinit var conquistasAdapter: ConquistasAdapter
    private lateinit var buttonFotos: Button
    private lateinit var buttonConquistas: Button
    private var viewedUserId: String? = null
    private lateinit var bottomNavigationHandler: BottomNavigationHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Initialize Firebase Auth
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser!!

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewFotos)
        buttonFotos = findViewById(R.id.btnFotos)
        buttonConquistas = findViewById(R.id.btnConquistas)

        // Initialize adapters
        fotosAdapter = FotosAdapter(emptyList())
        conquistasAdapter = ConquistasAdapter(emptyList())

        val layoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = layoutManager

        buttonFotos.setOnClickListener {
            Log.d("PerfilActivity", "Clicou no botão Fotos")
            exibirFotosUsuario(viewedUserId)
        }
        buttonConquistas.setOnClickListener {
            Log.d("PerfilActivity", "Clicou no botão Conquistas")
            exibirConquistasUsuario(viewedUserId)
        }

        // Verificar se um userId foi passado como extra
        val userId = intent.getStringExtra("userId")
        viewedUserId = userId ?: currentUser.uid

        // Display user information
        exibirInformacoesUsuario(viewedUserId!!)
        exibirFotosUsuario(viewedUserId!!)
        exibirConquistasUsuario(viewedUserId!!)
        atualizarVisualizacoesPerfil(viewedUserId!!)

        // Seguir button logic
        setupSeguirButton(viewedUserId!!)
    }

    override fun onResume() {
        super.onResume()
        atualizarNumeroFotosTiradas(viewedUserId!!)
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
                editarPerfil()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSeguirButton(userId: String) {
        val btnSeguir = findViewById<Button>(R.id.btnSeguir)
        val txtSeguidores = findViewById<TextView>(R.id.txtSeguidores)

        if (userId != currentUser.uid) {
            btnSeguir.visibility = View.VISIBLE

            val userDocRef = db.collection("users").document(userId)
            userDocRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val seguidores = document.get("seguidores") as? List<String> ?: emptyList()
                    val jaSegue = seguidores.contains(currentUser.uid)

                    if (jaSegue) {
                        btnSeguir.text = "Seguindo"
                        btnSeguir.isEnabled = false
                    } else {
                        btnSeguir.text = "Seguir"
                        btnSeguir.isEnabled = true
                    }

                    // Atualiza o número de seguidores
                    txtSeguidores.text = "Seguidores: ${seguidores.size}"
                }
            }

            btnSeguir.setOnClickListener {
                userDocRef.update("seguidores", FieldValue.arrayUnion(currentUser.uid))
                    .addOnSuccessListener {
                        Log.d("PerfilActivity", "Usuário seguido com sucesso")
                        btnSeguir.text = "Seguindo"
                        btnSeguir.isEnabled = false

                        // Atualiza o número de seguidores
                        txtSeguidores.text = "Seguidores: ${txtSeguidores.text.split(": ")[1].toInt() + 1}"
                    }
                    .addOnFailureListener { e ->
                        Log.e("PerfilActivity", "Erro ao seguir usuário: $e")
                    }
            }
        } else {
            btnSeguir.visibility = View.GONE
        }
    }

    private fun exibirInformacoesUsuario(userId: String) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nomeUsuario = document.getString("name")
                    val emailUsuario = document.getString("email")
                    findViewById<TextView>(R.id.txtNome).text = "Nome: $nomeUsuario"
                    findViewById<TextView>(R.id.txtEmail).text = "Email: $emailUsuario"

                    val imageUri = document.getString("imageUri")
                    Picasso.get().load(imageUri).into(findViewById<ImageView>(R.id.imgPerfil))

                    val visualizacoesPerfil = document.getLong("visualizacoes_perfil") ?: 0
                    findViewById<TextView>(R.id.txtVisualizacoes).text = "$visualizacoesPerfil"
                } else {
                    Log.e("PerfilActivity", "Documento do usuário não encontrado")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao buscar informações do usuário: $e")
            }
    }

    private fun atualizarVisualizacoesPerfil(userId: String) {
        if (userId == currentUser.uid) return

        val userDocRef = db.collection("users").document(userId)
        val profileViewsRef = userDocRef.collection("profileViews").document(currentUser.uid)

        profileViewsRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    profileViewsRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                        .addOnSuccessListener {
                            userDocRef.update("visualizacoes_perfil", FieldValue.increment(1))
                                .addOnSuccessListener {
                                    Log.d("PerfilActivity", "Visualizações do perfil atualizadas com sucesso")
                                    exibirInformacoesUsuario(userId)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("PerfilActivity", "Erro ao atualizar as visualizações do perfil: $e")
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("PerfilActivity", "Erro ao registrar visualização do perfil: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilActivity", "Erro ao verificar visualização do perfil: $e")
            }
    }

    private fun atualizarNumeroFotosTiradas(userId: String) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val photoUrls = document.get("photoUrl") as? List<String>
                    val numeroFotosTiradas = photoUrls?.size ?: 0
                    findViewById<TextView>(R.id.txtFotosTiradas).text = "$numeroFotosTiradas"
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

    private fun editarPerfil() {
        startActivity(Intent(this, AtualizarActivity::class.java))
    }

    private fun exibirFotosUsuario(userId: String? = null) {
        val userDocRef = if (userId != null) {
            db.collection("users").document(userId)
        } else {
            db.collection("users").document(currentUser.uid)
        }

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

    private fun exibirConquistasUsuario(userId: String? = null) {
        val userDocRef = if (userId != null) {
            db.collection("users").document(userId)
        } else {
            db.collection("users").document(currentUser.uid)
        }

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
