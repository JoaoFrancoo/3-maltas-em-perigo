package com.example.a3_maltas_em_perigo_n1

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

class Social : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var bottomNavigationHandler: BottomNavigationHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.social)

        // Inicialize o BottomNavigationHandler passando o contexto e a BottomNavigationView
        bottomNavigationHandler = BottomNavigationHandler(this, findViewById(R.id.bottomNavigationView))

        // Configurar a navegação com os IDs dos itens do menu
        bottomNavigationHandler.setupWithNavigation(R.id.navigation_camera, R.id.navigation_profile, R.id.navigation_feed)

        // Consulta ao Firestore para buscar os dados dos usuários
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                // Exibir as fotos dos usuários
                for (document in result.documents) {
                    val photoUrls = document["photoUrl"] as List<String>?
                    val userName = document["name"] as String? // Supondo que "name" seja o nome da chave que armazena os nomes dos usuários
                    val userProfilePhotoUrl = document["imageUri"] as String? // Supondo que "imageUri" seja a chave que armazena a URL da foto de perfil dos usuários
                    val userId = document.id // Obtém o ID do usuário

                    photoUrls?.forEach { photoUrl ->
                        Log.d("PhotoURL", photoUrl)

                        // Criar um CardView para envolver todas as informações do usuário e a imagem postada
                        val cardView = CardView(this)
                        val cardViewLayoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        cardViewLayoutParams.setMargins(0, 0, 0, 16) // Adiciona margem inferior
                        cardView.layoutParams = cardViewLayoutParams
                        cardView.cardElevation = 4f // Define a elevação do CardView

                        // Layout principal dentro do CardView
                        val layout = LinearLayout(this)
                        layout.orientation = LinearLayout.VERTICAL
                        val layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        layoutParams.setMargins(16, 16, 16, 16) // Adiciona margem
                        layout.layoutParams = layoutParams

                        // Layout para imagem de perfil e nome de usuário
                        val userProfileLayout = LinearLayout(this)
                        userProfileLayout.orientation = LinearLayout.HORIZONTAL
                        val userProfileLayoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        userProfileLayoutParams.setMargins(0, 0, 0, 16) // Adiciona margem inferior
                        userProfileLayout.layoutParams = userProfileLayoutParams

                        // Adicionar a imagem de perfil do usuário
                        val userProfileImageView = ImageView(this)
                        val userProfileImageParams = LinearLayout.LayoutParams(
                            150, // Ajuste o tamanho da imagem de perfil conforme necessário
                            150
                        )
                        userProfileImageView.layoutParams = userProfileImageParams
                        Picasso.get().load(userProfilePhotoUrl).transform(CircleTransformation()).into(userProfileImageView) // Carrega a imagem de perfil e aplica a transformação
                        userProfileImageView.setOnClickListener {

                            val userId= document.id

                            val intent = Intent(this@Social, PerfilActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                        }
                        userProfileLayout.addView(userProfileImageView)

                        // Adicionar o nome de usuário
                        val usernameTextView = TextView(this)
                        usernameTextView.text = userName
                        val usernameLayoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        usernameLayoutParams.setMargins(16, 0, 0, 0) // Adiciona margem à esquerda
                        usernameTextView.layoutParams = usernameLayoutParams
                        usernameTextView.setOnClickListener {
                            val intent = Intent(this@Social, PerfilActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                        }
                        userProfileLayout.addView(usernameTextView)

                        layout.addView(userProfileLayout)

                        // Adicionar a imagem postada
                        val imageView = ImageView(this)
                        val imageParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        imageView.layoutParams = imageParams
                        Picasso.get().load(photoUrl).into(imageView) // Carrega a imagem postada
                        layout.addView(imageView)

                        cardView.addView(layout) // Adicione o layout principal ao CardView
                        val linearLayout = findViewById<LinearLayout>(R.id.linear_layout)
                        linearLayout.addView(cardView) // Adicione o CardView ao layout principal

                    }
                }
            }
            .addOnFailureListener { exception ->
                // Lidar com falhas na consulta
                Log.e("SocialActivity", "Erro ao buscar fotos: $exception")
            }
    }

    // Classe CircleTransformation igual ao que você já tinha implementado
    private class CircleTransformation : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = Math.min(source.width, source.height)
            val x = (source.width - size) / 2
            val y = (source.height - size) / 2
            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }
            val bitmap = Bitmap.createBitmap(size, size, source.config)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            paint.shader = shader
            paint.isAntiAlias = true
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)
            squaredBitmap.recycle()
            return bitmap
        }

        override fun key(): String {
            return "circle"
        }

    }
}
