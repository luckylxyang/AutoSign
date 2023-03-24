// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.gson.Gson
import com.google.gson.JsonObject
import sun.nio.ch.IOUtil
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
    val config = readConfig()
    if (!config.isNullOrEmpty()){
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            TextField(
                value = alias.value,
                onValueChange = { alias.value = it },
                label = { Text("别名：") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            TextField(
                value = aliasPassword.value,
                onValueChange = { aliasPassword.value = it },
                label = { Text("别名密码：") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Button(onClick = { runCommand(toolsPath.value, folderPath.value, apkPath.value, keyPassword.value, alias.value, aliasPassword.value) }) {
                Text("对齐")
            }
            Button(onClick = { runCommand(toolsPath.value, folderPath.value, apkPath.value, keyPassword.value, alias.value, aliasPassword.value) }) {
                Text("开始签名")
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

fun runCommand(buildToolsDir: String, jksPath: String, apkPath: String, keyPassword: String, alias: String, aliasPassword: String) {

    saveInputToJson(buildToolsDir, jksPath, "dist", "distmobile", "distmobile")
    if (buildToolsDir.isEmpty() || jksPath.isEmpty() || apkPath.isEmpty()) {
        return
    }
// 创建命令行参数，使用文件路径作为参数
    val apkName = apkPath.substring(apkPath.lastIndexOf(File.separator) + 1)
    val apkUnSignName = "unSign-$apkName"
    val apkSignName = "sign-$apkName"
    val fileDir = apkPath.substring(0, apkPath.lastIndexOf(File.separator))

    val zipalignCmd = "$buildToolsDir\\zipalign -v -p 4 $apkPath ${fileDir + File.separator + apkUnSignName}"
    val apksignerCmd =
        "$buildToolsDir\\apksigner sign --ks $jksPath --out ${fileDir + File.separator + apkSignName} ${fileDir + File.separator + apkUnSignName}"

    // 执行命令
    executeCommand(zipalignCmd)
    executeCommand("exit")
    executeCommand(apksignerCmd)

    val command = """
        $buildToolsDir\zipalign -v -p 4 $apkPath ${fileDir + File.separator + apkUnSignName}
        $buildToolsDir\apksigner sign --ks $jksPath --ks-pass pass:$keyPassword --key-alias $alias --key-pass pass:$aliasPassword --out ${fileDir + File.separator + apkSignName} ${fileDir + File.separator + apkUnSignName}
    """.trimIndent()
}

fun executeCommand(cmd: String) {
//    val runtime = Runtime.getRuntime()
//    val exec = runtime.exec(cmd)
//    val exitCode = exec.waitFor()
    val processBuilder = ProcessBuilder("cmd", "/c", cmd).directory(File("."))
    val process = processBuilder.start()
    val exitCode = process.waitFor()
    if (exitCode == 0) {
        println("Command completed successfully")
    } else {
        println("Command failed with exit code $exitCode")
    }
}

fun saveInputToJson(buildToolsDir: String, jksPath :String, alias: String, jksPassword: String,keyPassword : String ){
    val map = HashMap<String, String>()
    map["buildToolsDir"] = buildToolsDir
    map["jksPath"] = jksPath
    map["alias"] = alias
    map["keyPassword"] = jksPassword
    map["aliasPassword"] = keyPassword

    val toJson = Gson().toJson(map)
    val file = File("C:\\Users\\HUAWEI\\Documents\\db.json")
    if (!file.exists()){
        file.createNewFile()
    }
    // 创建 File 对象

    // 将输入框的值写入文件
    file.writeText(toJson, Charsets.UTF_8)
}

fun readConfig() : String {
    val fileChooser = JFileChooser()
    var text = ""
    if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        // 创建 File 对象
        val file = fileChooser.selectedFile

        // 将输入框的值写入文件
        text = file.readText(Charsets.UTF_8)

    }
    return text
}
