package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonRootName("queryresult")
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
final class QueryResult {
    @JacksonXmlProperty(isAttribute = true)
    private boolean success;
    @JacksonXmlProperty(isAttribute = true)
    private boolean error;
    @JacksonXmlProperty(isAttribute = true, localName = "numpods")
    private int numberOfPods;
    @JacksonXmlProperty(isAttribute = true)
    private String version;
    @JacksonXmlProperty(isAttribute = true, localName = "datatypes")
    private String dataTypes;
    @JacksonXmlProperty(isAttribute = true)
    private double timing;
    @JacksonXmlProperty(isAttribute = true, localName = "timedout")
    private String timedOutPods;
    @JacksonXmlProperty(isAttribute = true, localName = "parsetiming")
    private double parseTiming;
    @JacksonXmlProperty(isAttribute = true, localName = "parsetimedout")
    private boolean parseTimedOut;
    @JacksonXmlProperty(isAttribute = true, localName = "recalculate")
    private String recalculateUrl;
    private Tips tips;
    private DidYouMeans didYouMeans;
    private LanguageMsg languageMsg;
    private ExamplePage examplePage;
    private FutureTopic futureTopic;
    private RelatedExamples relatedExamples;
    @JsonProperty("pod")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Pod> pods;

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return error;
    }

    public int getNumberOfPods() {
        return numberOfPods;
    }

    public String getVersion() {
        return version;
    }

    public String getDataTypes() {
        return dataTypes;
    }

    public double getTiming() {
        return timing;
    }

    public String getTimedOutPods() {
        return timedOutPods;
    }

    public int getNumberOfTimedOutPods() {
        return timedOutPods.isEmpty() ? 0 : timedOutPods.split(",").length;
    }

    public double getParseTiming() {
        return parseTiming;
    }

    public boolean isParseTimedOut() {
        return parseTimedOut;
    }

    public String getRecalculateUrl() {
        return recalculateUrl;
    }

    public List<Pod> getPods() {
        return Collections.unmodifiableList(pods);
    }

    public Tips getTips() {
        return tips;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public void setNumberOfPods(int numberOfPods) {
        this.numberOfPods = numberOfPods;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDataTypes(String dataTypes) {
        this.dataTypes = dataTypes;
    }

    public void setTiming(double timing) {
        this.timing = timing;
    }

    public void setTimedOutPods(String timedOutPods) {
        this.timedOutPods = timedOutPods;
    }

    public void setParseTiming(double parseTiming) {
        this.parseTiming = parseTiming;
    }

    public void setParseTimedOut(boolean parseTimedOut) {
        this.parseTimedOut = parseTimedOut;
    }

    public void setRecalculateUrl(String recalculateUrl) {
        this.recalculateUrl = recalculateUrl;
    }

    public void setPods(List<Pod> pods) {
        this.pods = new ArrayList<>(pods);
    }

    public void setTips(Tips tips) {
        this.tips = tips;
    }
}
