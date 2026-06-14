package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Contact
import com.example.ui.AetherisViewModel
import com.example.ui.theme.*

@Composable
fun ContactsScreen(viewModel: AetherisViewModel) {
    val contactsList by viewModel.contacts.collectAsState()
    var showAddContactDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Secure Address Book", style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
                Text("Locally encrypted contact card reference directory.", style = MaterialTheme.typography.bodyLarge, color = SoftMutedGray)
            }

            IconButton(
                onClick = { showAddContactDialog = true },
                modifier = Modifier
                    .background(NeonTeal, RoundedCornerShape(10.dp))
                    .testTag("add_contact_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (contactsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DeepSlate, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SoftMutedGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No contacts saved.", color = SoftMutedGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("contacts_list")
            ) {
                items(contactsList) { contact ->
                    ContactRowItem(contact, onDelete = { viewModel.deleteContact(contact) })
                }
            }
        }
    }

    if (showAddContactDialog) {
        var nameInput by remember { mutableStateOf("") }
        var addressInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = RichOnyx,
            title = { Text("Add Contact Card", color = NeonTeal, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Contact Label / Name", color = SoftMutedGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderDark,
                            focusedBorderColor = NeonTeal,
                            unfocusedTextColor = WarmWhite,
                            focusedTextColor = WarmWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Aetheris address (aet1...)", color = SoftMutedGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderDark,
                            focusedBorderColor = NeonTeal,
                            unfocusedTextColor = WarmWhite,
                            focusedTextColor = WarmWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_address_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notes / Tags", color = SoftMutedGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderDark,
                            focusedBorderColor = NeonTeal,
                            unfocusedTextColor = WarmWhite,
                            focusedTextColor = WarmWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMsg, color = PhantomRed, style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showAddContactDialog = false }) {
                        Text("Cancel", color = SoftMutedGray)
                    }

                    Button(
                        onClick = {
                            val res = viewModel.addContact(nameInput, addressInput, notesInput)
                            if (res.first) {
                                showAddContactDialog = false
                            } else {
                                errorMsg = res.second
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        modifier = Modifier.testTag("save_contact_btn")
                    ) {
                        Text("Save Card", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
fun ContactRowItem(contact: Contact, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DeepSlate, RoundedCornerShape(12.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.label, fontWeight = FontWeight.Bold, color = NeonTeal, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = contact.address,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = SoftMutedGray,
                fontSize = 11.sp
            )
            if (contact.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Notes: ${contact.notes}", color = WarmWhite, fontSize = 12.sp)
            }
        }

        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = PhantomRed.copy(alpha = 0.8f))
        }
    }
}
