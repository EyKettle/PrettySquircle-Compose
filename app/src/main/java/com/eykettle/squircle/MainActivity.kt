package com.eykettle.squircle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eykettle.squircle.ui.components.LabeledSlider
import com.eykettle.squircle.shape.CornerSmoothing
import com.eykettle.squircle.shape.Squircle
import com.eykettle.squircle.ui.SquircleViewModel
import com.eykettle.squircle.ui.theme.PrettySquircleTheme

private val peekHeight = 408.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrettySquircleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) { innerPadding ->
                    SquircleDemoScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquircleDemoScreen(
    modifier: Modifier = Modifier,
    viewModel: SquircleViewModel = viewModel(),
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        modifier = modifier, scaffoldState = scaffoldState,
        sheetContent = {
            SquircleAdjustArea(
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )
        },
        sheetPeekHeight = peekHeight
    ) {
        SquirclePreview(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            viewModel = viewModel
        )
    }
}

@Composable
fun SquirclePreview(
    modifier: Modifier = Modifier,
    viewModel: SquircleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp, bottom = peekHeight, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = uiState.width.dp, height = uiState.height.dp)
                .clip(
                    Squircle(
                        topLeftRadius = uiState.topLeftRadius.dp,
                        topRightRadius = uiState.topRightRadius.dp,
                        bottomRightRadius = uiState.bottomRightRadius.dp,
                        bottomLeftRadius = uiState.bottomLeftRadius.dp,
                        cornerSmoothing = CornerSmoothing(uiState.cornerSmoothing)
                    )
                )
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun SquircleAdjustArea(
    modifier: Modifier = Modifier,
    viewModel: SquircleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var unifiedCornerRadius by remember { mutableFloatStateOf(uiState.topLeftRadius) }

    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LabeledSlider(
            label = "Width",
            value = uiState.width,
            valueRange = 50f..300f,
            onValueChange = viewModel::updateWidth
        )
        LabeledSlider(
            label = "Height",
            value = uiState.height,
            valueRange = 50f..300f,
            onValueChange = viewModel::updateHeight
        )

        LabeledSlider(
            label = "Corner Smoothing",
            value = uiState.cornerSmoothing,
            valueRange = 0f..1f,
            onValueChange = viewModel::updateSmoothing
        )

        val radiusRange = 0f..300f;
        LabeledSlider(
            label = "All Radius",
            value = unifiedCornerRadius,
            valueRange = radiusRange,
            onValueChange = { newValue ->
                unifiedCornerRadius = newValue
                viewModel.updateAllRadius(newValue)
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LabeledSlider(
                modifier = Modifier.weight(0.5f),
                label = "Top Left",
                value = uiState.topLeftRadius,
                valueRange = radiusRange,
                onValueChange = viewModel::updateTopLeftRadius
            )
            LabeledSlider(
                modifier = Modifier.weight(0.5f),
                label = "Top Right",
                value = uiState.topRightRadius,
                valueRange = radiusRange,
                onValueChange = viewModel::updateTopRightRadius
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LabeledSlider(
                modifier = Modifier.weight(0.5f),
                label = "Bottom Left",
                value = uiState.bottomLeftRadius,
                valueRange = radiusRange,
                onValueChange = viewModel::updateBottomLeftRadius
            )
            LabeledSlider(
                modifier = Modifier.weight(0.5f),
                label = "Bottom Right",
                value = uiState.bottomRightRadius,
                valueRange = radiusRange,
                onValueChange = viewModel::updateBottomRightRadius
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AdjustationPreview() {
    PrettySquircleTheme {
        SquircleDemoScreen(modifier = Modifier.fillMaxSize())
    }
}