/*
 * Copyright 2025 Nikan Radan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smushytaco.event_library_plugin.inspections

import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.uast.UastVisitorAdapter
import com.smushytaco.event_library_plugin.MyBundle
import com.smushytaco.event_library_plugin.Utility
import com.smushytaco.event_library_plugin.quickfixes.ChangeReturnTypeToVoidOrUnitFix
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class EventLibraryReturnTypeInspection : AbstractBaseUastLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {

            override fun visitMethod(node: UMethod): Boolean {
                if (node.isConstructor) return true
                if (!node.isEventLibraryHandler()) return true
                if (node.returnsVoidOrUnit()) return true

                val psi = node.sourcePsi
                val returnTypeAnchor = when (psi) {
                    is PsiMethod -> psi.returnTypeElement
                    is KtNamedFunction -> psi.typeReference
                    else -> null
                }

                val anchor = returnTypeAnchor?.takeIf { it.textLength > 0 }
                    ?: node.uastAnchor?.sourcePsi
                    ?: psi
                    ?: return true

                holder.registerProblem(
                    anchor,
                    MyBundle.message("inspection.returnType.mustBeVoidOrUnit"),
                    ProblemHighlightType.ERROR,
                    ChangeReturnTypeToVoidOrUnitFix()
                )
                return true
            }
        }, true)
    }

    private fun UMethod.isEventLibraryHandler(): Boolean {
        return findAnnotation(Utility.EVENT_HANDLER_FQN) != null || findAnnotation(Utility.EXCEPTION_HANDLER_FQN) != null
    }

    private fun UMethod.returnsVoidOrUnit(): Boolean {
        val t = returnType?.canonicalText ?: return false
        return t == "void" || t == "kotlin.Unit"
    }
}
