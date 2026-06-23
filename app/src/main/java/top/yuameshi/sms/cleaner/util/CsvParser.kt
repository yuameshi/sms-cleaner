package top.yuameshi.sms.cleaner.util

import java.io.BufferedReader

/**
 * RFC 4180 compliant CSV line parser.
 *
 * Handles:
 * - Fields enclosed in double quotes
 * - Quoted fields containing line breaks (CRLF), commas, and escaped quotes
 * - Escaped quotes represented as ""
 */
object CsvParser {

    /**
     * Read a single CSV record from the reader, handling multi-line quoted fields.
     * Returns a list of field values, or null if end of stream.
     */
    fun readCsvLine(reader: BufferedReader): List<String>? {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        // Read until we get a complete record (not in the middle of a quoted field)
        while (true) {
            val c = reader.read()
            if (c == -1) {
                // End of stream
                if (current.isNotEmpty() || fields.isNotEmpty()) {
                    // Add the last field
                    fields.add(current.toString())
                    return if (fields.isNotEmpty()) fields else null
                }
                return null
            }

            val char = c.toChar()

            when {
                char == '"' -> {
                    if (inQuotes) {
                        // Inside quotes: check for escaped quote ("")
                        val nextC = reader.read()
                        if (nextC != -1) {
                            val nextChar = nextC.toChar()
                            if (nextChar == '"') {
                                // Escaped quote - add a single quote
                                current.append('"')
                            } else {
                                // End of quoted field
                                inQuotes = false
                                // Process the next character
                                when {
                                    nextChar == ',' -> {
                                        fields.add(current.toString())
                                        current.clear()
                                    }
                                    nextChar == '\r' || nextChar == '\n' -> {
                                        // End of record
                                        fields.add(current.toString())
                                        // Consume LF if CRLF
                                        if (nextChar == '\r') {
                                            reader.mark(1)
                                            val peek = reader.read()
                                            if (peek != -1 && peek.toChar() != '\n') {
                                                reader.reset()
                                            }
                                        }
                                        return fields
                                    }
                                    else -> {
                                        // Unexpected character after closing quote
                                        current.append(nextChar)
                                    }
                                }
                            }
                        } else {
                            // End of stream after closing quote
                            inQuotes = false
                            fields.add(current.toString())
                            return fields
                        }
                    } else {
                        // Start of quoted field
                        inQuotes = true
                    }
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                (char == '\r' || char == '\n') && !inQuotes -> {
                    // End of record (outside quotes)
                    fields.add(current.toString())
                    // Consume LF if CRLF
                    if (char == '\r') {
                        reader.mark(1)
                        val peek = reader.read()
                        if (peek != -1 && peek.toChar() != '\n') {
                            reader.reset()
                        }
                    }
                    return fields
                }
                else -> {
                    current.append(char)
                }
            }
        }
    }
}
