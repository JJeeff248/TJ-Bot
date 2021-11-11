package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("langugemsg")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguageMsg {

    @JacksonXmlProperty(isAttribute = true)
    private String english;

    @JacksonXmlProperty(isAttribute = true)
    private String other;

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }


}
