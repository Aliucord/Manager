/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.installer.util

import android.Manifest
import pxb.android.axml.*

object ManifestPatcher {
    private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    private const val USES_CLEARTEXT_TRAFFIC = "usesCleartextTraffic"
    private const val DEBUGGABLE = "debuggable"
    private const val LABEL = "label"
    private const val PACKAGE = "package"
    private const val COMPILE_SDK_VERSION = "compileSdkVersion"
    private const val COMPILE_SDK_VERSION_CODENAME = "compileSdkVersionCodename"

    fun patchManifest(
        manifestBytes: ByteArray,
        packageName: String,
        appName: String,
        debuggable: Boolean
    ): ByteArray {
        val reader = AxmlReader(manifestBytes)
        val writer = AxmlWriter()

        reader.accept(object : AxmlVisitor(writer) {
            override fun child(ns: String?, name: String?) =
                object : ReplaceAttrsVisitor(
                    super.child(ns, name),
                    mapOf(
                        PACKAGE to packageName,
                        COMPILE_SDK_VERSION to 23,
                        COMPILE_SDK_VERSION_CODENAME to "6.0-2438415",
                        "platformBuildVersionCode" to 23,
                        "platformBuildVersionName" to 6
                    )
                ) {
                    private var addExternalStoragePerm = false

                    override fun child(ns: String?, name: String): NodeVisitor {
                        val nv = super.child(ns, name)

                        // Add MANAGE_EXTERNAL_STORAGE when necessary
                        if (addExternalStoragePerm) super.child(null, "uses-permission").attr(
                            ANDROID_NAMESPACE, "name", 16842755, TYPE_STRING, Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        ).also { addExternalStoragePerm = false }

                        return when (name) {
                            "uses-permission" -> object : NodeVisitor(nv) {
                                override fun attr(ns: String?, name: String?, resourceId: Int, type: Int, obj: Any?) {
                                    // Set the add external storage permission to be added after WRITE_EXTERNAL_STORAGE (which is after read)
                                    if (name == "name" && obj as String == Manifest.permission.READ_EXTERNAL_STORAGE) addExternalStoragePerm = true
                                    if (name != "maxSdkVersion") super.attr(ns, name, resourceId, type, obj)
                                }
                            }
                            "application" -> object : ReplaceAttrsVisitor(
                                nv,
                                mapOf(
                                    LABEL to appName,
                                    DEBUGGABLE to debuggable,
                                    USES_CLEARTEXT_TRAFFIC to true
                                )
                            ) {
                                private var addDebuggable = debuggable
                                private var addUseClearTextTraffic = true

                                override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                    super.attr(ns, name, resourceId, type, value)
                                    if (name == DEBUGGABLE) addDebuggable = false
                                    if (name == USES_CLEARTEXT_TRAFFIC) addUseClearTextTraffic = false
                                }

                                override fun child(ns: String?, name: String): NodeVisitor {
                                    val visitor = super.child(ns, name)

                                    return when (name) {
                                        "activity" -> ReplaceAttrsVisitor(visitor, mapOf("label" to appName))
                                        "provider" -> object : NodeVisitor(visitor) {
                                            override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                                super.attr(
                                                    /* ns = */ ns,
                                                    /* name = */ name,
                                                    /* resourceId = */ resourceId,
                                                    /* type = */ type,
                                                    /* obj = */ if (name == "authorities") (value as String).replace("com.discord", packageName) else value
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
                }
        })

        return writer.toByteArray()
    }

    fun renamePackage(
        manifestBytes: ByteArray,
        packageName: String
    ): ByteArray {
        val reader = AxmlReader(manifestBytes)
        val writer = AxmlWriter()

        reader.accept(
            object : AxmlVisitor(writer) {
                override fun child(ns: String?, name: String?): ReplaceAttrsVisitor {
                    return ReplaceAttrsVisitor(super.child(ns, name), mapOf("package" to packageName))
                }
            }
        )

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
