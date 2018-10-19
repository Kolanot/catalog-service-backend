package org.apache.marmotta.knowledge.vis.dao;

import java.util.Locale;

import org.apache.jena.ext.com.google.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PropertyValue {
    private Locale locale;
    private String value;
    private String oldValue;
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getOldValue() {
        return oldValue;
    }
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    public Locale getLocale() {
        return locale;
    }
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
    @JsonIgnore
    public boolean isModified() {
        if (Strings.emptyToNull(value) != null && !value.equals(oldValue)) {
            return true;
        }
        return false;
    }
}
