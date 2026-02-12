package dev.noctud.plugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.PhpUnset
import dev.noctud.plugin.ext.*

class ReadOnlyCollectionWriteInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (DumbService.getInstance(element.project).isDumb) return

                when (element) {
                    is AssignmentExpression -> {
                        val arrayAccess = element.variable as? ArrayAccessExpression ?: return
                        checkArrayAccess(arrayAccess, holder)
                    }
                    is PhpUnset -> {
                        for (argument in element.arguments) {
                            val arrayAccess = argument as? ArrayAccessExpression ?: continue
                            checkArrayAccess(arrayAccess, holder)
                        }
                    }
                }
            }
        }
    }

    private fun checkArrayAccess(arrayAccess: ArrayAccessExpression, holder: ProblemsHolder) {
        val receiver = arrayAccess.value ?: return
        val classes = receiver.resolvePhpClasses(useIndex = true)
        if (classes.isEmpty()) return

        val isMap = classes.any { it.isInstanceOf(MAP_FQN) }
        val isWritableMap = classes.any { it.isInstanceOf(WRITABLE_MAP_FQN) }
        if (isMap && !isWritableMap) {
            holder.registerProblem(arrayAccess, "Cannot modify a read-only map", ProblemHighlightType.GENERIC_ERROR)
            return
        }

        val isList = classes.any { it.isInstanceOf(LIST_FQN) }
        val isWritableList = classes.any { it.isInstanceOf(WRITABLE_LIST_FQN) }
        if (isList && !isWritableList) {
            holder.registerProblem(arrayAccess, "Cannot modify a read-only list", ProblemHighlightType.GENERIC_ERROR)
        }
    }
}
