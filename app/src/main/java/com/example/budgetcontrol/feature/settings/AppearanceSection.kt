package com.example.budgetcontrol.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetcontrol.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSection(
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val themes = listOf(
                "light" to stringResource(R.string.theme_light),
                "dark" to stringResource(R.string.theme_dark)
            )

            val currentThemeName = themes.firstOrNull { it.first == currentTheme }?.second
                ?: stringResource(R.string.theme_light)

            var themeExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = !themeExpanded }
            ) {
                OutlinedTextField(
                    value = currentThemeName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false }
                ) {
                    themes.forEach { (tag, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onThemeChange(tag)
                                themeExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSection(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val languages = listOf(
                "en" to stringResource(R.string.language_english),
                "ru" to stringResource(R.string.language_russian)
            )

            val currentName = languages.firstOrNull { it.first == currentLanguage }?.second
                ?: stringResource(R.string.language_english)

            var langExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = langExpanded,
                onExpandedChange = { langExpanded = !langExpanded }
            ) {
                OutlinedTextField(
                    value = currentName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = langExpanded,
                    onDismissRequest = { langExpanded = false }
                ) {
                    languages.forEach { (tag, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onLanguageChange(tag)
                                langExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
