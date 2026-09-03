package ir.ghestyar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.ghestyar.app.presentation.home.LoanCardUiModel
import ir.ghestyar.app.ui.theme.LocalInstallmentStatusColors
import ir.ghestyar.app.utils.PersianNumberUtils
import ir.ghestyar.app.domain.calculator.PersianDateConverter

@Composable
fun LoanCard(
    loan: LoanCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalInstallmentStatusColors.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoanImage(loan.imagePath, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(loan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        PersianNumberUtils.formatToman(loan.totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${PersianNumberUtils.formatNumber(loan.installmentCount)} قسط",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("🟢", "پرداخت‌شده", loan.paidCount, statusColors.paidBg, statusColors.paid, Modifier.weight(1f))
                MiniStat("🔴", "معوق", loan.overdueCount, statusColors.overdueBg, statusColors.overdue, Modifier.weight(1f))
                MiniStat("⚪", "آینده", loan.upcomingCount, statusColors.upcomingBg, statusColors.upcoming, Modifier.weight(1f))
            }

            if (loan.nextInstallmentAmount != null && loan.nextInstallmentDueDate != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("قسط بعدی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${PersianNumberUtils.formatToman(loan.nextInstallmentAmount)} — ${PersianDateConverter.formatFull(loan.nextInstallmentDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(
    emoji: String,
    label: String,
    count: Int,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(4.dp))
        Text(
            PersianNumberUtils.formatNumber(count),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoanImage(imagePath: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    if (imagePath != null) {
        AsyncImage(
            model = imagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
