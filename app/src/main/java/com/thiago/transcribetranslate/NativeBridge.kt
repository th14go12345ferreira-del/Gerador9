package com.thiago.transcribetranslate

import android.content.res.AssetManager

object NativeBridge {
    init { System.loadLibrary("transcribetranslate") }

    external fun loadModelFromAsset(assetManager: AssetManager, assetPath: String): Boolean
    external fun transcribe(samples: FloatArray, language: String): String
    external fun releaseModel()
    external fun status(): String
}
