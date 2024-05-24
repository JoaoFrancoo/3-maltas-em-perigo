package com.example.a3_maltas_em_perigo_n1

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationHandler(private val context: Context, private val bottomNavigationView: BottomNavigationView) {

    fun setupWithNavigation(
        @IdRes cameraItemId: Int,
        @IdRes profileItemId: Int,
        @IdRes feedItemId: Int,
        @IdRes notificationItemId: Int
    ) {
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                cameraItemId -> {
                    navigateToActivity(DetectarImagem::class.java)
                    true
                }
                profileItemId -> {
                    navigateToActivity(PerfilActivity::class.java)
                    true
                }
                feedItemId -> {
                    navigateToActivity(Social::class.java)
                    true
                }
                notificationItemId -> {
                    navigateToActivity(notificacao::class.java)
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        context.startActivity(intent)
    }
}
