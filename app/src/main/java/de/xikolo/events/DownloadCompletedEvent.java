package de.xikolo.events;

import de.xikolo.events.base.Event;
import de.xikolo.models.Download;

public class DownloadCompletedEvent extends Event {

    private Download dl;

    public DownloadCompletedEvent(Download download) {
        super(DownloadCompletedEvent.class.getSimpleName() + ": id = " + download.id + ", uri = " + download.uri);
        this.dl = download;
    }

    public Download getDownload() {
        return dl;
    }
    
}
