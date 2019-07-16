package org.jetbrains.plugins.iasemenov.gitext

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.annotate.UpToDateLineNumberListener
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog
import com.intellij.vcs.log.VcsCommitMetadata
import git4idea.history.GitHistoryUtils
import git4idea.rebase.GitAutoSquashCommitAction
import git4idea.rebase.GitCommitEditingAction
import git4idea.repo.GitRepositoryManager

class FixupBySubjectGutterActionProvider : AnnotationGutterActionProvider {
    override fun createAction(annotation: FileAnnotation): AnAction {
        return object : GitCommitEditingAction(), UpToDateLineNumberListener {
            private var myLineNumber = -1

            override fun actionPerformedAfterChecks(e: AnActionEvent) {
                val project = e.project!!

                val revisionHash = annotation.getLineRevisionNumber(myLineNumber)?.asString() ?: return

                val changeList = ChangeListManager.getInstance(project).defaultChangeList
                val repository = getRepository(e)

                object : Task.Modal(project, "Getting commit details", true) {
                    private lateinit var details: List<VcsCommitMetadata>

                    override fun run(indicator: ProgressIndicator) {
                        details = GitHistoryUtils.collectCommitsMetadata(project, repository.root, revisionHash)
                                ?: throw ProcessCanceledException()
                    }

                    override fun onSuccess() {
                        if (details.isEmpty()) return

                        val gitRepositoryManager = GitRepositoryManager.getInstance(project)

                        val changes = changeList.changes.filter {
                            gitRepositoryManager.getRepositoryForFile(ChangesUtil.getFilePath(it)) == repository
                        }

                        val executors = repository.vcs.commitExecutors + if (getProhibitedStateMessage(e, "rebase") == null)
                            listOf(GitAutoSquashCommitAction.GitRebaseAfterCommitExecutor(project, repository, "$revisionHash~"))
                        else listOf()
                        CommitChangeListDialog.commitChanges(project,
                                changes,
                                changes,
                                changeList,
                                executors,
                                true,
                                null,
                                "fixup! ${details.first().subject}",
                                null,
                                true)
                    }
                }.queue()
            }

            override fun getFailureTitle() = "Can't Create Fixup Commit"

            override fun consume(integer: Int) {
                myLineNumber = integer
            }
        }
    }
}