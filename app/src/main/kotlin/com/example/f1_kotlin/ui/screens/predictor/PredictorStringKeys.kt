package com.example.f1_kotlin.ui.screens.predictor

import androidx.annotation.StringRes
import com.example.f1_kotlin.R

@StringRes
fun predictorFormErrorRes(key: String?): Int? = when (key) {
    "predictorNicknameErrorLength" -> R.string.predictor_nickname_error_length
    "predictorNicknameErrorChars" -> R.string.predictor_nickname_error_chars
    "predictorNicknameErrorTaken" -> R.string.predictor_nickname_error_taken
    "predictorLeaderboardErrorGeneric" -> R.string.predictor_leaderboard_error_generic
    "predictorLeaderboardOptInRequired" -> R.string.predictor_leaderboard_opt_in_required
    else -> null
}
