package de.xikolo.download

import androidx.lifecycle.LiveData

/**
 * Represents the status of a download.
 *
 * @param totalBytes The total size of the download data in bytes.
 *
 * @param downloadedBytes The currently downloaded number of bytes.
 * Equal to [totalBytes] when [state] is [DOWNLOADED].
 *
 * @param state The current state of the download process.
 *
 * @param error A throwable indicating the error that occurred when the download failed.
 */
data class DownloadStatus(
    var totalBytes: Long?,
    var downloadedBytes: Long?,
    var state: State,
    var error: Throwable?
) {
    /**
     * Represents the state a download is in.
     */
    enum class State {

        /**
         * The download has been initiated but did not start to download.
         */
        PENDING,

        /**
         * The download is running.
         */
        RUNNING,

        /**
         * The download has been downloaded successfully and is persisted.
         */
        DOWNLOADED,

        /**
         * The download is not downloaded or persisted.
         */
        DELETED;

        /**
         * Determines the combined state of two download states.
         */
        fun and(other: State): State {
            return when {
                this == DELETED || other == DELETED -> DELETED
                this == RUNNING || other == RUNNING -> RUNNING
                this == PENDING || other == PENDING -> PENDING
                else -> DOWNLOADED
            }
        }
    }

    /**
     * [LiveData] wrapper for a [DownloadStatus].
     */
    class DownloadStatusLiveData(
        totalBytes: Long? = null,
        downloadedBytes: Long? = null,
        state: State = State.DELETED,
        error: Throwable? = null
    ) : LiveData<DownloadStatus>() {

        public override fun setValue(value: DownloadStatus) {
            super.setValue(value)
        }

        override fun getValue(): DownloadStatus {
            return super.getValue()!!
        }

        var totalBytes: Long?
            get() = value.totalBytes
            set(value) {
                this.value = this.value.apply { totalBytes = value }
            }

        var downloadedBytes: Long?
            get() = value.downloadedBytes
            set(value) {
                this.value = this.value.apply { downloadedBytes = value }
            }

        var state: State
            get() = value.state
            set(value) {
                this.value = this.value.apply { state = value }
            }

        var error: Throwable?
            get() = value.error
            set(value) {
                this.value = this.value.apply { error = value }
            }

        init {
            super.setValue(DownloadStatus(totalBytes, downloadedBytes, state, error))
        }
    }
}
