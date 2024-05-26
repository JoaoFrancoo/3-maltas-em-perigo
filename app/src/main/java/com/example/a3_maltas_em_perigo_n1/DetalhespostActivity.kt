package com.example.a3_maltas_em_perigo_n1

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import java.util.Date

class Detalhespost : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalhespost)

        val imageViewUser = findViewById<ImageView>(R.id.imageViewUser)
        val imageViewPost = findViewById<ImageView>(R.id.imageViewPost)
        val textViewUserName = findViewById<TextView>(R.id.textViewUserName)
        val textViewTimestamp = findViewById<TextView>(R.id.textViewTimestamp)
        val textViewObjectName = findViewById<TextView>(R.id.textViewObjectName)
        val textViewAdditionalInfo = findViewById<TextView>(R.id.textViewAdditionalInfo)

        val userId = intent.getStringExtra("userId")
        val userName = intent.getStringExtra("userName")
        val userProfileImageUrl = intent.getStringExtra("userProfileImageUrl")
        val timestamp = intent.getLongExtra("timestamp", 0)
        val objectName = intent.getStringExtra("objectName")
        val postImageUrl = intent.getStringExtra("postImageUrl")
        val additionalInfo = intent.getStringExtra("additionalInfo")

        // Carrega a imagem de perfil do usuário
        if (userProfileImageUrl != null) {
            Picasso.get().load(userProfileImageUrl).into(imageViewUser)
        }

        // Carrega a imagem do post
        if (postImageUrl != null) {
            Picasso.get().load(postImageUrl).into(imageViewPost)
        }

        // Exibe o nome do usuário
        if (userName != null) {
            textViewUserName.text = userName
        }

        // Exibe o timestamp
        if (timestamp != 0L) {
            val timestampString = DateFormat.format("dd/MM/yyyy HH:mm:ss", Date(timestamp)).toString()
            textViewTimestamp.text = timestampString
        }

        // Exibe o nome do objeto
        if (objectName != null) {
            textViewObjectName.text = objectName
        }

        // Exibe informações adicionais
        if (additionalInfo != null) {
            textViewAdditionalInfo.text = additionalInfo
        }
    }
}
