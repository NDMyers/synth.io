package com.synthio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthio.app.audio.MusicConstants
import com.synthio.app.audio.MusicConstants.KeySignature
import com.synthio.app.audio.MusicConstants.KeyboardNote
import com.synthio.app.ui.theme.*

/**
 * Extended piano keyboard for fullscreen landscape mode.
 * Supports dynamic scaling (zoom) and precise key alignment.
 */
@Composable
fun FullscreenKeyboard(
    visibleWhiteKeys: Int = 8,
    onNoteOn: (KeyboardNote, Int) -> Unit = { _, _ -> },
    onNoteOff: (KeyboardNote, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    isChordMode: Boolean = false,
    keySignature: KeySignature = KeySignature.C,
    isDarkMode: Boolean = false
) {
    // Virtual Key Representation
    data class VirtualKey(
        val note: KeyboardNote,
        val octaveShift: Int,
        // Pre-calculated layout properties
        val isBlack: Boolean,
        val index: Int // For white keys: 0..N, For black keys: relative to which white key gap
    )

    // Generate keys based on visible count
    val generatedKeys = remember(visibleWhiteKeys) {
        val keys = mutableListOf<VirtualKey>()
        
        // Generate white keys
        for (i in 0 until visibleWhiteKeys) {
            val octaveRelativeIndex = i % 7
            val octaveShift = i / 7
            
            // Map 0..6 to C, D, E, F, G, A, B
            val note = when(octaveRelativeIndex) {
                0 -> KeyboardNote.C4
                1 -> KeyboardNote.D4
                2 -> KeyboardNote.E4
                3 -> KeyboardNote.F4
                4 -> KeyboardNote.G4
                5 -> KeyboardNote.A4
                6 -> KeyboardNote.B4
                else -> KeyboardNote.C4
            }
            
            keys.add(VirtualKey(note, octaveShift, false, i))
            
            // Determine if there's a black key to the right (except for E and B)
            // E is index 2, B is index 6
            if (octaveRelativeIndex != 2 && octaveRelativeIndex != 6) {
                val blackNote = when(octaveRelativeIndex) {
                    0 -> KeyboardNote.CS4
                    1 -> KeyboardNote.DS4
                    3 -> KeyboardNote.FS4
                    4 -> KeyboardNote.GS4
                    5 -> KeyboardNote.AS4
                    else -> null
                }
                
                if (blackNote != null) {
                    keys.add(VirtualKey(blackNote, octaveShift, true, i))
                }
            }
        }
        keys
    }

    // Track pressed states (VirtualKey -> Boolean)
    val pressedKeys = remember { mutableStateMapOf<VirtualKey, Boolean>() }
    
    // Track pointers for multi-touch
    val pointerKeys = remember { mutableStateMapOf<PointerId, VirtualKey>() }
    
    val containerBackground = if (isDarkMode) {
        Brush.verticalGradient(listOf(DarkPastelLavenderDark, DarkSurface))
    } else {
        Brush.verticalGradient(listOf(PastelLavenderLight, PastelLavender))
    }
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(SynthShapes.large)
            .background(containerBackground)
            .padding(8.dp) // Outer padding
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() - 16.dp.toPx() } // Subtract padding
        val heightPx = with(density) { maxHeight.toPx() - 16.dp.toPx() }
        
        val whiteKeyWidthPx = widthPx / visibleWhiteKeys
        val blackKeyWidthPx = whiteKeyWidthPx * 0.6f
        val blackKeyHeightPx = heightPx * 0.6f

        fun getKeyBounds(key: VirtualKey): Rect {
            return if (key.isBlack) {
                // Centered on the line between index and index+1
                val centerX = (key.index + 1) * whiteKeyWidthPx
                Rect(
                    left = centerX - (blackKeyWidthPx / 2),
                    top = 0f,
                    right = centerX + (blackKeyWidthPx / 2),
                    bottom = blackKeyHeightPx
                )
            } else {
                Rect(
                    left = key.index * whiteKeyWidthPx,
                    top = 0f,
                    right = (key.index + 1) * whiteKeyWidthPx,
                    bottom = heightPx
                )
            }
        }

        fun findKeyAt(x: Float, y: Float): VirtualKey? {
            // Check black keys first (z-order top)
            val blackKeys = generatedKeys.filter { it.isBlack }
            for (key in blackKeys) {
                val bounds = getKeyBounds(key)
                if (x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom) {
                    return key
                }
            }
            
            // Check white keys
            if (y >= 0 && y <= heightPx) {
                val index = (x / whiteKeyWidthPx).toInt()
                if (index in 0 until visibleWhiteKeys) {
                    // Find the white key with this index
                    return generatedKeys.find { !it.isBlack && it.index == index }
                }
            }
            return null
        }

        fun isNotePlayable(key: VirtualKey): Boolean {
            return !isChordMode || MusicConstants.isNotePlayableInKey(key.note, keySignature)
        }
        
        // Touch Input Handler
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(visibleWhiteKeys, widthPx) { // Update on scale OR width change
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            for (change in event.changes) {
                                val id = change.id
                                val position = change.position
                                // Adjust position for padding? No, pointerInput is on the inner Box matching constraints? 
                                // Actually, pointerInput is on fillMaxSize Box inside the padding. 
                                // Constraints refer to total available space, but our BoxWithConstraints context includes padding in calculation?
                                // Wait, `maxWidth` in BoxWithConstraints is the available width *inside* parent padding. 
                                // But I added `.padding(8.dp)` to the BoxWithConstraints modifier itself. 
                                // So content inside starts at (0,0) relative to padding. 
                                // So pointer events (0,0) are correct relative to content.
                                
                                if (change.changedToDown()) {
                                    change.consume()
                                    findKeyAt(position.x, position.y)?.let { key ->
                                        if (isNotePlayable(key)) {
                                            pointerKeys[id] = key
                                            pressedKeys[key] = true
                                            onNoteOn(key.note, key.octaveShift)
                                        }
                                    }
                                } else if (change.changedToUp()) {
                                    change.consume()
                                    pointerKeys[id]?.let { key ->
                                        // Check if other pointers are holding this key
                                        if (pointerKeys.values.count { it == key } == 1) {
                                            pressedKeys[key] = false
                                            onNoteOff(key.note, key.octaveShift)
                                        }
                                        pointerKeys.remove(id)
                                    }
                                } else if (change.pressed) {
                                    // Dragging
                                    val newKey = findKeyAt(position.x, position.y)
                                    val oldKey = pointerKeys[id]
                                    
                                    if (newKey != oldKey) {
                                        change.consume()
                                        
                                        // Release old
                                        oldKey?.let { key ->
                                            if (pointerKeys.values.count { it == key } == 1) {
                                                pressedKeys[key] = false
                                                onNoteOff(key.note, key.octaveShift)
                                            }
                                        }
                                        
                                        // Press new
                                        if (newKey != null && isNotePlayable(newKey)) {
                                            pointerKeys[id] = newKey
                                            pressedKeys[newKey] = true
                                            
                                            // Only trigger noteOn if not already pressed (or allow retrigger? Synth usually allows retrigger)
                                            // But for consistent visual feedback we should be careful.
                                            // Logic: If already pressed by other finger, we might not want to re-trigger noteOn?
                                            // But standard behavior is: NoteOn for every new touch. 
                                            // SynthEngine handles voice allocation.
                                            onNoteOn(newKey.note, newKey.octaveShift)
                                        } else {
                                            pointerKeys.remove(id)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Render Keys
            
            // White Keys
            generatedKeys.filter { !it.isBlack }.forEach { key ->
                val bounds = getKeyBounds(key)
                val isPressed = pressedKeys[key] == true
                val isPlayable = isNotePlayable(key)
                
                // Convert pixels to Dp for rendering
                val widthDp = with(density) { bounds.width.toDp() }
                // Height is fillMax
                val leftOffsetDp = with(density) { bounds.left.toDp() }
                
                FullscreenWhiteKeyVisual(
                    note = key.note,
                    isPressed = isPressed,
                    isPlayable = isPlayable,
                    keySignature = keySignature,
                    isDarkMode = isDarkMode,
                    modifier = Modifier
                        .width(widthDp)
                        .fillMaxHeight()
                        .offset(x = leftOffsetDp)
                )
            }
            
            // Black Keys
            generatedKeys.filter { it.isBlack }.forEach { key ->
                val bounds = getKeyBounds(key)
                val isPressed = pressedKeys[key] == true
                val isPlayable = isNotePlayable(key)
                
                val widthDp = with(density) { bounds.width.toDp() }
                val heightDp = with(density) { bounds.height.toDp() }
                val leftOffsetDp = with(density) { bounds.left.toDp() }
                
                FullscreenBlackKeyVisual(
                    note = key.note,
                    isPressed = isPressed,
                    isPlayable = isPlayable,
                    keySignature = keySignature,
                    isDarkMode = isDarkMode,
                    modifier = Modifier
                        .width(widthDp)
                        .height(heightDp)
                        .offset(x = leftOffsetDp)
                )
            }
        }
    }
}

@Composable
private fun FullscreenWhiteKeyVisual(
    note: KeyboardNote,
    isPressed: Boolean,
    isPlayable: Boolean,
    keySignature: KeySignature,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !isPlayable && isDarkMode -> DarkDisabledKey
        !isPlayable -> DisabledKeyLight
        isPressed && isDarkMode -> DarkWhiteKeyPressed
        isPressed -> WhiteKeyPressed
        isDarkMode -> DarkWhiteKeyDefault
        else -> WhiteKeyDefault
    }
    
    val shadowElevation = if (isPressed || !isPlayable) 2.dp else 6.dp
    val alpha = if (isPlayable) 1f else 0.6f
    val borderColor = if (isDarkMode) DarkPastelPink.copy(alpha = 0.3f) else PastelPink.copy(alpha = 0.3f)
    
    Box(
        modifier = modifier
            .padding(1.dp) // Gap between keys
            .alpha(alpha)
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.9f))))
            .border(0.5.dp, borderColor, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isPlayable) {
            Text(
                text = note.getDisplayNameForKey(keySignature),
                style = SynthTypography.keyLabel.copy(
                    color = if (isDarkMode) DarkTextPrimary else TextPrimary
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun FullscreenBlackKeyVisual(
    note: KeyboardNote,
    isPressed: Boolean,
    isPlayable: Boolean,
    keySignature: KeySignature,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        !isPlayable && isDarkMode -> DarkDisabledKey.copy(alpha = 0.5f)
        !isPlayable -> DisabledKeyLight.copy(alpha = 0.7f)
        isPressed && isDarkMode -> DarkBlackKeyPressed
        isPressed -> BlackKeyPressed
        isDarkMode -> DarkBlackKeyDefault
        else -> BlackKeyDefault
    }
    
    val shadowElevation = if (isPressed || !isPlayable) 2.dp else 8.dp
    val alpha = if (isPlayable) 1f else 0.5f
    
    Box(
        modifier = modifier
            .alpha(alpha)
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.85f)))),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isPlayable) {
            Text(
                text = note.getDisplayNameForKey(keySignature).replace("#", "♯"),
                style = SynthTypography.keyLabel.copy(color = TextOnDark, fontSize = 10.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
