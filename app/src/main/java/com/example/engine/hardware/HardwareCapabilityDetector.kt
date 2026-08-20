package com.example.engine.hardware

import android.os.Build
import com.example.model.HardwareAccelerator
import com.example.model.LocalAiModel

data class SystemSpecs(
  val availableCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
  val totalMemoryMb: Long = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).coerceAtLeast(1024),
  val isOfflineModeActive: Boolean = true,
  val hasGpuVulkan: Boolean = true,
  val hasNpu: Boolean = false,
  val storageUsedFormatted: String = "2.4 GB libres"
)

object HardwareCapabilityDetector {

  fun detectSystemSpecs(): SystemSpecs {
    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
    val maxMemMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).coerceAtLeast(1024)

    val hardwareStr = Build.HARDWARE.lowercase()
    val boardStr = Build.BOARD.lowercase()
    val hasNpuHeuristic = hardwareStr.contains("qcom") || hardwareStr.contains("kirin") ||
      hardwareStr.contains("tensor") || boardStr.contains("tensor")

    return SystemSpecs(
      availableCores = cores,
      totalMemoryMb = maxMemMb,
      isOfflineModeActive = true,
      hasGpuVulkan = true,
      hasNpu = hasNpuHeuristic,
      storageUsedFormatted = "Espacio optimizado para mmap"
    )
  }

  fun resolveActiveHardwareInfo(
    accelerator: HardwareAccelerator,
    deviceHasNpu: Boolean
  ): String {
    return when (accelerator) {
      HardwareAccelerator.NPU -> if (deviceHasNpu) "NPU (NNAPI / Hexagon)" else "GPU (Vulkan - Fallback NPU)"
      HardwareAccelerator.GPU -> "GPU (Vulkan / Adreno-Mali)"
      HardwareAccelerator.CPU -> "CPU (ARM NEON multihilo)"
      HardwareAccelerator.AUTO -> if (deviceHasNpu) "NPU (Aceleración Automática)" else "GPU (Vulkan Acelerado)"
    }
  }

  fun estimateModelRamUsage(model: LocalAiModel): String {
    val name = model.name.lowercase()
    val quant = model.quantization.lowercase()

    return when {
      name.contains("1.1b") || name.contains("1.5b") || name.contains("1.7b") -> {
        if (quant.contains("q4") || quant.contains("q2")) "650 MB - 1.1 GB RAM" else "1.5 GB - 2.2 GB RAM"
      }
      name.contains("2b") || name.contains("3b") -> {
        if (quant.contains("q4")) "1.6 GB - 2.4 GB RAM" else "3.2 GB - 4.5 GB RAM"
      }
      name.contains("7b") || name.contains("8b") -> {
        if (quant.contains("q4")) "3.8 GB - 4.8 GB RAM" else "7.5 GB - 9.0 GB RAM"
      }
      else -> "Carga dinámica mapeada por mmap"
    }
  }
}
