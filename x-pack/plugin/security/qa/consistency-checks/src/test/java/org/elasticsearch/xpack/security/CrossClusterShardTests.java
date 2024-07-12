/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.RemoteClusterActionType;
import org.elasticsearch.action.admin.cluster.shards.TransportClusterSearchShardsAction;
import org.elasticsearch.action.search.TransportSearchShardsAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.action.support.single.shard.TransportSingleShardAction;
import org.elasticsearch.common.inject.TypeLiteral;
import org.elasticsearch.datastreams.DataStreamsPlugin;
import org.elasticsearch.index.rankeval.RankEvalPlugin;
import org.elasticsearch.ingest.IngestTestPlugin;
import org.elasticsearch.ingest.common.IngestCommonPlugin;
import org.elasticsearch.node.Node;
import org.elasticsearch.painless.PainlessPlugin;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SystemIndexPlugin;
import org.elasticsearch.plugins.interceptor.RestServerActionPlugin;
import org.elasticsearch.reindex.ReindexPlugin;
import org.elasticsearch.script.mustache.MustachePlugin;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.elasticsearch.xpack.analytics.AnalyticsPlugin;
import org.elasticsearch.xpack.autoscaling.Autoscaling;
import org.elasticsearch.xpack.ccr.Ccr;
import org.elasticsearch.xpack.core.LocalStateCompositeXPackPlugin;
import org.elasticsearch.xpack.core.XPackClientPlugin;
import org.elasticsearch.xpack.core.XPackPlugin;
import org.elasticsearch.xpack.core.security.action.apikey.CrossClusterApiKeyRoleDescriptorBuilder;
import org.elasticsearch.xpack.core.security.authz.privilege.IndexPrivilege;
import org.elasticsearch.xpack.downsample.Downsample;
import org.elasticsearch.xpack.downsample.DownsampleShardPersistentTaskExecutor;
import org.elasticsearch.xpack.eql.plugin.EqlPlugin;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.elasticsearch.xpack.frozen.FrozenIndices;
import org.elasticsearch.xpack.graph.Graph;
import org.elasticsearch.xpack.ilm.IndexLifecycle;
import org.elasticsearch.xpack.inference.InferencePlugin;
import org.elasticsearch.xpack.profiling.ProfilingPlugin;
import org.elasticsearch.xpack.rollup.Rollup;
import org.elasticsearch.xpack.search.AsyncSearch;
import org.elasticsearch.xpack.slm.SnapshotLifecycle;
import org.elasticsearch.xpack.sql.plugin.SqlPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CrossClusterShardTests extends ESSingleNodeTestCase {

    Set<String> MANUALLY_CHECKED_SHARD_ACTIONS = Set.of(
        // The request types for these actions are all subtypes of SingleShardRequest, and have been evaluated to make sure their
        // `shards()` methods return the correct thing.
        TransportSearchShardsAction.NAME,

        // These types have had the interface implemented manually.
        DownsampleShardPersistentTaskExecutor.DelegatingAction.NAME,

        // These actions do not have any references to shard IDs in their requests.
        TransportClusterSearchShardsAction.TYPE.name()
    );

    Set<Class<?>> CHECKED_ABSTRACT_CLASSES = Set.of(
        // This abstract class implements the interface so we can assume all of its subtypes do so properly as well.
        TransportSingleShardAction.class
    );

    private static final Set<Class<?>> ignoredPlugins = Set.of(
//        RestServerActionPlugin.class,
        PainlessPlugin.class //has extendtino that causes issues,
//        XPackPlugin.class, //using xpacklocal plugin instead
//        XPackClientPlugin.class
        );

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<Class<? extends Plugin>> getPlugins() {
        final ArrayList<Class<? extends Plugin>> plugins = new ArrayList<>(super.getPlugins());

        Reflections reflections = new Reflections(
            new ConfigurationBuilder().forPackages("org.elasticsearch").addScanners(Scanners.SubTypes)
        );


        Set<Class<? extends ActionPlugin>> actionPlugins = reflections.getSubTypesOf(ActionPlugin.class);
        //actionPlugins.stream().filter(plugin -> ignoredPlugins.contains(RestServerActionPlugin.class) == false).forEach(plugin -> {
        actionPlugins.stream().forEach(plugin -> {

            if(ignoredPlugins.contains(plugin)){
                System.out.println("** ignoring: " + plugin);
            }else {

                boolean hasDefaultConstructor = false;
                Constructor<?>[] constructors = plugin.getDeclaredConstructors();
                for (Constructor<?> constructor : constructors) {
                    if (constructor.getParameterCount() == 0) {
                        hasDefaultConstructor =true;
                    }
                }

                if(hasDefaultConstructor) {

                    if(plugin.getCanonicalName().contains("xpack")){
                        System.out.println("** ignoring (handled by xpack local): " + plugin);
                    }else {

                        System.out.println("** adding: " + plugin);
                        plugins.add((Class<? extends Plugin>) plugin);
                    }
                } else {
                    System.out.println("** ignoring (no default constructor): " + plugin);
                }
            }

        });

        plugins.add(LocalStateCompositeXPackPlugin.class);


       // plugins.addAll(actionPlugins);
//            List.of(
//                LocalStateCompositeXPackPlugin.class,
//                AnalyticsPlugin.class,
//                AsyncSearch.class,
//                Autoscaling.class,
//                Ccr.class,
//                DataStreamsPlugin.class,
//                Downsample.class,
//                EqlPlugin.class,
//                EsqlPlugin.class,
//                FrozenIndices.class,
//                Graph.class,
//                IndexLifecycle.class,
//                InferencePlugin.class,
//                IngestCommonPlugin.class,
//                IngestTestPlugin.class,
//                MustachePlugin.class,
//                ProfilingPlugin.class,
//                RankEvalPlugin.class,
//                ReindexPlugin.class,
//                Rollup.class,
//                SnapshotLifecycle.class,
//                SqlPlugin.class
//            )
       // );
        return plugins;
    }





    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCheckForNewShardLevelTransportActions() throws Exception {
        Node node = node();
        Reflections reflections = new Reflections(
            new ConfigurationBuilder().forPackages("org.elasticsearch").addScanners(Scanners.SubTypes)
        );

        // Find all subclasses of IndicesRequest
        Set<Class<? extends IndicesRequest>> indicesRequest = reflections.getSubTypesOf(IndicesRequest.class);

        //Find all transport actions instances from Guice (depends on the set of plugins this test is configured to use)
        List<TransportAction> allTransportActions = node.injector()
            .findBindingsByType(TypeLiteral.get(TransportAction.class)).stream()
            .map(binding -> binding.getProvider().get())
            .toList();


        for (TransportAction transportAction : allTransportActions) {
            System.out.println("transportAction: " + transportAction.getClass());
        }


        // Find the ActionType -> TransportAction mapping, both share the same action name
        Field field = ActionModule.class.getDeclaredField("debugActions");
        field.setAccessible(true);
        Map<String, ActionPlugin.ActionHandler<?, ?>> actionRegistry = (Map<String, ActionPlugin.ActionHandler<?, ?>>) field.get(null);

        // Find the TransportActions that can go across clusters based on the ActionType having a RemoteClusterActionType
        Set<ActionType> crossClusterActions = new HashSet<>();
        for (TransportAction transportAction : allTransportActions) {
            //read the parameters from the transport action superclass
            ActionType actionType = actionRegistry.get(transportAction.actionName).getAction();
                crossClusterActions.add(actionType);
            if(hasMemberWithType(actionType.getClass(), RemoteClusterActionType.class)){
            }
        }



        for (ActionType actionType : crossClusterActions) {
            System.out.println("actionType: " + actionType.getClass());
        }
      //  allowedRemoteClusterRequestTypes.add(getRequestTypeName(transportAction.getClass().getGenericSuperclass()));

        // Find the permissions allowed for RCS 2.0
        Set<String> crossClusterPrivilegeNames = new HashSet<>();
        crossClusterPrivilegeNames.addAll(List.of(CrossClusterApiKeyRoleDescriptorBuilder.CCS_INDICES_PRIVILEGE_NAMES));
        crossClusterPrivilegeNames.addAll(List.of(CrossClusterApiKeyRoleDescriptorBuilder.CCR_INDICES_PRIVILEGE_NAMES));

        // Find the allowed cluster action names based on the permissions for RCS 2.0
        Set<String> allowedRemoteClusterActionNames = actionRegistry.keySet().stream()
            .filter(actionName -> IndexPrivilege.get(crossClusterPrivilegeNames).predicate().test(actionName)).collect(Collectors.toSet());




        // Get a reference to all the TransportAction instances that are allowed by RCS 2.0
        List<TransportAction> allowedRemoteClusterActions = node.injector()
            .findBindingsByType(TypeLiteral.get(TransportAction.class)).stream()
            .map(binding -> binding.getProvider().get())
            .filter(action -> allowedRemoteClusterActionNames.contains(action.actionName))
            .toList();


        // reverse the actionRegistry map to key by ActionHandler



        //Ignore any indices requests that are already marked with the RemoteClusterShardRequest interface
        indicesRequest.removeAll(reflections.getSubTypesOf(IndicesRequest.RemoteClusterShardRequest.class));



        //Type erasure makes the relationship between the transport action and the request type difficult to determine

      //  List<Binding<ActionModule>>
//        List<String> allTransportActionNames = transportActionBindings.stream()
//            .map(binding -> binding.getProvider().get())
//            .map(action -> action.actionName)
//            .toList();



//        List<Binding<TransportAction>> transportActionBindings = node.injector().findBindingsByType(TypeLiteral.get(TransportAction.class));
//        Set<String> crossClusterPrivilegeNames = new HashSet<>();
//        crossClusterPrivilegeNames.addAll(List.of(CrossClusterApiKeyRoleDescriptorBuilder.CCS_INDICES_PRIVILEGE_NAMES));
//        crossClusterPrivilegeNames.addAll(List.of(CrossClusterApiKeyRoleDescriptorBuilder.CCR_INDICES_PRIVILEGE_NAMES));
//        List<String> allTransportActionNames = transportActionBindings.stream()
//            .map(binding -> binding.getProvider().get())
//            .map(action -> action.actionName)
//            .toList();
//


//        // Find subclasses of RemoteClusterActionType
//        Set<Class<? extends RemoteClusterActionType>> remoteClusterActionType = reflections.getSubTypesOf(RemoteClusterActionType.class);
//        for (Class<? extends RemoteClusterActionType> clazz : remoteClusterActionType) {
//            System.out.println("**************** " + clazz.getName());
//        }




//        for (Class<? extends IndicesRequest> clazz : candidatesToAddMarkerInterface) {
//
//                System.out.println("**************** " + clazz.getName());
//
//
//        }





//        // Filter down that are likely to be missing the RemoteClusterShardRequest interface
//        Class<IndicesRequest.RemoteClusterShardRequest> remoteClusterShardRequestClass = IndicesRequest.RemoteClusterShardRequest.class;
//        Class<IndicesRequest> indicesRequest = IndicesRequest.class;
//        Set< Class<? extends TransportRequest>> indicesRequestsWithoutShardInterface = new HashSet<>();
//
//
//        for (Class<? extends TransportRequest> subclassWithShard : subclassesWithShards) {
//            if (indicesRequest.isAssignableFrom(subclassWithShard)) {
//                if (remoteClusterShardRequestClass.isAssignableFrom(subclassWithShard) == false) {
//                    // indices request does not have shard level interface - candidates to add the interface
//                    System.out.println(subclassWithShard.getCanonicalName() + ": does not have shard level interface");
//                    indicesRequestsWithoutShardInterface.add(subclassWithShard);
//                }
//            }
//        }
//
//        // Find all subclasses of TransportRequest
//        Class<TransportRequest> transportRequestClass = TransportRequest.class;
//        Reflections reflections = new Reflections(
//            new ConfigurationBuilder().forPackages("org.elasticsearch").addScanners(Scanners.SubTypes)
//        );
//        Set<Class<? extends TransportRequest>> subclasses = reflections.getSubTypesOf(transportRequestClass);
//
//
//
//
//
//        // Get a reference to all transport actions (on classpath)
//        List<Binding<TransportAction>> transportActionBindings = node.injector().findBindingsByType(TypeLiteral.get(TransportAction.class));
//        Set<String> crossClusterPrivilegeNames = new HashSet<>();
//        crossClusterPrivilegeNames.addAll(List.of(CrossClusterApiKeyRoleDescriptorBuilder.CCS_INDICES_PRIVILEGE_NAMES));
//        crossClusterPrivilegeNames.addAll(List.of(CrossClusterApiKeyRoleDescriptorBuilder.CCR_INDICES_PRIVILEGE_NAMES));
//        List<String> allTransportActionNames = transportActionBindings.stream()
//            .map(binding -> binding.getProvider().get())
//            .map(action -> action.actionName)
//            .toList();
//
//        // Find the transport actions names that can go cross cluters
//        RemoteClusterActionType


//        List<String> shardActions = transportActionBindings.stream()
//            .map(binding -> binding.getProvider().get())
//            .filter(action -> IndexPrivilege.get(crossClusterPrivilegeNames).predicate().test(action.actionName))
//            .filter(this::actionIsLikelyShardAction)
//            .map(action -> action.actionName)
//            .toList();
//
//        List<String> actionsNotOnAllowlist = shardActions.stream().filter(Predicate.not(MANUALLY_CHECKED_SHARD_ACTIONS::contains)).toList();
//        if (actionsNotOnAllowlist.isEmpty() == false) {
//            fail("""
//                If this test fails, you likely just added a transport action, probably with `shard` in the name. Transport actions which
//                operate on shards directly and can be used across clusters must meet some additional requirements in order to be
//                handled correctly by all Elasticsearch infrastructure, so please make sure you have read the javadoc on the
//                IndicesRequest.RemoteClusterShardRequest interface and implemented it if appropriate and not already appropriately
//                implemented by a supertype, then add the name (as in "indices:data/read/get") of your new transport action to
//                MANUALLY_CHECKED_SHARD_ACTIONS above. Found actions not in allowlist:
//                """ + actionsNotOnAllowlist);
//        }
//
//        // Also make sure the allowlist stays up to date and doesn't have any unnecessary entries.
//        List<String> actionsOnAllowlistNotFound = MANUALLY_CHECKED_SHARD_ACTIONS.stream()
//            .filter(Predicate.not(shardActions::contains))
//            .toList();
//        if (actionsOnAllowlistNotFound.isEmpty() == false) {
//            fail(
//                "Some actions were on the allowlist but not found in the list of cross-cluster capable transport actions, please remove "
//                    + "these from MANUALLY_CHECKED_SHARD_ACTIONS if they have been removed from Elasticsearch: "
//                    + actionsOnAllowlistNotFound
//            );
//        }



        //        // Find any IndicesRequest that have methods related to shards, these are the candidate requests for the marker interface
//        Set<Class<? extends IndicesRequest>> candidatesToAddMarkerInterface = new HashSet<>();
//        Set<String> methodNames = Set.of("shard", "shards", "getShard", "getShards", "shardId", "shardIds", "getShardId", "getShardIds");
//        for (Class<? extends IndicesRequest> clazz : indicesRequest) {
//            for (Method method : clazz.getDeclaredMethods()) {
//                for (String methodName : methodNames) {
//                    if (method.getName().equals(methodName)) {
//                        candidatesToAddMarkerInterface.add((Class<? extends IndicesRequest>) method.getDeclaringClass());
//                    }
//                }
//            }
//        }


    }
//
//    /**
//     * Getting to the actual request classes themselves is made difficult by the design of Elasticsearch's transport
//     * protocol infrastructure combined with JVM type erasure. Therefore, we resort to a crude heuristic here.
//     * @param transportAction The transportport action to be checked.
//     * @return True if the action is suspected of being an action which may operate on shards directly.
//     */
//    private boolean actionIsLikelyShardAction(TransportAction<?, ?> transportAction) {
//        Class<?> clazz = transportAction.getClass();
//        Set<Class<?>> classHeirarchy = new HashSet<>();
//        while (clazz != TransportAction.class) {
//            classHeirarchy.add(clazz);
//            clazz = clazz.getSuperclass();
//        }
//        boolean hasCheckedSuperclass = classHeirarchy.stream().anyMatch(clz -> CHECKED_ABSTRACT_CLASSES.contains(clz));
//        boolean shardInClassName = classHeirarchy.stream().anyMatch(clz -> clz.getName().toLowerCase(Locale.ROOT).contains("shard"));
//        return hasCheckedSuperclass == false
//            && (shardInClassName
//                || transportAction.actionName.toLowerCase(Locale.ROOT).contains("shard")
//                || transportAction.actionName.toLowerCase(Locale.ROOT).contains("[s]"));
//    }

    private static String getRequestTypeName(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            return getRequestTypeName(typeArguments[0]);
        } else if (type instanceof Class) {
            return ((Class<?>) type).getName();
        }
        throw new RuntimeException("Unknown type: " + type);
    }

    private static boolean hasMemberWithType(Class<?> clazz, Class<?> targetType) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (isTypeMatching(field.getGenericType(), targetType)) {
                return true;
            }
        }
        // Check superclass recursively
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            hasMemberWithType(superclass, targetType);
        }
        return false;
    }

    private static boolean isTypeMatching(Type type, Class<?> targetType) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return parameterizedType.getRawType().equals(targetType);
        } else if (type instanceof Class) {
            return type.equals(targetType);
        }
        return false;
    }

}
