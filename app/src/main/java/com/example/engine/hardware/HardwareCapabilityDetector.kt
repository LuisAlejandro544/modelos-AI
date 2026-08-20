package com.example.engine.hardware

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.model.HardwareAccelerator
import com.example.model.LocalAiModel
import java.io.File
import java.util.Locale

data class SystemSpecs(
  val deviceManufacturer: String = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
  val deviceModel: String = Build.MODEL,
  val chipsetName: String = "SoC Móvil",
  val availableCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
  val primaryAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
  val totalRamGb: Double = 6.0,
  val freeRamGb: Double = 3.2,
  val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
  val freeStorageGb: Double = 32.0,
  val isLowRamDevice: Boolean = false,
  val isOfflineModeActive: Boolean = true,
  val hasGpuVulkan: Boolean = true,
  val hasNpu: Boolean = false
) {
  val fullDeviceName: String
    get() {
      val brand = deviceManufacturer.trim()
      val model = deviceModel.trim()
      return if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model"
    }

  val ramSummaryFormatted: String
    get() = String.format(Locale.US, "%.1f GB libres / %.1f GB", freeRamGb, totalRamGb)

  val storageSummaryFormatted: String
    get() = String.format(Locale.US, "%.1f GB disponibles", freeStorageGb)
}

object HardwareCapabilityDetector {

  fun detectSystemSpecs(context: Context? = null): SystemSpecs {
    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

    // Real Memory Info via ActivityManager
    var totalRamGb = (Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0)).coerceAtLeast(2.0)
    var freeRamGb = totalRamGb * 0.5
    var isLowRam = false

    if (context != null) {
      try {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (actManager != null) {
          val memInfo = ActivityManager.MemoryInfo()
          actManager.getMemoryInfo(memInfo)
          totalRamGb = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
          freeRamGb = memInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
          isLowRam = memInfo.lowMemory
        }
      } catch (_: Throwable) {}
    }

    // Real Available Storage
    var freeStorageGb = 16.0
    try {
      val dataDir = Environment.getDataDirectory()
      val stat = StatFs(dataDir.path)
      freeStorageGb = stat.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    } catch (_: Throwable) {
      try {
        val filesDir = context?.filesDir ?: File("/data")
        val stat = StatFs(filesDir.path)
        freeStorageGb = stat.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
      } catch (_: Throwable) {}
    }

    // Chipset / Board detection
    val hardwareStr = Build.HARDWARE.lowercase(Locale.ROOT)
    val boardStr = Build.BOARD.lowercase(Locale.ROOT)
    
    val detectedChipset = when {
      Build.VERSION.SDK_INT >= 31 && Build.SOC_MODEL.isNotBlank() && Build.SOC_MODEL != "unknown" -> {
        val mfr = if (Build.SOC_MANUFACTURER.isNotBlank() && Build.SOC_MANUFACTURER != "unknown") {
          Build.SOC_MANUFACTURER.replaceFirstChar { it.titlecase(Locale.ROOT) } + " "
        } else ""
        "$mfr${Build.SOC_MODEL}"
      }
      hardwareStr.contains("qcom") || boardStr.contains("msm") || boardStr.contains("sm") || boardStr.contains("sdm") -> "Qualcomm Snapdragon"
      hardwareStr.contains("mt") || boardStr.contains("mt") || boardStr.contains("dimensity") || hardwareStr.contains("helio") -> "MediaTek Dimensity / Helio"
      hardwareStr.contains("exynos") || boardStr.contains("universal") -> "Samsung Exynos"
      hardwareStr.contains("tensor") || boardStr.contains("tensor") || boardStr.contains("gs") -> "Google Tensor"
      hardwareStr.contains("kirin") || boardStr.contains("kirin") -> "HiSilicon Kirin"
      Build.HARDWARE.isNotBlank() && Build.HARDWARE != "unknown" -> Build.HARDWARE
      else -> "ARM64 SoC"
    }

    val hasNpuHeuristic = hardwareStr.contains("qcom") || hardwareStr.contains("kirin") ||
      hardwareStr.contains("tensor") || boardStr.contains("tensor") ||
      detectedChipset.contains("Snapdragon", ignoreCase = true) ||
      detectedChipset.contains("Tensor", ignoreCase = true) ||
      detectedChipset.contains("Dimensity", ignoreCase = true)

    return SystemSpecs(
      deviceManufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
      deviceModel = Build.MODEL,
      chipsetName = detectedChipset,
      availableCores = cores,
      primaryAbi = abi,
      totalRamGb = totalRamGb,
      freeRamGb = freeRamGb,
      androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
      freeStorageGb = freeStorageGb,
      isLowRamDevice = isLowRam,
      isOfflineModeActive = true,
      hasGpuVulkan = true,
      hasNpu = hasNpuHeuristic
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
    val name = model.name.lowercase(Locale.ROOT)
    val quant = model.quantization.lowercase(Locale.ROOT)

    return when {
      name.contains("1.1b") || name.contains("1.5b") || name.contains("1.7b") || name.contains("0.5b") || name.contains("360m") -> {
        if (quant.contains("q4") || quant.contains("q2")) "450 MB - 900 MB RAM" else "1.1 GB - 1.8 GB RAM"
      }
      name.contains("2b") || name.contains("3b") -> {
        if (quant.contains("q4")) "1.6 GB - 2.4 GB RAM" else "3.2 GB - 4.5 GB RAM"
      }
      name.contains("7b") || name.contains("8b") -> {
        if (quant.contains("q4")) "3.8 GB - 4.8 GB RAM" else "7.5 GB - 9.0 GB RAM"
      }
      else -> "Mapeado eficiente por mmap"
    }
  }
}
