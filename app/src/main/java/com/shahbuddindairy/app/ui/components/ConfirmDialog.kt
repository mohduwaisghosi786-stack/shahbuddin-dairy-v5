package com.shahbuddindairy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahbuddindairy.app.domain.CalculationEngine
import com.shahbuddindairy.app.ui.theme.DairyGreenDark
import com.shahbuddindairy.app.ui.theme.StatusRed

@Composable
fun TransactionConfirmDialog(
    customerName: String,
    customerId: Long,
    liters: Double,
    rate: Double,
    todayBill: Double,
    prevDue: Double,
    todayCash: Double,
    newDue: Double,
    advance: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Confirm Delivery & Payment",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "#$customerId  $customerName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow("Milk Delivered", "%.2f L @ ₹%.0f/L".format(liters, rate))
                DetailRow("Today's Bill", CalculationEngine.formatMoney(todayBill), isBold = true)
                DetailRow("Previous Due", CalculationEngine.formatMoney(prevDue), isWarning = prevDue > 0)
                DetailRow("Today's Cash Paid", CalculationEngine.formatMoney(todayCash), isGreen = todayCash > 0)

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(6.dp))

                if (newDue > 0) {
                    DetailRow("Remaining Due", CalculationEngine.formatMoney(newDue), isWarning = true, isLarge = true)
                } else {
                    DetailRow("Remaining Due", "₹0 (Cleared)", isGreen = true, isLarge = true)
                }

                if (advance > 0) {
                    DetailRow("Advance Balance", CalculationEngine.formatMoney(advance), isGreen = true, isLarge = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        onConfirm()
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...", fontWeight = FontWeight.Bold)
                } else {
                    Text("CONFIRM & SAVE", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = colorScheme.surface
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isWarning: Boolean = false,
    isGreen: Boolean = false,
    isLarge: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isLarge) 13.5.sp else 12.5.sp,
            color = colorScheme.onSurfaceVariant,
            fontWeight = if (isLarge) FontWeight.SemiBold else FontWeight.Normal
        )
        val textColor = when {
            isWarning -> StatusRed
            isGreen -> colorScheme.primary
            else -> colorScheme.onSurface
        }
        Text(
            text = value,
            fontSize = if (isLarge) 15.sp else 13.sp,
            fontWeight = if (isBold || isLarge) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor
        )
    }
}
