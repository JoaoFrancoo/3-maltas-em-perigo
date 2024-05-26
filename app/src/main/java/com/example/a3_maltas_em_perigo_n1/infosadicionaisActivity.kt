package com.example.a3_maltas_em_perigo_n1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class infosadicionais : AppCompatActivity() {

    private lateinit var tvObjectName: TextView
    private lateinit var etAdditionalInfo: EditText
    private lateinit var btnSubmit: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.infosadicionais)

        tvObjectName = findViewById(R.id.tvObjectName)
        etAdditionalInfo = findViewById(R.id.etAdditionalInfo)
        btnSubmit = findViewById(R.id.btnSubmit)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val resultText = intent.getStringExtra("RESULT_TEXT")
        tvObjectName.text = "Objeto encontrado: $resultText"


        btnSubmit.setOnClickListener {
            val additionalInfo = etAdditionalInfo.text.toString()
            if (additionalInfo.isNotBlank()) {
                saveAdditionalInfo(resultText ?: "", additionalInfo)
            } else {
                Toast.makeText(this, "Por favor, forneça informações adicionais.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAdditionalInfo(resultText: String, additionalInfo: String) {
        val user = auth.currentUser
        val photoUrl = intent.getStringExtra("PHOTO_URL")
        if (user != null) {
            val infoMap = hashMapOf(
                "objectName" to resultText,
                "photoUrl" to photoUrl,
                "additionalInfo" to additionalInfo,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("users").document(user.uid).collection("infos")
                .add(infoMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Informações salvas com sucesso.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao salvar informações: $e", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
