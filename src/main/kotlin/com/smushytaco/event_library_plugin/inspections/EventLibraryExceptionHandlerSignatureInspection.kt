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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.uast.UastVisitorAdapter
import com.smushytaco.event_library_plugin.MyBundle
import com.smushytaco.event_library_plugin.Utility
import com.smushytaco.event_library_plugin.quickfixes.SwapExceptionHandlerParametersFix
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class EventLibraryExceptionHandlerSignatureInspection : AbstractBaseUastLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {

            override fun visitMethod(node: UMethod): Boolean {
                if (node.isConstructor) return true
                if (node.findAnnotation(Utility.EXCEPTION_HANDLER_FQN) == null) return true

                val psi = node.sourcePsi ?: return true
                val params = node.uastParameters

                if (params.size != 1 && params.size != 2) {
                    val anchor = Utility.anchorForParamList(psi, node) ?: return true
                    holder.registerProblem(
                        anchor,
                        MyBundle.message("inspection.exceptionHandler.invalidShape"),
                        ProblemHighlightType.ERROR
                    )
                    return true
                }

                if (params.size == 1) {
                    val p0Type = params[0].type
                    val isEvent = Utility.isAssignableToFqn(p0Type, Utility.EVENT_FQN, psi)
                    val isThrowable = Utility.isAssignableToFqn(p0Type, Utility.JAVA_THROWABLE_FQN, psi) ||
                            Utility.isAssignableToFqn(p0Type, Utility.KOTLIN_THROWABLE_FQN, psi)

                    if (!isEvent && !isThrowable) {
                        val anchor = Utility.anchorForSingleParamType(psi, node) ?: Utility.anchorForParamList(psi, node) ?: return true
                        holder.registerProblem(
                            anchor,
                            MyBundle.message("inspection.exceptionHandler.singleParamMustBeEventOrThrowable"),
                            ProblemHighlightType.ERROR
                        )
                    }
                    return true
                }

                val p0Type = params[0].type
                val p1Type = params[1].type

                val p0IsEvent = Utility.isAssignableToFqn(p0Type, Utility.EVENT_FQN, psi)
                val p1IsEvent = Utility.isAssignableToFqn(p1Type, Utility.EVENT_FQN, psi)
                val p0IsThrowable = Utility.isAssignableToFqn(p0Type, Utility.JAVA_THROWABLE_FQN, psi) ||
                        Utility.isAssignableToFqn(p0Type, Utility.KOTLIN_THROWABLE_FQN, psi)
                val p1IsThrowable = Utility.isAssignableToFqn(p1Type, Utility.JAVA_THROWABLE_FQN, psi) ||
                        Utility.isAssignableToFqn(p1Type, Utility.KOTLIN_THROWABLE_FQN, psi)

                if (p0IsEvent && p1IsThrowable) return true

                if (p0IsThrowable && p1IsEvent) {
                    val anchor = Utility.anchorForParamList(psi, node) ?: return true
                    holder.registerProblem(
                        anchor,
                        MyBundle.message("inspection.exceptionHandler.reversedOrder"),
                        ProblemHighlightType.ERROR,
                        SwapExceptionHandlerParametersFix()
                    )
                    return true
                }

                val listAnchor = Utility.anchorForParamList(psi, node)

                if (!p0IsEvent) {
                    val anchor = Utility.anchorForSingleParamType(psi, node) ?: listAnchor
                    if (anchor != null) {
                        holder.registerProblem(
                            anchor,
                            MyBundle.message("inspection.exceptionHandler.firstParamMustBeEvent"),
                            ProblemHighlightType.ERROR
                        )
                    }
                }

                if (!p1IsThrowable) {
                    val anchor = anchorForParam1Type(psi, node) ?: listAnchor
                    if (anchor != null) {
                        holder.registerProblem(
                            anchor,
                            MyBundle.message("inspection.exceptionHandler.secondParamMustBeThrowable"),
                            ProblemHighlightType.ERROR
                        )
                    }
                }

                return true
            }
        }, true)
    }

    private fun anchorForParam1Type(psi: PsiElement?, node: UMethod): PsiElement? {
        val candidate: PsiElement? = when (psi) {
            is ScFunctionDefinition -> {
                val seq = psi.paramClauses().params()
                val it = seq.iterator()
                if (it.hasNext()) it.next()
                val p0: ScParameter? = if (it.hasNext()) it.next() else null
                val teOpt = p0?.typeElement()
                when {
                    teOpt != null && teOpt.isDefined() -> teOpt.get()
                    else -> p0
                }
            }
            is PsiMethod -> psi.parameterList.parameters.getOrNull(1)?.typeElement
            is KtNamedFunction -> psi.valueParameters.getOrNull(1)?.typeReference
            else -> null
        }
        return candidate?.takeIf { it.textLength > 0 } ?: Utility.anchorForParamList(psi, node)
    }
}
