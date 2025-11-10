package com.eykettle.squircle.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SquircleUiState(
    val width: Float = 150f,
    val height: Float = 150f,
    val cornerSmoothing: Float = 0.8f,
    val topLeftRadius: Float = 32f,
    val topRightRadius: Float = 32f,
    val bottomRightRadius: Float = 32f,
    val bottomLeftRadius: Float = 32f,
)

class SquircleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SquircleUiState())
    val uiState = _uiState.asStateFlow()

    fun updateWidth(width: Float) {
        _uiState.update { currentState ->
            currentState.copy(width = width)
        }
    }

    fun updateHeight(height: Float) {
        _uiState.update { currentState ->
            currentState.copy(height = height)
        }
    }

    fun updateSmoothing(smoothing: Float) {
        _uiState.update { currentState ->
            currentState.copy(cornerSmoothing = smoothing)
        }
    }

    fun updateTopLeftRadius(radius: Float) {
        _uiState.update { currentState ->
            currentState.copy(topLeftRadius = radius)
        }
    }

    fun updateTopRightRadius(radius: Float) {
        _uiState.update { currentState ->
            currentState.copy(topRightRadius = radius)
        }
    }

    fun updateBottomRightRadius(radius: Float) {
        _uiState.update { currentState ->
            currentState.copy(bottomRightRadius = radius)
        }
    }

    fun updateBottomLeftRadius(radius: Float) {
        _uiState.update { currentState ->
            currentState.copy(bottomLeftRadius = radius)
        }
    }

    fun updateAllRadius(radius: Float) {
        _uiState.update { currentState ->
            currentState.copy(
                topLeftRadius = radius,
                topRightRadius = radius,
                bottomRightRadius = radius,
                bottomLeftRadius = radius
            )
        }
    }
}