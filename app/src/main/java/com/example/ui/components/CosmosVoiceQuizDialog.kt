package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rag.VoiceQuizStep
import com.example.rag.VoiceQuizUiState
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette

@Composable
fun CosmosVoiceQuizDialog(
    state: VoiceQuizUiState,
    onSubmitAnswer: (String) -> Unit,
    onRetryListening: () -> Unit,
    onNextQuestion: () -> Unit,
    onStopQuiz: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalIndoPalette.current
    var manualInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = {
            onStopQuiz()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("cosmos_voice_quiz_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = palette.elevatedSurface,
            border = BorderStroke(1.4.dp, palette.accentGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFE88D14), CircleShape)
                                .border(1.dp, palette.accentGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Voice Quiz Arena",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Textbook Question & Answer",
                                fontSize = 11.sp,
                                color = palette.accentGold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onStopQuiz()
                            onDismiss()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Quiz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Quiz Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (state.step == VoiceQuizStep.ROUND_SUMMARY) {
                        // Round Summary Screen
                        VoiceQuizSummaryCard(
                            score = state.score,
                            total = state.totalQuestions,
                            onFinish = {
                                onStopQuiz()
                                onDismiss()
                            }
                        )
                    } else {
                        // Question Card
                        val q = state.currentQuestion
                        if (q != null) {
                            // Subject & Chapter Tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.secondaryIndigo,
                                    border = BorderStroke(0.8.dp, palette.goldHairline)
                                ) {
                                    Text(
                                        text = "${q.subjectName} • Ch.${q.chapterNumber} ${q.topicTitle}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentCyan
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.cardSurface,
                                    border = BorderStroke(0.8.dp, palette.goldHairline)
                                ) {
                                    Text(
                                        text = "Q ${state.currentQuestionIndex + 1}/${state.totalQuestions} • Score: ${state.score}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.accentGold
                                    )
                                }
                            }

                            // Question Prose
                            IndoToneSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = palette.cardSurface,
                                hairlineColor = palette.goldHairline
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "QUESTION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = Color(0xFFE88D14)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.questionPrompt,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            // Interactive Audio & Listening Stage
                            VoiceQuizListeningStage(
                                step = state.step,
                                rms = state.micRms,
                                transcribedText = state.transcribedAnswer,
                                statusText = state.statusText,
                                onRetryListening = onRetryListening
                            )

                            // Feedback Card (when evaluating or feedback speaking)
                            AnimatedVisibility(
                                visible = state.step == VoiceQuizStep.FEEDBACK_SPEAKING || state.isAnswerCorrect != null
                            ) {
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = if (state.isAnswerCorrect == true) Color(0xFF0F382A) else palette.cardSurface,
                                    hairlineColor = if (state.isAnswerCorrect == true) Color(0xFF10B981) else palette.accentGold
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (state.isAnswerCorrect == true) "✨ CORRECT!" else "📖 TEXTBOOK CONCEPT",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (state.isAnswerCorrect == true) Color(0xFF34D399) else palette.accentGold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.feedbackMessage.ifBlank { q.explanation },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (q.formulaLatex.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = palette.elevatedSurface,
                                                border = BorderStroke(0.8.dp, palette.goldHairline)
                                            ) {
                                                Text(
                                                    text = "Formula: ${q.formulaLatex}",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.accentCyan
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Manual Text Input Fallback (if environment is noisy or mic is silent)
                            if (state.step == VoiceQuizStep.LISTENING || state.step == VoiceQuizStep.ASKING_QUESTION) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualInput,
                                        onValueChange = { manualInput = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("voice_quiz_text_fallback"),
                                        placeholder = {
                                            Text("Or type your answer here...", fontSize = 12.sp)
                                        },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = palette.accentCyan,
                                            unfocusedBorderColor = palette.goldHairline
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            if (manualInput.isNotBlank()) {
                                                onSubmitAnswer(manualInput)
                                                manualInput = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFE88D14), CircleShape)
                                            .testTag("voice_quiz_submit_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Submit Answer",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onStopQuiz()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accentVermilion),
                        border = BorderStroke(1.dp, palette.accentVermilion.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Quiz", fontSize = 12.sp)
                    }

                    if (state.step != VoiceQuizStep.ROUND_SUMMARY) {
                        Button(
                            onClick = onNextQuestion,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88D14)),
                            modifier = Modifier.testTag("voice_quiz_next_button")
                        ) {
                            Text("Next Question", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceQuizListeningStage(
    step: VoiceQuizStep,
    rms: Float,
    transcribedText: String,
    statusText: String,
    onRetryListening: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val transition = rememberInfiniteTransition(label = "audio_wave")
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audio_pulse"
    )

    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = palette.elevatedSurface,
        hairlineColor = if (step == VoiceQuizStep.LISTENING) palette.accentCyan else palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated Audio Wave Visualizer
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.secondaryIndigo),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension / 2f - 4.dp.toPx()
                    val waveR = if (step == VoiceQuizStep.LISTENING) baseR * (1f + rms * 0.6f * pulse) else baseR

                    drawCircle(
                        color = if (step == VoiceQuizStep.LISTENING) palette.accentCyan else palette.accentGold,
                        radius = waveR,
                        center = c,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (step == VoiceQuizStep.LISTENING) palette.accentCyan else palette.accentGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Real-time spoken transcript preview
            if (transcribedText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.cardSurface,
                    border = BorderStroke(0.8.dp, palette.accentCyan)
                ) {
                    Text(
                        text = "\"$transcribedText\"",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentCyan
                    )
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            if (step == VoiceQuizStep.LISTENING) {
                IconButton(
                    onClick = onRetryListening,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Microphone",
                        tint = palette.accentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceQuizSummaryCard(
    score: Int,
    total: Int,
    onFinish: () -> Unit
) {
    val palette = LocalIndoPalette.current

    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.accentGold
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(palette.secondaryIndigo, CircleShape)
                    .border(2.dp, palette.accentGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score/$total",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accentGold
                )
            }

            Text(
                text = "QUIZ ROUND COMPLETED!",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = palette.accentGold
            )

            Text(
                text = when {
                    score == total -> "Spectacular mastery! You got every textbook question right!"
                    score >= total / 2 -> "Great performance! Your textbook fundamentals are strong."
                    else -> "Good practice! Review the concepts and try another round in Cosmos."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88D14)),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Return to Cosmos", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
