package com.jothivel.chits.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.R

import com.jothivel.chits.ui.theme.*

@Composable
fun AppBottomNavPager(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
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
    ) {
        data class NavItem(val iconRes: Int, val labelRes: Int)
        
        val items = listOf(
            NavItem(R.drawable.ic_home_app_logo, R.string.nav_home),
            NavItem(R.drawable.ic_book, R.string.nav_chits),
            NavItem(R.drawable.ic_group_add, R.string.members),
            NavItem(R.drawable.ic_checkbook, R.string.nav_ledger),
            NavItem(R.drawable.ic_settings, R.string.nav_settings)
        )
        
        items.forEachIndexed { index, item ->
            val isSelected = selectedIndex == index
            
            // Icon bounce when selected
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.12f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "navIconScale_$index"
            )
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        painterResource(id = item.iconRes), 
                        contentDescription = stringResource(id = item.labelRes), 
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
                    onTabSelected(index)
                }
            )
        }
    }
}
