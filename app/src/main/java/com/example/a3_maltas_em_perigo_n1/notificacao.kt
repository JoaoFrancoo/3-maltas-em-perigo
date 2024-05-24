package com.example.a3_maltas_em_perigo_n1

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class notificacao : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textNoNotifications: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notificacao)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        textNoNotifications = findViewById(R.id.textNoNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter()
        recyclerView.adapter = notificationAdapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val user = auth.currentUser
        user?.let {
            db.collection("users").document(it.uid).collection("notifications")
                .get()
                .addOnSuccessListener { documents ->
                    val notifications = documents.map { doc -> doc.getString("message") ?: "" }
                    if (notifications.isEmpty()) {
                        textNoNotifications.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        textNoNotifications.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        notificationAdapter.setNotifications(notifications)
                    }
                }
        }
    }
}
