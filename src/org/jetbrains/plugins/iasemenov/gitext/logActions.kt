package org.jetbrains.plugins.iasemenov.gitext

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.branch.GitRebaseParams
import git4idea.config.GitExecutableManager
import git4idea.rebase.GitCommitEditingAction
import git4idea.rebase.GitInteractiveRebaseEditorHandler
import git4idea.rebase.GitRebaseUtils

class GitInteractiveAutoRebaseAction : GitCommitEditingAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        prohibitRebaseDuringRebase(e, "rebase")
    }

    override fun actionPerformedAfterChecks(e: AnActionEvent) {
        val commit = getSelectedCommit(e)
        val project = e.project!!
        val gitVersion = GitExecutableManager.getInstance().getVersion(project)

        val repository = getRepository(e)

        object : Task.Backgroundable(project, "Rebasing") {
            override fun run(indicator: ProgressIndicator) {
                val editor = MyRebaseEditor(project, repository.root)
                val params = GitRebaseParams.editCommits(gitVersion, commit.parents.first().asString(), editor, false)
                GitRebaseUtils.rebase(project, listOf(repository), params, indicator)
            }
        }.queue()
    }

    override fun getFailureTitle() = "Couldn't Start Rebase"

    class MyRebaseEditor(project: Project, root: VirtualFile)
        : GitInteractiveRebaseEditorHandler(project, root) {
        override fun handleInteractiveEditor(path: String) = true
    }
}

