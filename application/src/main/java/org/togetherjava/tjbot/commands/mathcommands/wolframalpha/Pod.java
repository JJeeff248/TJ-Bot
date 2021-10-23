package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JsonRootName("pod")
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
final class Pod {
    @JacksonXmlProperty(isAttribute = true)
    private String title;
    @JacksonXmlProperty(isAttribute = true)
    private boolean error;
    @JacksonXmlProperty(isAttribute = true)
    private int position;
    @JacksonXmlProperty(isAttribute = true)
    private String scanner;
    @JacksonXmlProperty(isAttribute = true)
    private String id;
    @JacksonXmlProperty(isAttribute = true, localName = "numsubpods")
    private int numberOfSubPods;

    @JsonProperty("subpod")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<SubPod> subPods;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getScanner() {
        return scanner;
    }

    public void setScanner(String scanner) {
        this.scanner = scanner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNumberOfSubPods() {
        return numberOfSubPods;
    }

    public void setNumberOfSubPods(int numberOfSubPods) {
        this.numberOfSubPods = numberOfSubPods;
    }

    public List<SubPod> getSubPods() {
        return Collections.unmodifiableList(subPods);
    }

    public void setSubPods(List<SubPod> subPods) {
        this.subPods = new ArrayList<>(subPods);
    }
}
