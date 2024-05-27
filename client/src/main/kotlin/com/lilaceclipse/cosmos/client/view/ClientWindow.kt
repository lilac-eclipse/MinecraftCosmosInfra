package com.lilaceclipse.cosmos.client.view

import com.lilaceclipse.cosmos.client.model.FileUtil
import com.lilaceclipse.cosmos.client.model.ModInstaller
import com.lilaceclipse.cosmos.common.CURRENT_CLIENT_VERSION
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.*
import javax.inject.Inject
import javax.swing.*


class ClientWindow @Inject constructor(
    private val fileUtil: FileUtil
) : JFrame("Cosmos Installer v$CURRENT_CLIENT_VERSION") {

    // Model bindings
    lateinit var modInstaller: ModInstaller

    // Others
    private val cosmosInstallerLabel = JLabel()
    private val gameVersionLabel = JLabel()
    private val gameVersionList = JComboBox<String>()
    private val installationDirectory = JLabel()
    private val directoryName = JButton()
    private val progressBar = JProgressBar()
    private val installButton = JButton()

    private val log = KotlinLogging.logger {}


    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        iconImage = ImageIcon(ClientWindow::class.java.getResource("/cosmos_icon.png")).image
        size = Dimension(480, 400)
        minimumSize = Dimension(480, 400)
        maximumSize = Dimension(480, 400)
        isResizable = false

        contentPane.layout = GridBagLayout()

        cosmosInstallerLabel.apply {
            font = font.deriveFont(36.0f)
            horizontalAlignment = SwingConstants.CENTER
            icon = ImageIcon(ClientWindow::class.java.getResource("/cosmos_icon.png"))
            text = " Cosmos Installer"
            maximumSize = Dimension(350, 64)
        }
        addComponent(cosmosInstallerLabel, 1, 0, insets = Insets(30, 0, 0, 0))

        gameVersionLabel.apply {
            font = font.deriveFont(font.style or Font.BOLD, 16.0f)
            horizontalAlignment = SwingConstants.CENTER
            text = "Select game version:"
            toolTipText = ""
            horizontalTextPosition = SwingConstants.CENTER
            maximumSize = Dimension(300, 24)
            minimumSize = Dimension(168, 24)
            preferredSize = Dimension(168, 24)
            isRequestFocusEnabled = false
        }
        addComponent(gameVersionLabel, 1, 1, fill = GridBagConstraints.HORIZONTAL, insets = Insets(20, 0, 0, 0))

        gameVersionList.apply {
            font = Font("Arial", Font.PLAIN, 14)
            model = DefaultComboBoxModel(arrayOf("1.20.6"))
            maximumSize = Dimension(168, 35)
            minimumSize = Dimension(168, 35)
            preferredSize = Dimension(168, 35)
        }
        addComponent(gameVersionList, 1, 2, insets = Insets(6, 0, 0, 0))

        installationDirectory.apply {
            font = font.deriveFont(font.style or Font.BOLD, 16.0f)
            horizontalAlignment = SwingConstants.CENTER
            text = "Installation directory:"
            horizontalTextPosition = SwingConstants.CENTER
            maximumSize = Dimension(300, 24)
            minimumSize = Dimension(165, 24)
        }
        addComponent(installationDirectory, 1, 3, fill = GridBagConstraints.HORIZONTAL, insets = Insets(20, 0, 0, 0))

        directoryName.apply {
            font = font.deriveFont(16.0f)
            text = fileUtil.getDefaultInstallDirectory().path
            maximumSize = Dimension(300, 36)
            minimumSize = Dimension(300, 36)
            preferredSize = Dimension(300, 36)
            addActionListener { selectInstallDirectory() }
        }
        addComponent(directoryName, 1, 4, insets = Insets(6, 0, 0, 0))

        progressBar.apply {
            font = font.deriveFont(16.0f)
            alignmentX = Component.CENTER_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
            maximumSize = Dimension(380, 25)
            minimumSize = Dimension(380, 25)
            preferredSize = Dimension(380, 25)
        }
        addComponent(progressBar, 1, 5, insets = Insets(40, 0, 0, 0))

        installButton.apply {
            font = font.deriveFont(16.0f)
            text = "Install"
            toolTipText = ""
            margin = Insets(10, 70, 10, 70)
            maximumSize = Dimension(320, 45)
            minimumSize = Dimension(173, 45)
            addActionListener { modInstaller.installMods() }
        }
        addComponent(installButton, 1, 6, insets = Insets(12, 0, 30, 0))

        pack()
        setLocationRelativeTo(null)
    }

    private fun addComponent(component: Component, gridx: Int, gridy: Int, fill: Int = GridBagConstraints.NONE, weightx: Double = 0.0, insets: Insets = Insets(0, 0, 0, 0)) {
        val constraints = GridBagConstraints().apply {
            this.gridx = gridx
            this.gridy = gridy
            this.fill = fill
            this.weightx = weightx
            this.insets = insets
        }
        contentPane.add(component, constraints)
    }

    private fun selectInstallDirectory() {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            currentDirectory = fileUtil.getDefaultInstallDirectory()
        }
        val result = fileChooser.showOpenDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            directoryName.text = fileChooser.selectedFile.path
        }
    }
}