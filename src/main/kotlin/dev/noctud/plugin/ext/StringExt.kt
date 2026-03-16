package dev.noctud.plugin.ext

fun log(message: String) {
    com.intellij.openapi.diagnostic.Logger.getInstance("NoctudPlugin").debug(message)
}
