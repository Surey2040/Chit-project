package com.jothivel.chits.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jothivel.chits.ui.theme.MaroonPrimary
import com.jothivel.chits.ui.theme.MaroonSurfaceLight

/**
 * Standard anchored dropdown picker - tap the field, the option list expands directly
 * underneath it (Material's usual ExposedDropdownMenu open/close animation), instead of a
 * full-screen sheet sliding up from the bottom. Used in place of ad-hoc DropdownMenu triggers
 * across the app so every dropdown/selector field opens the same way: field's label as a bold
 * header, an optional search box for long lists, and a radio-style option list - tapping an
 * option selects it and closes the menu.
 *
 * This generic overload is for picking a domain object (e.g. ChitGroupEntity, MemberEntity,
 * ChitTemplate) where the display text differs from the value you actually need. [optionKey]
 * must return a stable id used only to compare options for the selected/radio state (it is
 * never shown). For simple String-option pickers (payment mode, gender, status, …) use the
 * other [BottomSheetPickerField] overload below instead.
 *
 * The closed/collapsed trigger defaults to the outlined-field look already used for dropdowns
 * in this app (label above, bordered box, trailing arrow). Pass [trigger] to keep a call site's
 * existing bespoke trigger visual (e.g. a PremiumInputField, a header title, a filter chip)
 * while still switching the opening interaction over to this anchored menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheetPickerField(
    label: String,
    options: List<T>,
    selectedOption: T?,
    optionKey: (T) -> String,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select ${label.trim().trimEnd('*').trim()}",
    optionSubLabel: ((T) -> String)? = null,
    searchableText: ((T) -> String)? = null,
    showSearch: Boolean = options.size > 6,
    enabled: Boolean = true,
    emptyMessage: String = "No matching results",
    trigger: (@Composable (displayText: String, onClick: () -> Unit) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val displayText = selectedOption?.let(optionLabel).orEmpty()

    fun dismiss() {
        expanded = false
        query = ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        Box(Modifier.menuAnchor()) {
            if (trigger != null) {
                trigger(displayText) { if (enabled) expanded = true }
            } else {
                DefaultPickerTrigger(
                    label = label,
                    displayText = displayText,
                    placeholder = placeholder,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { expanded = true }
                )
            }
        }

        val filtered = remember(options, query) {
            if (query.isBlank()) options
            else options.filter { (searchableText ?: optionLabel)(it).contains(query.trim(), ignoreCase = true) }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { dismiss() },
            modifier = Modifier.widthIn(min = 220.dp)
        ) {
            // Light card matching the app's own white background - kept the staggered
            // per-option entrance animation (fade + slide-in + scale) from the reference
            // dropdown, just dropped the dark "glass" color scheme for a plain elevated
            // white card with a soft shadow, consistent with the rest of the app's look.
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEDEDED)),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp
            ) {
                Column {
                    Text(
                        text = label.trim().trimEnd('*').trim(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaroonPrimary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                    if (showSearch) {
                        // No explicit height here - forcing OutlinedTextField below its own
                        // minimum intrinsic height clipped the field's content. Kept it compact
                        // via smaller text/icon/padding instead, letting the field size itself.
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search...", color = Color.Gray, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp)) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaroonPrimary,
                                unfocusedBorderColor = Color(0xFF9E9E9E)
                            )
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    if (filtered.isEmpty()) {
                        Text(
                            emptyMessage,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                        )
                    } else {
                        // Plain, always-rendered rows - an earlier version staggered each row's
                        // entrance with a per-index delayed LaunchedEffect, which was fragile
                        // (Compose can reuse a loop slot for a different option across
                        // recompositions when the filtered list's size/order changes while
                        // typing, occasionally leaving a freshly-filtered row stuck invisible).
                        // Search must never silently show nothing, so this list has no
                        // animation gating its visibility - only the menu's own open/close does.
                        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(bottom = 6.dp)) {
                            filtered.forEach { option ->
                                val selected = selectedOption != null && optionKey(option) == optionKey(selectedOption)
                                val interactionSource = remember(optionKey(option)) { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                val rowBg by animateColorAsState(
                                    targetValue = when {
                                        pressed -> MaroonSurfaceLight
                                        selected -> MaroonSurfaceLight.copy(alpha = .6f)
                                        else -> Color.Transparent
                                    },
                                    label = "menuRowBg"
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 5.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(rowBg)
                                        .clickable(interactionSource = interactionSource, indication = null) { onOptionSelected(option); dismiss() }
                                        .padding(horizontal = 9.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            optionLabel(option),
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) MaroonPrimary else Color(0xFF2A2A2A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        optionSubLabel?.let { sub ->
                                            Text(sub(option), fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (selected) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaroonPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple String-option overload for pickers like Payment Mode, Gender, Status,
 * Branch, etc. where the display text and the underlying value are the same.
 */
@Composable
fun BottomSheetPickerField(
    label: String,
    options: List<String>,
    selectedValue: String?,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select ${label.trim().trimEnd('*').trim()}",
    showSearch: Boolean = options.size > 6,
    enabled: Boolean = true,
    emptyMessage: String = "No matching results",
    trigger: (@Composable (displayText: String, onClick: () -> Unit) -> Unit)? = null
) {
    BottomSheetPickerField(
        label = label,
        options = options,
        selectedOption = selectedValue,
        optionKey = { it },
        optionLabel = { it },
        onOptionSelected = onValueSelected,
        modifier = modifier,
        placeholder = placeholder,
        showSearch = showSearch,
        enabled = enabled,
        emptyMessage = emptyMessage,
        trigger = trigger
    )
}

// ─── Default closed-state trigger ──────────────────────────────────────────
// Matches the existing outlined dropdown-field look used across the app
// (see AddMemberActivity's DropdownField/group selector): gray label above,
// a bordered box holding a disabled OutlinedTextField with a trailing arrow.
@Composable
private fun DefaultPickerTrigger(
    label: String,
    displayText: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = false,
                placeholder = { Text(placeholder, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color(0xFFBDBDBD),
                    disabledPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray) }
            )
        }
    }
}
