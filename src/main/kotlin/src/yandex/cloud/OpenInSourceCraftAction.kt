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

        val githubRepoUrl = "https://github.com/username/repository" // URL репозитория GitHub
        val projectBasePath = project.basePath ?: return
        val filePath = file.path.replace(projectBasePath, "")

        // Определяем строку (или используем строку, на которой стоит курсор)
        val selectionModel: SelectionModel = editor.selectionModel
        val line = if (selectionModel.hasSelection()) {
            editor.offsetToLogicalPosition(selectionModel.selectionStart).line
        } else {
            editor.caretModel.logicalPosition.line
        }

        // Формируем URL с номером строки
        val githubUrl = "$githubRepoUrl/blob/main$filePath#L${line + 1}"

        try {
            Desktop.getDesktop().browse(URI(githubUrl))
        } catch (e: Exception) {
            Messages.showErrorDialog("Could not open GitHub URL", "Error")
        }
    }
}
