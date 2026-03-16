package dev.noctud.plugin.type

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.Parameter
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.elements.Function as PhpFunction
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import dev.noctud.plugin.ext.COLLECTION_FQN
import dev.noctud.plugin.ext.MAP_FQN
import dev.noctud.plugin.ext.isInstanceOf

/**
 * Resolves types for closure/arrow function parameters passed to Noctud Collection methods.
 *
 * For example, in `$list->filter(fn ($v) => $v->...)`, this provider resolves `$v`
 * to the element type E from `ListInterface<E>`.
 *
 * Supported mappings:
 * - Collection<E> methods: param[0] = E (element type)
 * - Map<K,V> methods: param[0] = V (value), param[1] = K (key)
 */
class ClosureParameterTypeProvider : PhpTypeProvider4 {
    companion object {
        private const val KEY = '\u1A14'
    }

    override fun getKey(): Char = KEY

    override fun getType(element: PsiElement): PhpType? {
        if (element !is Parameter) return null

        // Skip if parameter already has an explicit type hint
        if (!element.declaredType.isEmpty) return null

        // Walk up: Parameter → ParameterList → Function (closure/arrow function)
        val closureParamList = element.parent as? ParameterList ?: return null
        val closure = closureParamList.parent as? PhpFunction ?: return null
        if (!closure.isClosure) return null

        val paramIndex = closureParamList.parameters.indexOf(element)
        if (paramIndex < 0 || paramIndex > 1) return null

        // Find the MethodReference this closure is passed as an argument to
        val methodRef = PsiTreeUtil.getParentOfType(closure, MethodReference::class.java) ?: return null

        // Get the receiver (the expression before ->)
        val receiver = methodRef.classReference as? PhpTypedElement ?: return null

        val project = element.project
        if (DumbService.getInstance(project).isDumb) return null

        // Try docType first (includes generics from @var), then fall back to type
        val docType = receiver.docType
        val receiverType = if (!docType.isEmpty) docType else receiver.type
        if (receiverType.isEmpty) return null

        val typesWithParams = receiverType.typesWithParametrisedParts
        val phpIndex = PhpIndex.getInstance(project)

        for (typeString in typesWithParams) {
            val paramParts = PhpType.getParametrizedParts(typeString)
            if (paramParts.isEmpty()) continue

            val baseFqn = typeString.substringBefore("<")
            if (!baseFqn.startsWith("\\")) continue

            val classes = phpIndex.getClassesByFQN(baseFqn) + phpIndex.getInterfacesByFQN(baseFqn)

            for (cls in classes) {
                // Map<K,V>: callbacks receive (V, K) — value first, key second
                if (cls.isInstanceOf(MAP_FQN) && paramParts.size >= 2) {
                    return when (paramIndex) {
                        0 -> PhpType().add(paramParts[1]) // V (value)
                        1 -> PhpType().add(paramParts[0]) // K (key)
                        else -> null
                    }
                }

                // Collection<E> (List, Set, etc.): callbacks receive (E, int)
                if (cls.isInstanceOf(COLLECTION_FQN) && paramParts.isNotEmpty()) {
                    if (paramIndex == 0) {
                        return PhpType().add(paramParts[0]) // E (element)
                    }
                    return null // Don't type other params — varies by method (int for most, E for reduce)
                }
            }
        }

        return null
    }

    override fun complete(expression: String, project: Project): PhpType? = null

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement>? = null
}
