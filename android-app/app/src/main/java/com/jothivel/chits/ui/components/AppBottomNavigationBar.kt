package com.jothivel.chits.ui.components

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R
import com.jothivel.chits.ui.dashboard.DashboardActivity
import com.jothivel.chits.ui.groups.GroupListActivity
import com.jothivel.chits.ui.ledger.LedgerActivity
import com.jothivel.chits.ui.members.AddMemberActivity
import com.jothivel.chits.ui.settings.SettingsActivity
import com.jothivel.chits.ui.theme.JothiVelChitsTheme
import com.jothivel.chits.ui.theme.*

@Composable
fun AppBottomNavigationBar(currentRoute: String) {
    val context = LocalContext.current
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        unselectedIconColor = Color.White.copy(alpha = 0.5f),
        selectedTextColor = Color.White,
        unselectedTextColor = Color.White.copy(alpha = 0.5f),
        indicatorColor = MaroonDark // Use solid dark maroon to avoid M3 alpha-stripping bug
    )

    val glassColor = MaroonPrimary.copy(alpha = 0.95f)

    NavigationBar(
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .border(1.dp, Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp)),
        containerColor = glassColor,
        tonalElevation = 8.dp
    ) {
        data class NavItem(val route: String, val iconRes: Int, val labelRes: Int, val targetClass: Class<*>)
        
        val items = listOf(
            NavItem("Dashboard", R.drawable.ic_home_app_logo, R.string.nav_home, DashboardActivity::class.java),
            NavItem("Chits", R.drawable.ic_book, R.string.nav_chits, GroupListActivity::class.java),
            NavItem("Members", R.drawable.ic_group_add, R.string.members, AddMemberActivity::class.java),
            NavItem("Ledger", R.drawable.ic_checkbook, R.string.nav_ledger, LedgerActivity::class.java),
            NavItem("Settings", R.drawable.ic_settings, R.string.nav_settings, SettingsActivity::class.java)
        )
        
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            
            // Icon bounce when selected
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.12f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "navIconScale_${item.route}"
            )
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        painterResource(id = item.iconRes), 
                        contentDescription = item.route, 
                        modifier = Modifier.size(24.dp).scale(iconScale)
                    ) 
                },
                label = { 
                    Text(
                        stringResource(id = item.labelRes),
                        fontSize = if (isSelected) 11.sp else 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                selected = isSelected,
                colors = navColors,
                onClick = {
                    if (currentRoute != item.route) {
                        val intent = Intent(context, item.targetClass)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        context.startActivity(intent)
                        // Apply smooth transition
                        if (context is Activity) {
                            SmoothTransitions.applyEnterTransition(context)
                        }
                    }
                }
            )
        }
    }
}

fun setupAppBottomNavigation(composeView: ComposeView, currentRoute: String) {
    composeView.setContent {
        JothiVelChitsTheme {
            AppBottomNavigationBar(currentRoute = currentRoute)
        }
    }
}
