package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@JsonRootName("relatedexamples")
@JsonIgnoreProperties(ignoreUnknown = true)
final class RelatedExamples {

    @JacksonXmlProperty(isAttribute = true)
    private int count;

    @JsonProperty("relatedexample")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<RelatedExample> relatedExamples = new ArrayList<>();

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<RelatedExample> getRelatedExamples() {
        return Collections.unmodifiableList(relatedExamples);
    }

    public void setRelatedExamples(List<RelatedExample> relatedExamples) {
        this.relatedExamples = new ArrayList<>(relatedExamples);
    }
}
