/*
 * Copyright (c) 2022 Juby210 & zt
 * Licensed under the Open Software License version 3.0
 */


package com.aliucord.manager.utils

import pxb.android.axml.NodeVisitor

open class ReplaceAttrsVisitor(
    nv: NodeVisitor,
    private val attrs: Map<String, Any>
) : NodeVisitor(nv) {
    override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
        val replace = attrs.containsKey(name)
        val newValue = attrs[name]
        super.attr(ns, name, resourceId, if (newValue is String) TYPE_STRING else type, if (replace) newValue else value)
    }
}
