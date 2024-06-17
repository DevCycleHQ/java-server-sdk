package com.devcycle.sdk.server.local.model;

public class Project {
    public String _id;
    public String key;
    public String a0_organization;
    public ProjectSettings settings;

    static class ProjectSettings {
        public EdgeDBSettings edgeDB;
        public OptInSettings optIn;

        static class EdgeDBSettings {
            public Boolean enabled;
        }

        static class OptInSettings {
            public Boolean enabled;
            public String title;
            public String description;
            public String imageURL;
            public Colors colors;
            public String poweredByAlignment;

            static class Colors {
                public String primary;
                public String secondary;
            }
        }
    }
}
