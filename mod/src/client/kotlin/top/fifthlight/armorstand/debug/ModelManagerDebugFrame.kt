package top.fifthlight.armorstand.debug

import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.util.TimeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ModelManagerDebugFrame : JFrame("Model Manager Status") {
    private val itemTableItem = DefaultTableModel(
        arrayOf("UUID", "Status", "Last Access", "Time Left"), 0
    )
    private val modelTableItem = DefaultTableModel(
        arrayOf("Path", "Status"), 0
    )
    private val itemTable = JTable(itemTableItem)
    private val modelTable = JTable(modelTableItem)
    private val updateTimer = Timer(1000) { updateData() }

    init {
        setupUI()
        setupListeners()
        startTracking()
    }

    private fun setupUI() {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(600, 400)
        layout = BorderLayout()

        itemTable.fillsViewportHeight = true

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        add(splitPane, BorderLayout.CENTER)

        splitPane.add(JScrollPane(itemTable), JSplitPane.TOP)
        splitPane.add(JScrollPane(modelTable), JSplitPane.BOTTOM)

        pack()
        splitPane.setDividerLocation(.5)
    }

    private fun setupListeners() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                updateTimer.stop()
            }
        })
    }

    private fun startTracking() {
        updateData()
        updateTimer.start()
    }

    private fun updateData() {
        val now = System.nanoTime()
        itemTableItem.rowCount = 0
        ModelInstanceManager.getItems().forEach { (uuid, item) ->
            val status = when (item) {
                is ModelInstanceManager.Item.Empty -> "Empty"
                is ModelInstanceManager.Item.Loading -> "Loading"
                is ModelInstanceManager.Item.Model -> "Loaded"
            }
            val lastAccessTime = when (item) {
                is ModelInstanceManager.Item.Model -> "${(now - item.lastAccessTime) / TimeUtil.NANOSECONDS_PER_SECOND}s"
                else -> "N/A"
            }
            val timeLeft = if (item is ModelInstanceManager.Item.Model) {
                if (item.path == ModelInstanceManager.selfPath) {
                    "Permanent"
                } else {
                    val expireTime = now - item.lastAccessTime
                    "${(ModelInstanceManager.INSTANCE_EXPIRE_NS - expireTime) / TimeUtil.NANOSECONDS_PER_SECOND}s"
                }
            } else {
                "N/A"
            }

            itemTableItem.addRow(arrayOf(
                uuid.toString(),
                status,
                lastAccessTime,
                timeLeft
            ))
        }

        modelTableItem.rowCount = 0
        ModelInstanceManager.getCache().forEach { (path, item) ->
            modelTableItem.addRow(arrayOf(
                path,
                when (item) {
                    ModelInstanceManager.ModelCache.Failed -> "Failed"
                    is ModelInstanceManager.ModelCache.Loaded -> "Loaded"
                }
            ))
        }
    }
}
