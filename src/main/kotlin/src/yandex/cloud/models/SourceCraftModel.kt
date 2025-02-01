package src.yandex.cloud.models

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.application.ReadAction
import git4idea.GitUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture

val hostRegex = Regex("""(https?|ssh)://(.*?)\.?(git|ssh)\.(o\.(cloud(?:-preprod)?)\.yandex\.net)/(.*?)/(.*?)\.git""")

class SourceCraftModel(e: AnActionEvent) {
    private val project: Project? = e.getData(CommonDataKeys.PROJECT)
    private val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
    private val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

    fun getSelectedUrl(): CompletableFuture<String?> {
        if (project == null || file == null) {
            Messages.showErrorDialog("Project or file not found", "Error")
            return CompletableFuture.completedFuture(null)
        }

        val repo: GitRepository? = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file)
        if (repo == null) {
            Messages.showErrorDialog("Repo not found", "Error")
            return CompletableFuture.completedFuture(null)
        }

        val remoteUrl = getRemoteUrl(repo)
        val branch = getCurrentBranch(repo)
        val (startLine, endLine) = getSelectedLines(editor)

        return getRelativePath(project, file).thenApply { relativePath ->
            makeUrl(remoteUrl, relativePath, branch, startLine, endLine)
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
                env == "cloud" -> "https://src.yandex.cloud/$user/$repo/browse"
                env == "cloud-preprod" && subdomain.isNotEmpty() -> "https://$subdomain.stand.o.ui.yandex.ru/$user/$repo/browse"
                env == "cloud-preprod" -> "https://src.yandex-preprod.cloud/$user/$repo/browse"
                else -> null
            }
        } else {
            null
        }
    }

    private fun getSelectedLines(editor: Editor?): Pair<Int?, Int?> {
        if (editor == null) return Pair(null, null)

        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            val startLine = editor.offsetToLogicalPosition(selectionModel.selectionStart).line + 1
            val endLine = editor.offsetToLogicalPosition(selectionModel.selectionEnd).line + 1
            Pair(startLine, endLine)
        } else {
            val currentLine = editor.caretModel.logicalPosition.line + 1
            Pair(currentLine, null)
        }
    }

    private fun getRelativePath(project: Project, file: VirtualFile): CompletableFuture<String?> {
        return getGitRelativePathAsync(project, file).thenApply { relativePath ->
            relativePath?.let { URLEncoder.encode(it, "UTF-8").replace("%2F", "/") }
        }
    }

    private fun getCurrentBranch(repo: GitRepository): String? {
        val branch = repo.currentBranchName ?: return null
        return URLEncoder.encode(branch, "UTF-8").replace("%2F", "/")
    }

    private fun makeUrl(
        remoteURL: String?,
        relativePath: String?,
        branch: String?,
        startLine: Int?, endLine: Int?,
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
        url += if (startLine != null && endLine != null) {
            prefix + "l=$startLine-$endLine"
        } else if (startLine != null) {
            prefix + "l=$startLine"
        } else {
            ""
        }

        return url
    }

    private fun getGitRelativePathAsync(project: Project, file: VirtualFile): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching Git Path", false) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                val relativePath = ReadAction.compute<String?, Throwable> {
                    val repositoryManager = GitRepositoryManager.getInstance(project)
                    val repository = repositoryManager.getRepositoryForFile(file) ?: return@compute null
                    val root = repository.root
                    VfsUtilCore.getRelativePath(file, root)
                }
                future.complete(relativePath)
            }
        })

        return future
    }
}
