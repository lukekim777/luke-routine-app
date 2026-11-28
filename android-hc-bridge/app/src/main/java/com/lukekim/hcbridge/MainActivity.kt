package com.lukekim.hcbridge

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Health Connect 에서 오늘 걸음수·최근 수면·최근 혈압을 읽어
 * 하루+ 웹 앱(lukekim777.github.io/luke-routine-app)으로 넘겨주는 미니 앱입니다.
 *
 * - 서버 없음: 읽은 값은 화면에 보여주고 URL 파라미터로 넘길 뿐, 어디로도 저장·전송하지 않습니다.
 * - 값이 없는 항목은 URL 에서 아예 빠집니다.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
        private const val HEALTH_CONNECT_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
        private const val SLEEP_LOOKBACK_HOURS = 24L
        private const val BP_LOOKBACK_DAYS = 365L
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)
    }

    /** 필요한 Health Connect 읽기 권한 3종 */
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class)
    )

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            loadHealthData()
        } else {
            statusText.text = getString(R.string.status_permission_needed)
            Toast.makeText(this, R.string.toast_permission_needed, Toast.LENGTH_LONG).show()
            render()
        }
    }

    private var client: HealthConnectClient? = null
    private var loading = false

    private var steps: Long? = null
    private var sleepHours: Double? = null
    private var sleepEndedAt: Instant? = null
    private var systolic: Int? = null
    private var diastolic: Int? = null
    private var bloodPressureAt: Instant? = null
    private var loadedAt: Instant? = null

    private lateinit var statusText: TextView
    private lateinit var stepsValue: TextView
    private lateinit var stepsCaption: TextView
    private lateinit var sleepValue: TextView
    private lateinit var sleepCaption: TextView
    private lateinit var bloodPressureValue: TextView
    private lateinit var bloodPressureCaption: TextView
    private lateinit var permissionButton: Button
    private lateinit var refreshButton: Button
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        stepsValue = findViewById(R.id.stepsValue)
        stepsCaption = findViewById(R.id.stepsCaption)
        sleepValue = findViewById(R.id.sleepValue)
        sleepCaption = findViewById(R.id.sleepCaption)
        bloodPressureValue = findViewById(R.id.bloodPressureValue)
        bloodPressureCaption = findViewById(R.id.bloodPressureCaption)
        permissionButton = findViewById(R.id.permissionButton)
        refreshButton = findViewById(R.id.refreshButton)
        sendButton = findViewById(R.id.sendButton)

        permissionButton.setOnClickListener { requestPermissions() }
        refreshButton.setOnClickListener { loadHealthData(force = true) }
        sendButton.setOnClickListener { sendToWebApp() }

        checkAvailability()
    }

    override fun onResume() {
        super.onResume()
        // 권한 화면이나 브라우저에서 돌아왔을 때 값이 바뀌어 있을 수 있어 다시 읽습니다.
        if (client != null) loadHealthData()
    }

    // ---------------------------------------------------------------- 가용성

    private fun checkAvailability() {
        when (HealthConnectClient.getSdkStatus(this)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                client = HealthConnectClient.getOrCreate(this)
                statusText.text = getString(R.string.status_available)
                setButtonsEnabled(true)
                loadHealthData()
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                client = null
                statusText.text = getString(R.string.status_update_required)
                setButtonsEnabled(false)
                showInstallDialog(getString(R.string.dialog_update_message))
            }

            else -> {
                client = null
                statusText.text = getString(R.string.status_unavailable)
                setButtonsEnabled(false)
                showInstallDialog(getString(R.string.dialog_install_message))
            }
        }
    }

    private fun showInstallDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_install_title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_open_store) { dialog, _ ->
                dialog.dismiss()
                openUrl(HEALTH_CONNECT_STORE_URL)
            }
            .setNegativeButton(R.string.dialog_close) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        permissionButton.isEnabled = enabled
        refreshButton.isEnabled = enabled
        sendButton.isEnabled = enabled
        permissionButton.alpha = if (enabled) 1f else 0.4f
        refreshButton.alpha = if (enabled) 1f else 0.4f
        sendButton.alpha = if (enabled) 1f else 0.4f
    }

    // ---------------------------------------------------------------- 권한

    private fun requestPermissions() {
        val healthConnectClient = client ?: return
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            val missing = permissions - granted
            if (missing.isEmpty()) {
                statusText.text = getString(R.string.status_permissions_granted)
                loadHealthData(force = true)
            } else {
                permissionLauncher.launch(missing)
            }
        }
    }

    // ---------------------------------------------------------------- 읽기

    private fun loadHealthData(force: Boolean = false) {
        val healthConnectClient = client ?: return
        if (loading && !force) return
        loading = true

        lifecycleScope.launch {
            try {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if (!granted.containsAll(permissions)) {
                    steps = null
                    sleepHours = null
                    sleepEndedAt = null
                    systolic = null
                    diastolic = null
                    bloodPressureAt = null
                    loadedAt = null
                    statusText.text = getString(R.string.status_permission_needed)
                    permissionButton.visibility = View.VISIBLE
                    render()
                    return@launch
                }

                permissionButton.visibility = View.GONE

                val zone = ZoneId.systemDefault()
                val now = Instant.now()
                val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()

                steps = readTodaySteps(healthConnectClient, startOfToday, now)

                val sleep = readLatestSleep(healthConnectClient, now)
                sleepHours = sleep?.hours
                sleepEndedAt = sleep?.endedAt

                val bloodPressure = readLatestBloodPressure(healthConnectClient, now)
                systolic = bloodPressure?.systolic
                diastolic = bloodPressure?.diastolic
                bloodPressureAt = bloodPressure?.measuredAt

                loadedAt = now
                if (steps == null && sleepHours == null && systolic == null) {
                    statusText.text = getString(R.string.status_no_data)
                } else {
                    statusText.text = getString(R.string.status_loaded)
                }
            } catch (e: SecurityException) {
                statusText.text = getString(R.string.status_permission_needed)
            } catch (e: Exception) {
                statusText.text = getString(
                    R.string.status_error,
                    e.message ?: e.javaClass.simpleName
                )
            } finally {
                loading = false
                render()
            }
        }
    }

    /** 오늘 0시부터 지금까지의 걸음수 합계 */
    private suspend fun readTodaySteps(
        healthConnectClient: HealthConnectClient,
        start: Instant,
        end: Instant
    ): Long? {
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return response[StepsRecord.COUNT_TOTAL]
    }

    private data class SleepSummary(val hours: Double, val endedAt: Instant)

    /** 최근 24시간 안에 끝난 수면 세션 중 가장 최근 것 (시간 단위, 소수 1자리) */
    private suspend fun readLatestSleep(
        healthConnectClient: HealthConnectClient,
        now: Instant
    ): SleepSummary? {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                now.minus(Duration.ofHours(SLEEP_LOOKBACK_HOURS)),
                now
            ),
            ascendingOrder = false,
            pageSize = 25
        )
        val records = healthConnectClient.readRecords(request).records
        val latest = records.maxByOrNull { it.endTime } ?: return null
        val minutes = Duration.between(latest.startTime, latest.endTime).toMinutes()
        if (minutes <= 0L) return null
        val hours = HealthPayload.roundMinutesToHours(minutes) // 분 → 시간, 소수 1자리
        return SleepSummary(hours = hours, endedAt = latest.endTime)
    }

    private data class BloodPressureSummary(
        val systolic: Int,
        val diastolic: Int,
        val measuredAt: Instant
    )

    /** 최근 1년 안에서 가장 최근 혈압 기록 (mmHg) */
    private suspend fun readLatestBloodPressure(
        healthConnectClient: HealthConnectClient,
        now: Instant
    ): BloodPressureSummary? {
        val request = ReadRecordsRequest(
            recordType = BloodPressureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                now.minus(Duration.ofDays(BP_LOOKBACK_DAYS)),
                now
            ),
            ascendingOrder = false,
            pageSize = 1
        )
        val record = healthConnectClient.readRecords(request).records.firstOrNull() ?: return null
        val sbp = record.systolic.inMillimetersOfMercury.roundToInt()
        val dbp = record.diastolic.inMillimetersOfMercury.roundToInt()
        if (sbp <= 0 || dbp <= 0) return null
        return BloodPressureSummary(systolic = sbp, diastolic = dbp, measuredAt = record.time)
    }

    // ---------------------------------------------------------------- 화면

    private fun render() {
        stepsValue.text = steps?.let { getString(R.string.value_steps, it) }
            ?: getString(R.string.value_empty)
        stepsCaption.text = getString(R.string.caption_steps)

        sleepValue.text = sleepHours?.let { getString(R.string.value_sleep, it) }
            ?: getString(R.string.value_empty)
        sleepCaption.text = sleepEndedAt?.let {
            getString(R.string.caption_sleep, formatTime(it))
        } ?: getString(R.string.caption_sleep_empty)

        bloodPressureValue.text = if (systolic != null && diastolic != null) {
            getString(R.string.value_blood_pressure, systolic, diastolic)
        } else {
            getString(R.string.value_empty)
        }
        bloodPressureCaption.text = bloodPressureAt?.let {
            getString(R.string.caption_blood_pressure, formatTime(it))
        } ?: getString(R.string.caption_blood_pressure_empty)

        val hasAnyValue = steps != null || sleepHours != null || systolic != null
        sendButton.isEnabled = hasAnyValue && client != null
        sendButton.alpha = if (sendButton.isEnabled) 1f else 0.4f
    }

    private fun formatTime(instant: Instant): String =
        DATE_FORMAT.format(instant.atZone(ZoneId.systemDefault()))

    // ---------------------------------------------------------------- 전송

    private fun sendToWebApp() {
        if (!HealthPayload.hasAnyValue(steps, sleepHours, systolic, diastolic)) {
            Toast.makeText(this, R.string.toast_no_data, Toast.LENGTH_LONG).show()
            return
        }

        // 값이 있는 항목만 하루+ 주소의 파라미터로 넘깁니다.
        openUrl(
            HealthPayload.buildUrl(
                steps = steps,
                sleepHours = sleepHours,
                systolic = systolic,
                diastolic = diastolic,
                date = LocalDate.now(ZoneId.systemDefault())
            )
        )
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.toast_no_browser, url), Toast.LENGTH_LONG)
                .show()
        }
    }
}
