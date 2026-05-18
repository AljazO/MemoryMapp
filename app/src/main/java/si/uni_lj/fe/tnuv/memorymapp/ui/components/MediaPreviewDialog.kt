package si.uni_lj.fe.tnuv.memorymapp.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaType

@Composable
fun MediaPreviewDialog(
    mediaPoint: MediaPoint,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (mediaPoint.type == MediaType.VIDEO) {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    setVideoURI(Uri.parse(mediaPoint.uri))
                                    val controller = MediaController(context)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { it.start() }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(mediaPoint.uri),
                            contentDescription = "Media Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}
