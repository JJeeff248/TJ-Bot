package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * <p>
 * Example Query: Operating Systems <br>
 * Result: {@code <futuretopic topic='Operating Systems'
 *      msg='Development of this topic is under investigation...' />}
 * </p>
 */
@JsonRootName("futuretopic")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FutureTopic {

    @JacksonXmlProperty(isAttribute = true)
    private String topic;

    @JacksonXmlProperty(isAttribute = true)
    private String msg;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
