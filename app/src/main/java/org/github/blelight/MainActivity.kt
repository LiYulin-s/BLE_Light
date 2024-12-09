/*
author: LiYulin-s
license: GPLv3
time: CST 2024/9/12
 */


package org.github.blelight

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.github.blelight.ui.theme.BLELightTheme


val requiredPermissions = listOfNotNull(
    android.Manifest.permission.BLUETOOTH,
    android.Manifest.permission.BLUETOOTH_ADMIN,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.Manifest.permission.BLUETOOTH_SCAN else null,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.Manifest.permission.BLUETOOTH_CONNECT else null,
    android.Manifest.permission.ACCESS_FINE_LOCATION
)

fun checkAndRequestPermissions(activity: ComponentActivity) {
    val missingPermissions = requiredPermissions.filter {
        ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
    }
    if (missingPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), 1)
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions(this)
        enableEdgeToEdge()
        setContent {
            BLELightApp()
        }
    }
}

@Composable
fun BLELightApp() {
    BLELightTheme {
        App(modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(modifier: Modifier) {
    val model = BLEViewModel(LocalContext.current)
    var redValue by remember { mutableIntStateOf(0) }
    var blueValue by remember { mutableIntStateOf(0) }
    var greenValue by remember { mutableIntStateOf(0) }
    val connectionStatus by remember { model.connectionStatus }
    var textFieldVisibility by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "ESP BLE LED Example")
                        ConnectionStatus(status = connectionStatus)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(connectionStatus == "Disconnected") {
                FloatingActionButton(
                    onClick = { model.scan() },

                    ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShowColor(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                color = Color(
                    red = redValue,
                    green = greenValue,
                    blue = blueValue
                ),
                onClick = { textFieldVisibility = !textFieldVisibility }
            )
            AnimatedVisibility(textFieldVisibility) {
                HexColorInputField(
                    onColorSubmit = {
                        redValue = (it.red * 255).toInt()
                        greenValue = (it.green * 255).toInt()
                        blueValue = (it.blue * 255).toInt()
                        model.sendData(redValue, greenValue, blueValue)
                        textFieldVisibility = false
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColorSlider(
                    label = "Red",
                    value = redValue,
                    onValueChange = { redValue = it },
                    onValueChangeFinished = { model.sendData(redValue, greenValue, blueValue) },
                    sliderColor = Color.Red,
                )
                ColorSlider(
                    label = "Green",
                    value = greenValue,
                    onValueChange = { greenValue = it },
                    onValueChangeFinished = { model.sendData(redValue, greenValue, blueValue) },
                    sliderColor = Color.Green
                )
                ColorSlider(
                    label = "Blue",
                    value = blueValue,
                    onValueChange = { blueValue = it },
                    onValueChangeFinished = { model.sendData(redValue, greenValue, blueValue) },
                    sliderColor = Color.Blue,
                )
            }
        }
    }
}



@Composable
fun ColorSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    sliderColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label: $value",
            style = typography.bodyLarge,
            modifier = Modifier.width(100.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor
            ),
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ShowColor(modifier: Modifier = Modifier, color: Color, onClick: (() -> Unit)? = null) {
    val hexColor = String.format(
        "#%02X%02X%02X",
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )

    Box(
        modifier = modifier
            .background(color, shape = MaterialTheme.shapes.medium)
            .fillMaxWidth()
            .aspectRatio(2f)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center // 居中显示文本
    ) {
        Text(
            text = hexColor,
            color = if (color.luminance() > 0.5f) Color.Black else Color.White, // 根据背景亮度调整文本颜色
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun ConnectionStatus(
    status: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        "Connected" -> Color.Green
        "Disconnected" -> Color.Red
        "Connecting" -> Color.Yellow
        else -> Color.Gray // 未知状态的默认颜色
    }

    val statusText = when (status) {
        "Connected" -> "Connected"
        "Disconnected" -> "Disconnected"
        "Connecting" -> "Connecting"
        else -> "Unknown"
    }

    Row(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = statusText,
            modifier = Modifier.padding(start = 8.dp), // 指示器与文本之间的间距
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview
@Composable
private fun ConnectionStatusPreview() {
    BLELightTheme {
        Column {
            ConnectionStatus(status = "Connecting")
            ConnectionStatus(status = "Connected")
            ConnectionStatus(status = "Disconnected")
            ConnectionStatus(status = "Unknown")
        }
    }
}


@Composable
fun HexColorInputField(
    modifier: Modifier = Modifier,
    onColorSubmit: (Color) -> Unit = {}
) {
    var hexColor by remember { mutableStateOf("") } // 默认颜色为空
    var isValidColor by remember { mutableStateOf(true) } // 验证颜色格式是否有效

    // 动画状态
    val previewSize by animateDpAsState(
        targetValue = if (isValidColor && hexColor.isNotEmpty()) 100.dp else 0.dp, // 动态调整预览大小
        animationSpec = tween(durationMillis = 500) // 设置动画持续时间
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isValidColor && hexColor.isNotEmpty()) 1f else 0.5f, // 动态调整按钮透明度
        animationSpec = tween(durationMillis = 300) // 设置动画持续时间
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = hexColor,
                onValueChange = {
                    hexColor = it.uppercase() // 自动转为大写
                    isValidColor = isValidHexColor(hexColor) // 验证颜色格式
                },
                label = { Text("Enter Hex Color") },
                placeholder = { Text("#RRGGBB") },
                isError = !isValidColor && hexColor.isNotEmpty(), // 如果格式无效且非空，显示错误样式
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp), // 添加按钮间的间距
            )

            // 发送按钮
            IconButton(
                onClick = {
                    if (isValidColor && hexColor.isNotEmpty()) {
                        onColorSubmit(Color(android.graphics.Color.parseColor(hexColor)).copy())
                        hexColor = ""
                        isValidColor = false
                    }
                },
                enabled = isValidColor && hexColor.isNotEmpty(), // 按钮仅在有效输入时可用
                modifier = Modifier
                    .height(56.dp)
                    .alpha(buttonAlpha) // 使用动画调整透明度
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(id = R.string.send_color)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 颜色预览动画
        if (previewSize > 0.dp) { // 仅在有预览大小时绘制
            Box(
                modifier = Modifier
                    .size(previewSize) // 动态大小动画
                    .background(
                        color = if (isValidColor) Color(android.graphics.Color.parseColor(hexColor)) else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }

        if (!isValidColor && hexColor.isNotEmpty()) {
            Text(
                text = "Invalid Hex Color",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// 验证十六进制颜色的工具函数
private fun isValidHexColor(color: String): Boolean {
    val hexPattern = "^#([A-Fa-f0-9]{6})$".toRegex()
    return hexPattern.matches(color)
}


@Preview
@Composable
private fun HexColorInputFieldPreview() {
    BLELightTheme {
        HexColorInputField()
    }
}