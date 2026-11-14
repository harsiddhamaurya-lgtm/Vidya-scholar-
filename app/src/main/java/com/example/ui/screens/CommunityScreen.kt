package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.CommunityProject
import com.example.data.EduRepository
import com.example.data.UserProfile
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.MandalaUnfoldContainer
import com.example.utils.SoundHelper
import com.example.utils.indoTactileMotion

@Composable
fun CommunityScreen(
    userProfile: UserProfile?
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    var projects by remember { mutableStateOf(EduRepository.sampleProjects.toList()) }
    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("AI & Code") }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentGold) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sabha • Community Projects",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Create, share & grow together with fellow scholars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            SoundHelper.playClick(context)
                            showDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentSaffron,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publish", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(projects, key = { it.id }) { project ->
                val interaction = remember { MutableInteractionSource() }
                IndoToneSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .indoTactileMotion(interaction, sweepTint = palette.accentGold),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = palette.goldHairline
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.secondaryCopper
                            ) {
                                Text(
                                    text = project.category,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        SoundHelper.playWoodyTock(context)
                                        projects = projects.map {
                                            if (it.id == project.id) it.copy(upvotes = it.upvotes + 1) else it
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ThumbUp,
                                        contentDescription = "Upvote",
                                        tint = palette.accentSaffron,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${project.upvotes}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = palette.accentGold
                            )
                            Text(
                                text = "By ${project.authorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                MandalaUnfoldContainer {
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Publish Your Project",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Project Title") },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accentSaffron,
                                    unfocusedBorderColor = palette.goldHairline
                                )
                            )
                            OutlinedTextField(
                                value = newDesc,
                                onValueChange = { newDesc = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Short Description") },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accentSaffron,
                                    unfocusedBorderColor = palette.goldHairline
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newTitle.isNotBlank() && newDesc.isNotBlank()) {
                                            SoundHelper.playTempleChime(context)
                                            val newProj = CommunityProject(
                                                id = java.util.UUID.randomUUID().toString(),
                                                title = newTitle,
                                                description = newDesc,
                                                authorName = userProfile?.name ?: "Student",
                                                authorEmail = userProfile?.email ?: "",
                                                category = newCategory,
                                                upvotes = 1
                                            )
                                            projects = listOf(newProj) + projects
                                            showDialog = false
                                            newTitle = ""
                                            newDesc = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accentSaffron,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Post Project", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
