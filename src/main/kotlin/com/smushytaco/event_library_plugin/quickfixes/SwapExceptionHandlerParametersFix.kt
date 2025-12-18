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

package com.smushytaco.event_library_plugin.quickfixes

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.smushytaco.event_library_plugin.MyBundle
import org.jetbrains.kotlin.psi.KtNamedFunction

class SwapExceptionHandlerParametersFix : LocalQuickFix {
    override fun getFamilyName(): String = MyBundle.message("quickfix.swapParameters")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement

        PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)?.let { psiMethod ->
            val p0 = psiMethod.parameterList.parameters.getOrNull(0) ?: return
            val p1 = psiMethod.parameterList.parameters.getOrNull(1) ?: return
            val doc = PsiDocumentManager.getInstance(project).getDocument(psiMethod.containingFile) ?: return
            swapAdjacentRanges(doc, p0.textRange, p1.textRange)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
            return
        }

        PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java, false)?.let { ktFn ->
            val p0 = ktFn.valueParameters.getOrNull(0) ?: return
            val p1 = ktFn.valueParameters.getOrNull(1) ?: return
            val doc = PsiDocumentManager.getInstance(project).getDocument(ktFn.containingFile) ?: return
            swapAdjacentRanges(doc, p0.textRange, p1.textRange)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
            return
        }
    }

    private fun swapAdjacentRanges(doc: Document, first: TextRange, second: TextRange) {
        val start = first.startOffset
        val end = second.endOffset
        if (start >= end) return
        if (first.endOffset > second.startOffset) return

        val firstText = doc.getText(first)
        val secondText = doc.getText(second)
        val sep = doc.getText(TextRange(first.endOffset, second.startOffset))

        doc.replaceString(start, end, secondText + sep + firstText)
    }

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY
}
