package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;



import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("expressiontype")
final class ExpressionType {
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
