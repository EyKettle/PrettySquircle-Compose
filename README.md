# PrettySquircle Compose

English | [中文](README_CN.md)

[![JitPack](https://jitpack.io/v/EyKettle/PrettySquircle-Compose.svg)](https://jitpack.io/#EyKettle/PrettySquircle-Compose)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Made for Jetpack Compose](https://img.shields.io/badge/Made%20for-Jetpack%20Compose-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

A `Shape` library for Jetpack Compose that generates rectangles with smoothly continuous corners.

You can see a live demo [here](https://squircle.eykettle.top/) (The implementation logic is consistent between the Compose and Web versions).

If you're not familiar with what a squircle is, or want to understand the principles behind it, check out the [original article](https://www.figma.com/blog/desperately-seeking-squircles/).

## Setup

### Basic

**Step 1.** Add the JitPack repository to your root `settings.gradle.kts` file:

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... other repositories
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2.** Add the dependency to your module's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.github.EyKettle:PrettySquircle-Compose:1.0.0")
}
```

### With Version Catalog

If you are using `libs.versions.toml` to manage your dependencies, you can add it as follows:

**Step 1.** (Same as before) Add the JitPack repository in your root `settings.gradle.kts` file.

**Step 2.** Add the following entries to your `libs.versions.toml` file:

```toml
[versions]
prettySquircle = "1.0.0"

[libraries]
prettySquircle = { group = "com.github.EyKettle", name = "PrettySquircle-Compose", version.ref = "prettySquircle" }
```

**Step 3.** Add the dependency to your module's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation(libs.prettySquircle)
}
```

## Usage

`Squircle` can be used just like any standard `Shape`.

### Basic Usage

You can specify a single corner radius for all corners, just like with `RoundedCornerShape`.

```kotlin
import com.eykettle.squircle.shape.Squircle

@Composable
fun MyComponent() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(Squircle(cornerRadius = 20.dp))
            .background(Color.Red)
    )
}
```

### Controlling the Smoothness

`cornerSmoothing` is the core parameter of `Squircle`. It controls the smoothness of the curve (G2 continuity) and accepts a value from `0.0f` to `1.0f`.

- **`0.0f`**: Equivalent to a standard `RoundedCornerShape` (no smoothing).
- **`1.0f`**: Maximum smoothness, where the curve starts transitioning to the straight edge as early as possible.

For convenience, the library provides several presets:

```kotlin
import com.eykettle.squircle.shape.CornerSmoothing
import com.eykettle.squircle.shape.Squircle

// Use the default iOS smoothness (0.6f)
val iosStyleShape = Squircle(
    cornerRadius = 24.dp,
    cornerSmoothing = CornerSmoothing.iOS
)

// Use a well-balanced preset (0.8f)
val prettyShape = Squircle(
    cornerRadius = 24.dp,
    cornerSmoothing = CornerSmoothing.Pretty // Default value
)

// Use maximum smoothness (1.0f)
val maxSmoothShape = Squircle(
    cornerRadius = 24.dp,
    cornerSmoothing = CornerSmoothing.Max
)
```

### Individual Corner Radii

Similar to `RoundedCornerShape`, you can also specify an independent radius for each corner.

```kotlin
val customShape = Squircle(
    topLeftRadius = 20.dp,
    topRightRadius = 8.dp,
    bottomRightRadius = 20.dp,
    bottomLeftRadius = 8.dp,
    cornerSmoothing = CornerSmoothing.iOS
)
```

## License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.

## Acknowledgements

- Original Article: [Desperately seeking squircles](https://www.figma.com/blog/desperately-seeking-squircles/)
- Original Project: [FigmaSquircle](https://github.com/phamfoo/figma-squircle)
- Optimized Version: [PrettySquircle](https://github.com/eykettle/pretty-squircle)
