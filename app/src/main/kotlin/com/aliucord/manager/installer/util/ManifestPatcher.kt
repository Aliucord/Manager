/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.installer.util

import com.aliucord.manager.preferences.Prefs
import pxb.android.axml.*

object ManifestPatcher {
    private const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
    private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    private const val USES_CLEARTEXT_TRAFFIC = "usesCleartextTraffic"
    private const val DEBUGGABLE = "debuggable"
    private const val LABEL = "label"

    fun patchManifest(manifestBytes: ByteArray): ByteArray {
        val packageName = Prefs.packageName.get()
        val reader = AxmlReader(manifestBytes)
        val writer = AxmlWriter()

        reader.accept(object : AxmlVisitor(writer) {
            override fun child(ns: String?, name: String?) =
                object : ReplaceAttrsVisitor(super.child(ns, name), mapOf("package" to packageName)) {
                    private var addManagePerm = true

                    override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                        super.attr(ns, name, resourceId, type, value)

                        if (value == MANAGE_EXTERNAL_STORAGE) addManagePerm = false
                    }

                    override fun child(ns: String?, name: String): NodeVisitor {
                        val nv = super.child(ns, name)

                        return when (name) {
                            "application" -> object : ReplaceAttrsVisitor(
                                nv,
                                mapOf(
                                    LABEL to Prefs.appName.get(),
                                    DEBUGGABLE to Prefs.debuggable.get(),
                                    USES_CLEARTEXT_TRAFFIC to true
                                )
                            ) {
                                private var addDebuggable = Prefs.debuggable.get()
                                private var addUseClearTextTraffic = true

                                override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                    super.attr(ns, name, resourceId, type, value)
                                    if (name == DEBUGGABLE) addDebuggable = false
                                    if (name == USES_CLEARTEXT_TRAFFIC) addUseClearTextTraffic = false
                                }

                                override fun child(ns: String?, name: String): NodeVisitor {
                                    val visitor = super.child(ns, name)

                                    return when (name) {
                                        "activity" -> ReplaceAttrsVisitor(visitor, mapOf("label" to Prefs.appName.get()))
                                        "provider" -> object : NodeVisitor(visitor) {
                                            override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                                super.attr(
                                                    ns, name, resourceId, type,
                                                    if (name == "authorities") (value as String).replace("com.discord", packageName) else value
                                                )
                                            }
                                        }
                                        else -> visitor
                                    }
                                }

                                override fun end() {
                                    if (addDebuggable) super.attr(ANDROID_NAMESPACE, DEBUGGABLE, -1, TYPE_INT_BOOLEAN, 1)
                                    if (addUseClearTextTraffic) super.attr(ANDROID_NAMESPACE, USES_CLEARTEXT_TRAFFIC, -1, TYPE_INT_BOOLEAN, 1)
                                    super.end()
                                }
                            }
                            else -> nv
                        }
                    }

                    override fun end() {
                        if (addManagePerm) super.child(null, "uses-permission").attr(
                            ANDROID_NAMESPACE, "name", -1, TYPE_STRING, MANAGE_EXTERNAL_STORAGE
                        )
                        super.end()
                    }
                }
        })

        return writer.toByteArray()
    }

    fun renamePackage(manifestBytes: ByteArray): ByteArray {
        val packageName = Prefs.packageName.get()
        val reader = AxmlReader(manifestBytes)
        val writer = AxmlWriter()

        reader.accept(object : AxmlVisitor(writer) {
            override fun child(ns: String?, name: String?): ReplaceAttrsVisitor {
                return ReplaceAttrsVisitor(super.child(ns, name), mapOf("package" to packageName))
            }
        })

        return writer.toByteArray()
    }

    private open class ReplaceAttrsVisitor(
        nv: NodeVisitor,
        private val attrs: Map<String, Any>
    ) : NodeVisitor(nv) {
        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
            val replace = attrs.containsKey(name)
            val newValue = attrs[name]

            super.attr(ns, name, resourceId, if (newValue is String) TYPE_STRING else type, if (replace) newValue else value)
        }
    }
}
