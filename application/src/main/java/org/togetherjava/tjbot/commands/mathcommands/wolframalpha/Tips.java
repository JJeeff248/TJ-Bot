package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@JsonRootName("tips")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tips {
    @JacksonXmlProperty(isAttribute = true)
    int count;
    List<Tip> tips;

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

    public String toMessage() {
        StringBuilder sb = new StringBuilder();
        tips.forEach(x -> sb.append(x).append("\n"));
        return count == 0 ? "" : count + " tips:\n" + sb;
    }
}
