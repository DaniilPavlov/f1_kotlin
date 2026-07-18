package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.util.openUrl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverInfoBottomSheet(driver: DriverModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun value(value: String?): String = value?.takeUnless { it.isBlank() || it.equals("none", true) } ?: "—"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(driver.fullName, style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            Text("${stringResource(R.string.driver_code)}: ${value(driver.code)}", style = AppStyles.body)
            Text("${stringResource(R.string.driver_number)}: ${value(driver.permanentNumber)}", style = AppStyles.body)
            Text("${stringResource(R.string.nationality)}: ${value(driver.nationality)}", style = AppStyles.body)
            Text(
                "${stringResource(R.string.date_of_birth)}: ${formatDate(driver.dateOfBirth)}",
                style = AppStyles.body,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { openUrl(context, driver.url) }) {
                Text(stringResource(R.string.open_in_wikipedia))
            }
        }
    }
}

private fun formatDate(value: String?): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
}.getOrElse { "—" }
