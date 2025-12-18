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

package com.smushytaco.event_library_plugin.usage

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.smushytaco.event_library_plugin.Utility

class EventLibraryImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitUsage(element: PsiElement): Boolean {
        (element as? PsiMethod)?.let { method ->
            return method.isEventLibraryHandler()
        }

        (element as? PsiParameter)?.let { param ->
            val method = param.declarationScope as? PsiMethod ?: return false
            return method.isEventLibraryHandler()
        }

        return false
    }

    override fun isImplicitRead(element: PsiElement): Boolean = false
    override fun isImplicitWrite(element: PsiElement): Boolean = false

    private fun PsiMethod.isEventLibraryHandler() = hasAnnotation(Utility.EVENT_HANDLER_FQN) || hasAnnotation(Utility.EXCEPTION_HANDLER_FQN)
}
