package com.example.api

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A robust, memory-efficient local GGUF parser that reads GGUF model files
 * to verify their integrity, parse metadata, architecture details, and tensor counts
 * directly on-device.
 */
object GgufParser {
    private const val TAG = "GgufParser"
    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in Little Endian

    enum class GgufValueType(val id: Int) {
        UINT8(0),
        INT8(1),
        UINT16(2),
        INT16(3),
        UINT32(4),
        INT32(5),
        FLOAT32(6),
        BOOL(7),
        STRING(8),
        ARRAY(9),
        UINT64(10),
        INT64(11),
        FLOAT64(12);

        companion object {
            fun fromId(id: Int): GgufValueType? {
                return values().find { it.id == id }
            }
        }
    }

    data class GgufMetadata(
        val isValid: Boolean,
        val version: Int,
        val tensorCount: Long,
        val kvCount: Long,
        val architecture: String = "unknown",
        val modelName: String = "unknown",
        val alignment: Int = 32,
        val contextLength: Int = 2048,
        val keyValues: Map<String, Any> = emptyMap()
    )

    fun parseHeader(file: File): GgufMetadata {
        if (!file.exists() || file.length() < 24) {
            return GgufMetadata(false, 0, 0, 0)
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(24)
                raf.readFully(headerBytes)
                val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

                val magic = buffer.int
                if (magic != GGUF_MAGIC) {
                    Log.w(TAG, "${file.name} is not a valid GGUF file. Magic: ${Integer.toHexString(magic)}")
                    return GgufMetadata(false, 0, 0, 0)
                }

                val version = buffer.int
                val tensorCount = buffer.long
                val kvCount = buffer.long

                Log.d(TAG, "Parsed GGUF Header: Name=${file.name}, Version=$version, Tensors=$tensorCount, KV-Pairs=$kvCount")

                // Let's read and parse key-value pairs safely up to a limit
                val kvs = mutableMapOf<String, Any>()
                var architecture = "unknown"
                var modelName = "unknown"
                var alignment = 32
                var contextLength = 2048

                // Set file pointer to continue reading after header (24 bytes)
                raf.seek(24)

                // Read KV pairs sequentially
                for (i in 0 until kvCount.coerceAtMost(100)) { // Limit to 100 KV pairs to prevent infinite loops or huge reads
                    if (raf.filePointer >= file.length() - 8) break

                    val key = readGgufString(raf) ?: break
                    if (key.isEmpty()) continue

                    val typeId = readInt32(raf) ?: break
                    val type = GgufValueType.fromId(typeId) ?: break

                    val value = readGgufValue(raf, type) ?: break
                    kvs[key] = value

                    when (key) {
                        "general.architecture" -> architecture = value.toString()
                        "general.name" -> modelName = value.toString()
                        "general.alignment" -> alignment = (value as? Number)?.toInt() ?: 32
                        "llama.context_length", "mamba.context_length", "lfm.context_length" -> {
                            contextLength = (value as? Number)?.toInt() ?: 2048
                        }
                    }
                }

                return GgufMetadata(
                    isValid = true,
                    version = version,
                    tensorCount = tensorCount,
                    kvCount = kvCount,
                    architecture = architecture,
                    modelName = modelName,
                    alignment = alignment,
                    contextLength = contextLength,
                    keyValues = kvs
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GGUF file ${file.name}", e)
            return GgufMetadata(false, 0, 0, 0)
        }
    }

    private fun readGgufString(raf: RandomAccessFile): String? {
        val len = readUint64(raf) ?: return null
        if (len < 0 || len > 1024) return null // String sanity check
        val bytes = ByteArray(len.toInt())
        raf.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readGgufValue(raf: RandomAccessFile, type: GgufValueType): Any? {
        return when (type) {
            GgufValueType.UINT8 -> raf.readUnsignedByte()
            GgufValueType.INT8 -> raf.readByte().toInt()
            GgufValueType.UINT16 -> readUint16(raf)
            GgufValueType.INT16 -> readInt16(raf)
            GgufValueType.UINT32 -> readUint32(raf)
            GgufValueType.INT32 -> readInt32(raf)
            GgufValueType.FLOAT32 -> readFloat32(raf)
            GgufValueType.BOOL -> raf.readByte() != 0.toByte()
            GgufValueType.STRING -> readGgufString(raf)
            GgufValueType.ARRAY -> {
                val itemTypeId = readInt32(raf) ?: return null
                val itemType = GgufValueType.fromId(itemTypeId) ?: return null
                val arrayLen = readUint64(raf) ?: return null
                if (arrayLen < 0 || arrayLen > 200) {
                    // Skip if too large
                    skipArrayBytes(raf, itemType, arrayLen)
                    return "[Array of type $itemType, size $arrayLen]"
                }
                val list = mutableListOf<Any>()
                for (i in 0 until arrayLen.toInt()) {
                    val item = readGgufValue(raf, itemType) ?: break
                    list.add(item)
                }
                list
            }
            GgufValueType.UINT64 -> readUint64(raf)
            GgufValueType.INT64 -> readInt64(raf)
            GgufValueType.FLOAT64 -> readFloat64(raf)
        }
    }

    private fun skipArrayBytes(raf: RandomAccessFile, type: GgufValueType, len: Long) {
        val sizePerItem = when (type) {
            GgufValueType.UINT8, GgufValueType.INT8, GgufValueType.BOOL -> 1
            GgufValueType.UINT16, GgufValueType.INT16 -> 2
            GgufValueType.UINT32, GgufValueType.INT32, GgufValueType.FLOAT32 -> 4
            GgufValueType.UINT64, GgufValueType.INT64, GgufValueType.FLOAT64 -> 8
            else -> 0
        }
        if (sizePerItem > 0) {
            raf.skipBytes((len * sizePerItem).toInt())
        } else {
            // For strings or nested arrays, we must parse and skip individually
            for (i in 0 until len.coerceAtMost(50).toInt()) {
                readGgufValue(raf, type)
            }
        }
    }

    private fun readUint16(raf: RandomAccessFile): Int? {
        val b = ByteArray(2)
        if (raf.read(b) != 2) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
    }

    private fun readInt16(raf: RandomAccessFile): Int? {
        val b = ByteArray(2)
        if (raf.read(b) != 2) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
    }

    private fun readUint32(raf: RandomAccessFile): Long? {
        val b = ByteArray(4)
        if (raf.read(b) != 4) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
    }

    private fun readInt32(raf: RandomAccessFile): Int? {
        val b = ByteArray(4)
        if (raf.read(b) != 4) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readFloat32(raf: RandomAccessFile): Float? {
        val b = ByteArray(4)
        if (raf.read(b) != 4) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).float
    }

    private fun readUint64(raf: RandomAccessFile): Long? {
        val b = ByteArray(8)
        if (raf.read(b) != 8) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readInt64(raf: RandomAccessFile): Long? {
        val b = ByteArray(8)
        if (raf.read(b) != 8) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readFloat64(raf: RandomAccessFile): Double? {
        val b = ByteArray(8)
        if (raf.read(b) != 8) return null
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).double
    }
}
