package su.pank.yamapi.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*


/**
 * Сериализация типов int или string в string
 *
 * @author Pankov Vasya <pank@pank.su>
 */
object IntOrStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor
        get() = serialDescriptor<String>()

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: throw Exception("Only JSON format is expected")
        val json = jsonDecoder.decodeJsonElement()
        return if (json is JsonPrimitive && json.intOrNull != null) json.int.toString()
        else return json.jsonPrimitive.content
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

}