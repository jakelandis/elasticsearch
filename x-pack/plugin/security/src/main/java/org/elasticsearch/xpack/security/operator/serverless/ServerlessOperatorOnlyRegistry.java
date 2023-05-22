/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.operator.serverless;

import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsAction;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.xpack.core.security.authc.AuthenticationField;
import org.elasticsearch.xpack.security.operator.OperatorOnlyRegistry;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ServerlessOperatorOnlyRegistry implements OperatorOnlyRegistry {

    public static final Set<String> SIMPLE_ACTIONS = Set.of(
        // Serverless is blocked at REST API level, so blocking transport actions is generally not necessary
    );

    private final ClusterSettings clusterSettings;

    public ServerlessOperatorOnlyRegistry(ClusterSettings clusterSettings) {
        this.clusterSettings = clusterSettings;
    }

    /**
     * Check whether the given action and request qualify as operator-only. The method returns
     * null if the action+request is NOT operator-only. Other it returns a violation object
     * that contains the message for details.
     * @return
     */
    public OperatorOnlyRegistry.OperatorPrivilegesViolation checkTransportAction(String action, TransportRequest request) {
        if (SIMPLE_ACTIONS.contains(action)) {
            return () -> "action [" + action + "]";
        } else if (ClusterUpdateSettingsAction.NAME.equals(action)) {
            assert request instanceof ClusterUpdateSettingsRequest;
            return checkClusterUpdateSettings((ClusterUpdateSettingsRequest) request);
        } else {
            return null;
        }
    }

    @Override
    public OperatorOnlyRegistry.OperatorPrivilegesViolation checkRestAccess(RestHandler restHandler, ThreadContext threadContext) {

        //TODO: use a different model (that accepts operator privs as an arugument) to determine public vs. no-access since that has
        //nothing to do with operators privs. ...go back to the original restAccess control but call this from that..so 2 different SPI's
        System.out.println("Checking REST Access here !");
        Scope scope = restHandler.getServerlessScope();
        if(scope == null) {
            return () -> "no access is allowed at all !";
        } else {
            switch (scope) {
                case PUBLIC -> {
                    return null;  // allow anyone to access
                }
                case INTERNAL -> {
                    boolean isOperator = AuthenticationField.PRIVILEGE_CATEGORY_VALUE_OPERATOR.equals(
                        threadContext.getHeader(AuthenticationField.PRIVILEGE_CATEGORY_KEY));
                    if (isOperator) {
                        System.out.println("IS OPerator -> Checking REST Access here !");
                        return null;
                    } else {
                        System.out.println("IS NOT OPerator -> Checking REST Access here !");
                        return () -> "you must be an operator to call this rest handler";
                    }
                }
                default -> {
                    throw new RuntimeException("boom... can not happen");
                }
            }

        }
    }

    public OperatorOnlyRegistry.OperatorPrivilegesViolation checkClusterUpdateSettings(ClusterUpdateSettingsRequest request) {
        List<String> operatorOnlySettingKeys = Stream.concat(
            request.transientSettings().keySet().stream(),
            request.persistentSettings().keySet().stream()
        ).filter(k -> {
            final Setting<?> setting = clusterSettings.get(k);
            return setting != null && setting.isOperatorOnly();
        }).toList();
        if (false == operatorOnlySettingKeys.isEmpty()) {
            return () -> (operatorOnlySettingKeys.size() == 1 ? "setting" : "settings")
                + " ["
                + Strings.collectionToDelimitedString(operatorOnlySettingKeys, ",")
                + "]";
        } else {
            return null;
        }
    }

    @FunctionalInterface
    public interface OperatorPrivilegesViolation {
        String message();
    }
}
