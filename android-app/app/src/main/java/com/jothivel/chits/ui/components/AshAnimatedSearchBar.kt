@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.jothivel.chits.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.MaroonPrimary

private val SearchAsh = Color(0xFFECEDEF)
private val SearchAshBorder = Color(0xFFD3D5D8)
private val SearchText = Color(0xFF303236)

/** Native Compose version of the expanding search pattern supplied by the client. */
@Composable
fun AshAnimatedSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(value.isNotBlank()) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width by animateDpAsState(
            targetValue = if (expanded) maxWidth else 104.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "ashSearchWidth"
        )

        Row(
            modifier = Modifier
                .width(width)
                .height(44.dp)
                .background(SearchAsh, RoundedCornerShape(22.dp))
                .border(1.dp, if (expanded) MaroonPrimary.copy(alpha = .35f) else SearchAshBorder, RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = MaroonPrimary)
                } else {
                    Icon(Icons.Default.Search, "Search", tint = MaroonPrimary, modifier = Modifier.size(19.dp))
                }
            }

            if (!expanded) {
                Text("Search", color = SearchText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }

            AnimatedVisibility(expanded, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(color = SearchText, fontSize = 13.sp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) Text(placeholder, color = Color(0xFF7D8085), fontSize = 12.sp)
                            inner()
                        }
                    }
                )
            }

            AnimatedVisibility(expanded) {
                IconButton(onClick = {
                    if (value.isNotEmpty()) onValueChange("") else {
                        expanded = false
                        keyboard?.hide()
                    }
                }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, if (value.isEmpty()) "Close search" else "Clear search", tint = Color(0xFF686B70), modifier = Modifier.size(17.dp))
                }
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}
