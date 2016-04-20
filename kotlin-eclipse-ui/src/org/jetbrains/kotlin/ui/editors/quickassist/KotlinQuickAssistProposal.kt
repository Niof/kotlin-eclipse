package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.eclipse.ui.utils.getOffsetByDocument
import org.jetbrains.kotlin.eclipse.ui.utils.getEndLfOffset

abstract class KotlinQuickAssistProposal : KotlinQuickAssist(), IJavaCompletionProposal {
    abstract fun apply(document: IDocument, psiElement: PsiElement)
    
    abstract override fun getDisplayString(): String
    
    override fun apply(document: IDocument) {
        getActiveElement()?.let { apply(document, it) }
    }
    
    fun getActiveFile(): IFile? {
        val editor = getActiveEditor()
        return if (editor != null) EditorUtil.getFile(editor) else null
    }
    
    fun getStartOffset(element: PsiElement, editor: AbstractTextEditor): Int {
        return element.getOffsetByDocument(EditorUtil.getDocument(editor), element.getTextRange().getStartOffset())
    }
    
    fun getEndOffset(element:PsiElement, editor:AbstractTextEditor): Int {
        return element.getEndLfOffset(EditorUtil.getDocument(editor))
    }
    
    fun insertAfter(element: PsiElement, text: String) {
        val kotlinFileEditor = getActiveEditor()
        if (kotlinFileEditor == null) {
            throw IllegalStateException("Active editor cannot be null")
        }
        
        kotlinFileEditor.getViewer().getDocument().replace(getEndOffset(element, kotlinFileEditor), 0, text)
    }
    
    fun replaceBetween(from:PsiElement, till:PsiElement, text:String) {
        val kotlinFileEditor = getActiveEditor()
        if (kotlinFileEditor == null) {
            throw IllegalStateException("Active editor cannot be null")
        }
        
        val startOffset = getStartOffset(from, kotlinFileEditor)
        val endOffset = getEndOffset(till, kotlinFileEditor)
        kotlinFileEditor.getViewer().getDocument().replace(startOffset, endOffset - startOffset, text)
    }
    
    fun replace(toReplace: PsiElement, text: String) {
        replaceBetween(toReplace, toReplace, text)
    }
    
    protected fun getBindingContext(jetFile: KtFile): BindingContext? {
        return getAnalysisResultWithProvider(jetFile)?.analysisResult?.bindingContext
    }
    
    protected fun getAnalysisResultWithProvider(jetFile:KtFile): AnalysisResultWithProvider? {
        val file = getActiveFile()
        if (file == null) return null
        
        val javaProject = JavaCore.create(file.getProject())
        return KotlinAnalyzer.analyzeFile(javaProject, jetFile)
    }
    
    override fun getSelection(document:IDocument?): Point? = null
    
    override fun getAdditionalProposalInfo(): String? = null
    
    override fun getImage(): Image? = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)
    
    override fun getContextInformation(): IContextInformation? = null
    
    override fun getRelevance(): Int = 0
}

fun getBindingContext(jetFile: KtFile, javaProject: IJavaProject): BindingContext? {
    return KotlinAnalyzer.analyzeFile(javaProject, jetFile).analysisResult.bindingContext
}