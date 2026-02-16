package com.baijum.ukufretboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.domain.ChordImageSharer
import com.baijum.ukufretboard.domain.ChordVoicing
import kotlinx.coroutines.launch

/**
 * Data class bundling all information needed to display the share bottom sheet.
 *
 * @param voicing The chord voicing to share.
 * @param chordName The display name (e.g., "Am7", "C").
 * @param inversionLabel Optional inversion label (e.g., "1st Inv").
 */
data class ShareChordInfo(
    val voicing: ChordVoicing,
    val chordName: String,
    val inversionLabel: String? = null,
)

/**
 * A modal bottom sheet that previews a chord diagram in the traditional
 * vertical chart style and provides a "Share as Image" button.
 *
 * Uses [rememberGraphicsLayer] to capture the [ShareableChordCard] composable
 * as an image, then delegates to [ChordImageSharer] for saving and sharing.
 *
 * @param info The [ShareChordInfo] containing voicing, name, and optional inversion label.
 * @param onDismiss Callback when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareChordBottomSheet(
    info: ShareChordInfo,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Preview of the shareable chord card (captured via GraphicsLayer)
            ShareableChordCard(
                voicing = info.voicing,
                chordName = info.chordName,
                inversionLabel = info.inversionLabel,
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Share button
            Button(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap()
                        ChordImageSharer.share(context, bitmap, info.chordName)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.cd_share_chord_image),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.share_as_image))
            }
        }
    }
}
