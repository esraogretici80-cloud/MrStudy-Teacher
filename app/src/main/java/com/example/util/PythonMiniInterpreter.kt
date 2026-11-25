package com.example.util

import kotlin.math.pow

data class PythonExecutionResult(
    val outputLines: List<String>,
    val errorMessage: String? = null
)

object PythonMiniInterpreter {

    private const val MAX_STEPS = 800

    fun execute(code: String): PythonExecutionResult {
        val rawLines = code.replace("\r\n", "\n").split("\n")
        val output = mutableListOf<String>()
        val env = linkedMapOf<String, Any>()
        var steps = 0

        fun indentOf(line: String): Int = line.takeWhile { it == ' ' || it == '\t' }.sumOf { if (it == '\t') 4 else 1 }

        fun evalExpr(exprRaw: String): Any {
            val e = exprRaw.trim()
            if (e.isEmpty()) return ""
            // String literal
            if ((e.startsWith("\"") && e.endsWith("\"") && e.length >= 2) ||
                (e.startsWith("'") && e.endsWith("'") && e.length >= 2)
            ) {
                return e.substring(1, e.length - 1)
            }
            if (e == "True") return true
            if (e == "False") return false
            if (e.startsWith("[") && e.endsWith("]")) {
                val inner = e.substring(1, e.length - 1).trim()
                if (inner.isEmpty()) return emptyList<Any>()
                return splitArgs(inner).map { evalExpr(it) }
            }
            if (e.startsWith("len(") && e.endsWith(")")) {
                val innerVal = evalExpr(e.substring(4, e.length - 1))
                return when (innerVal) {
                    is List<*> -> innerVal.size
                    is String -> innerVal.length
                    else -> 0
                }
            }
            if (e.startsWith("sum(") && e.endsWith(")")) {
                val innerVal = evalExpr(e.substring(4, e.length - 1))
                return if (innerVal is List<*>) innerVal.sumOf { toDouble(it) } else 0.0
            }
            // Comparison ops
            for (op in listOf("==", "!=", ">=", "<=", ">", "<")) {
                val idx = findTopLevelOp(e, op)
                if (idx > 0) {
                    val left = evalExpr(e.substring(0, idx))
                    val right = evalExpr(e.substring(idx + op.length))
                    return compareValues(left, right, op)
                }
            }
            // Arithmetic (+, -, *, /, //, %, **)
            for (op in listOf("+", "-")) {
                val idx = findLastTopLevelOp(e, op)
                if (idx > 0) {
                    val l = evalExpr(e.substring(0, idx))
                    val r = evalExpr(e.substring(idx + op.length))
                    return if (op == "+") {
                        if (l is String || r is String) "${formatVal(l)}${formatVal(r)}"
                        else numResult(toDouble(l) + toDouble(r))
                    } else {
                        numResult(toDouble(l) - toDouble(r))
                    }
                }
            }
            for (op in listOf("//", "*", "/", "%")) {
                val idx = findLastTopLevelOp(e, op)
                if (idx > 0) {
                    val l = toDouble(evalExpr(e.substring(0, idx)))
                    val r = toDouble(evalExpr(e.substring(idx + op.length)))
                    return when (op) {
                        "*" -> numResult(l * r)
                        "//" -> {
                            require(r != 0.0) { "Sıfıra bölme hatası" }
                            (l.toLong() / r.toLong()).toDouble().let(::numResult)
                        }
                        "/" -> {
                            require(r != 0.0) { "Sıfıra bölme hatası" }
                            numResult(l / r)
                        }
                        "%" -> {
                            require(r != 0.0) { "Sıfıra bölme hatası" }
                            numResult(l % r)
                        }
                        else -> 0
                    }
                }
            }
            val powIdx = findLastTopLevelOp(e, "**")
            if (powIdx > 0) {
                val l = toDouble(evalExpr(e.substring(0, powIdx)))
                val r = toDouble(evalExpr(e.substring(powIdx + 2)))
                return numResult(l.pow(r))
            }
            if (e.startsWith("(") && e.endsWith(")")) {
                return evalExpr(e.substring(1, e.length - 1))
            }
            e.toLongOrNull()?.let { return it }
            e.toDoubleOrNull()?.let { return it }
            if (env.containsKey(e)) return env[e] ?: ""
            throw IllegalArgumentException("Tanımsız ifade veya değişken: '$e'")
        }

        fun runBlock(startIdx: Int, endIdx: Int, minIndent: Int) {
            var i = startIdx
            while (i < endIdx) {
                steps++
                if (steps > MAX_STEPS) throw IllegalStateException("Maksimum adım sınırı ($MAX_STEPS) aşıldı (sonsuz döngü koruması).")
                val raw = rawLines[i]
                val trimmed = raw.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    i++
                    continue
                }
                val indent = indentOf(raw)
                if (indent < minIndent) break

                when {
                    trimmed.startsWith("print(") && trimmed.endsWith(")") -> {
                        val inner = trimmed.substring(6, trimmed.length - 1)
                        val args = if (inner.isBlank()) emptyList() else splitArgs(inner).map { formatVal(evalExpr(it)) }
                        output.add(args.joinToString(" "))
                        i++
                    }

                    trimmed.startsWith("for ") && trimmed.endsWith(":") -> {
                        val header = trimmed.removePrefix("for ").removeSuffix(":").trim()
                        val inParts = header.split(" in ", limit = 2)
                        require(inParts.size == 2) { "Satır ${i + 1}: Geçersiz for sözdizimi" }
                        val varName = inParts[0].trim()
                        val iterExpr = inParts[1].trim()
                        val items: List<Any> = if (iterExpr.startsWith("range(") && iterExpr.endsWith(")")) {
                            val rArgs = splitArgs(iterExpr.substring(6, iterExpr.length - 1)).map { toDouble(evalExpr(it)).toInt() }
                            when (rArgs.size) {
                                1 -> (0 until rArgs[0]).map { it.toLong() }
                                2 -> (rArgs[0] until rArgs[1]).map { it.toLong() }
                                3 -> {
                                    val step = rArgs[2].takeIf { it != 0 } ?: 1
                                    IntProgression.fromClosedRange(rArgs[0], if (step > 0) rArgs[1] - 1 else rArgs[1] + 1, step).map { it.toLong() }
                                }
                                else -> emptyList()
                            }
                        } else {
                            val v = evalExpr(iterExpr)
                            if (v is List<*>) v.filterNotNull() else emptyList()
                        }
                        val blockEnd = findBlockEnd(rawLines, i + 1, indent)
                        for (item in items) {
                            env[varName] = item
                            runBlock(i + 1, blockEnd, indent + 1)
                        }
                        i = blockEnd
                    }

                    trimmed.startsWith("while ") && trimmed.endsWith(":") -> {
                        val condExpr = trimmed.removePrefix("while ").removeSuffix(":").trim()
                        val blockEnd = findBlockEnd(rawLines, i + 1, indent)
                        while (isTruthy(evalExpr(condExpr))) {
                            steps++
                            if (steps > MAX_STEPS) throw IllegalStateException("while döngüsü güvenlik sınırını aştı.")
                            runBlock(i + 1, blockEnd, indent + 1)
                        }
                        i = blockEnd
                    }

                    trimmed.startsWith("if ") && trimmed.endsWith(":") -> {
                        val condExpr = trimmed.removePrefix("if ").removeSuffix(":").trim()
                        val ifEnd = findBlockEnd(rawLines, i + 1, indent)
                        var nextIdx = ifEnd
                        var executed = false
                        if (isTruthy(evalExpr(condExpr))) {
                            runBlock(i + 1, ifEnd, indent + 1)
                            executed = true
                        }
                        while (nextIdx < endIdx) {
                            val nextTrim = rawLines[nextIdx].trim()
                            val nextIndent = indentOf(rawLines[nextIdx])
                            if (nextIndent != indent) break
                            if (nextTrim.startsWith("elif ") && nextTrim.endsWith(":")) {
                                val elifEnd = findBlockEnd(rawLines, nextIdx + 1, indent)
                                if (!executed && isTruthy(evalExpr(nextTrim.removePrefix("elif ").removeSuffix(":")))) {
                                    runBlock(nextIdx + 1, elifEnd, indent + 1)
                                    executed = true
                                }
                                nextIdx = elifEnd
                            } else if (nextTrim == "else:") {
                                val elseEnd = findBlockEnd(rawLines, nextIdx + 1, indent)
                                if (!executed) {
                                    runBlock(nextIdx + 1, elseEnd, indent + 1)
                                }
                                nextIdx = elseEnd
                                break
                            } else break
                        }
                        i = nextIdx
                    }

                    trimmed.contains("=") && !trimmed.contains("==") -> {
                        val eqIdx = trimmed.indexOf('=')
                        val lhs = trimmed.substring(0, eqIdx).trim()
                        val rhs = trimmed.substring(eqIdx + 1).trim()
                        if (lhs.endsWith("+") || lhs.endsWith("-") || lhs.endsWith("*")) {
                            val op = lhs.last()
                            val varName = lhs.dropLast(1).trim()
                            val cur = toDouble(env[varName] ?: 0L)
                            val delta = toDouble(evalExpr(rhs))
                            env[varName] = when (op) {
                                '+' -> numResult(cur + delta)
                                '-' -> numResult(cur - delta)
                                '*' -> numResult(cur * delta)
                                else -> cur
                            }
                        } else {
                            env[lhs] = evalExpr(rhs)
                        }
                        i++
                    }

                    else -> {
                        evalExpr(trimmed)
                        i++
                    }
                }
            }
        }

        return try {
            runBlock(0, rawLines.size, 0)
            PythonExecutionResult(outputLines = output)
        } catch (e: Exception) {
            PythonExecutionResult(
                outputLines = output,
                errorMessage = e.localizedMessage ?: "Çalıştırma hatası"
            )
        }
    }

    private fun findBlockEnd(lines: List<String>, start: Int, parentIndent: Int): Int {
        var idx = start
        while (idx < lines.size) {
            val l = lines[idx]
            if (l.trim().isNotEmpty() && !l.trim().startsWith("#")) {
                val ind = l.takeWhile { it == ' ' || it == '\t' }.sumOf { if (it == '\t') 4 else 1 }
                if (ind <= parentIndent) break
            }
            idx++
        }
        return idx
    }

    private fun splitArgs(raw: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var inQuotes = false
        var quoteChar = '"'
        for (ch in raw) {
            if ((ch == '"' || ch == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = ch
            } else if (inQuotes && ch == quoteChar) {
                inQuotes = false
            } else if (!inQuotes) {
                if (ch == '(' || ch == '[') depth++
                else if (ch == ')' || ch == ']') depth--
                else if (ch == ',' && depth == 0) {
                    result.add(cur.toString().trim())
                    cur.clear()
                    continue
                }
            }
            cur.append(ch)
        }
        if (cur.isNotBlank()) result.add(cur.toString().trim())
        return result
    }

    private fun findTopLevelOp(s: String, op: String): Int {
        var depth = 0
        var inQ = false
        var qCh = '"'
        var i = 0
        while (i <= s.length - op.length) {
            val c = s[i]
            if ((c == '"' || c == '\'') && !inQ) { inQ = true; qCh = c }
            else if (inQ && c == qCh) inQ = false
            else if (!inQ) {
                if (c == '(' || c == '[') depth++
                else if (c == ')' || c == ']') depth--
                else if (depth == 0 && s.startsWith(op, i)) return i
            }
            i++
        }
        return -1
    }

    private fun findLastTopLevelOp(s: String, op: String): Int {
        var depth = 0
        var inQ = false
        var qCh = '"'
        var last = -1
        var i = 0
        while (i <= s.length - op.length) {
            val c = s[i]
            if ((c == '"' || c == '\'') && !inQ) { inQ = true; qCh = c }
            else if (inQ && c == qCh) inQ = false
            else if (!inQ) {
                if (c == '(' || c == '[') depth++
                else if (c == ')' || c == ']') depth--
                else if (depth == 0 && s.startsWith(op, i)) {
                    if (op == "*" && (s.startsWith("**", i) || (i > 0 && s[i - 1] == '*'))) {
                        i++
                        continue
                    }
                    if (op == "/" && (s.startsWith("//", i) || (i > 0 && s[i - 1] == '/'))) {
                        i++
                        continue
                    }
                    last = i
                }
            }
            i++
        }
        return last
    }

    private fun toDouble(v: Any?): Double = when (v) {
        is Number -> v.toDouble()
        is Boolean -> if (v) 1.0 else 0.0
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private fun numResult(d: Double): Any = if (d % 1.0 == 0.0 && d in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) d.toLong() else d

    private fun isTruthy(v: Any?): Boolean = when (v) {
        is Boolean -> v
        is Number -> v.toDouble() != 0.0
        is String -> v.isNotEmpty()
        is List<*> -> v.isNotEmpty()
        else -> false
    }

    private fun compareValues(l: Any, r: Any, op: String): Boolean {
        val ld = (l as? Number)?.toDouble()
        val rd = (r as? Number)?.toDouble()
        return if (ld != null && rd != null) {
            when (op) {
                "==" -> ld == rd; "!=" -> ld != rd; ">" -> ld > rd; "<" -> ld < rd; ">=" -> ld >= rd; "<=" -> ld <= rd
                else -> false
            }
        } else {
            val ls = formatVal(l)
            val rs = formatVal(r)
            when (op) {
                "==" -> ls == rs; "!=" -> ls != rs; else -> false
            }
        }
    }

    private fun formatVal(v: Any?): String = when (v) {
        is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        is List<*> -> "[${v.joinToString(", ") { formatVal(it) }}]"
        else -> v?.toString().orEmpty()
    }
}
