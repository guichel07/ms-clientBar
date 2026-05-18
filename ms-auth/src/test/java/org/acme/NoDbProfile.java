package org.acme;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class NoDbProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.datasource.jdbc",
            "false",
            "quarkus.datasource.active",
            "false",
            "quarkus.hibernate-orm.enabled",
            "false"
        );
    }
}
