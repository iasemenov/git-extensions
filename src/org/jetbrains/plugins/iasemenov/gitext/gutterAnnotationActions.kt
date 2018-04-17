package org.jetbrains.plugins.iasemenov.gitext

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.annotate.UpToDateLineNumberListener
import com.intellij.vcs.log.VcsShortCommitDetails
import git4idea.GitUtil
import git4idea.history.GitLogUtil

class FixupByHashGutterActionProvider : AnnotationGutterActionProvider {
    override fun createAction(annotation: FileAnnotation): AnAction {
        return object : AnAction("Fixup by hash"), UpToDateLineNumberListener {
            private var myLineNumber = -1

            override fun actionPerformed(e: AnActionEvent) {
                val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
                val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

                FileDocumentManager.getInstance().getDocument(file) ?: return
                GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file) ?: return

                val revisionHash = annotation.getLineRevisionNumber(myLineNumber)?.asString() ?: return

                commitFixup(project, file, revisionHash)
            }

            override fun consume(integer: Int) {
                myLineNumber = integer
            }
        }
    }
}

class FixupBySubjectGutterActionProvider : AnnotationGutterActionProvider {
    override fun createAction(annotation: FileAnnotation): AnAction {
        return object : AnAction("Fixup by subject"), UpToDateLineNumberListener {
            private var myLineNumber = -1

            override fun actionPerformed(e: AnActionEvent) {
                val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return
                val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

                FileDocumentManager.getInstance().getDocument(file) ?: return
                val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file) ?: return

                val revisionHash = annotation.getLineRevisionNumber(myLineNumber)?.asString() ?: return

                object : Task.Modal(project, "Getting commit details", true) {
                    private lateinit var details: List<VcsShortCommitDetails>

                    override fun run(indicator: ProgressIndicator) {
                        details = GitLogUtil.collectShortDetails(project, repository.vcs, repository.root, listOf(revisionHash))
                    }

                    override fun onSuccess() {
                        if (details.isEmpty()) return
                        commitFixup(project, file, details.first().subject)
                    }
                }.queue()
            }

            override fun consume(integer: Int) {
                myLineNumber = integer
            }
        }
    }
}