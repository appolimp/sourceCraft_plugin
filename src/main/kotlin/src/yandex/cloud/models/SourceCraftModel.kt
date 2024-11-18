package src.yandex.cloud.models

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository
import src.yandex.cloud.hostRegex
import java.net.URLEncoder

class SourceCraftModel(e: AnActionEvent) {
    private val project: Project? = e.getData(CommonDataKeys.PROJECT)
    private val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
    private val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

    fun getSelectedUrl(): String? {
        if (project == null || editor == null || file == null) {
            Messages.showErrorDialog("Project, editor, or file not found", "Error")
            return null
        }

        val repo: GitRepository? = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file)
        if (repo == null) {
            Messages.showErrorDialog("Repo not found", "Error")
            return null
        }

        val remoteUrl = getRemoteUrl(repo)
        val relativePath = getRelativePath(project, file)
        val branch = getCurrentBranch(repo)
        val (startLine, endLine) = getSelectedLines(editor)

        return makeUrl(remoteUrl, relativePath, branch, startLine, endLine)
    }

    private fun getRemoteUrl(repo: GitRepository): String? {
        val remote = repo.remotes.stream().findFirst().orElse(null) ?: return null
        val url = remote.firstUrl ?: return null
        return convertToRepoUrl(url)
    }

    private fun convertToRepoUrl(url: String): String? {
        val match = hostRegex.find(url)

        return if (match != null) {
            val (_, subdomain, _, _, env, user, repo) = match.destructured

            when {
                env == "cloud" -> {
                    "https://src.yandex.cloud/repo/browse/$user/$repo"
                }

                env == "cloud-preprod" && subdomain.isNotEmpty() -> {  // stand
                    "https://$subdomain.stand.o.ui.yandex.ru/repo/browse/$user/$repo"
                }

                env == "cloud-preprod" -> {
                    "https://src.yandex-preprod.cloud/repo/browse/$user/$repo"
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun getSelectedLines(editor: Editor): Pair<Int, Int> {
        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            val startLine = editor.offsetToLogicalPosition(selectionModel.selectionStart).line
            val endLine = editor.offsetToLogicalPosition(selectionModel.selectionEnd).line
            Pair(startLine, endLine)
        } else {
            val currentLine = editor.caretModel.logicalPosition.line
            Pair(currentLine, currentLine)
        }
    }

    private fun getRelativePath(project: Project, file: VirtualFile): String? {
        val root = project.guessProjectDir() ?: return null
        val relativePath = VfsUtilCore.getRelativePath(file, root) ?: return null
        return URLEncoder.encode(relativePath, "UTF-8")
    }

    private fun getCurrentBranch(repo: GitRepository): String? {
        val branch = repo.currentBranchName ?: return null
        return URLEncoder.encode(branch, "UTF-8")
    }

    private fun makeUrl(
        remoteURL: String?,
        relativePath: String?,
        branch: String?,
        startLine: Int, endLine: Int,
    ): String? {
        if (remoteURL == null) {
            Messages.showErrorDialog("Empty remoteURL", "Error")
            return null
        }
        if (relativePath == null) {
            Messages.showErrorDialog("Empty relativePath", "Error")
            return null
        }

        var url = "$remoteURL/$relativePath"

        // queries
        var prefix = "?"

        // branch
        if (branch != null) {
            url += prefix + "rev=$branch"
            prefix = "&"
        }

        // lines
        url += if (startLine == endLine) {
            prefix + "l=$startLine"
        } else {
            prefix + "l=$startLine-$endLine"
        }

        return url
    }
}
