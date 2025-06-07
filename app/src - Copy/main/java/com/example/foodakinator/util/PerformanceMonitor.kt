package com.example.foodakinator.util

import android.util.Log

class PerformanceMonitor {
    val TAG = "PerformanceMonitor"
    val metrics = mutableMapOf<String, Long>()
    val operationCounts = mutableMapOf<String, Int>()

    // FIXED: Changed from private inline to public inline
    inline fun <T> measureTime(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()

        val duration = endTime - startTime
        metrics[operation] = duration
        operationCounts[operation] = (operationCounts[operation] ?: 0) + 1

        // Log slow operations (threshold: 100ms)
        if (duration > 100) {
            Log.w(TAG, "SLOW: $operation took ${duration}ms")
        } else {
            Log.d(TAG, "$operation took ${duration}ms")
        }

        return result
    }

    fun getAllMetrics(): Map<String, Long> = metrics.toMap()

    fun getAverageTime(operation: String): Float {
        val totalTime = metrics[operation] ?: 0L
        val count = operationCounts[operation] ?: 1
        return totalTime.toFloat() / count
    }

    fun logPerformanceReport() {
        Log.d(TAG, "=== PERFORMANCE REPORT ===")
        metrics.forEach { (operation, time) ->
            val count = operationCounts[operation] ?: 1
            val avg = time.toFloat() / count
            Log.d(TAG, "$operation: ${time}ms (avg: ${"%.1f".format(avg)}ms, count: $count)")
        }
        Log.d(TAG, "========================")
    }

    fun clearMetrics() {
        metrics.clear()
        operationCounts.clear()
    }

    fun getSlowOperations(thresholdMs: Long = 50): List<Pair<String, Long>> {
        return metrics.filter { it.value > thresholdMs }.toList()
    }
}