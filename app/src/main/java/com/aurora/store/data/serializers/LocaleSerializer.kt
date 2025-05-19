/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.util.Locale

/**
 * Serializer for [Locale] for working with Kotlin's Serialization library
 */
object LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Locale") {
        element<String>("language")
        element<String>("region")
    }

    override fun serialize(encoder: Encoder, value: Locale) {
        return encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.language)
            encodeStringElement(descriptor, 1, value.country)
        }
    }

    override fun deserialize(decoder: Decoder): Locale {
        return decoder.decodeStructure(descriptor) {
            var language: String? = null
            var region: String? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> language = decodeStringElement(descriptor, 0)
                    1 -> region = decodeStringElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }

            require(!language.isNullOrBlank() && !region.isNullOrBlank())
            Locale.Builder()
                .setLanguage(language)
                .setRegion(region)
                .build()
        }
    }
}
