package com.baltajmn.flowtime.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.Title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerAlertDialog(isOpen: Boolean, onCloseDialog: (Boolean) -> Unit) {
    var openDialog = remember { mutableStateOf(isOpen) }
    if (openDialog.value) {
        BasicAlertDialog(
            onDismissRequest = {
                openDialog.value = false
                onCloseDialog.invoke(true)
            },
            content = {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()

                            .background(color = MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = LocalContext.current.getString(R.string.dialog_title),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = Title.copy(
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = LocalContext.current.getString(R.string.dialog_subtitle),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = SmallTitle.copy(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = {
                                    openDialog.value = false
                                    onCloseDialog.invoke(false)
                                },
                                modifier = Modifier.padding(4.dp),
                            ) {
                                Text(
                                    text = LocalContext.current.getString(R.string.dialog_cancel),
                                    style = SmallTitle.copy(color = MaterialTheme.colorScheme.tertiary)
                                )
                            }
                            TextButton(
                                onClick = {
                                    openDialog.value = false
                                    onCloseDialog.invoke(true)
                                },
                                modifier = Modifier.padding(4.dp),
                            ) {
                                Text(
                                    text = LocalContext.current.getString(R.string.dialog_confirm),
                                    style = SmallTitle.copy(color = MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }

                }
            }
        )
    }
}