/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */

package com.aliucord.manager.utils

import com.aliucord.manager.preferences.Prefs
import pxb.android.axml.*

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
                        "uses-sdk" -> ReplaceAttrsVisitor(nv, mapOf("targetSdkVersion" to 29))
                        "permission" -> ReplaceAttrsVisitor(nv, mapOf("name" to "$packageName.permission.CONNECT"))
                        "application" -> object : ReplaceAttrsVisitor(
                            nv,
                            mapOf(REQUEST_LEGACY_STORAGE to 1, "label" to Prefs.appName.get(), DEBUGGABLE to Prefs.debuggable.get())
                        ) {
                            private var addLegacyStorage = true
                            private var addDebuggable = Prefs.debuggable.get()

                            override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                super.attr(ns, name, resourceId, type, value)
                                if (name == REQUEST_LEGACY_STORAGE) addLegacyStorage = false
                                else if (name == DEBUGGABLE) addDebuggable = false
                            }

                            override fun child(ns: String?, name: String): NodeVisitor {
                                val visitor = super.child(ns, name)
                                return when (name) {
                                    "activity" -> ReplaceAttrsVisitor(visitor, mapOf("label" to Prefs.appName.get()))
                                    "service" -> object : NodeVisitor(visitor) {
                                        private var disableService = false

                                        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                            if (name == "name" && (
                                                    value == "com.google.android.gms.analytics.AnalyticsService" ||
                                                        value == "com.google.android.gms.analytics.AnalyticsJobService")
                                            ) disableService = true
                                            super.attr(
                                                ns, name, resourceId, type,
                                                if (value is String && value.endsWith("CONNECT"))
                                                    "$packageName.permission.CONNECT"
                                                else if (disableService && name == "enabled") 0
                                                else value
                                            )
                                        }
                                    }
                                    "provider" -> object : NodeVisitor(visitor) {
                                        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                            super.attr(
                                                ns, name, resourceId, type,
                                                if (name == "authorities") (value as String).replace("com.discord", packageName) else value
                                            )
                                        }
                                    }
                                    "meta-data" -> object : NodeVisitor(visitor) {
                                        private var crashlytics = false

                                        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
                                            if (name == "name" && value == "firebase_crashlytics_collection_enabled") crashlytics = true
                                            super.attr(
                                                ns, name, resourceId, type,
                                                if (name == "value" && crashlytics) 0 else value
                                            )
                                        }
                                    }
                                    else -> visitor
                                }
                            }

                            override fun end() {
                                if (addLegacyStorage)
                                    super.attr(ANDROID_NAMESPACE, REQUEST_LEGACY_STORAGE, -1, TYPE_INT_BOOLEAN, 1)
                                if (addDebuggable)
                                    super.attr(ANDROID_NAMESPACE, DEBUGGABLE, -1, TYPE_INT_BOOLEAN, 1)
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
