package src.yandex.cloud

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import src.yandex.cloud.models.SourceCraftModel
import java.awt.Desktop
import java.net.URI

val hostRegex = Regex("""(https?|ssh)://(.*?)\.?(git|ssh)\.(o\.(cloud(?:-preprod)?)\.yandex\.net)/(.*?)/(.*?)\.git""")


class OpenInSourceCraftAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val url = SourceCraftModel(event).getSelectedUrl() ?: return

        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: Exception) {
            Messages.showErrorDialog("Could not open SourceCraft URL", "Error")
        }
    }
}
