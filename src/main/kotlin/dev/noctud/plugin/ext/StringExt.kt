package dev.noctud.plugin.ext

fun log(message: String) {
    com.intellij.openapi.diagnostic.Logger.getInstance("NoctudPlugin").warn(message)
}
