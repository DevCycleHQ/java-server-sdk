package com.devcycle.sdk.server.local.model;

import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.Variable;

import java.util.List;
import java.util.Map;

public class BucketedUserConfig {
    public Project project;
    public Environment environment;
    public Map<String, Feature> features;
    public Map<String, String> featureVariationMap;
    public Map<String, Variable> internalVariables;
    public Map<String, Variable> variables;
    public List<Double> knownVariableKeys;
    public Map<String, FeatureVariation> variableVariationMap;
}
