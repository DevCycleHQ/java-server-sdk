package com.devcycle.sdk.server.local.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Project {
    public String _id;
    public String key;
    public String a0_organization;
    public ProjectSettings settings;

    static class ProjectSettings {
        static class EdgeDBSettings {
            public Boolean enabled;
        }

        static class OptInSettings {
            public Boolean enabled;
            public String title;
            public String description;
            public String imageURL;

            static class Colors {
                public String primary;
                public String secondary;
            }
            public Colors colors;
            public String poweredByAlignment;
        }

        static class SDKTypeVisibility {
            public Boolean enabledInFeatureSettings;
        }

        public EdgeDBSettings edgeDB;
        public OptInSettings optIn;
        public SDKTypeVisibility sdkTypeVisibility;
    }
}
