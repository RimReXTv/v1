package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.RichOnyx
import java.security.MessageDigest

@Composable
fun CyberQrCode(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 180,
    glowColor: Color = NeonTeal
) {
    // Generate a secure, deterministic grid (17x17) based on the SHA-256 of the address
    val hashBytes = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
    val grid = Array(17) { BooleanArray(17) }

    // Constants for QR Finder patterns at corners (Top-Left, Top-Right, Bottom-Left)
    for (x in 0 until 17) {
        for (y in 0 until 17) {
            // Corner finders
            val isFinder = (x < 5 && y < 5) || (x > 11 && y < 5) || (x < 5 && y > 11)
            if (isFinder) {
                // Outer ring
                val isOuter = (x == 0 || x == 4 || y == 0 || y == 4) && x < 5 && y < 5 ||
                              (x == 12 || x == 16 || y == 0 || y == 4) && x > 11 && y < 5 ||
                              (x == 0 || x == 4 || y == 12 || y == 16) && x < 5 && y > 11
                // Inner solid square
                val isInner = (x in 1..3 && y in 1..3) || (x in 13..15 && y in 1..3) || (x in 1..3 && y in 13..15)
                
                grid[x][y] = isOuter || isInner
            } else {
                // Deterministic dot mapping from hash bytes
                val byteIndex = (x * 17 + y) % hashBytes.size
                val bitOffset = (x * 17 + y) % 8
                val value = (hashBytes[byteIndex].toInt() ushr bitOffset) and 1
                grid[x][y] = value == 1
            }
        }
    }

    // Draw the generated matrix using canvas
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(RichOnyx, RoundedCornerShape(12.dp))
            .border(1.5.dp, glowColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numCells = 17
            val cellSize = size.width / numCells

            for (x in 0 until numCells) {
                for (y in 0 until numCells) {
                    if (grid[x][y]) {
                        drawRect(
                            color = glowColor,
                            topLeft = Offset(x * cellSize + 1f, y * cellSize + 1f),
                            size = Size(cellSize - 2f, cellSize - 2f)
                        )
                    }
                }
            }
        }
    }
}
