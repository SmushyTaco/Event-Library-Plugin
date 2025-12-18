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
import com.intellij.uast.UastVisitorAdapter
import com.smushytaco.event_library_plugin.MyBundle
import com.smushytaco.event_library_plugin.Utility
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class EventLibraryEventHandlerSignatureInspection : AbstractBaseUastLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(object : AbstractUastNonRecursiveVisitor() {

            override fun visitMethod(node: UMethod): Boolean {
                if (node.isConstructor) return true
                if (node.findAnnotation(Utility.EVENT_HANDLER_FQN) == null) return true

                val psi = node.sourcePsi ?: return true

                val paramCount = node.uastParameters.size
                if (paramCount != 1) {
                    val anchor = Utility.anchorForParamList(psi, node) ?: return true
                    holder.registerProblem(
                        anchor,
                        MyBundle.message("inspection.eventHandler.mustHaveExactlyOneParameter"),
                        ProblemHighlightType.ERROR
                    )
                    return true
                }

                val uParam = node.uastParameters.first()
                val paramType = uParam.type

                if (!Utility.isAssignableToFqn(paramType, Utility.EVENT_FQN, psi)) {
                    val anchor = Utility.anchorForSingleParamType(psi, node) ?: return true
                    holder.registerProblem(
                        anchor,
                        MyBundle.message("inspection.eventHandler.parameterMustImplementEvent"),
                        ProblemHighlightType.ERROR
                    )
                }

                return true
            }
        }, true)
    }
}
