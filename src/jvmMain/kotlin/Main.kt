// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.imageio.ImageIO
import javax.swing.JFileChooser

@Composable
@Preview
fun App() {

    val folderPath = remember { mutableStateOf("") }
    val apkPath = remember { mutableStateOf("") }
    val toolsPath = remember { mutableStateOf("") }
    val alias = remember { mutableStateOf("") }
    val keyPassword = remember { mutableStateOf("") }
    val aliasPassword = remember { mutableStateOf("") }
    val result = remember { mutableStateOf("") }
    val passwordHidden1 = remember { mutableStateOf(false) }
    val passwordHidden2 = remember { mutableStateOf(false) }
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }

    val config = readConfig()
    if (!config.isNullOrEmpty()) {
        val json = Gson().fromJson<JsonObject>(config, JsonObject::class.java)
        toolsPath.value = json["buildToolsDir"].asString
        folderPath.value = json["jksPath"].asString
        alias.value = json["alias"].asString
        keyPassword.value = json["keyPassword"].asString
        aliasPassword.value = json["aliasPassword"].asString
    }
    MaterialTheme {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "请选择 tools 路径：${toolsPath.value}",
                modifier = Modifier.clickable(onClick = {
                    val fileChooser = JFileChooser()
                    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    val result = fileChooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val folder = fileChooser.selectedFile
                        toolsPath.value = folder.absolutePath
                    }
                })
            )
            Text(
                text = "请选择安装包：${apkPath.value}",
                modifier = Modifier.clickable(onClick = {
                    val fileChooser = JFileChooser()
                    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    val result = fileChooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val folder = fileChooser.selectedFile
                        apkPath.value = folder.absolutePath
                    }
                })
            )
            Text(
                text = "请选择签名文件：${folderPath.value}",
                modifier = Modifier.clickable(onClick = {
                    val fileChooser = JFileChooser()
                    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    val result = fileChooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val folder = fileChooser.selectedFile
                        folderPath.value = folder.absolutePath
                    }
                })
            )

            TextField(
                value = keyPassword.value,
                onValueChange = { keyPassword.value = it },
                label = { Text("签名密码：") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).focusRequester(focusRequester1),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusRequester2.requestFocus() }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordHidden1.value = !passwordHidden1.value
                        }
                    ){
                        if(!passwordHidden1.value){
                            Icon(painter = painterResource("images/visibility_off.png"), null)
                        } else {
                            Icon(painter = painterResource("images/visibility.png"), null)
                        }
                    }
                },
                visualTransformation = if(!passwordHidden1.value) PasswordVisualTransformation() else VisualTransformation.None
            )

            TextField(
                value = alias.value,
                onValueChange = { alias.value = it },
                label = { Text("别名：") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).focusRequester(focusRequester2),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusRequester3.requestFocus() }
                )
            )
            TextField(
                value = aliasPassword.value,
                onValueChange = { aliasPassword.value = it },
                label = { Text("别名密码：") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordHidden2.value = !passwordHidden2.value
                        }
                    ){
                        if(!passwordHidden2.value){
                            Icon(painter = painterResource("images/visibility_off.png"), null)
                        } else {
                            Icon(painter = painterResource("images/visibility.png"), null)
                        }
                    }
                },
                visualTransformation = if(!passwordHidden2.value) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).focusRequester(focusRequester3),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            Row {
                Button(

                    modifier = Modifier.padding(horizontal = 8.dp), onClick = {
                        runCommand(
                            toolsPath.value,
                            folderPath.value,
                            apkPath.value,
                            keyPassword.value,
                            alias.value,
                            aliasPassword.value,
                            result
                        )
                    }) {
                    Text("开始签名")
                }

                Button(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { openDir(apkPath.value, result) }) {
                    Text("打开文件夹")
                }
            }

            ScrollableText(result.value)
        }
    }
}


fun openDir(path: String, result: MutableState<String>): Unit {
    val file = File(path)
    val folder = file.parentFile
    if (folder == null || !folder.exists()) {
        result.value = result.value.plus("先选择要打包的文件\n")
        return
    }

    if (folder.exists() && folder.isDirectory) {
        val desktop = Desktop.getDesktop()
        desktop.open(folder)
    } else {
        println("文件夹不存在或者不是一个有效的文件夹路径。")
    }
}

@Composable
fun ScrollableText(text: String) {
    val lines = text.split("\n")
    val scrollState = rememberLazyListState()

    LazyColumn(
        state = scrollState
    ) {
        items(lines.size) { index ->
            Text(lines[index])
        }
    }

    LaunchedEffect(lines.size) {
        // Scroll to the last item
        lines.size.takeIf { it > 0 }?.let { lastIndex ->
            // Scroll to the last item
            coroutineScope {
                launch {
                    scrollState.scrollToItem(lastIndex)
                }
            }
        }

    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "自动签名工具"
    ) {
        App()
    }
}

fun runCommand(
    buildToolsDir: String,
    jksPath: String,
    apkPath: String,
    keyPassword: String,
    alias: String,
    aliasPassword: String,
    result: MutableState<String>
) {



    saveInputToJson(buildToolsDir, jksPath, alias, keyPassword, aliasPassword)
    if (buildToolsDir.isEmpty() || jksPath.isEmpty() || apkPath.isEmpty()) {
        result.value = "请先完善所需参数...\n"
        return
    }
    result.value = "开始打包...\n"
    // 创建命令行参数，使用文件路径作为参数
    val apkName = apkPath.substring(apkPath.lastIndexOf(File.separator) + 1)
    val apkUnSignName = "unSign-$apkName"
    val apkSignName = "sign-$apkName"
    val fileDir = apkPath.substring(0, apkPath.lastIndexOf(File.separator))

    val zipalignCmd = "$buildToolsDir\\zipalign -v -p 4 $apkPath ${fileDir + File.separator + apkUnSignName}"

    // 执行命令
    executeCommand(zipalignCmd, result)

    val command = """
        $buildToolsDir\./apksigner sign --ks $jksPath --ks-pass pass:$keyPassword --ks-key-alias $alias --key-pass pass:$aliasPassword --out ${fileDir + File.separator + apkSignName} ${fileDir + File.separator + apkUnSignName}
    """.trimIndent()
    executeCommand2(command, result, fileDir + File.separator + apkUnSignName)
}

fun executeCommand(cmd: String, result: MutableState<String>) {

    println(cmd)

    val process = Runtime.getRuntime().exec(cmd)
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    var line: String?
    runBlocking {
        while (reader.readLine().also { line = it } != null) {
            // 处理命令输出
            println(line)
            result.value = result.value.plus(line + "\n")
        }
    }

    process.waitFor()
    reader.close()
}

fun executeCommand2(cmd: String, result: MutableState<String>, cacheFile: String) {

    println(cmd)

    val processBuilder = ProcessBuilder("cmd", "/c", cmd).directory(File("."))
    val process = processBuilder.start()
    val exitCode = process.waitFor()
    if (exitCode == 0) {
        result.value = result.value.plus("签名成功")
        clearCacheFile(cacheFile)
        println("Command completed successfully")
    } else {
        result.value = result.value.plus("签名失败")
        println("Command failed with exit code $exitCode")
    }
}

fun clearCacheFile(cacheFile: String) {
    val file = File(cacheFile)

    if (file.exists()) {
        if (file.delete()) {
            println("文件已成功删除")
        } else {
            println("无法删除文件")
        }
    } else {
        println("文件不存在")
    }
}

fun saveInputToJson(buildToolsDir: String, jksPath: String, alias: String, jksPassword: String, keyPassword: String) {
    val map = HashMap<String, String>()
    map["buildToolsDir"] = buildToolsDir
    map["jksPath"] = jksPath
    map["alias"] = alias
    map["keyPassword"] = jksPassword
    map["aliasPassword"] = keyPassword

    val toJson = Gson().toJson(map)
    val cacheDir = System.getProperty("java.io.tmpdir")
    val file = File("$cacheDir/auto_sign_config.json")
//    val file = File("C:\\Users\\HUAWEI\\Documents\\db.json")

    if (!file.exists()) {
        file.createNewFile()
    }
    // 创建 File 对象

    // 将输入框的值写入文件
    file.writeText(toJson, Charsets.UTF_8)
}

fun readConfig(): String {
    var text = ""
//    val fileChooser = JFileChooser()
//    if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//        // 创建 File 对象
//        val file = fileChooser.selectedFile
//
//        // 将输入框的值写入文件
//        text = file.readText(Charsets.UTF_8)
//
//    }

    val cacheDir = System.getProperty("java.io.tmpdir")
    val file = File("$cacheDir/auto_sign_config.json")
    if (file.exists()) {
        text = file.readText(Charsets.UTF_8)
    }
    return text
}
