package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@JsonRootName("relatedexamples")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelatedExamples {

    @JacksonXmlProperty(isAttribute = true)
    int count;

    List<RelatedExample> relatedExamples = new ArrayList<>();

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
