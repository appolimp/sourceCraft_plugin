package src.yandex.cloud

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Desktop
import java.net.URI

class OpenInSourceCraftAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
        val file: VirtualFile? = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || editor == null || file == null) {
            Messages.showErrorDialog("Project, editor, or file not found", "Error")
            return
        }

        val (startLine, endLine) = getSelectedLines(editor)

        val githubRepoUrl = "https://github.com/username/repository" // URL репозитория GitHub
        val projectBasePath = project.basePath ?: return
        val filePath = file.path.replace(projectBasePath, "")

        // Формируем URL с номером строки
        val githubUrl = "$githubRepoUrl/blob/main$filePath#L${startLine + 1}"

        try {
            Desktop.getDesktop().browse(URI(githubUrl))
        } catch (e: Exception) {
            Messages.showErrorDialog("Could not open GitHub URL", "Error")
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
}
