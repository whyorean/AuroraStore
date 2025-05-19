/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Properties

/**
 * Serializer for [Properties] for working with Kotlin's Serialization library
 */
object PropertiesSerializer : KSerializer<Properties> {
    override val descriptor: SerialDescriptor =
        MapSerializer(String.serializer(), String.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: Properties) {
        val map = value.stringPropertyNames().associateWith { value.getProperty(it) }
        MapSerializer(String.serializer(), String.serializer()).serialize(encoder, map)
    }

    override fun deserialize(decoder: Decoder): Properties {
        val map = MapSerializer(String.serializer(), String.serializer()).deserialize(decoder)
        return Properties().apply { putAll(map) }
    }
}
