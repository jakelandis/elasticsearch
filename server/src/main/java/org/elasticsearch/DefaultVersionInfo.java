/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch;

import java.util.HashMap;
import java.util.Map;

public record DefaultVersionInfo(Build build, Version version) implements VersionInfo {

    @Override
    public Map<String, String> get() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("number", build.qualifiedVersion());
        versionInfo.put("build_flavor", "default");
        versionInfo.put("build_type", build.type().displayName());
        versionInfo.put("build_hash", build.hash());
        versionInfo.put("build_date", build.date());
        versionInfo.put("build_snapshot", String.valueOf(build.isSnapshot()).toLowerCase());
        versionInfo.put("lucene_version", version.luceneVersion().toString());
        versionInfo.put("minimum_wire_compatibility_version", version.minimumCompatibilityVersion().toString());
        versionInfo.put("minimum_index_compatibility_version", version.minimumIndexCompatibilityVersion().toString());
        return versionInfo;
    }
}
