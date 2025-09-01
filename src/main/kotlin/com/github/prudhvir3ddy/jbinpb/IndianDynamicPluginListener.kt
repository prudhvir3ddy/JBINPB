package com.github.prudhvir3ddy.jbinpb

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId

class IndianDynamicPluginListener : DynamicPluginListener {

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId == PluginId.getId("com.github.prudhvir3ddy.jbinpb")) {
            service<IndianService>()
        }
    }
}