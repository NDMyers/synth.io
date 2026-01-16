package com.synthio.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synthio.app.R
import com.synthio.app.audio.ChorusMode
import com.synthio.app.audio.LooperState
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.synthio.app.ui.components.*
import com.synthio.app.audio.ExportQuality
import kotlinx.coroutines.delay
import com.synthio.app.ui.theme.*
import com.synthio.app.viewmodel.SynthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SynthesizerScreen(
    viewModel: SynthViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Fullscreen keyboard state
    var isFullscreenKeyboard by rememberSaveable { mutableStateOf(false) }
    
    // Detect orientation for auto-fullscreen
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Helper to lock/unlock orientation
    fun setOrientation(landscape: Boolean) {
        val activity = context as? Activity ?: return
        activity.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    // Auto-trigger fullscreen when rotated to landscape
    LaunchedEffect(isLandscape) {
        // Only toggle fullscreen keyboard if the rotation wasn't forced by the drum map modal
        if (!viewModel.showDrumBeatMapModal) {
            isFullscreenKeyboard = isLandscape
        }
    }
    
    // Initialize ViewModel context for audio export service
    LaunchedEffect(Unit) {
        viewModel.initContext(context)
    }

    // Auto-rotate for drum beat map modal
    LaunchedEffect(viewModel.showDrumBeatMapModal) {
        if (viewModel.showDrumBeatMapModal) {
            setOrientation(true)
        } else {
            // Only revert if we're not explicitly in fullscreen keyboard mode (though that also follows landscape)
            // Ideally we revert to portrait
            setOrientation(false)
        }
    }
    
    // Accordion state - remember which sections are expanded (Synth mode)
    var oscillatorExpanded by rememberSaveable { mutableStateOf(true) }
    var filterExpanded by rememberSaveable { mutableStateOf(true) }
    var envelopeExpanded by rememberSaveable { mutableStateOf(false) }
    var lfoExpanded by rememberSaveable { mutableStateOf(false) }
    var effectsExpanded by rememberSaveable { mutableStateOf(false) }
    var synthTremoloExpanded by rememberSaveable { mutableStateOf(false) }
    var synthDelayExpanded by rememberSaveable { mutableStateOf(false) }
    var synthReverbExpanded by rememberSaveable { mutableStateOf(false) }
    var outputExpanded by rememberSaveable { mutableStateOf(true) }
    
    // Accordion state - Wurlitzer mode
    var wurliTremoloExpanded by rememberSaveable { mutableStateOf(true) }
    var wurliChorusExpanded by rememberSaveable { mutableStateOf(true) }
    var wurliDelayExpanded by rememberSaveable { mutableStateOf(false) }
    var wurliReverbExpanded by rememberSaveable { mutableStateOf(false) }
    var wurliOutputExpanded by rememberSaveable { mutableStateOf(true) }
    
    // Theme colors based on dark mode
    val isDark = viewModel.isDarkMode
    val backgroundColor = if (isDark) {
        Brush.verticalGradient(listOf(DarkSurface, DarkBackground))
    } else {
        Brush.verticalGradient(listOf(BackgroundCream, BackgroundLight))
    }
    val surfaceColor = if (isDark) DarkSurfaceCard.copy(alpha = 0.7f) else SurfaceWhite.copy(alpha = 0.7f)
    val textColor = if (isDark) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDark) DarkTextSecondary else TextSecondary
    
    // Accent colors for dark mode
    val accentPink = if (isDark) DarkPastelPink else PastelPink
    val accentLavender = if (isDark) DarkPastelLavender else PastelLavender
    val accentMint = if (isDark) DarkPastelMint else PastelMint
    val accentPeach = if (isDark) DarkPastelPeach else PastelPeach
    val accentBlue = if (isDark) DarkPastelBlue else PastelBlue
    val accentYellow = if (isDark) DarkPastelYellow else PastelYellow
    val accentCoral = if (isDark) DarkPastelCoral else PastelCoral
    
    // Snackbar for export confirmation
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Root container for Z-ordering (Drawer -> Modals -> Snackbar)
    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // Only enable gestures when drawer is open (to close it)
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (isDark) DarkSurface else BackgroundCream
            ) {
                SideMenuContent(
                    selectedKeySignature = viewModel.selectedKeySignature,
                    onKeySignatureSelected = { key ->
                        viewModel.updateKeySignature(key)
                    },
                    selectedChordType = viewModel.selectedChordType,
                    onChordTypeSelected = { type ->
                        viewModel.updateChordType(type)
                    },
                    isDarkMode = isDark,
                    onDarkModeToggle = { viewModel.toggleDarkMode() },
                    // Drum machine props
                    isDrumEnabled = viewModel.isDrumEnabled,
                    onDrumToggle = { viewModel.toggleDrumMachine() },
                    isKickEnabled = viewModel.isKickEnabled,
                    onKickToggle = { viewModel.toggleKick() },
                    isSnareEnabled = viewModel.isSnareEnabled,
                    onSnareToggle = { viewModel.toggleSnare() },
                    isHiHatEnabled = viewModel.isHiHatEnabled,
                    onHiHatToggle = { viewModel.toggleHiHat() },
                    isHiHat16thNotes = viewModel.isHiHat16thNotes,
                    onHiHatModeToggle = { viewModel.toggleHiHatMode() },
                    drumBPM = viewModel.drumBPM,
                    onDrumBPMChange = { viewModel.updateDrumBPM(it) },
                    drumVolume = viewModel.drumVolume,
                    onDrumVolumeChange = { viewModel.updateDrumVolume(it) },

                    // Wurlitzer props
                    isWurlitzerMode = viewModel.isWurlitzerMode,
                    onWurlitzerToggle = { viewModel.updateWurlitzerMode(it) },
                    // Advanced settings
                    onEditDrumPattern = { 
                        scope.launch { drawerState.close() }
                        viewModel.openDrumBeatMapModal()
                    },
                    // Exports
                    hasActiveExports = viewModel.hasActiveExports(),
                    exportCount = viewModel.exportJobs.size,
                    onOpenExports = {
                        scope.launch { drawerState.close() }
                        viewModel.openExportsPage()
                    },
                    onCloseMenu = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        // Scroll state for auto-scrolling to keyboard
        val scrollState = rememberScrollState()
        var keyboardPositionY by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        
        // Track previous looper state to detect transitions
        var previousLooperState by remember { mutableStateOf(LooperState.IDLE) }
        
        // Poll looper state while active
        LaunchedEffect(viewModel.looperState) {
            while (viewModel.looperState == LooperState.PRE_COUNT ||
                   viewModel.looperState == LooperState.RECORDING ||
                   viewModel.looperState == LooperState.PLAYING) {
                viewModel.updateLooperState()
                delay(50) // Poll at 20Hz
            }
        }
        
        // Auto-scroll to keyboard when recording starts (from IDLE or STOPPED)
        LaunchedEffect(viewModel.looperState) {
            val isStartingRecording = viewModel.looperState == LooperState.PRE_COUNT
            val wasNotRecording = previousLooperState != LooperState.PRE_COUNT && 
                                  previousLooperState != LooperState.RECORDING
            
            if (isStartingRecording && wasNotRecording) {
                // Scroll to keyboard position with some offset to show context
                val scrollTarget = (keyboardPositionY - 200f).coerceAtLeast(0f).toInt()
                scrollState.animateScrollTo(scrollTarget)
            }
            previousLooperState = viewModel.looperState
        }
        
        // Modals moved to end of file for Z-ordering
        
        // Exports page overlay
        if (viewModel.showExportsPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                val context = LocalContext.current
                
                // SAF Launcher for saving files
                var pendingExportJob by remember { mutableStateOf<com.synthio.app.audio.ExportJob?>(null) }
                
                val saveLauncher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument(
                        if (pendingExportJob?.filename?.endsWith(".wav") == true) "audio/wav" else "audio/aac"
                    )
                ) { uri ->
                    uri?.let { targetUri ->
                        pendingExportJob?.let { job ->
                            viewModel.saveExportToUri(job, targetUri) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Saved to ${targetUri.lastPathSegment}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                }

                ExportsPage(
                    exportJobs = viewModel.exportJobs,
                    onCancelJob = { viewModel.cancelExport(it) },
                    onRemoveJob = { viewModel.removeExportJob(it) },
                    onDownloadJob = { job ->
                        pendingExportJob = job
                        saveLauncher.launch(job.filename)
                    },
                    onClearCompleted = { viewModel.clearCompletedExports() },
                    onClose = { viewModel.closeExportsPage() },
                    isDarkMode = isDark,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (isFullscreenKeyboard) {
            // Fullscreen keyboard view - shows when toggled or in landscape
            FullscreenKeyboardScreen(
                currentOctave = viewModel.octave,
                onOctaveUp = { viewModel.octaveUp() },
                onOctaveDown = { viewModel.octaveDown() },
                onNoteOn = { note, shift -> viewModel.noteOn(note, shift) },
                onNoteOff = { note, shift -> viewModel.noteOff(note, shift) },
                onExitFullscreen = { 
                    isFullscreenKeyboard = false
                    setOrientation(false)
                },
                isChordMode = viewModel.isChordMode,
                keySignature = viewModel.selectedKeySignature,
                isDarkMode = isDark,
                modifier = modifier.systemBarsPadding()
            )
        } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Close drawer when clicking anywhere on main content
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                // === STICKY HEADER (always visible) ===
                StickyHeader(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    isDark = isDark,
                    backgroundColor = if (isDark) DarkSurface else BackgroundCream,
                    accentPink = accentPink,
                    accentLavender = accentLavender,
                    accentMint = accentMint,
                    accentPeach = accentPeach,
                    accentCoral = accentCoral,
                    accentBlue = accentBlue
                )
                
                // === SCROLLABLE CONTENT ===
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                
                if (!viewModel.isWurlitzerMode) {
                    // Waveform Display
                    WaveformDisplay(
                        waveform = viewModel.waveform,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Waveform Selector
                    WaveformSelector(
                        selectedWaveforms = viewModel.activeWaveforms,
                        onWaveformToggled = { viewModel.toggleWaveform(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // ===== OSCILLATOR SECTION =====
                    CollapsibleSection(
                        title = "Oscillator",
                        isExpanded = oscillatorExpanded,
                        onToggle = { oscillatorExpanded = !oscillatorExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentLavender
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.pulseWidth,
                                onValueChange = { viewModel.updatePulseWidth(it) },
                                label = "Pulse Width",
                                minValue = 0.1f,
                                maxValue = 0.9f,
                                color = accentLavender
                            )
                            
                            Knob(
                                value = viewModel.subOscLevel,
                                onValueChange = { viewModel.updateSubOscLevel(it) },
                                label = "Sub Osc",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentBlue
                            )
                            
                            Knob(
                                value = viewModel.noiseLevel,
                                onValueChange = { viewModel.updateNoiseLevel(it) },
                                label = "Noise",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentPeach
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== FILTER SECTION =====
                    CollapsibleSection(
                        title = "Filter",
                        isExpanded = filterExpanded,
                        onToggle = { filterExpanded = !filterExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentCoral
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.filterCutoff,
                                onValueChange = { viewModel.updateFilterCutoff(it) },
                                label = "Cutoff",
                                minValue = 100f,
                                maxValue = 15000f,
                                color = accentCoral
                            )
                            
                            Knob(
                                value = viewModel.filterResonance,
                                onValueChange = { viewModel.updateFilterResonance(it) },
                                label = "Resonance",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentBlue
                            )
                            
                            Knob(
                                value = viewModel.filterKeyTracking,
                                onValueChange = { viewModel.updateFilterKeyTracking(it) },
                                label = "Key Track",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentMint
                            )
                            
                            Knob(
                                value = viewModel.hpfCutoff,
                                onValueChange = { viewModel.updateHPFCutoff(it) },
                                label = "HPF",
                                minValue = 0f,
                                maxValue = 500f,
                                color = accentYellow
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== ENVELOPE SECTION =====
                    CollapsibleSection(
                        title = "Envelope (ADSR)",
                        isExpanded = envelopeExpanded,
                        onToggle = { envelopeExpanded = !envelopeExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentMint
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.attack,
                                onValueChange = { viewModel.updateAttack(it) },
                                label = "Attack",
                                minValue = 0.001f,
                                maxValue = 2.0f,
                                color = accentMint
                            )
                            
                            Knob(
                                value = viewModel.decay,
                                onValueChange = { viewModel.updateDecay(it) },
                                label = "Decay",
                                minValue = 0.01f,
                                maxValue = 2.0f,
                                color = accentPeach
                            )
                            
                            Knob(
                                value = viewModel.sustain,
                                onValueChange = { viewModel.updateSustain(it) },
                                label = "Sustain",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentLavender
                            )
                            
                            Knob(
                                value = viewModel.release,
                                onValueChange = { viewModel.updateRelease(it) },
                                label = "Release",
                                minValue = 0.01f,
                                maxValue = 3.0f,
                                color = accentYellow
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== LFO SECTION =====
                    CollapsibleSection(
                        title = "LFO",
                        isExpanded = lfoExpanded,
                        onToggle = { lfoExpanded = !lfoExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentPink
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.lfoRate,
                                onValueChange = { viewModel.updateLFORate(it) },
                                label = "Rate",
                                minValue = 0.1f,
                                maxValue = 20f,
                                color = accentPink
                            )
                            
                            Knob(
                                value = viewModel.lfoPitchDepth,
                                onValueChange = { viewModel.updateLFOPitchDepth(it) },
                                label = "Pitch",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentMint
                            )
                            
                            Knob(
                                value = viewModel.lfoFilterDepth,
                                onValueChange = { viewModel.updateLFOFilterDepth(it) },
                                label = "Filter",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentCoral
                            )
                            
                            Knob(
                                value = viewModel.lfoPWMDepth,
                                onValueChange = { viewModel.updateLFOPWMDepth(it) },
                                label = "PWM",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentLavender
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== EFFECTS SECTION =====
                    CollapsibleSection(
                        title = "Effects",
                        isExpanded = effectsExpanded,
                        onToggle = { effectsExpanded = !effectsExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentBlue
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Chorus Mode Selector
                            Text(
                                text = "Chorus",
                                style = SynthTypography.label.copy(color = secondaryTextColor),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ChorusMode.entries.forEach { mode ->
                                    ChorusModeButton(
                                        label = when (mode) {
                                            ChorusMode.OFF -> "Off"
                                            ChorusMode.MODE_I -> "I"
                                            ChorusMode.MODE_II -> "II"
                                        },
                                        isSelected = viewModel.chorusMode == mode,
                                        onClick = { viewModel.updateChorusMode(mode) },
                                        accentColor = accentPink,
                                        isDarkMode = isDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Glide Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Glide",
                                            style = SynthTypography.label.copy(color = textColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = viewModel.glideEnabled,
                                            onCheckedChange = { viewModel.toggleGlide() },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = accentMint,
                                                checkedTrackColor = if (isDark) DarkPastelMintDark else PastelMintLight
                                            )
                                        )
                                    }
                                }
                                
                                if (viewModel.glideEnabled) {
                                    Knob(
                                        value = viewModel.glideTime,
                                        onValueChange = { viewModel.updateGlideTime(it) },
                                        label = "Time",
                                        minValue = 0.01f,
                                        maxValue = 2f,
                                        color = accentMint
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Unison Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Unison",
                                    style = SynthTypography.label.copy(color = textColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = viewModel.unisonEnabled,
                                    onCheckedChange = { viewModel.toggleUnison() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = accentBlue,
                                        checkedTrackColor = if (isDark) DarkPastelBlueDark else PastelBlueLight
                                    )
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = viewModel.unisonEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Knob(
                                            value = viewModel.unisonVoices.toFloat(),
                                            onValueChange = { viewModel.updateUnisonVoices(it.toInt()) },
                                            label = "Voices",
                                            minValue = 2f,
                                            maxValue = 8f,
                                            color = accentBlue
                                        )
                                        
                                        Knob(
                                            value = viewModel.unisonDetune,
                                            onValueChange = { viewModel.updateUnisonDetune(it) },
                                            label = "Detune",
                                            minValue = 0f,
                                            maxValue = 50f,
                                            color = accentLavender
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== TREMOLO SECTION =====
                    CollapsibleSection(
                        title = "Tremolo",
                        isExpanded = synthTremoloExpanded,
                        onToggle = { synthTremoloExpanded = !synthTremoloExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentPeach
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.synthTremoloRate,
                                onValueChange = { viewModel.updateSynthTremolo(it, viewModel.synthTremoloDepth) },
                                label = "Rate",
                                minValue = 0.5f,
                                maxValue = 10f,
                                color = accentPeach
                            )
                            
                            Knob(
                                value = viewModel.synthTremoloDepth,
                                onValueChange = { viewModel.updateSynthTremolo(viewModel.synthTremoloRate, it) },
                                label = "Depth",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentCoral
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== DELAY SECTION =====
                    CollapsibleSection(
                        title = "Delay",
                        isExpanded = synthDelayExpanded,
                        onToggle = { synthDelayExpanded = !synthDelayExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentBlue
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.synthDelayTime,
                                onValueChange = { viewModel.updateSynthDelay(it, viewModel.synthDelayFeedback, viewModel.synthDelayMix) },
                                label = "Time",
                                minValue = 0.05f,
                                maxValue = 0.5f,
                                color = accentBlue
                            )
                            
                            Knob(
                                value = viewModel.synthDelayFeedback,
                                onValueChange = { viewModel.updateSynthDelay(viewModel.synthDelayTime, it, viewModel.synthDelayMix) },
                                label = "Feedback",
                                minValue = 0f,
                                maxValue = 0.9f,
                                color = accentLavender
                            )
                            
                            Knob(
                                value = viewModel.synthDelayMix,
                                onValueChange = { viewModel.updateSynthDelay(viewModel.synthDelayTime, viewModel.synthDelayFeedback, it) },
                                label = "Mix",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentMint
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== REVERB SECTION =====
                    CollapsibleSection(
                        title = "Reverb",
                        isExpanded = synthReverbExpanded,
                        onToggle = { synthReverbExpanded = !synthReverbExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentMint
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.synthReverbSize,
                                onValueChange = { viewModel.updateSynthReverb(it, viewModel.synthReverbMix) },
                                label = "Size",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentMint
                            )
                            
                            Knob(
                                value = viewModel.synthReverbMix,
                                onValueChange = { viewModel.updateSynthReverb(viewModel.synthReverbSize, it) },
                                label = "Mix",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentPink
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== OUTPUT SECTION =====
                    CollapsibleSection(
                        title = "Output",
                        isExpanded = outputExpanded,
                        onToggle = { outputExpanded = !outputExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentPink
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.synthVolume,
                                onValueChange = { viewModel.updateSynthVolume(it) },
                                label = "Volume",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentPink
                            )
                        }
                    }
                } else {
                    // ===== WURLITZER 200A MODE UI =====
                    
                    // Wurlitzer banner image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.wurlitzer_banner),
                            contentDescription = "Wurlitzer 200A",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentScale = ContentScale.Crop
                        )
                        // Subtle gradient overlay for text readability if needed
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )
                    }
                    
                    // ===== TREMOLO SECTION =====
                    CollapsibleSection(
                        title = "Tremolo",
                        isExpanded = wurliTremoloExpanded,
                        onToggle = { wurliTremoloExpanded = !wurliTremoloExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentPeach
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.wurliTremoloRate,
                                onValueChange = { viewModel.updateWurliTremolo(it, viewModel.wurliTremoloDepth) },
                                label = "Rate",
                                minValue = 0.5f,
                                maxValue = 10f,
                                color = accentPeach
                            )
                            
                            Knob(
                                value = viewModel.wurliTremoloDepth,
                                onValueChange = { viewModel.updateWurliTremolo(viewModel.wurliTremoloRate, it) },
                                label = "Depth",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentCoral
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== CHORUS SECTION =====
                    CollapsibleSection(
                        title = "Chorus",
                        isExpanded = wurliChorusExpanded,
                        onToggle = { wurliChorusExpanded = !wurliChorusExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentPink
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ChorusMode.entries.forEach { mode ->
                                ChorusModeButton(
                                    label = when (mode) {
                                        ChorusMode.OFF -> "Off"
                                        ChorusMode.MODE_I -> "I"
                                        ChorusMode.MODE_II -> "II"
                                    },
                                    isSelected = viewModel.wurliChorusMode == mode,
                                    onClick = { viewModel.updateWurliChorusMode(mode) },
                                    accentColor = accentPink,
                                    isDarkMode = isDark,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== DELAY SECTION =====
                    CollapsibleSection(
                        title = "Delay",
                        isExpanded = wurliDelayExpanded,
                        onToggle = { wurliDelayExpanded = !wurliDelayExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentBlue
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.wurliDelayTime,
                                onValueChange = { viewModel.updateWurliDelay(it, viewModel.wurliDelayFeedback, viewModel.wurliDelayMix) },
                                label = "Time",
                                minValue = 0.05f,
                                maxValue = 0.5f,
                                color = accentBlue
                            )
                            
                            Knob(
                                value = viewModel.wurliDelayFeedback,
                                onValueChange = { viewModel.updateWurliDelay(viewModel.wurliDelayTime, it, viewModel.wurliDelayMix) },
                                label = "Feedback",
                                minValue = 0f,
                                maxValue = 0.8f,
                                color = accentLavender
                            )
                            
                            Knob(
                                value = viewModel.wurliDelayMix,
                                onValueChange = { viewModel.updateWurliDelay(viewModel.wurliDelayTime, viewModel.wurliDelayFeedback, it) },
                                label = "Mix",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentBlue
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== REVERB SECTION =====
                    CollapsibleSection(
                        title = "Reverb",
                        isExpanded = wurliReverbExpanded,
                        onToggle = { wurliReverbExpanded = !wurliReverbExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentMint
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.wurliReverbSize,
                                onValueChange = { viewModel.updateWurliReverb(it, viewModel.wurliReverbMix) },
                                label = "Size",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentMint
                            )
                            
                            Knob(
                                value = viewModel.wurliReverbMix,
                                onValueChange = { viewModel.updateWurliReverb(viewModel.wurliReverbSize, it) },
                                label = "Mix",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentYellow
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ===== OUTPUT SECTION =====
                    CollapsibleSection(
                        title = "Output",
                        isExpanded = wurliOutputExpanded,
                        onToggle = { wurliOutputExpanded = !wurliOutputExpanded },
                        isDarkMode = isDark,
                        surfaceColor = surfaceColor,
                        accentColor = accentCoral
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Knob(
                                value = viewModel.wurliVolume,
                                onValueChange = { viewModel.updateWurliVolume(it) },
                                label = "Volume",
                                minValue = 0f,
                                maxValue = 1f,
                                color = accentCoral
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Mode Toggle with Key Signature indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SynthShapes.large)
                        .background(
                            if (viewModel.isChordMode) {
                                if (isDark) Brush.horizontalGradient(listOf(DarkPastelPinkDark, DarkPastelLavenderDark))
                                else Brush.horizontalGradient(listOf(PastelPinkLight, PastelLavenderLight))
                            } else {
                                if (isDark) Brush.horizontalGradient(listOf(DarkSurfaceLight, DarkSurfaceCard))
                                else Brush.horizontalGradient(listOf(BackgroundLight, SurfaceWhite))
                            }
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Single",
                        style = SynthTypography.label.copy(
                            color = if (!viewModel.isChordMode) accentPink else secondaryTextColor
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Switch(
                        checked = viewModel.isChordMode,
                        onCheckedChange = { viewModel.toggleChordMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentPink,
                            checkedTrackColor = if (isDark) DarkPastelPinkDark else PastelPinkLight,
                            uncheckedThumbColor = accentLavender,
                            uncheckedTrackColor = if (isDark) DarkPastelLavenderDark else PastelLavenderLight
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "7th Chords",
                            style = SynthTypography.label.copy(
                                color = if (viewModel.isChordMode) accentPink else secondaryTextColor
                            )
                        )
                        if (viewModel.isChordMode) {
                            Text(
                                text = "Key: ${viewModel.selectedKeySignature.displayName}",
                                style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                            )
                        }
                    }
                }
                
                // Current chord display - fixed height container to prevent layout shift
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val chordName = viewModel.currentChordName
                    if (chordName != null) {
                        Text(
                            text = chordName,
                            style = SynthTypography.chordName.copy(
                                color = if (isDark) DarkTextPrimary else TextPrimary
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(SynthShapes.pill)
                                .background(if (isDark) DarkPastelPinkDark else PastelPinkLight)
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Octave Selector with Fullscreen Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OctaveSelector(
                        currentOctave = viewModel.octave,
                        onOctaveUp = { viewModel.octaveUp() },
                        onOctaveDown = { viewModel.octaveDown() },
                        isDarkMode = isDark
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Fullscreen toggle button
                    FullscreenKeyboardButton(
                        isFullscreen = false,
                        onClick = { 
                            isFullscreenKeyboard = true
                            setOrientation(true)
                        },
                        isDarkMode = isDark
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Keyboard - track position for auto-scroll
                Keyboard(
                    onNoteOn = { viewModel.noteOn(it) },
                    onNoteOff = { viewModel.noteOff(it) },
                    isChordMode = viewModel.isChordMode,
                    keySignature = viewModel.selectedKeySignature,
                    isDarkMode = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            keyboardPositionY = coordinates.positionInParent().y
                        }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        } // end else (non-fullscreen mode)
        
        // Drum beat map modal - placed here to ensure z-index on top
        if (viewModel.showDrumBeatMapModal) {
            DrumBeatMapModal(
                kickPattern = viewModel.kickPattern,
                snarePattern = viewModel.snarePattern,
                hiHatPattern = viewModel.hiHatPattern,
                kickVolume = viewModel.kickVolume,
                snareVolume = viewModel.snareVolume,
                hiHatVolume = viewModel.hiHatVolume,
                isDrumPlaying = viewModel.isDrumEnabled,
                onTogglePlay = { viewModel.toggleDrumMachine() },
                onToggleStep = { instrument, step -> viewModel.toggleDrumStep(instrument, step) },
                onInstrumentVolumeChange = { instrument, volume -> viewModel.updateDrumInstrumentVolume(instrument, volume) },
                onResetPattern = { viewModel.resetDrumPattern() },
                onDismiss = { viewModel.closeDrumBeatMapModal() },
                isDarkMode = isDark,
                modifier = modifier
            )
        }
        
    }
    }
    
        // Loop override confirmation dialog
        if (viewModel.showLoopOverrideDialog) {
            LoopOverrideDialog(
                onConfirm = { viewModel.confirmLoopOverride() },
                onDismiss = { viewModel.dismissLoopOverrideDialog() },
                isDarkMode = isDark
            )
        }
        
        // Multi-track looper modal
        if (viewModel.showLooperModal) {
            LooperModal(
                tracks = viewModel.loopTracks,
                looperState = viewModel.looperState,
                activeRecordingTrack = viewModel.activeRecordingTrack,
                currentBeat = viewModel.looperCurrentBeat,
                currentBar = viewModel.looperCurrentBar,
                onStartRecordingTrack = { trackIndex -> viewModel.startRecordingTrack(trackIndex) },
                onTrackVolumeChange = { index, volume -> viewModel.updateTrackVolume(index, volume) },
                onToggleMute = { index -> viewModel.toggleTrackMute(index) },
                onToggleSolo = { index -> viewModel.toggleTrackSolo(index) },
                onDeleteTrack = { index -> viewModel.showDeleteTrackConfirmation(index) },
                onDeleteAll = { viewModel.showDeleteAllConfirmation() },
                onPlayStop = { viewModel.playStopLoop() },
                onDismiss = { viewModel.dismissLooperModal() },
                isDarkMode = isDark,
                metronomeVolume = viewModel.metronomeVolume,
                onMetronomeVolumeChange = { viewModel.updateMetronomeVolume(it) },
                barCount = viewModel.looperBarCount,
                onBarCountChange = { viewModel.updateLooperBarCount(it) },
                onOpenExport = { viewModel.openExportModal() }
            )
        }
        
        // Export modal
        if (viewModel.showExportModal) {
            ExportModal(
                tracks = viewModel.loopTracks,
                selectedTracks = viewModel.selectedExportTracks,
                includesDrums = viewModel.includeExportDrums,
                onToggleTrack = { viewModel.toggleExportTrack(it) },
                onSelectAll = { viewModel.selectAllExportTracks() },
                onSetIncludeDrums = { viewModel.updateIncludeExportDrums(it) },
                onStartExport = { quality -> 
                    viewModel.startExport(quality)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Export started! Check progress in Exports (Menu → Exports)",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onDismiss = { viewModel.closeExportModal() },
                isDarkMode = isDark
            )
        }
        
        // Delete single track confirmation
        viewModel.deleteTrackConfirmIndex?.let { trackIndex ->
            DeleteTrackDialog(
                trackIndex = trackIndex,
                onConfirm = { viewModel.confirmDeleteTrack() },
                onDismiss = { viewModel.dismissDeleteTrackConfirmation() },
                isDarkMode = isDark
            )
        }
        
        // Delete all tracks confirmation
        if (viewModel.showDeleteAllDialog) {
            DeleteAllTracksDialog(
                onConfirm = { viewModel.confirmDeleteAllTracks() },
                onDismiss = { viewModel.dismissDeleteAllConfirmation() },
                isDarkMode = isDark
            )
        }
        
        // Bar count change confirmation dialog
        if (viewModel.showBarCountChangeDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissBarCountChangeDialog() },
                title = { Text("Change Bar Count?") },
                text = { 
                    Text("Changing the bar count to ${viewModel.pendingBarCount} will clear all recorded loops. Continue?") 
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmBarCountChange() }) {
                        Text("Clear & Change", color = if (isDark) DarkPastelPink else PastelPink)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissBarCountChangeDialog() }) {
                        Text("Cancel")
                    }
                },
                containerColor = if (isDark) DarkSurfaceCard else SurfaceWhite,
                titleContentColor = if (isDark) DarkTextPrimary else TextPrimary,
                textContentColor = if (isDark) DarkTextSecondary else TextSecondary
            )
        }
        
        // Snackbar for notifications - Z-ordered on top of everything
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .padding(bottom = 80.dp),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isDark) DarkSurfaceCard else SurfaceWhite,
                    contentColor = if (isDark) DarkTextPrimary else TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        )
    }
    }
}

/**
 * Sticky header that stays visible at all times, showing app title and looper controls
 */
@Composable
private fun StickyHeader(
    viewModel: SynthViewModel,
    onMenuClick: () -> Unit,
    isDark: Boolean,
    backgroundColor: Color,
    accentPink: Color,
    accentLavender: Color,
    accentMint: Color,
    accentPeach: Color,
    accentCoral: Color,
    accentBlue: Color
) {
    val looperState = viewModel.looperState
    val isRecordingActive = looperState == LooperState.PRE_COUNT || looperState == LooperState.RECORDING
    
    // Header background with subtle elevation when recording
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shadowElevation = if (isRecordingActive) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left side controls
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SideMenuButton(
                        onClick = onMenuClick,
                        isDarkMode = isDark
                    )
                    
                    // MIDI connection indicator
                    if (viewModel.isMidiAvailable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        MidiIndicator(
                            isConnected = viewModel.isMidiDeviceConnected,
                            isDarkMode = isDark,
                            accentColor = accentBlue
                        )
                    }
                }
                
                // App Title - absolutely centered
                Text(
                    text = if (viewModel.isWurlitzerMode) "Wurly" else "Synth.io",
                    style = SynthTypography.heading.copy(
                        brush = Brush.horizontalGradient(
                            colors = if (viewModel.isWurlitzerMode) 
                                listOf(accentPeach, accentCoral, accentPink)
                            else 
                                listOf(accentPink, accentLavender, accentMint)
                        )
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Looper Controls - right side
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Loop/Record Button
                    LoopButton(
                        looperState = viewModel.looperState,
                        hasLoop = viewModel.looperHasLoop,
                        currentBeat = viewModel.looperCurrentBeat,
                        onClick = { viewModel.loopButtonClicked() },
                        onCancelRecording = { viewModel.cancelRecording() },
                        isDarkMode = isDark,
                        accentColor = accentMint
                    )
                    
                    // Play/Stop Button (only visible when loop exists)
                    if (viewModel.looperHasLoop || viewModel.looperState == LooperState.PLAYING) {
                        PlayStopButton(
                            isPlaying = viewModel.looperState == LooperState.PLAYING,
                            onClick = { viewModel.playStopLoop() },
                            isDarkMode = isDark,
                            accentColor = accentMint
                        )
                    }
                }
            }
            
            // Recording status indicator (shows during pre-count and recording)
            AnimatedVisibility(
                visible = isRecordingActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val statusText = when (looperState) {
                    LooperState.PRE_COUNT -> "Get Ready... ${viewModel.looperCurrentBeat + 1}"
                    LooperState.RECORDING -> "Recording... Bar ${viewModel.looperCurrentBar + 1}"
                    else -> ""
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentMint.copy(alpha = 0.2f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText,
                        style = SynthTypography.subheading.copy(
                            color = accentMint
                        )
                    )
                }
            }
        }
    }
}

/**
 * Collapsible/Accordion section with animated expand/collapse
 */
@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isDarkMode: Boolean,
    surfaceColor: Color,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val iconColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SynthShapes.large)
            .background(surfaceColor)
    ) {
        // Header (always visible, clickable to toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Accent indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = SynthTypography.subheading.copy(color = textColor)
                )
            }
            
            // Expand/collapse icon with rotation animation
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (isExpanded) 180f else 0f)
            )
        }
        
        // Content (animated visibility)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top
            ) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * MIDI connection indicator - shows when MIDI devices are available and connected
 */
@Composable
private fun MidiIndicator(
    isConnected: Boolean,
    isDarkMode: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isConnected) {
        accentColor.copy(alpha = 0.3f)
    } else {
        if (isDarkMode) DarkSurfaceLight.copy(alpha = 0.5f) else SurfaceWhite.copy(alpha = 0.5f)
    }
    
    val textColor = if (isConnected) {
        accentColor
    } else {
        if (isDarkMode) DarkTextSecondary else TextSecondary
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MIDI",
            style = SynthTypography.smallLabel.copy(
                color = textColor,
                fontWeight = if (isConnected) androidx.compose.ui.text.font.FontWeight.Bold 
                             else androidx.compose.ui.text.font.FontWeight.Normal
            )
        )
    }
}

@Composable
private fun ChorusModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        accentColor.copy(alpha = 0.3f)
    } else {
        if (isDarkMode) DarkSurfaceLight else SurfaceWhite
    }
    
    val borderColor = if (isSelected) accentColor else Color.Transparent
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = SynthTypography.label.copy(color = textColor)
        )
    }
}

@Composable
private fun LoopButton(
    looperState: LooperState,
    hasLoop: Boolean,
    currentBeat: Int,
    onClick: () -> Unit,
    onCancelRecording: () -> Unit,
    isDarkMode: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isActive = looperState == LooperState.PRE_COUNT || 
                   looperState == LooperState.RECORDING
    
    val backgroundColor = when {
        isActive -> accentColor.copy(alpha = 0.4f)
        hasLoop -> accentColor.copy(alpha = 0.3f)
        else -> if (isDarkMode) DarkSurfaceLight else SurfaceWhite.copy(alpha = 0.8f)
    }
    
    val iconColor = when {
        isActive -> if (isDarkMode) Color.White else accentColor
        hasLoop -> accentColor
        else -> if (isDarkMode) DarkTextSecondary else TextSecondary
    }
    
    val borderColor = when {
        isActive -> accentColor
        hasLoop -> accentColor.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable { 
                if (isActive) {
                    onCancelRecording()
                } else {
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            // Show beat counter during pre-count or recording - tap to cancel
            Text(
                text = "${currentBeat + 1}",
                style = SynthTypography.label.copy(
                    color = iconColor,
                    fontSize = 18.sp
                )
            )
        } else {
            // Show record icon (red circle)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasLoop) accentColor  // Green when loop exists
                        else Color(0xFFE53935)    // Red when no loop (record ready)
                    )
            )
        }
    }
}

@Composable
private fun PlayStopButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isPlaying) {
        accentColor.copy(alpha = 0.4f)
    } else {
        accentColor.copy(alpha = 0.3f)
    }
    
    val iconColor = accentColor
    
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, accentColor.copy(alpha = 0.5f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            // Custom stop icon (square)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(iconColor)
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play loop",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LoopOverrideDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) DarkSurfaceCard else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        title = {
            Text(
                text = "Override Loop?",
                style = SynthTypography.heading.copy(color = textColor)
            )
        },
        text = {
            Text(
                text = "You already have a recorded loop. Do you want to replace it with a new recording?",
                style = SynthTypography.body.copy(color = textColor)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text("Record New")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDarkMode) DarkTextSecondary else TextSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
