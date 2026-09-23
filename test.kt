import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin

fun test() {
    Modifier.graphicsLayer {
        scaleX = 1f
        scaleY = 1f
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}
