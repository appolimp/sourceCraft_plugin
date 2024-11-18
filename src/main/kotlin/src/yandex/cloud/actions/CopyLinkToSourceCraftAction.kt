package src.yandex.cloud.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Messages
import src.yandex.cloud.models.SourceCraftModel
import java.awt.datatransfer.StringSelection

class CopyLinkToSourceCraftAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val link = SourceCraftModel(event).getSelectedUrl() ?: return

        try {
            CopyPasteManager.getInstance().setContents(StringSelection(link))
        } catch (e: Exception) {
            Messages.showErrorDialog("Could not copy SourceCraft URL", "Error")
        }
    }
}
