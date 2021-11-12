package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Example Query: bhefjuhkynmbtg <br>
 * Result: <br>
 * {@code <tips count='1'>
 *   <tip text='Check your spelling, and use English' />
 *  </tips>}
 * </p>
 */
@SuppressWarnings("unused")
@JsonRootName("tips")
@JsonIgnoreProperties(ignoreUnknown = true)
final class Tips {
    @JacksonXmlProperty(isAttribute = true)
    private int count;

    @JsonProperty("tip")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Tip> tips;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Tip> getTips() {
        return Collections.unmodifiableList(tips);
    }

    public void setTips(List<Tip> tips) {
        this.tips = new ArrayList<>(tips);
    }

}