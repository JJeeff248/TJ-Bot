package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonRootName("expressiontypes")
final class ExpressionTypes {
    @JacksonXmlProperty(isAttribute = true)
    private int count;
    @JacksonXmlProperty
    private List<ExpressionType> expressionTypes;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<ExpressionType> getExpressionTypes() {
        return expressionTypes;
    }

    public void setExpressionTypes(List<ExpressionType> expressionTypes) {
        this.expressionTypes = expressionTypes;
    }
}
@JsonRootName("expressiontype")
final class ExpressionType{
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
