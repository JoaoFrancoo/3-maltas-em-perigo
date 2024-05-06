package com.example.a3_maltas_em_perigo_n1

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object ConquistasManager {
    private const val TAG = "ConquistasManager"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Função para conceder uma conquista ao usuário quando ele tirar uma foto
    fun concederConquistaTirarFoto(context: Context) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Aqui você pode adicionar sua lógica para verificar se o usuário realmente tirou uma foto
            // Suponha que após o usuário tirar uma foto, você chame esta função para conceder a conquista
            // Exemplo de concessão da conquista:
            val conquista = hashMapOf(
                "nome" to "Fotógrafo Iniciante",
                "descrição" to "Conquistada ao tirar a primeira foto!",
                "imagem" to "https://exemplo.com/imagens/conquista_fotografo_iniciante.jpg"
            )

            // Adicionando a conquista ao documento do usuário no Firestore
            val userDocRef = db.collection("users").document(user.uid)
            userDocRef.update("conquistas", FieldValue.arrayUnion(conquista))
                .addOnSuccessListener {
                    Log.d(TAG, "Conquista concedida com sucesso: Fotógrafo Iniciante")
                    // Adicione qualquer lógica de interface do usuário necessária para refletir a nova conquista concedida
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro ao conceder conquista: ${e.message}")
                }
        } ?: run {
            Log.e(TAG, "Usuário não autenticado")
            // Adicione aqui qualquer lógica adicional para lidar com o caso em que o usuário não está autenticado
        }
    }
}
