package com.example.homeaccountingapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CategoryButton(
    text: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientButton(
        text = text,
        gradient = Brush.horizontalGradient(colors),
        modifier = modifier.padding(vertical = 8.dp),
        onClick = onClick
    )
}
