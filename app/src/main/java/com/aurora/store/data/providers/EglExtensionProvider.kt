/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aurora.store.data.providers

import android.opengl.GLES10
import android.text.TextUtils
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

object EglExtensionProvider {

    @JvmStatic
    val eglExtensions: List<String>
        get() {
            val extensions = mutableSetOf<String>()
            val egl = EGLContext.getEGL() as EGL10
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

            egl.eglInitialize(display, null)
            val configCount = IntArray(1)

            if (egl.eglGetConfigs(display, null, 0, configCount)) {
                val configs = arrayOfNulls<EGLConfig>(configCount[0])
                if (egl.eglGetConfigs(display, configs, configCount[0], configCount)) {
                    val pbufferAttribs = intArrayOf(
                        EGL10.EGL_WIDTH, EGL10.EGL_PBUFFER_BIT,
                        EGL10.EGL_HEIGHT, EGL10.EGL_PBUFFER_BIT,
                        EGL10.EGL_NONE
                    )
                    val contextAttributes = intArrayOf(12440, EGL10.EGL_PIXMAP_BIT, EGL10.EGL_NONE)

                    for (config in configs) {
                        if (isValidConfig(egl, display, config)) {
                            addExtensionsForConfig(
                                egl,
                                display,
                                config,
                                pbufferAttribs,
                                null,
                                extensions
                            )
                            addExtensionsForConfig(
                                egl,
                                display,
                                config,
                                pbufferAttribs,
                                contextAttributes,
                                extensions
                            )
                        }
                    }
                }
            }

            egl.eglTerminate(display)

            return extensions
                .filter { it.isNotEmpty() }
                .sorted()
        }

    private fun isValidConfig(egl: EGL10, display: EGLDisplay, config: EGLConfig?): Boolean {
        val configAttrib = IntArray(1)
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_CONFIG_CAVEAT, configAttrib)
        if (configAttrib[0] == EGL10.EGL_SLOW_CONFIG) return false

        egl.eglGetConfigAttrib(display, config, EGL10.EGL_SURFACE_TYPE, configAttrib)
        if (configAttrib[0] and 1 == 0) return false

        egl.eglGetConfigAttrib(display, config, EGL10.EGL_RENDERABLE_TYPE, configAttrib)
        return configAttrib[0] and 1 != 0
    }

    private fun addExtensionsForConfig(
        egl: EGL10,
        display: EGLDisplay,
        config: EGLConfig?,
        pbufferAttribs: IntArray,
        contextAttribs: IntArray?,
        extensions: MutableSet<String>
    ) {
        val context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttribs)
        if (context == EGL10.EGL_NO_CONTEXT) return

        val surface = egl.eglCreatePbufferSurface(display, config, pbufferAttribs)
        if (surface == EGL10.EGL_NO_SURFACE) {
            egl.eglDestroyContext(display, context)
            return
        }

        egl.eglMakeCurrent(display, surface, surface, context)
        val extensionString = GLES10.glGetString(GLES10.GL_EXTENSIONS)

        if (!TextUtils.isEmpty(extensionString)) {
            extensions.addAll(extensionString.split(" "))
        }

        egl.eglMakeCurrent(
            display,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        egl.eglDestroySurface(display, surface)
        egl.eglDestroyContext(display, context)
    }
}
