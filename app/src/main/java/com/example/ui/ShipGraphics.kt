package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun DrawScope.drawProceduralShip(shipId: String, center: Offset, size: Float, paintColor: Color) {
    val h = size / 2f
    val path = Path()

    // Primary bright core stroke and glow width
    val coreColor = Color.White
    val glowColor = paintColor.copy(alpha = 0.5f)

    when (shipId) {
        "solar_wing" -> {
            // Sleek basic triangle fighter
            path.moveTo(center.x, center.y - h) // Top nose
            path.lineTo(center.x - h * 0.8f, center.y + h) // Back Left wingtip
            path.lineTo(center.x - h * 0.3f, center.y + h * 0.5f) // Back central recess
            path.lineTo(center.x + h * 0.3f, center.y + h * 0.5f)
            path.lineTo(center.x + h * 0.8f, center.y + h) // Back Right wingtip
            path.close()
        }
        "quantum_falcon" -> {
            // Delta wings with dual forward mandibles
            path.moveTo(center.x - h * 0.15f, center.y - h) // Left nose mandible
            path.lineTo(center.x - h * 0.15f, center.y - h * 0.3f)
            path.lineTo(center.x - h * 0.85f, center.y + h * 0.7f) // Wing Left
            path.lineTo(center.x, center.y + h * 0.3f) // Exhaust recess
            path.lineTo(center.x + h * 0.85f, center.y + h * 0.7f) // Wing Right
            path.lineTo(center.x + h * 0.15f, center.y - h * 0.3f)
            path.lineTo(center.x + h * 0.15f, center.y - h) // Right nose mandible
            path.close()
        }
        "nova_phantom" -> {
            // Smooth curved crescent boomerang
            path.moveTo(center.x, center.y - h * 0.9f)
            path.quadraticBezierTo(
                center.x - h * 0.9f, center.y - h * 0.3f,
                center.x - h * 0.95f, center.y + h * 0.8f
            ) // Left tip
            path.quadraticBezierTo(
                center.x, center.y + h * 0.2f,
                center.x + h * 0.95f, center.y + h * 0.8f
            ) // Right tip
            path.quadraticBezierTo(
                center.x + h * 0.9f, center.y - h * 0.3f,
                center.x, center.y - h * 0.9f
            )
            path.close()
        }
        "eclipse_runner" -> {
            // Elongated dart with solar side arrays
            path.moveTo(center.x, center.y - h * 1.1f) // Extended nose
            path.lineTo(center.x - h * 0.2f, center.y + h * 0.6f)
            path.lineTo(center.x - h * 0.9f, center.y + h * 0.1f) // Left fin array
            path.lineTo(center.x - h * 0.8f, center.y + h * 0.4f)
            path.lineTo(center.x - h * 0.2f, center.y + h * 0.8f) // Back Left
            path.lineTo(center.x, center.y + h * 1.0f) // Thruster
            path.lineTo(center.x + h * 0.2f, center.y + h * 0.8f) // Back Right
            path.lineTo(center.x + h * 0.8f, center.y + h * 0.4f)
            path.lineTo(center.x + h * 0.9f, center.y + h * 0.1f) // Right fin array
            path.lineTo(center.x + h * 0.2f, center.y + h * 0.6f)
            path.close()
        }
        "void_hunter" -> {
            // Star X cross chassis
            path.moveTo(center.x, center.y - h * 0.4f) // Central hub top
            path.lineTo(center.x - h * 0.9f, center.y - h * 0.9f) // Forward Left tip
            path.lineTo(center.x - h * 0.4f, center.y) // LHS middle
            path.lineTo(center.x - h * 0.9f, center.y + h * 0.9f) // Rear Left tip
            path.lineTo(center.x, center.y + h * 0.4f) // Central hub bottom
            path.lineTo(center.x + h * 0.9f, center.y + h * 0.9f) // Rear Right tip
            path.lineTo(center.x + h * 0.4f, center.y) // RHS middle
            path.lineTo(center.x + h * 0.9f, center.y - h * 0.9f) // Forward Right tip
            path.close()
        }
        "stellar_dragon" -> {
            // Sharp dragon wings with spiked crests
            path.moveTo(center.x, center.y - h * 1.0f) // Spiked nose
            path.lineTo(center.x - h * 0.25f, center.y - h * 0.2f)
            path.lineTo(center.x - h * 0.95f, center.y + h * 0.3f) // Wing left wingtip
            path.lineTo(center.x - h * 0.45f, center.y + h * 0.4f) // Wing spike
            path.lineTo(center.x - h * 0.6f, center.y + h * 0.95f) // Tail spike left
            path.lineTo(center.x, center.y + h * 0.65f) // Tail center
            path.lineTo(center.x + h * 0.6f, center.y + h * 0.95f) // Tail spike right
            path.lineTo(center.x + h * 0.45f, center.y + h * 0.4f)
            path.lineTo(center.x + h * 0.95f, center.y + h * 0.3f) // Wing right wingtip
            path.lineTo(center.x + h * 0.25f, center.y - h * 0.2f)
            path.close()
        }
        "prism_knight" -> {
            // Highly faceted gem octagon shape
            path.moveTo(center.x, center.y - h)
            path.lineTo(center.x - h * 0.65f, center.y - h * 0.5f)
            path.lineTo(center.x - h * 0.85f, center.y + h * 0.2f)
            path.lineTo(center.x - h * 0.45f, center.y + h * 0.95f)
            path.lineTo(center.x, center.y + h * 0.65f)
            path.lineTo(center.x + h * 0.45f, center.y + h * 0.95f)
            path.lineTo(center.x + h * 0.85f, center.y + h * 0.2f)
            path.lineTo(center.x + h * 0.65f, center.y - h * 0.5f)
            path.close()
        }
        "nebula_wraith" -> {
            // Asymmetric twin-hull strike fighter
            path.moveTo(center.x - h * 0.25f, center.y - h) // Left main gun
            path.lineTo(center.x - h * 0.55f, center.y + h * 0.3f)
            path.lineTo(center.x - h * 0.85f, center.y + h * 0.85f) // Left stabilizer
            path.lineTo(center.x - h * 0.1f, center.y + h * 0.5f)
            path.lineTo(center.x + h * 0.45f, center.y - h * 0.3f) // Right offset gun
            path.lineTo(center.x + h * 0.8f, center.y + h * 0.85f)
            path.close()
        }
        "crystal_titan" -> {
            // Thick wedge armored fortress
            path.moveTo(center.x - h * 0.1f, center.y - h)
            path.lineTo(center.x + h * 0.1f, center.y - h)
            path.lineTo(center.x + h * 1.0f, center.y + h * 0.7f) // Ultra wide right flange
            path.lineTo(center.x + h * 0.4f, center.y + h)
            path.lineTo(center.x - h * 0.4f, center.y + h)
            path.lineTo(center.x - h * 1.0f, center.y + h * 0.7f) // Ultra wide left flange
            path.close()
        }
        else -> {
            // Custom majestic Omega crescent ring shape
            path.moveTo(center.x, center.y - h)
            path.quadraticBezierTo(
                center.x - h * 1.1f, center.y - h * 0.5f,
                center.x - h * 1.0f, center.y + h * 0.8f
            )
            path.lineTo(center.x - h * 0.6f, center.y + h * 0.4f)
            path.quadraticBezierTo(
                center.x, center.y - h * 0.1f,
                center.x + h * 0.6f, center.y + h * 0.4f
            )
            path.lineTo(center.x + h * 1.0f, center.y + h * 0.8f)
            path.quadraticBezierTo(
                center.x + h * 1.1f, center.y - h * 0.5f,
                center.x, center.y - h
            )
            path.close()
        }
    }

    // Render underlying thick neon aura glow
    drawPath(
        path = path,
        color = glowColor,
        style = Stroke(width = 8.dp.toPx())
    )

    // Render primary bright solid outline
    drawPath(
        path = path,
        color = paintColor,
        style = Stroke(width = 3.dp.toPx())
    )

    // Render high-intensity core highlights
    drawPath(
        path = path,
        color = coreColor,
        style = Stroke(width = 1.dp.toPx())
    )

    // Add engine thruster flame
    val flamePath = Path()
    flamePath.moveTo(center.x - 12f, center.y + h * 0.65f)
    flamePath.lineTo(center.x, center.y + h * 1.25f + (0..15).random().toFloat())
    flamePath.lineTo(center.x + 12f, center.y + h * 0.65f)
    flamePath.close()

    drawPath(
        path = flamePath,
        color = PaintColorMapForFire(shipId).copy(alpha = 0.8f)
    )
}

fun PaintColorMapForFire(id: String): Color {
    return when (id) {
        "solar_wing" -> Color(0xFFFF2E63)
        "quantum_falcon" -> Color(0xFF00ADB5)
        "nova_phantom" -> Color(0xFF10B981)
        "eclipse_runner" -> Color(0xFFFBBF24)
        "void_hunter" -> Color(0xFFBD00FF)
        "stellar_dragon" -> Color(0xFFFF5252)
        "prism_knight" -> Color(0xFF2979FF)
        "nebula_wraith" -> Color(0xFFF50057)
        "crystal_titan" -> Color(0xFF00E5FF)
        else -> Color.White
    }
}
