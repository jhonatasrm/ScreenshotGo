package org.mozilla.scryer.telemetry

import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.Nullable
import org.mozilla.scryer.*
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.annotation.TelemetryDoc
import org.mozilla.telemetry.annotation.TelemetryExtra
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.SettingsMeasurement
import org.mozilla.telemetry.measurement.TelemetryMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage


class TelemetryWrapper {

    private object Category {
        const val START_SESSION = "Start session"
        const val STOP_SESSION = "Stop session"
        const val VISIT_WELCOME_PAGE = "Visit welcome page"
        const val GRANT_STORAGE_PERMISSION = "Grant storage permission"
        const val PROMPT_OVERLAY_PERMISSION = "Prompt overlay permission"
        const val GRANT_OVERLAY_PERMISSION = "Grant overlay permission"
        const val NOT_GRANT_OVERLAY_PERMISSION = "Not grant overlay permission"
        const val VISIT_PERMISSION_ERROR_PAGE = "Visit permission error page"
        const val VISIT_HOME_PAGE = "Visit home page"
        const val START_SEARCH = "Start search"
        const val CLICK_ON_QUICK_ACCESS = "Click on quick access"
        const val CLICK_MORE_ON_QUICK_ACCESS = "Click more on quick access"
        const val CLICK_ON_COLLECTION = "Click on collection"
        const val CREATE_COLLECTION_FROM_HOME = "Create collection from home"
        const val ENTER_SETTINGS = "Enter settings"
        const val VISIT_COLLECTION_PAGE = "Visit collection page"
        const val CLICK_ON_SORTING_BUTTON = "Click on sorting button"
        const val COLLECTION_ITEM = "Click on collection item"
        const val CREATE_COLLECTION_WHEN_SORTING = "Create collection when sorting"
        const val PROMPT_SORTING_PAGE = "Prompt sorting page"
        const val SORT_SCREENSHOT = "Sort screenshot"
        const val CANCEL_SORTING = "Cancel sorting"
        const val CAPTURE_VIA_FAB = "Capture via FAB"
        const val CAPTURE_VIA_NOTIFICATION = "Capture via notification"
        const val CAPTURE_VIA_EXTERNAL = "Capture via external"
        const val VIEW_SCREENSHOT = "View screenshot"
        const val EXTRACT_TEXT_FROM_SCREENSHOT = "Extract text from screenshot"
        const val VIEW_TEXT_IN_SCREENSHOT = "View text in screenshot"
        const val PROMPT_EXTRACTED_TEXT_MENU = "Prompt extracted text menu"
        const val SEARCH_FROM_EXTRACTED_TEXT = "Search from extracted text"
        const val COPY_EXTRACTED_TEXT = "Copy extracted text"
        const val SHARE_EXTRACTED_TEXT = "Share extracted text"
        const val CLICK_ON_TEXT_BLOCK = "Click on text block"
        const val CLICK_LINK_IN_EXTRACTED_TEXT = "Click link in extracted text"
        const val SELECT_ALL_EXTRACTED_TEXT = "Select all extracted text"
        const val CLICK_ON_OCR_BOTTOM_TIP = "Click on OCR bottom tip"
        const val CLICK_ON_OCR_ERROR_TIP = "Click on OCR error tip"
        const val VISIT_SEARCH_PAGE = "Visit search page"
        const val INTERESTED_IN_SEARCH = "Interested in search"
        const val NOT_INTERESTED_IN_SEARCH = "Not interested in search"
        const val CLICK_SEARCH_RESULT = "Click search result"
        const val CLOSE_FAB = "Close FAB"
        const val STOP_CAPTURE_SERVICE = "Stop capture service"
        const val PROMPT_FEEDBACK_DIALOG = "Prompt feedback dialog"
        const val CLICK_FEEDBACK = "Click feedback"
        const val PROMPT_SHARE_DIALOG = "Prompt share dialog"
        const val BACKGROUND_SERVICE_ACTIVE = "Background service active"
        const val SHARE_APP = "Share app"
        const val LONG_PRESS_ON_SCREENSHOT = "Long press on screenshot"
        const val MOVE_SCREENSHOT = "Move screenshot"
        const val DELETE_SCREENSHOT = "Delete screenshot"
        const val SHARE_SCREENSHOT = "Share screenshot"
    }

    private object Method {
        const val V1 = "1"
    }

    private object Object {
        const val GO = "go"
    }

    object Value {
        const val APP = "app"
        const val SUCCESS = "success"
        const val WEIRD_SIZE = "weird_size"
        const val FAIL = "fail"
        const val NOTIFICATION = "notification"
        const val SETTINGS = "settings"
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
    }

    private object Extra {
        const val ON = "on"
        const val MODE = "mode"
        const val TIMES = "times"
        const val MESSAGE = "message"
        const val TRIGGER = "trigger"
        const val FROM = "from"
        const val LINKS_FOUND = "links_found"
        const val TEXT_BLOCKS = "text_blocks"
        const val TOTAL_LENGTH = "total_length"
        const val INDEX_PROGRESS = "index_progress"
        const val COLLECTION = "collection"
        const val RESULT_SIZE = "result_size"
        const val RESULT_POSITION = "result_position"
        const val KEYWORD_LENGTH = "keyword_length"
        const val DURATION = "duration"
        const val SELECTED_ITEMS = "selected_items"
    }

    object ExtraValue {
        const val SINGLE = "single"
        const val MULTIPLE = "multiple"

        const val SEARCH = "search"
        const val COLLECTION = "collection"

        const val FROM_PROMPT = "prompt"
        const val FROM_SETTINGS = "settings"

        const val TRIGGER_CAPTURE = "capture"
        const val TRIGGER_SORT = "sort"
        const val TRIGGER_OCR = "ocr"
    }

    companion object {

        private const val TELEMETRY_APP_NAME = "Scryer"

        fun init(context: Context) {
            try {
                val resources = context.resources
                val telemetryEnabled = isTelemetryEnabled(context)

                val configuration = TelemetryConfiguration(context)
                        .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                        .setAppName(TELEMETRY_APP_NAME)
                        .setUpdateChannel(BuildConfig.BUILD_TYPE)
                        .setPreferencesImportantForTelemetry(
                                resources.getString(R.string.pref_key_enable_capture_service),
                                resources.getString(R.string.pref_key_enable_floating_screenshot_button),
                                resources.getString(R.string.pref_key_enable_add_to_collection))
                        .setSettingsProvider(CustomSettingsProvider())
                        .setCollectionEnabled(telemetryEnabled)
                        .setUploadEnabled(telemetryEnabled)

                val serializer = JSONPingSerializer()
                val storage = FileTelemetryStorage(configuration, serializer)
                val client = HttpURLConnectionTelemetryClient()
                val scheduler = JobSchedulerTelemetryScheduler()

                TelemetryHolder.set(Telemetry(configuration, storage, client, scheduler)
                        .addPingBuilder(TelemetryCorePingBuilder(configuration))
                        .addPingBuilder(TelemetryEventPingBuilder(configuration)))
            } finally {
            }
        }

        private fun isTelemetryEnabled(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isEnabledByDefault = BuildConfig.BUILD_TYPE == "release"
            return preferences.getBoolean(context.resources.getString(R.string.pref_key_enable_send_usage_data), isEnabledByDefault)
        }

        @TelemetryDoc(
                name = Category.START_SESSION,
                category = Category.START_SESSION,
                method = Method.V1,
                `object` = Object.GO,
                value = Value.APP,
                extras = [])
        fun startSession() {
            TelemetryHolder.get().recordSessionStart()
            EventBuilder(Category.START_SESSION, Method.V1, Object.GO, Value.APP).queue()
        }

        @TelemetryDoc(
                name = Category.STOP_SESSION,
                category = Category.STOP_SESSION,
                method = Method.V1,
                `object` = Object.GO,
                value = Value.APP,
                extras = [])
        fun stopSession() {
            TelemetryHolder.get().recordSessionEnd()
            EventBuilder(Category.STOP_SESSION, Method.V1, Object.GO, Value.APP).queue()
        }

        fun stopMainActivity() {
            TelemetryHolder.get()
                    .queuePing(TelemetryCorePingBuilder.TYPE)
                    .queuePing(TelemetryEventPingBuilder.TYPE)
                    .scheduleUpload()
        }

        @TelemetryDoc(
                name = Category.VISIT_WELCOME_PAGE,
                category = Category.VISIT_WELCOME_PAGE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun visitWelcomePage() {
            EventBuilder(Category.VISIT_WELCOME_PAGE, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.GRANT_STORAGE_PERMISSION,
                category = Category.GRANT_STORAGE_PERMISSION,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.TIMES, value = "times")])
        fun grantStoragePermission(times: Int) {
            EventBuilder(Category.GRANT_STORAGE_PERMISSION, Method.V1, Object.GO).extra(Extra.TIMES, times.toString()).queue()
        }

        @TelemetryDoc(
                name = Category.PROMPT_OVERLAY_PERMISSION,
                category = Category.PROMPT_OVERLAY_PERMISSION,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun promptOverlayPermission() {
            EventBuilder(Category.PROMPT_OVERLAY_PERMISSION, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.GRANT_OVERLAY_PERMISSION,
                category = Category.GRANT_OVERLAY_PERMISSION,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun grantOverlayPermission() {
            EventBuilder(Category.GRANT_OVERLAY_PERMISSION, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.NOT_GRANT_OVERLAY_PERMISSION,
                category = Category.NOT_GRANT_OVERLAY_PERMISSION,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun notGrantOverlayPermission() {
            EventBuilder(Category.NOT_GRANT_OVERLAY_PERMISSION, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.VISIT_PERMISSION_ERROR_PAGE,
                category = Category.VISIT_PERMISSION_ERROR_PAGE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun visitPermissionErrorPage() {
            EventBuilder(Category.VISIT_PERMISSION_ERROR_PAGE, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.VISIT_HOME_PAGE,
                category = Category.VISIT_HOME_PAGE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun visitHomePage() {
            EventBuilder(Category.VISIT_HOME_PAGE, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.START_SEARCH,
                category = Category.START_SEARCH,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.INDEX_PROGRESS, value = "(0-100)")])
        fun startSearch(indexProgress: Int) {
            EventBuilder(Category.START_SEARCH, Method.V1, Object.GO).extra(Extra.INDEX_PROGRESS, indexProgress.toString()).queue()
            AdjustHelper.trackEvent(ADJUST_EVENT_START_SEARCH)
        }

        @TelemetryDoc(
                name = Category.CLICK_ON_QUICK_ACCESS,
                category = Category.CLICK_ON_QUICK_ACCESS,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.ON, value = "index")])
        fun clickOnQuickAccess(index: Int) {
            EventBuilder(Category.CLICK_ON_QUICK_ACCESS, Method.V1, Object.GO).extra(Extra.ON, index.toString()).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_MORE_ON_QUICK_ACCESS,
                category = Category.CLICK_MORE_ON_QUICK_ACCESS,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun clickMoreOnQuickAccess() {
            EventBuilder(Category.CLICK_MORE_ON_QUICK_ACCESS, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_ON_COLLECTION,
                category = Category.CLICK_ON_COLLECTION,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun clickOnCollection() {
            EventBuilder(Category.CLICK_ON_COLLECTION, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CREATE_COLLECTION_FROM_HOME,
                category = Category.CREATE_COLLECTION_FROM_HOME,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun createCollectionFromHome() {
            EventBuilder(Category.CREATE_COLLECTION_FROM_HOME, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.ENTER_SETTINGS,
                category = Category.ENTER_SETTINGS,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun enterSettings() {
            EventBuilder(Category.ENTER_SETTINGS, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.VISIT_COLLECTION_PAGE,
                category = Category.VISIT_COLLECTION_PAGE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.ON, value = "collection_name")])
        fun visitCollectionPage(name: String) {
            EventBuilder(Category.VISIT_COLLECTION_PAGE, Method.V1, Object.GO).extra(Extra.ON, name).queue()
        }

        @TelemetryDoc(
                name = Category.COLLECTION_ITEM,
                category = Category.COLLECTION_ITEM,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.ON, value = "collection_name")])
        fun collectionItem(name: String) {
            EventBuilder(Category.COLLECTION_ITEM, Method.V1, Object.GO).extra(Extra.ON, name).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_ON_SORTING_BUTTON,
                category = Category.CLICK_ON_SORTING_BUTTON,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun clickOnSortingButton() {
            EventBuilder(Category.CLICK_ON_SORTING_BUTTON, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CREATE_COLLECTION_WHEN_SORTING,
                category = Category.CREATE_COLLECTION_WHEN_SORTING,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun createCollectionWhenSorting() {
            EventBuilder(Category.CREATE_COLLECTION_WHEN_SORTING, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.SORT_SCREENSHOT,
                category = Category.SORT_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.ON, value = "collection_name"),
                    TelemetryExtra(name = Extra.MODE, value = ExtraValue.SINGLE + "," + ExtraValue.MULTIPLE)])
        fun sortScreenshot(name: String, mode: String) {
            EventBuilder(Category.SORT_SCREENSHOT, Method.V1, Object.GO)
                    .extra(Extra.ON, name)
                    .extra(Extra.MODE, mode)
                    .queue()
            AdjustHelper.trackEvent(ADJUST_EVENT_SORT_SCREENSHOT)
        }

        @TelemetryDoc(
                name = Category.CANCEL_SORTING,
                category = Category.CANCEL_SORTING,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.MODE, value = ExtraValue.SINGLE + "," + ExtraValue.MULTIPLE)])
        fun cancelSorting(mode: String) {
            EventBuilder(Category.CANCEL_SORTING, Method.V1, Object.GO)
                    .extra(Extra.MODE, mode).queue()
        }


        @TelemetryDoc(
                name = Category.PROMPT_SORTING_PAGE,
                category = Category.PROMPT_SORTING_PAGE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.MODE, value = ExtraValue.SINGLE + "," + ExtraValue.MULTIPLE)])
        fun promptSortingPage(mode: String) {
            EventBuilder(Category.PROMPT_SORTING_PAGE, Method.V1, Object.GO).extra(Extra.MODE, mode).queue()
        }

        @TelemetryDoc(
                name = Category.CAPTURE_VIA_FAB,
                category = Category.CAPTURE_VIA_FAB,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun captureViaFab() {
            EventBuilder(Category.CAPTURE_VIA_FAB, Method.V1, Object.GO).queue()
            AdjustHelper.trackEvent(ADJUST_EVENT_CAPTURE_VIA_FAB)
        }

        @TelemetryDoc(
                name = Category.CAPTURE_VIA_NOTIFICATION,
                category = Category.CAPTURE_VIA_NOTIFICATION,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun captureViaNotification() {
            EventBuilder(Category.CAPTURE_VIA_NOTIFICATION, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CAPTURE_VIA_EXTERNAL,
                category = Category.CAPTURE_VIA_EXTERNAL,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun captureViaExternal() {
            EventBuilder(Category.CAPTURE_VIA_EXTERNAL, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.VIEW_SCREENSHOT,
                category = Category.VIEW_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun viewScreenshot() {
            EventBuilder(Category.VIEW_SCREENSHOT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.EXTRACT_TEXT_FROM_SCREENSHOT,
                category = Category.EXTRACT_TEXT_FROM_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun extractTextFromScreenshot() {
            EventBuilder(Category.EXTRACT_TEXT_FROM_SCREENSHOT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.VIEW_TEXT_IN_SCREENSHOT,
                category = Category.VIEW_TEXT_IN_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "success,weird_size,fail",
                extras = [TelemetryExtra(name = Extra.MESSAGE, value = "empty or failing reason"),
                    TelemetryExtra(name = Extra.LINKS_FOUND, value = "[0-9]+"),
                    TelemetryExtra(name = Extra.TEXT_BLOCKS, value = "[0-9]+"),
                    TelemetryExtra(name = Extra.TOTAL_LENGTH, value = "[0-9]+")])
        fun viewTextInScreenshot(textRecognitionResult: TextRecognitionResult) {
            EventBuilder(Category.VIEW_TEXT_IN_SCREENSHOT, Method.V1, Object.GO, textRecognitionResult.value)
                    .extra(Extra.MESSAGE, textRecognitionResult.message)
                    .extra(Extra.LINKS_FOUND, textRecognitionResult.linksFound.toString())
                    .extra(Extra.TEXT_BLOCKS, textRecognitionResult.textBlocks.toString())
                    .extra(Extra.TOTAL_LENGTH, textRecognitionResult.totalLength.toString())
                    .queue()
            AdjustHelper.trackEvent(ADJUST_EVENT_VIEW_TEXT_IN_SCREENSHOT)
        }

        @TelemetryDoc(
                name = Category.PROMPT_EXTRACTED_TEXT_MENU,
                category = Category.PROMPT_EXTRACTED_TEXT_MENU,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun promptExtractedTextMenu() {
            EventBuilder(Category.PROMPT_EXTRACTED_TEXT_MENU, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.SEARCH_FROM_EXTRACTED_TEXT,
                category = Category.SEARCH_FROM_EXTRACTED_TEXT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun searchFromExtractedText() {
            EventBuilder(Category.SEARCH_FROM_EXTRACTED_TEXT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.COPY_EXTRACTED_TEXT,
                category = Category.COPY_EXTRACTED_TEXT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun copyExtractedText() {
            EventBuilder(Category.COPY_EXTRACTED_TEXT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.SHARE_EXTRACTED_TEXT,
                category = Category.SHARE_EXTRACTED_TEXT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun shareExtractedText() {
            EventBuilder(Category.SHARE_EXTRACTED_TEXT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_ON_TEXT_BLOCK,
                category = Category.CLICK_ON_TEXT_BLOCK,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun clickOnTextBlock() {
            EventBuilder(Category.CLICK_ON_TEXT_BLOCK, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_LINK_IN_EXTRACTED_TEXT,
                category = Category.CLICK_LINK_IN_EXTRACTED_TEXT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun clickLinkInExtractedText() {
            EventBuilder(Category.CLICK_LINK_IN_EXTRACTED_TEXT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.SELECT_ALL_EXTRACTED_TEXT,
                category = Category.SELECT_ALL_EXTRACTED_TEXT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun selectAllExtractedText() {
            EventBuilder(Category.SELECT_ALL_EXTRACTED_TEXT, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_ON_OCR_BOTTOM_TIP,
                category = Category.CLICK_ON_OCR_BOTTOM_TIP,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun clickOnOCRBottomTip() {
            EventBuilder(Category.CLICK_ON_OCR_BOTTOM_TIP, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_ON_OCR_ERROR_TIP,
                category = Category.CLICK_ON_OCR_ERROR_TIP,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.MESSAGE, value = "ui message")])
        fun clickOnOCRErrorTip(message: String) {
            EventBuilder(Category.CLICK_ON_OCR_ERROR_TIP, Method.V1, Object.GO).extra(Extra.MESSAGE, message).queue()
        }

        @TelemetryDoc(
                name = Category.VISIT_SEARCH_PAGE,
                category = Category.VISIT_SEARCH_PAGE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun visitSearchPage() {
            EventBuilder(Category.VISIT_SEARCH_PAGE, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.INTERESTED_IN_SEARCH,
                category = Category.INTERESTED_IN_SEARCH,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun interestedInSearch() {
            EventBuilder(Category.INTERESTED_IN_SEARCH, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.NOT_INTERESTED_IN_SEARCH,
                category = Category.NOT_INTERESTED_IN_SEARCH,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun notInterestedInSearch() {
            EventBuilder(Category.NOT_INTERESTED_IN_SEARCH, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_SEARCH_RESULT,
                category = Category.CLICK_SEARCH_RESULT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [TelemetryExtra(name = Extra.COLLECTION, value = "collection name"),
                    TelemetryExtra(name = Extra.RESULT_SIZE, value = "[0-9]+"),
                    TelemetryExtra(name = Extra.RESULT_POSITION, value = "[0-9]+"),
                    TelemetryExtra(name = Extra.KEYWORD_LENGTH, value = "[0-9]+"),
                    TelemetryExtra(name = Extra.DURATION, value = "[0-9]+")])
        fun clickSearchResult(collection: String, size: Int, position: Int, keywordLength: Int, duration: Long) {
            EventBuilder(Category.CLICK_SEARCH_RESULT, Method.V1, Object.GO)
                    .extra(Extra.COLLECTION, collection)
                    .extra(Extra.RESULT_SIZE, size.toString())
                    .extra(Extra.RESULT_POSITION, position.toString())
                    .extra(Extra.KEYWORD_LENGTH, keywordLength.toString())
                    .extra(Extra.DURATION, duration.toString())
                    .queue()
        }

        @TelemetryDoc(
                name = Category.CLOSE_FAB,
                category = Category.CLOSE_FAB,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun closeFAB() {
            EventBuilder(Category.CLOSE_FAB, Method.V1, Object.GO).queue()
        }

        @TelemetryDoc(
                name = Category.STOP_CAPTURE_SERVICE,
                category = Category.STOP_CAPTURE_SERVICE,
                method = Method.V1,
                `object` = Object.GO,
                value = "notification,settings",
                extras = [])
        fun stopCaptureService(value: String) {
            EventBuilder(Category.STOP_CAPTURE_SERVICE, Method.V1, Object.GO, value).queue()
        }

        @TelemetryDoc(
                name = Category.PROMPT_FEEDBACK_DIALOG,
                category = Category.PROMPT_FEEDBACK_DIALOG,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.FROM,
                            value = ExtraValue.FROM_PROMPT + "," + ExtraValue.FROM_SETTINGS
                    ),
                    TelemetryExtra(
                            name = Extra.TRIGGER,
                            value = ExtraValue.TRIGGER_CAPTURE
                                    + "," + ExtraValue.TRIGGER_SORT
                                    + "," + ExtraValue.TRIGGER_OCR
                    )])
        fun promptFeedbackDialog(from: String, trigger: String = "") {
            EventBuilder(Category.PROMPT_FEEDBACK_DIALOG, Method.V1, Object.GO)
                    .extra(Extra.FROM, from)
                    .extra(Extra.TRIGGER, trigger)
                    .queue()
        }

        @TelemetryDoc(
                name = Category.CLICK_FEEDBACK,
                category = Category.CLICK_FEEDBACK,
                method = Method.V1,
                `object` = Object.GO,
                value = "positive,negative",
                extras = [
                    TelemetryExtra(
                            name = Extra.FROM,
                            value = ExtraValue.FROM_PROMPT + "," + ExtraValue.FROM_SETTINGS
                    ),
                    TelemetryExtra(
                            name = Extra.TRIGGER,
                            value = ExtraValue.TRIGGER_CAPTURE
                                    + "," + ExtraValue.TRIGGER_SORT
                                    + "," + ExtraValue.TRIGGER_OCR
                    )])
        fun clickFeedback(value: String, from: String, trigger: String = "") {
            EventBuilder(Category.CLICK_FEEDBACK, Method.V1, Object.GO, value)
                    .extra(Extra.FROM, from)
                    .extra(Extra.TRIGGER, trigger)
                    .queue()
            if (value == Value.POSITIVE) {
                AdjustHelper.trackEvent(ADJUST_EVENT_FEEDBACK_POSITIVE)
            }
        }

        @TelemetryDoc(
                name = Category.PROMPT_SHARE_DIALOG,
                category = Category.PROMPT_SHARE_DIALOG,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.FROM,
                            value = ExtraValue.FROM_PROMPT + "," + ExtraValue.FROM_SETTINGS
                    ),
                    TelemetryExtra(
                            name = Extra.TRIGGER,
                            value = ExtraValue.TRIGGER_CAPTURE
                                    + "," + ExtraValue.TRIGGER_SORT
                                    + "," + ExtraValue.TRIGGER_OCR
                    )])
        fun promptShareDialog(from: String, trigger: String = "") {
            EventBuilder(Category.PROMPT_SHARE_DIALOG, Method.V1, Object.GO)
                    .extra(Extra.FROM, from)
                    .extra(Extra.TRIGGER, trigger)
                    .queue()
        }

        @TelemetryDoc(
                name = Category.SHARE_APP,
                category = Category.SHARE_APP,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.FROM,
                            value = ExtraValue.FROM_PROMPT + "," + ExtraValue.FROM_SETTINGS
                    ),
                    TelemetryExtra(
                            name = Extra.TRIGGER,
                            value = ExtraValue.TRIGGER_CAPTURE
                                    + "," + ExtraValue.TRIGGER_SORT
                                    + "," + ExtraValue.TRIGGER_OCR
                    )])
        fun clickShareApp(from: String, trigger: String = "") {
            EventBuilder(Category.SHARE_APP, Method.V1, Object.GO)
                    .extra(Extra.FROM, from)
                    .extra(Extra.TRIGGER, trigger)
                    .queue()
            shareApp()
        }

        fun shareApp() {
            AdjustHelper.trackEvent(ADJUST_EVENT_SHARE_APP)
        }

        @TelemetryDoc(
                name = Category.BACKGROUND_SERVICE_ACTIVE,
                category = Category.BACKGROUND_SERVICE_ACTIVE,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [])
        fun logActiveBackgroundService() {
            EventBuilder(Category.BACKGROUND_SERVICE_ACTIVE, Method.V1, Object.GO).queue()
            // force to upload event since it came from service instead of MainActivity
            TelemetryHolder.get().queuePing(TelemetryEventPingBuilder.TYPE).scheduleUpload()
        }

        @TelemetryDoc(
                name = Category.LONG_PRESS_ON_SCREENSHOT,
                category = Category.LONG_PRESS_ON_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.MODE,
                            value = ExtraValue.SEARCH + "," + ExtraValue.COLLECTION
                    )])
        fun longPressOnScreenshot(mode: String) {
            EventBuilder(Category.LONG_PRESS_ON_SCREENSHOT, Method.V1, Object.GO)
                    .extra(Extra.MODE, mode)
                    .queue()
        }

        @TelemetryDoc(
                name = Category.MOVE_SCREENSHOT,
                category = Category.MOVE_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.MODE,
                            value = ExtraValue.SINGLE + "," + ExtraValue.SEARCH + "," + ExtraValue.COLLECTION
                    ),
                    TelemetryExtra(name = Extra.SELECTED_ITEMS, value = "[0-9]+")])
        fun moveScreenshot(mode: String, selectedItemCount: Int) {
            EventBuilder(Category.MOVE_SCREENSHOT, Method.V1, Object.GO)
                    .extra(Extra.MODE, mode)
                    .extra(Extra.SELECTED_ITEMS, selectedItemCount.toString())
                    .queue()
        }

        @TelemetryDoc(
                name = Category.DELETE_SCREENSHOT,
                category = Category.DELETE_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.MODE,
                            value = ExtraValue.SINGLE + "," + ExtraValue.SEARCH + "," + ExtraValue.COLLECTION
                    ),
                    TelemetryExtra(name = Extra.SELECTED_ITEMS, value = "[0-9]+")])
        fun deleteScreenshot(mode: String, selectedItemCount: Int) {
            EventBuilder(Category.DELETE_SCREENSHOT, Method.V1, Object.GO)
                    .extra(Extra.MODE, mode)
                    .extra(Extra.SELECTED_ITEMS, selectedItemCount.toString())
                    .queue()
        }

        @TelemetryDoc(
                name = Category.SHARE_SCREENSHOT,
                category = Category.SHARE_SCREENSHOT,
                method = Method.V1,
                `object` = Object.GO,
                value = "",
                extras = [
                    TelemetryExtra(
                            name = Extra.MODE,
                            value = ExtraValue.SINGLE + "," + ExtraValue.SEARCH + "," + ExtraValue.COLLECTION
                    ),
                    TelemetryExtra(name = Extra.SELECTED_ITEMS, value = "[0-9]+")])
        fun shareScreenshot(mode: String, selectedItemCount: Int) {
            EventBuilder(Category.SHARE_SCREENSHOT, Method.V1, Object.GO)
                    .extra(Extra.MODE, mode)
                    .extra(Extra.SELECTED_ITEMS, selectedItemCount.toString())
                    .queue()
        }
    }

    internal class EventBuilder @JvmOverloads constructor(category: String, method: String, @Nullable `object`: String, value: String? = null) {
        var telemetryEvent: TelemetryEvent = TelemetryEvent.create(category, method, `object`, value)
        var firebaseEvent: FirebaseEvent = FirebaseEvent.create(category, method, `object`, value)


        fun extra(key: String, value: String): EventBuilder {
            telemetryEvent.extra(key, value)
            firebaseEvent.param(key, value)
            return this
        }

        fun queue() {
            val context = TelemetryHolder.get().configuration.context
            if (context != null) {
                telemetryEvent.queue()
                firebaseEvent.event(context)
            }
        }
    }

    private class CustomSettingsProvider : SettingsMeasurement.SharedPreferenceSettingsProvider() {

        private val custom = HashMap<String, Any>(1)

        override fun update(configuration: TelemetryConfiguration) {
            super.update(configuration)

            addCustomPing(configuration, ScreenshotCountMeasurement())
            addCustomPing(configuration, IndexedScreenshotWithTextCountMeasurement())
        }


        internal fun addCustomPing(configuration: TelemetryConfiguration, measurement: TelemetryMeasurement) {
            var preferenceKeys: MutableSet<String>? = configuration.preferencesImportantForTelemetry
            if (preferenceKeys == null) {
                configuration.setPreferencesImportantForTelemetry()
                preferenceKeys = configuration.preferencesImportantForTelemetry
            }
            preferenceKeys!!.add(measurement.fieldName)
            custom[measurement.fieldName] = measurement.flush()
        }

        override fun containsKey(key: String): Boolean {
            return super.containsKey(key) or custom.containsKey(key)
        }

        override fun getValue(key: String): Any {
            return custom[key] ?: super.getValue(key)
        }
    }

    private class ScreenshotCountMeasurement : TelemetryMeasurement(MEASUREMENT_SCREENSHOT_COUNT) {

        override fun flush(): Any {
            if ("main" == Thread.currentThread().name) {
                throw RuntimeException("Call from main thread exception")
            }

            return try {
                ScryerApplication.getScreenshotRepository().getScreenshotList().size
            } catch (e: Exception) {
                -1
            }
        }

        companion object {
            private const val MEASUREMENT_SCREENSHOT_COUNT = "screenshot_count"
        }
    }

    private class IndexedScreenshotWithTextCountMeasurement : TelemetryMeasurement(MEASUREMENT_INDEXED_SCREENSHOT_WITH_TEXT_COUNT) {

        override fun flush(): Any {
            if ("main" == Thread.currentThread().name) {
                throw RuntimeException("Call from main thread exception")
            }

            return try {
                val list = ScryerApplication.getScreenshotRepository().getScreenshotList()
                val indexedList = list.filter {
                    val contextText = ScryerApplication.getScreenshotRepository().getContentText(it)
                    contextText != null && contextText.length >= 10
                }
                return ((indexedList.size / list.size.toFloat()) * 100).toInt()
            } catch (e: Exception) {
                -1
            }
        }

        companion object {
            private const val MEASUREMENT_INDEXED_SCREENSHOT_WITH_TEXT_COUNT = "indexable_screenshots"
        }
    }

    data class TextRecognitionResult(val value: String,
                                     val message: String = "",
                                     val linksFound: Int = 0,
                                     val textBlocks: Int = 0,
                                     val totalLength: Int = 0)
}
