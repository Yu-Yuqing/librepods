package me.kavishdevar.librepods.presentation.screens

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.wear.WearBackup
import me.kavishdevar.librepods.data.wear.WearStatisticsRepository
import java.io.IOException
import java.time.LocalDate

internal class WearBackupModel(private val app: Application) : AndroidViewModel(app) {
    var busy by mutableStateOf(false)
        private set

    fun transfer(uri: Uri, importing: Boolean) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val message = try {
                withContext(Dispatchers.IO) {
                    val repo = WearStatisticsRepository.get(app)
                    if (importing) {
                        val sessions = app.contentResolver.openInputStream(uri)?.use(WearBackup::read)
                            ?: throw IOException()
                        val changed = repo.mergeBackup(sessions)
                        app.getString(R.string.wear_import_done, changed, sessions.size - changed)
                    } else {
                        val sessions = repo.snapshot()
                        val output = app.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
                        output.use { WearBackup.write(it, sessions) }
                        app.getString(R.string.wear_export_done, sessions.size)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                app.getString(if (importing) R.string.wear_import_error else R.string.wear_export_error)
            } finally {
                busy = false
            }
            Toast.makeText(app, message, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
internal fun WearBackupMenu(model: WearBackupModel = viewModel()) {
    var expanded by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) model.transfer(it, false)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) model.transfer(it, true)
    }
    Box {
        IconButton(onClick = { expanded = true }, enabled = !model.busy) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.wear_backup_menu))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.wear_export)) }, onClick = {
                expanded = false
                save.launch("LibrePods-wear-${LocalDate.now()}.json")
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.wear_import)) }, onClick = {
                expanded = false
                open.launch(arrayOf("*/*"))
            })
        }
    }
}
