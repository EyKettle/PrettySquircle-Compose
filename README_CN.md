# PrettySquircle Compose

[English](README.md) | 中文

[![JitPack](https://jitpack.io/v/EyKettle/PrettySquircle-Compose.svg)](https://jitpack.io/#EyKettle/PrettySquircle-Compose)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Made for Jetpack Compose](https://img.shields.io/badge/Made%20for-Jetpack%20Compose-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

一个 Jetpack Compose 的 `Shape` 库，用于生成衔接平滑的圆角矩形。

可以[在此](https://squircle.eykettle.top/)直接实时查看真实效果（Compose 的实现逻辑和 Web 端一致）。

如果不了解 Squircle 是什么，或者想要了解其中的原理，可以查看[原始文章](https://www.figma.com/blog/desperately-seeking-squircles/)。

## 集成

**步骤 1.** 在你的根项目 `settings.gradle.kts` 文件中添加 JitPack 仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... 其他仓库
        maven { url = uri("https://jitpack.io") }
    }
}
```

**步骤 2.** 在你的模块 `build.gradle.kts` 文件中添加依赖：

```kotlin
dependencies {
    implementation("com.github.EyKettle:PrettySquircle-Compose:1.0.0")
}
```

## 使用方法

`Squircle` 的使用方式与任何标准的 `Shape` 完全相同。

### 基础用法

你可以像使用 `RoundedCornerShape` 一样，为所有角指定一个统一的圆角半径。

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

### 控制平滑度

`cornerSmoothing` 是 `Squircle` 的核心参数，它控制着曲线的平滑程度（G2 连续性）。它的取值范围为 `0.0f` 到 `1.0f`。

- **`0.0f`**: 完全等同于 `RoundedCornerShape` (无平滑)。
- **`1.0f`**: 最大程度的平滑，曲线会尽早开始向直线过渡。

为了方便使用，库中提供了一些预设值：

```kotlin
import com.eykettle.squircle.shape.CornerSmoothing
import com.eykettle.squircle.shape.Squircle

// 使用 iOS 默认的平滑度 (0.6f)
val iosStyleShape = Squircle(
    cornerRadius = 24.dp,
    cornerSmoothing = CornerSmoothing.iOS
)

// 使用一个中规中矩的预设值 (0.8f)
val prettyShape = Squircle(
    cornerRadius = 24.dp,
    cornerSmoothing = CornerSmoothing.Pretty // 默认值
)

// 使用最大平滑度 (1.0f)
val maxSmoothShape = Squircle(
    cornerRadius = 24.dp,
    cornerSmoothing = CornerSmoothing.Max
)
```

### 为每个角指定不同半径

与 `RoundedCornerShape` 一样，你也可以为每个角指定独立的半径值。

```kotlin
val customShape = Squircle(
    topLeftRadius = 20.dp,
    topRightRadius = 8.dp,
    bottomRightRadius = 20.dp,
    bottomLeftRadius = 8.dp,
    cornerSmoothing = CornerSmoothing.iOS
)
```

## 授权协议

本项目采用 Apache 2.0 许可证。详情请见 [LICENSE](LICENSE) 文件。

## 致谢

- 原始文献: [《Desperately seeking squircles》](https://www.figma.com/blog/desperately-seeking-squircles/)
- 原始项目: [FigmaSquircle](https://github.com/phamfoo/figma-squircle)
- 优化版本: [PrettySquircle](https://github.com/eykettle/pretty-squircle)
