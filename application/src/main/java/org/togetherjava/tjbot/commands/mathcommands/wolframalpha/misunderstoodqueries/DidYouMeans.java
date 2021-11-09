package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonRootName("didyoumeans")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DidYouMeans {

    @JacksonXmlProperty(isAttribute = true)
    int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<DidYouMean> getDidYouMeans() {
        return Collections.unmodifiableList(didYouMeans);
    }

    public void setDidYouMeans(List<DidYouMean> didYouMeans) {
        this.didYouMeans = new ArrayList<>(didYouMeans);
    }

    List<DidYouMean> didYouMeans = new ArrayList<>();

}
