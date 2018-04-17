package org.jetbrains.plugins.iasemenov.gitext

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.VcsFullCommitDetails
import git4idea.branch.GitRebaseParams
import git4idea.rebase.GitCommitEditingAction
import git4idea.rebase.GitInteractiveRebaseEditorHandler
import git4idea.rebase.GitRebaseEditorService
import git4idea.rebase.GitRebaseUtils
import git4idea.repo.GitRepository
import git4idea.reset.GitOneCommitPerRepoLogAction

class FixupByHashLogAction : GitOneCommitPerRepoLogAction() {
    override fun actionPerformed(project: Project, commits: Map<GitRepository, VcsFullCommitDetails>) {
        commitFixup(project, commits.values.first().id.asString())
    }
}

class FixupBySubjectLogAction : GitOneCommitPerRepoLogAction() {
    override fun actionPerformed(project: Project, commits: Map<GitRepository, VcsFullCommitDetails>) {
        commitFixup(project, commits.values.first().subject)
    }
}

class GitInteractiveAutoRebaseAction : GitCommitEditingAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        prohibitRebaseDuringRebase(e, "rebase")
    }

    override fun actionPerformed(e: AnActionEvent) {
        super.actionPerformed(e)

        val commit = getSelectedCommit(e)
        val project = e.project!!
        val repository = getRepository(e)

        object : Task.Backgroundable(project, "Rebasing") {
            override fun run(indicator: ProgressIndicator) {
                val editor = MyRebaseEditor(project, repository.root)
                val params = GitRebaseParams.editCommits(commit.parents.first().asString(), editor, false)
                GitRebaseUtils.rebase(project, listOf(repository), params, indicator)
            }
        }.queue()
    }

    override fun getFailureTitle() = "Couldn't Start Rebase"

    class MyRebaseEditor(project: Project, root: VirtualFile)
        : GitInteractiveRebaseEditorHandler(GitRebaseEditorService.getInstance(), project, root) {
        override fun handleInteractiveEditor(path: String) = true
    }
}

