package com.thiago.transcribetranslate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thiago.transcribetranslate.transcription.Resampler
import com.thiago.transcribetranslate.transcription.WavAudioReader
import com.thiago.transcribetranslate.translation.OfflineTranslationRepository
import com.thiago.transcribetranslate.translation.TranslationLanguage
import com.thiago.transcribetranslate.translation.TranslationLanguages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App() } } }
    }

    override fun onDestroy() {
        NativeBridge.releaseModel()
        super.onDestroy()
    }
}

@Composable
private fun App() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Transcrever", "Tradução", "Downloads")
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TranscribeTranslate", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        Spacer(Modifier.height(18.dp))
        when (selectedTab) {
            0 -> TranscriptionPage()
            1 -> TranslationPage()
            else -> DownloadsPage()
        }
    }
}

@Composable
private fun TranscriptionPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Carregando modelo Whisper...") }
    var result by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var language by remember { mutableStateOf("pt") }

    LaunchedEffect(Unit) {
        status = withContext(Dispatchers.Default) {
            if (NativeBridge.loadModelFromAsset(context.assets, "models/ggml-tiny.bin"))
                "Modelo Tiny carregado."
            else "Erro ao carregar ggml-tiny.bin."
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            status = "Arquivo selecionado."
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    ) {
        Text(status)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { picker.launch(arrayOf("audio/wav", "audio/x-wav")) }) {
            Text("Escolher áudio WAV")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Idioma (ex.: pt, en, es)") })
        Spacer(Modifier.height(10.dp))
        Button(enabled = selectedUri != null, onClick = {
            val uri = selectedUri ?: return@Button
            scope.launch {
                status = "Convertendo e transcrevendo..."
                result = withContext(Dispatchers.Default) {
                    runCatching {
                        val audio = WavAudioReader.read(context.contentResolver, uri)
                        val pcm16k = Resampler.to16k(audio.samples, audio.sampleRate)
                        NativeBridge.transcribe(pcm16k, language.ifBlank { "pt" })
                    }.getOrElse { "ERRO: ${it.message}" }
                }
                status = "Concluído."
            }
        }) { Text("Iniciar transcrição") }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = result,
            onValueChange = {},
            readOnly = true,
            label = { Text("Transcrição") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp)
        )
    }
}

@Composable
private fun TranslationPage() {
    val context = LocalContext.current
    val repository = remember { OfflineTranslationRepository() }
    var source by remember { mutableStateOf(TranslationLanguages.common.first { it.code == "pt" }) }
    var target by remember { mutableStateOf(TranslationLanguages.common.first { it.code == "en" }) }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Baixe os idiomas na aba Downloads uma vez. Depois, a tradução funciona offline.") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    ) {
        LanguageSelector("Idioma original", source) { source = it }
        Spacer(Modifier.height(10.dp))
        LanguageSelector("Traduzir para", target) { target = it }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Texto para traduzir") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp)
        )
        Spacer(Modifier.height(12.dp))
        Button(enabled = input.isNotBlank(), onClick = {
            status = "Traduzindo..."
            repository.translate(
                text = input,
                sourceCode = source.code,
                targetCode = target.code,
                onSuccess = { translated ->
                    output = translated
                    status = "Tradução concluída."
                },
                onError = { error ->
                    status = "Erro: ${error.message ?: "modelo ainda não está disponível"}"
                    Toast.makeText(context, "Verifique os modelos na aba Downloads", Toast.LENGTH_LONG).show()
                }
            )
        }) { Text("Traduzir offline") }
        Spacer(Modifier.height(12.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = output,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tradução") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
        )
    }
}

@Composable
private fun LanguageSelector(label: String, selected: TranslationLanguage, onSelected: (TranslationLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TranslationLanguages.common.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.name) },
                    onClick = { onSelected(language); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DownloadsPage() {
    val repository = remember { OfflineTranslationRepository() }
    var downloaded by remember { mutableStateOf(setOf<String>()) }
    var status by remember { mutableStateOf("Carregando modelos instalados...") }

    fun refresh() {
        repository.getDownloadedLanguages()
            .addOnSuccessListener { models ->
                downloaded = models.map { it.language }.toSet()
                status = "Modelos de tradução instalados: ${downloaded.size}"
            }
            .addOnFailureListener { error -> status = "Erro: ${error.message}" }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Whisper instalado no APK")
        Text("ggml-tiny.bin")
        Spacer(Modifier.height(16.dp))
        Text("Modelos de tradução")
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        TranslationLanguages.common.forEach { language ->
            val installed = downloaded.contains(language.code)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(language.name)
                        Text(if (installed) "Baixado — disponível offline" else "Não baixado", style = MaterialTheme.typography.bodySmall)
                    }
                    if (installed) {
                        OutlinedButton(onClick = {
                            status = "Removendo ${language.name}..."
                            repository.deleteLanguage(language.code)
                                .addOnSuccessListener { refresh() }
                                .addOnFailureListener { status = "Erro: ${it.message}" }
                        }) { Text("Remover") }
                    } else {
                        Button(onClick = {
                            status = "Baixando ${language.name}..."
                            repository.downloadLanguage(language.code)
                                .addOnSuccessListener { refresh() }
                                .addOnFailureListener { status = "Erro: ${it.message}" }
                        }) { Text("Baixar") }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { refresh() }) { Text("Atualizar lista") }
    }
}
