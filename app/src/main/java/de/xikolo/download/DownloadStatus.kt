package de.xikolo.download

import androidx.lifecycle.LiveData

data class DownloadStatus(
    var totalBytes: Long?,
    var downloadedBytes: Long?,
    var state: State
) {
    enum class State {
        PENDING, RUNNING, DOWNLOADED, DELETED;

        // Determines what two states result in together.
        fun and(other: State): State {
            return when {
                this == DELETED || other == DELETED -> DELETED
                this == RUNNING || other == RUNNING -> RUNNING
                this == PENDING || other == PENDING -> PENDING
                else -> DOWNLOADED
            }
        }
    }

    class DownloadStatusLiveData(
        totalBytes: Long? = null,
        downloadedBytes: Long? = null,
        state: State = State.DELETED
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

        init {
            super.setValue(DownloadStatus(totalBytes, downloadedBytes, state))
        }
    }
}
