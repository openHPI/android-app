package de.xikolo.presenters.course;

import de.xikolo.models.Course;
import de.xikolo.presenters.base.LoadingStateView;

public interface CertificatesView extends LoadingStateView {

    void showCertificates(Course course);

}
