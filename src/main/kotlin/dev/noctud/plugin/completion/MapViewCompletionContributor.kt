package dev.noctud.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.plugin.ext.*

private data class MapView(
    val propertyName: String,
    val interfaceFqn: String,
    val priority: Int,
)

private val MAP_VIEWS = listOf(
    MapView("values", COLLECTION_FQN, 300),
    MapView("keys", SET_FQN, 200),
    MapView("entries", SET_FQN, 100),
)

class MapViewCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
            MapViewCompletionProvider()
        )
    }
}

private class MapViewCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val project = position.project

        if (DumbService.getInstance(project).isDumb) return

        val ref = position.parent
        val receiver = when (ref) {
            is FieldReference -> ref.classReference
            is MethodReference -> ref.classReference
            else -> null
        } ?: return

        val classes = receiver.resolvePhpClasses(useIndex = true)
        if (classes.none { it.isMap() }) return

        val mapMemberNames = collectMemberNames(classes)
        val phpIndex = PhpIndex.getInstance(project)

        for (view in MAP_VIEWS) {
            val viewClasses = phpIndex.getInterfacesByFQN(view.interfaceFqn)
            if (viewClasses.isEmpty()) continue

            for (viewClass in viewClasses) {
                for (method in viewClass.methods) {
                    if (method.name in mapMemberNames) continue
                    if (method.access?.isPublic != true) continue

                    val tailText = buildMethodTailText(method)
                    // Use "view->method" as lookup string to prevent IntelliJ deduplication across views,
                    // then add the bare method name as extra lookup string for prefix matching
                    val element = LookupElementBuilder.create("${view.propertyName}->${method.name}")
                        .withLookupString(method.name)
                        .withTailText(tailText, true)
                        .withIcon(method.icon)
                        .withTypeText("via ${view.propertyName}")
                        .withInsertHandler(ViewMethodInsertHandler(view.propertyName, method.name))
                        .withBoldness(false)

                    result.addElement(PrioritizedLookupElement.withPriority(element, view.priority.toDouble()))
                }

                for (field in viewClass.fields) {
                    if (field.isConstant) continue
                    if (field.name in mapMemberNames) continue

                    val element = LookupElementBuilder.create("${view.propertyName}->${field.name}")
                        .withLookupString(field.name)
                        .withIcon(field.icon)
                        .withTypeText("via ${view.propertyName}")
                        .withInsertHandler(ViewPropertyInsertHandler(view.propertyName, field.name))
                        .withBoldness(false)

                    result.addElement(PrioritizedLookupElement.withPriority(element, view.priority.toDouble()))
                }
            }
        }
    }

    private fun buildMethodTailText(method: Method): String {
        val params = method.parameters
        if (params.isEmpty()) return "()"
        val paramText = params.joinToString(", ") { param ->
            buildString {
                param.declaredType.toString().takeIf { it.isNotEmpty() }?.let { append("$it ") }
                append("$${param.name}")
            }
        }
        return "($paramText)"
    }

    private fun collectMemberNames(classes: List<PhpClass>): Set<String> {
        val names = mutableSetOf<String>()
        for (cls in classes) {
            for (method in cls.methods) {
                names.add(method.name)
            }
            for (field in cls.fields) {
                names.add(field.name)
            }
        }
        return names
    }
}

private class ViewMethodInsertHandler(
    private val viewProperty: String,
    private val methodName: String,
) : InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
    override fun handleInsert(context: InsertionContext, item: com.intellij.codeInsight.lookup.LookupElement) {
        val document = context.editor.document

        // IntelliJ inserted the lookup string "view->method" — replace with just "view->method()"
        document.replaceString(context.startOffset, context.tailOffset, "$viewProperty->$methodName()")

        // Place caret between parentheses
        context.editor.caretModel.moveToOffset(context.startOffset + viewProperty.length + 2 + methodName.length + 1)
    }
}

private class ViewPropertyInsertHandler(
    private val viewProperty: String,
    private val fieldName: String,
) : InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
    override fun handleInsert(context: InsertionContext, item: com.intellij.codeInsight.lookup.LookupElement) {
        val document = context.editor.document

        // IntelliJ inserted the lookup string "view->field" — replace with just "view->field"
        document.replaceString(context.startOffset, context.tailOffset, "$viewProperty->$fieldName")

        context.editor.caretModel.moveToOffset(context.startOffset + viewProperty.length + 2 + fieldName.length)
    }
}
