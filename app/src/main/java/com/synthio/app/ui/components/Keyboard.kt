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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.synthio.app.audio.MusicConstants
import com.synthio.app.audio.MusicConstants.KeySignature
import com.synthio.app.audio.MusicConstants.KeyboardNote
import com.synthio.app.ui.theme.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToDown

/**
 * Piano keyboard with glissando support (slide between notes)
 * Supports both tap and drag gestures for expressive playing
 */
@Composable
fun Keyboard(
    onNoteOn: (KeyboardNote) -> Unit,
    onNoteOff: (KeyboardNote) -> Unit,
    modifier: Modifier = Modifier,
    isChordMode: Boolean = false,
    keySignature: KeySignature = KeySignature.C,
    isDarkMode: Boolean = false
) {
    val whiteKeys = KeyboardNote.entries.filter { !it.isBlackKey }
    val blackKeys = KeyboardNote.entries.filter { it.isBlackKey }
    
    // Track pressed states
    val pressedKeys = remember { mutableStateMapOf<KeyboardNote, Boolean>() }
    
    // Track key positions for hit testing
    val keyBounds = remember { mutableStateMapOf<KeyboardNote, KeyBounds>() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var containerOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Track notes per pointer for multi-touch support
    // Each finger can play and glissando independently
    val pointerNotes = remember { mutableStateMapOf<PointerId, KeyboardNote>() }
    
    val containerBackground = if (isDarkMode) {
        Brush.verticalGradient(listOf(DarkPastelLavenderDark, DarkSurface))
    } else {
        Brush.verticalGradient(listOf(PastelLavenderLight, PastelLavender))
    }
    
    // Function to find which key is at a given position
    fun findKeyAtPosition(x: Float, y: Float): KeyboardNote? {
        // ALWAYS check black keys first (they're on top and take priority)
        // Don't restrict by Y - just check if point is within black key bounds
        for (note in blackKeys) {
            val bounds = keyBounds[note] ?: continue
            if (x >= bounds.left && x <= bounds.right && 
                y >= bounds.top && y <= bounds.bottom) {
                return note
            }
        }
        
        // Then check white keys
        for (note in whiteKeys) {
            val bounds = keyBounds[note] ?: continue
            if (x >= bounds.left && x <= bounds.right && 
                y >= bounds.top && y <= bounds.bottom) {
                return note
            }
        }
        
        return null
    }
    
    // Function to check if a note is playable
    fun isNotePlayable(note: KeyboardNote): Boolean {
        return !isChordMode || MusicConstants.isNotePlayableInKey(note, keySignature)
    }
    
    // Function to find playable note - in single mode return exact key, in chord mode find nearest playable
    fun findPlayableNoteAtPosition(x: Float, y: Float): KeyboardNote? {
        val note = findKeyAtPosition(x, y)
        
        // In single mode, return exact key (all keys are playable)
        if (!isChordMode) {
            return note
        }
        
        // In chord mode, need to check if playable
        if (note != null && isNotePlayable(note)) {
            return note
        }
        
        // If not playable, find nearest playable one (only in chord mode)
        if (note != null && !isNotePlayable(note)) {
            val noteIndex = KeyboardNote.entries.indexOf(note)
            
            // Check neighbors in order of proximity
            for (offset in listOf(-1, 1, -2, 2, -3, 3)) {
                val adjacentIndex = noteIndex + offset
                if (adjacentIndex in KeyboardNote.entries.indices) {
                    val adjacent = KeyboardNote.entries[adjacentIndex]
                    if (isNotePlayable(adjacent)) {
                        return adjacent
                    }
                }
            }
        }
        
        return note  // Return found note even if not playable (will be filtered in noteOn)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(SynthShapes.large)
            .background(containerBackground)
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size
                containerOffset = coordinates.positionInRoot()
            }
            .pointerInput(isChordMode, keySignature) {
                // Multi-touch handling: each finger tracked independently
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        
                        for (change in event.changes) {
                            val pointerId = change.id
                            
                            if (change.changedToDown()) {
                                // New finger touched down
                                change.consume()
                                val note = findPlayableNoteAtPosition(change.position.x, change.position.y)
                                if (note != null) {
                                    pointerNotes[pointerId] = note
                                    pressedKeys[note] = true
                                    onNoteOn(note)
                                }
                            } else if (change.changedToUp()) {
                                // Finger lifted
                                change.consume()
                                pointerNotes[pointerId]?.let { note ->
                                    // Only release if no other finger is playing this note
                                    val otherFingersOnNote = pointerNotes.entries.any { 
                                        it.key != pointerId && it.value == note 
                                    }
                                    if (!otherFingersOnNote) {
                                        pressedKeys[note] = false
                                        onNoteOff(note)
                                    }
                                }
                                pointerNotes.remove(pointerId)
                            } else if (change.pressed) {
                                // Finger moved (glissando support)
                                change.consume()
                                val currentNoteForPointer = pointerNotes[pointerId]
                                val newNote = findPlayableNoteAtPosition(change.position.x, change.position.y)
                                
                                // If moved to a different note
                                if (newNote != null && newNote != currentNoteForPointer) {
                                    // Release old note if no other finger is on it
                                    currentNoteForPointer?.let { oldNote ->
                                        val otherFingersOnOld = pointerNotes.entries.any { 
                                            it.key != pointerId && it.value == oldNote 
                                        }
                                        if (!otherFingersOnOld) {
                                            pressedKeys[oldNote] = false
                                            onNoteOff(oldNote)
                                        }
                                    }
                                    
                                    // Play new note
                                    pointerNotes[pointerId] = newNote
                                    pressedKeys[newNote] = true
                                    // Only trigger noteOn if this note isn't already playing
                                    val alreadyPlaying = pointerNotes.entries.any { 
                                        it.key != pointerId && it.value == newNote 
                                    }
                                    if (!alreadyPlaying) {
                                        onNoteOn(newNote)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .padding(12.dp)
    ) {
        // White keys layer
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            whiteKeys.forEach { note ->
                val isPressed = pressedKeys[note] == true
                val isPlayable = !isChordMode || MusicConstants.isNotePlayableInKey(note, keySignature)
                
                WhiteKeyVisual(
                    note = note,
                    isPressed = isPressed,
                    isPlayable = isPlayable,
                    isChordMode = isChordMode,
                    keySignature = keySignature,
                    isDarkMode = isDarkMode,
                    containerOffset = containerOffset,
                    onBoundsChanged = { bounds ->
                        keyBounds[note] = bounds
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Black keys layer (positioned on top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Black key positions relative to white keys
            val blackKeyPositions = listOf(
                0.7f,  // C#
                1.7f,  // D#
                null,  // gap (E has no sharp)
                3.7f,  // F#
                4.7f,  // G#
                5.7f   // A#
            )
            
            blackKeyPositions.forEachIndexed { index, position ->
                if (position != null) {
                    val blackNote = blackKeys.getOrNull(
                        when (index) {
                            0 -> 0  // C#
                            1 -> 1  // D#
                            3 -> 2  // F#
                            4 -> 3  // G#
                            5 -> 4  // A#
                            else -> -1
                        }
                    )
                    
                    blackNote?.let { note ->
                        val isPressed = pressedKeys[note] == true
                        val isPlayable = !isChordMode || MusicConstants.isNotePlayableInKey(note, keySignature)
                        
                        Box(modifier = Modifier.weight(1f)) {
                            BlackKeyVisual(
                                note = note,
                                isPressed = isPressed,
                                isPlayable = isPlayable,
                                isChordMode = isChordMode,
                                keySignature = keySignature,
                                isDarkMode = isDarkMode,
                                containerOffset = containerOffset,
                                onBoundsChanged = { bounds ->
                                    keyBounds[note] = bounds
                                },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    } ?: Box(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

/**
 * Data class to store key bounds for hit testing
 */
data class KeyBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Composable
private fun WhiteKeyVisual(
    note: KeyboardNote,
    isPressed: Boolean,
    isPlayable: Boolean,
    isChordMode: Boolean,
    keySignature: KeySignature,
    isDarkMode: Boolean,
    containerOffset: Offset,
    onBoundsChanged: (KeyBounds) -> Unit,
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
    
    val borderColor = if (isDarkMode) {
        DarkPastelPink.copy(alpha = 0.3f)
    } else {
        PastelPink.copy(alpha = 0.3f)
    }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .alpha(alpha)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.9f))
                )
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .onGloballyPositioned { coordinates ->
                // Use positionInRoot and subtract container offset for accurate bounds
                val rootPosition = coordinates.positionInRoot()
                val size = coordinates.size
                onBoundsChanged(
                    KeyBounds(
                        left = rootPosition.x - containerOffset.x,
                        top = rootPosition.y - containerOffset.y,
                        right = rootPosition.x - containerOffset.x + size.width,
                        bottom = rootPosition.y - containerOffset.y + size.height
                    )
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Only show label if playable OR in single mode
        if (isPlayable || !isChordMode) {
            val labelColor = if (isDarkMode) DarkTextPrimary else TextPrimary
            Text(
                text = note.getDisplayNameForKey(keySignature),
                style = SynthTypography.keyLabel.copy(
                    color = if (isPlayable) labelColor else labelColor.copy(alpha = 0.4f)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun BlackKeyVisual(
    note: KeyboardNote,
    isPressed: Boolean,
    isPlayable: Boolean,
    isChordMode: Boolean,
    keySignature: KeySignature,
    isDarkMode: Boolean,
    containerOffset: Offset,
    onBoundsChanged: (KeyBounds) -> Unit,
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
            .width(32.dp)
            .fillMaxHeight()
            .alpha(alpha)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.85f))
                )
            )
            .onGloballyPositioned { coordinates ->
                // Use positionInRoot and subtract container offset for accurate bounds
                val rootPosition = coordinates.positionInRoot()
                val size = coordinates.size
                onBoundsChanged(
                    KeyBounds(
                        left = rootPosition.x - containerOffset.x,
                        top = rootPosition.y - containerOffset.y,
                        right = rootPosition.x - containerOffset.x + size.width,
                        bottom = rootPosition.y - containerOffset.y + size.height
                    )
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Only show label if playable
        if (isPlayable) {
            Text(
                text = note.getDisplayNameForKey(keySignature).replace("#", "♯"),
                style = SynthTypography.keyLabel.copy(color = TextOnDark),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
