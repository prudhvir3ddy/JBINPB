package com.github.prudhvir3ddy.jbinpb

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.service

class IndianAppLifecycleListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        service<IndianService>()
    }
}