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

package com.smushytaco.event_library_plugin

import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.sourcePsiElement

object Utility {
    const val EVENT_HANDLER_FQN = "com.smushytaco.event_library.api.EventHandler"
    const val EXCEPTION_HANDLER_FQN = "com.smushytaco.event_library.api.ExceptionHandler"
    const val EVENT_FQN = "com.smushytaco.event_library.api.Event"
    const val JAVA_THROWABLE_FQN = "java.lang.Throwable"
    const val KOTLIN_THROWABLE_FQN = "kotlin.Throwable"

    fun isAssignableToFqn(type: PsiType, targetFqn: String, context: PsiElement): Boolean {
        if (!type.isValid) return false
        if (type is PsiPrimitiveType) return false

        val captured = PsiUtil.captureToplevelWildcards(type, context)
        val erased = PsiUtil.convertAnonymousToBaseType(captured)

        val canonical = erased.canonicalText
        val normalized = canonical.removeSuffix("!").removeSuffix("?")
        if (normalized == targetFqn) return true

        val resolved = PsiUtil.resolveClassInType(erased) ?: return false
        if (resolved.qualifiedName == targetFqn) return true

        return hasSuperNamed(resolved, targetFqn)
    }

    private fun hasSuperNamed(start: PsiClass, targetFqn: String): Boolean {
        val seen = HashSet<PsiClass>()
        val queue = ArrayDeque<PsiClass>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val c = queue.removeFirst()
            if (!seen.add(c)) continue

            val sc = c.superClass
            if (sc != null) {
                if (sc.qualifiedName == targetFqn) return true
                queue.add(sc)
            }
            for (i in c.interfaces) {
                if (i.qualifiedName == targetFqn) return true
                queue.add(i)
            }
        }
        return false
    }

    fun anchorForSingleParamType(psi: PsiElement?, node: UMethod): PsiElement? {
        val candidate: PsiElement? = when (psi) {
            is ScFunctionDefinition -> {
                val seq = psi.paramClauses().params()
                val it = seq.iterator()
                val p0: ScParameter? = if (it.hasNext()) it.next() else null
                val teOpt = p0?.typeElement()
                when {
                    teOpt != null && teOpt.isDefined() -> teOpt.get()
                    else -> p0
                }
            }
            is PsiMethod -> psi.parameterList.parameters.firstOrNull()?.typeElement
            is KtNamedFunction -> psi.valueParameters.firstOrNull()?.typeReference
            else -> null
        }
        return candidate?.takeIf { it.textLength > 0 } ?: anchorForParamList(psi, node)
    }

    fun anchorForParamList(psi: PsiElement?, node: UMethod): PsiElement? {
        val candidate: PsiElement? = when (psi) {
            is ScFunctionDefinition -> psi.paramClauses()
            is PsiMethod -> psi.parameterList
            is KtNamedFunction -> psi.valueParameterList
            else -> null
        }
        return candidate?.takeIf { it.textLength > 0 }
            ?: node.uastAnchor?.sourcePsi
            ?: psi
            ?: node.sourcePsiElement
    }
}