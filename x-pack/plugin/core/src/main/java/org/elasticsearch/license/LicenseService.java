/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.license;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.protocol.xpack.license.PutLicenseResponse;

/**
 * Interface to read the current license. Consumers should generally not need to read the license directly and should instead
 * prefer {@link XPackLicenseState} to check if a feature is allowed by the license. This interface is not intended to be implemented
 * by alternative implementations and exists for internal use only.
 */
public interface LicenseService extends LifecycleComponent {

    /**
     * Get the current license. General consumption should prefer {@link LicenseService#getXPackLicenseState()} for license decisions.
     * @return the current license, null or {@link LicensesMetadata#LICENSE_TOMBSTONE} if no license is available.
     */
    License getLicense();

    /**
     * @return {@link XPackLicenseState} which should be the preferred way to read the license state to make license based decisions.
     */
    XPackLicenseState getXPackLicenseState();

    /**
     * Get the current license from the provided metadata. Implementations not backed by {@link org.elasticsearch.cluster.ClusterState}
     * should not implement this interface.
     * @param metaData the {@link Metadata} to read the license information from.
     * @return the current license, null or {@link LicensesMetadata#LICENSE_TOMBSTONE} if no license is available.
     */
    default License getLicense(Metadata metaData) {
        return getLicense();
    }

    /**
     * Interface to update the current license.
     */
    interface MutableLicense extends LicenseService, LifecycleComponent {

        /**
         * Updates the {@link XPackLicenseState}. The {@link XPackLicenseState} is the preferred way to make decisions based on the license.
         * The {@link XPackLicenseState} is derived from the current {@link License} and must be updated for any changes to the license.
         */
        default void updateXPackLicenseState(License license) {
            getXPackLicenseState().update(license.operationMode(), true, "");
        }

        /**
         * Creates or updates the current license as defined by the request.
         */
        void registerLicense(PutLicenseRequest request, ActionListener<PutLicenseResponse> listener);

        /**
         * Removes the current license. Implementations should remove the current license and ensure that attempts to read returns
         * {@link LicensesMetadata#LICENSE_TOMBSTONE} if a license was removed. Additionally the {@link XPackLicenseState} must be updated.
         */
        void removeLicense(ActionListener<? extends AcknowledgedResponse> listener);

        /**
         * Check and maybe expire the license.
         * @return true if the license was found to be expired, false otherwise (a null license should return false).
         */
        boolean maybeExpireLicense(License license);

        /**
         * Installs a basic license.
         */
        void startBasicLicense(PostStartBasicRequest request, ActionListener<PostStartBasicResponse> listener);

        /**
         * Installs a trial license.
         */
        void startTrialLicense(PostStartTrialRequest request, ActionListener<PostStartTrialResponse> listener);

    }
}
