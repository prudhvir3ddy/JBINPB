package com.github.prudhvir3ddy.jbinpb

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import javax.swing.UIManager

@Service
class IndianService : Disposable, LafManagerListener {

    init {
        updateProgressBarUi()
    }

    private fun updateProgressBarUi() {
        UIManager.put("ProgressBarUI", IndianProgressBarUiInstaller::class.java.name)
        UIManager.getDefaults()[IndianProgressBarUiInstaller::class.java.name] = IndianProgressBarUiInstaller::class.java
    }

    override fun dispose() {
        // TODO restore default ProgressBarUI
    }

    override fun lookAndFeelChanged(source: LafManager) {
        updateProgressBarUi()
    }
}