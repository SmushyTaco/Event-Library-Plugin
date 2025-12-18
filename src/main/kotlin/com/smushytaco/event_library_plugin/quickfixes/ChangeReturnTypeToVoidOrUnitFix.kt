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
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.smushytaco.event_library_plugin.MyBundle
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory

class ChangeReturnTypeToVoidOrUnitFix : LocalQuickFix {
    override fun getFamilyName(): String = MyBundle.message("quickfix.changeReturnTypeToVoidOrUnit")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement

        PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false)?.let { psiMethod ->
            WriteCommandAction.runWriteCommandAction(project) {
                val factory = JavaPsiFacade.getElementFactory(project)
                val voidType = factory.createTypeElementFromText("void", psiMethod)
                psiMethod.returnTypeElement?.replace(voidType)
            }
            return
        }

        PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java, false)?.let { ktFn ->
            WriteCommandAction.runWriteCommandAction(project) {
                val colon = ktFn.colon
                val typeRef = ktFn.typeReference

                if (colon != null && typeRef != null) {
                    val file = ktFn.containingFile
                    val doc: Document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return@runWriteCommandAction

                    val range = TextRange(colon.textRange.startOffset, typeRef.textRange.endOffset)
                    doc.replaceString(range.startOffset, range.endOffset, "")

                    PsiDocumentManager.getInstance(project).commitDocument(doc)
                } else {
                    val ktFactory = KtPsiFactory(project)
                    ktFn.typeReference = ktFactory.createType("Unit")
                }
            }
            return
        }
    }

    override fun generatePreview(project: Project, descriptor: ProblemDescriptor): IntentionPreviewInfo =
        if (IntentionPreviewUtils.isIntentionPreviewActive()) IntentionPreviewInfo.DIFF else IntentionPreviewInfo.EMPTY
}
