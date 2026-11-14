package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EduRepository
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.MandalaUnfoldContainer
import com.example.utils.SoundHelper
import com.example.viewmodel.AuthViewModel

@Composable
fun LessonDetailScreen(
    courseId: String,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val lessons = EduRepository.sampleLessons[courseId] ?: emptyList()
    var currentIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    val palette = LocalIndoPalette.current
    val context = LocalContext.current

    if (lessons.isEmpty()) {
        IndoAtmosphericBackdrop {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No lessons available for this course yet.", color = MaterialTheme.colorScheme.onBackground)
            }
        }
        return
    }

    val lesson = lessons[currentIndex]

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentSaffron) {
        MandalaUnfoldContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        text = "Lesson ${currentIndex + 1} of ${lessons.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showQuiz = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showQuiz) palette.accentSaffron else palette.elevatedSurface,
                            contentColor = if (!showQuiz) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Concept Notes", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showQuiz = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showQuiz) palette.accentSaffron else palette.elevatedSurface,
                            contentColor = if (showQuiz) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Interactive Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!showQuiz) {
                item {
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = palette.accentGold)
                                Text(
                                    text = "MASTER TEACHER NOTES",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentSaffron,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            HorizontalDivider(color = palette.goldHairline)

                            Text(
                                text = lesson.conceptNotes,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showQuiz = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentSaffron,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Take Quiz to Complete", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = palette.accentGold)
                                Text(
                                    text = "Earn +50 XP & Streak Bonus",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = lesson.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            lesson.options.forEachIndexed { index, option ->
                                val isSelected = selectedOption == index
                                val isCorrect = index == lesson.correctIndex
                                val containerColor = when {
                                    isAnswerSubmitted && isCorrect -> Color(0xFF10B981).copy(alpha = 0.22f)
                                    isAnswerSubmitted && isSelected && !isCorrect -> palette.accentVermilion.copy(alpha = 0.22f)
                                    isSelected -> palette.secondaryIndigo
                                    else -> palette.elevatedSurface
                                }

                                IndoToneSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(!isAnswerSubmitted) { selectedOption = index },
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = containerColor,
                                    hairlineColor = if (isSelected) palette.accentSaffron.copy(alpha = 0.7f) else palette.goldHairline
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { if (!isAnswerSubmitted) selectedOption = index },
                                            colors = RadioButtonDefaults.colors(selectedColor = palette.accentSaffron)
                                        )
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (isAnswerSubmitted) {
                                IndoToneSurface(
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = if (selectedOption == lesson.correctIndex) Color(0xFF10B981).copy(alpha = 0.14f) else palette.accentVermilion.copy(alpha = 0.14f),
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = if (selectedOption == lesson.correctIndex) "Correct Mastery!" else "Review Explanation:",
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedOption == lesson.correctIndex) Color(0xFF10B981) else palette.accentVermilion
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = lesson.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (!isAnswerSubmitted) {
                                Button(
                                    onClick = {
                                        if (selectedOption != null) {
                                            isAnswerSubmitted = true
                                            if (selectedOption == lesson.correctIndex) {
                                                SoundHelper.playTempleChime(context)
                                                authViewModel.updateProgress(50, lesson.id)
                                            } else {
                                                SoundHelper.playWoodyTock(context)
                                            }
                                        }
                                    },
                                    enabled = selectedOption != null,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accentSaffron,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Check Answer", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isAnswerSubmitted = false
                                        selectedOption = null
                                        if (currentIndex < lessons.size - 1) {
                                            currentIndex++
                                            showQuiz = false
                                        } else {
                                            isCompleted = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (currentIndex < lessons.size - 1) "Next Lesson" else "Course Completed!",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
