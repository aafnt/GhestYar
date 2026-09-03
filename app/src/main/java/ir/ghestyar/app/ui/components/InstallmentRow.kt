package ir.ghestyar.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.ghestyar.app.data.entity.InstallmentEntity
import ir.ghestyar.app.domain.calculator.InstallmentCalculator
import ir.ghestyar.app.domain.calculator.PersianDateConverter
import ir.ghestyar.app.domain.model.InstallmentStatus
import ir.ghestyar.app.ui.theme.LocalInstallmentStatusColors
import ir.ghestyar.app.utils.PersianNumberUtils
import java.time.LocalDate

/**
 * ردیف نمایش یک قسط. طبق بند ۳۱: دو ضربه سریع (Double Tap) دیالوگ ثبت پرداخت را باز می‌کند.
 * طبق بند ۳۲: فشار طولانی (Long Press) منوی ویرایش/پرداخت/لغو پرداخت را باز می‌کند.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallmentRow(
    installment: InstallmentEntity,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dueDate = LocalDate.parse(installment.dueDate)
    val paidDate = installment.paidDate?.let(LocalDate::parse)
    val status = InstallmentStatus.of(dueDate, paidDate, today)
    val colors = LocalInstallmentStatusColors.current

    val (dotColor, bgColor) = when (status) {
        InstallmentStatus.PAID -> colors.paid to colors.paidBg
        InstallmentStatus.OVERDUE -> colors.overdue to colors.overdueBg
        InstallmentStatus.UPCOMING -> colors.upcoming to colors.upcomingBg
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .combinedClickable(onClick = {}, onDoubleClick = onDoubleTap, onLongClick = onLongPress)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "قسط ${PersianNumberUtils.formatNumber(installment.installmentNumber)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "سررسید: ${PersianDateConverter.formatFull(dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (status) {
                InstallmentStatus.OVERDUE -> {
                    val days = InstallmentCalculator.overdueDays(dueDate, today)
                    Text(
                        "${PersianNumberUtils.formatNumber(days.toInt())} روز گذشته",
                        style = MaterialTheme.typography.bodySmall,
                        color = dotColor
                    )
                }
                InstallmentStatus.UPCOMING -> {
                    val days = InstallmentCalculator.remainingDays(dueDate, today)
                    Text(
                        "${PersianNumberUtils.formatNumber(days.toInt())} روز مانده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                InstallmentStatus.PAID -> {
                    if (paidDate != null) {
                        val delay = InstallmentCalculator.delayDays(dueDate, paidDate)
                        val text = if (delay > 0) {
                            "پرداخت: ${PersianDateConverter.formatFull(paidDate)} (${PersianNumberUtils.formatNumber(delay.toInt())} روز تأخیر)"
                        } else {
                            "پرداخت: ${PersianDateConverter.formatFull(paidDate)}"
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall, color = dotColor)
                    }
                }
            }
        }
        Text(
            PersianNumberUtils.formatToman(installment.amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
