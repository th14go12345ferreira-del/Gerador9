package com.thiago.transcribetranslate.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * Translation is performed locally after the required ML Kit language models
 * have been downloaded to the device.
 */
class OfflineTranslationRepository {
    private val modelManager = RemoteModelManager.getInstance()

    fun downloadLanguage(code: String): Task<Void> {
        val language = requireLanguage(code)
        val model = TranslateRemoteModel.Builder(language).build()
        val conditions = DownloadConditions.Builder().build()
        return modelManager.download(model, conditions)
    }

    fun deleteLanguage(code: String): Task<Void> {
        val language = requireLanguage(code)
        val model = TranslateRemoteModel.Builder(language).build()
        return modelManager.deleteDownloadedModel(model)
    }

    fun getDownloadedLanguages(): Task<Set<TranslateRemoteModel>> =
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)

    fun translate(
        text: String,
        sourceCode: String,
        targetCode: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (text.isBlank()) {
            onSuccess("")
            return
        }
        val source = requireLanguage(sourceCode)
        val target = requireLanguage(targetCode)
        if (source == target) {
            onSuccess(text)
            return
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)

        // If the model is already present, this succeeds without network access.
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated ->
                        translator.close()
                        onSuccess(translated)
                    }
                    .addOnFailureListener { error ->
                        translator.close()
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                translator.close()
                onError(error)
            }
    }

    private fun requireLanguage(code: String): String =
        TranslateLanguage.fromLanguageTag(code)
            ?: throw IllegalArgumentException("Idioma não suportado: $code")
}
