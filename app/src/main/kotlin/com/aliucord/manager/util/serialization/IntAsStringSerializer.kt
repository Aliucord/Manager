package com.aliucord.manager.util.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * (De)serializes an integer as a string.
 */
object IntAsStringSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Int {
        val stringValue = decoder.decodeString()

        return try {
            stringValue.toInt()
        } catch (e: NumberFormatException) {
            throw SerializationException(e)
        }
    }
}
