package dev.noctud.plugin.ext

import com.jetbrains.php.lang.psi.elements.PhpClass

const val MAP_FQN = "\\Noctud\\Collection\\Map\\Map"
const val WRITABLE_MAP_FQN = "\\Noctud\\Collection\\Map\\WritableMap"
const val COLLECTION_FQN = "\\Noctud\\Collection\\Collection"
const val SET_FQN = "\\Noctud\\Collection\\Set\\Set"
const val LIST_FQN = "\\Noctud\\Collection\\List\\ListInterface"
const val WRITABLE_LIST_FQN = "\\Noctud\\Collection\\List\\WritableList"

fun PhpClass.isInstanceOf(fqn: String): Boolean {
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<PhpClass>()
    queue.add(this)

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current.fqn)) continue

        if (current.fqn == fqn) return true

        queue.addAll(current.implementedInterfaces)
        current.superClass?.let { queue.add(it) }
    }

    return false
}

fun PhpClass.isMap(): Boolean {
    return isInstanceOf(MAP_FQN)
}
