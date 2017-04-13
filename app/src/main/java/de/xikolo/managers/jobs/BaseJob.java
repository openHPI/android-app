package de.xikolo.managers.jobs;

import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

public abstract class BaseJob extends Job {

    protected JobCallback callback;

    public BaseJob(Params params, JobCallback callback) {
        super(params);
        this.callback = callback;
    }

    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        if (callback != null) callback.onError(JobCallback.ErrorCode.CANCEL);
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.CANCEL;
    }

}