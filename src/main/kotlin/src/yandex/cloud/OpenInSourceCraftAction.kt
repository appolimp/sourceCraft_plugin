package src.yandex.cloud

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

val hostRegex = Regex("""(https?|ssh)://(.*?)\.?(git|ssh)\.(o\.(cloud(?:-preprod)?)\.yandex\.net)/(.*?)/(.*?)\.git""")


class OpenInSourceCraftAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
        val file: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || editor == null || file == null) {
            Messages.showErrorDialog("Project, editor, or file not found", "Error")
            return
        }

        val repo: GitRepository? = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file)
        if (repo == null) {
            Messages.showErrorDialog("Repo not found", "Error")
            return
        }

        val remoteUrl = getRemoteUrl(repo)
        val relativePath = getRelativePath(project, file)
        val branch = getCurrentBranch(repo)
        val (startLine, endLine) = getSelectedLines(editor)

        val url: String = makeUrl(remoteUrl, relativePath, branch, startLine, endLine) ?: return

        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: Exception) {
            Messages.showErrorDialog("Could not open SourceCraft URL", "Error")
        }
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
        val root = project.baseDir
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
