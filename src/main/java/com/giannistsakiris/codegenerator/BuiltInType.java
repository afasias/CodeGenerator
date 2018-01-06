package com.giannistsakiris.codegenerator;

public enum BuiltInType implements TypeEntity {
    
    String,
    Integer,
    Double,
    Date,
    Boolean;

    public String getName() {
        return name();
    }

    public String getFullyQualifiedName() {
        return name();
    }
}
