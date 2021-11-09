package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("tip")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tip {
    @JacksonXmlProperty(isAttribute = true)
    String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
