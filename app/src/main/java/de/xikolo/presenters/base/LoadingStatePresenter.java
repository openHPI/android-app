package de.xikolo.presenters.base;

import de.xikolo.jobs.base.JobCallback;

public abstract class LoadingStatePresenter<V extends LoadingStateView> extends Presenter<V> {

    public abstract void onRefresh();

    protected JobCallback getDefaultJobCallback() {
        return new JobCallback() {
            @Override
            public void onSuccess() {
                if (getView() != null) {
                    getView().hideProgress();
                }
            }

            @Override
            public void onError(ErrorCode code) {
                if (getView() != null) {
                    switch (code) {
                        case NO_NETWORK:
                            getView().showNetworkRequiredMessage();
                            break;
                        case CANCEL:
                        case ERROR:
                            getView().showErrorMessage();
                            break;
                    }
                }
            }
        };
    }

}
