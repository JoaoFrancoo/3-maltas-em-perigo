package com.example.a3_maltas_em_perigo_n1

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationHandler(
    private val context: Context,
    private val bottomNavigationView: BottomNavigationView
) {

    fun setupWithNavigation(
        @IdRes cameraItemId: Int,
        @IdRes profileItemId: Int,
        @IdRes feedItemId: Int,
        @IdRes notificationItemId: Int
    ) {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            val currentActivity = context as AppCompatActivity
            when (menuItem.itemId) {
                cameraItemId -> {
                    if (currentActivity !is DetectarImagem) {
                        navigateToActivity(DetectarImagem::class.java)
                    }
                    true
                }
                profileItemId -> {
                    if (currentActivity !is PerfilActivity) {
                        navigateToActivity(PerfilActivity::class.java)
                    }
                    true
                }
                feedItemId -> {
                    if (currentActivity !is Social) {
                        navigateToActivity(Social::class.java)
                    }
                    true
                }
                notificationItemId -> {
                    if (currentActivity !is notificacao) {
                        navigateToActivity(notificacao::class.java)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    fun updateSelectedItem(@IdRes selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId
    }
}
