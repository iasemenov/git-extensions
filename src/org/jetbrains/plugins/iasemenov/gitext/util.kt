package org.jetbrains.plugins.iasemenov.gitext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.CommitChangeListDialog
import com.intellij.openapi.vfs.VirtualFile

internal fun commitFixup(project: Project, file: VirtualFile, fixupWith: String) {
    CommitChangeListDialog.commitChanges(project, listOf(ChangeListManager.getInstance(project).getChange(file)),
            null, null, "fixup! $fixupWith")
}

internal fun commitFixup(project: Project, fixupWith: String) {
    CommitChangeListDialog.commitChanges(project, emptyList(), null, null, "fixup! $fixupWith")
}