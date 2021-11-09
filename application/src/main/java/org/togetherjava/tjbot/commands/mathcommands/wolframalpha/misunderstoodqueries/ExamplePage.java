package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("examplepage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamplePage {

    @JacksonXmlProperty(isAttribute = true)
    String category;
    @JacksonXmlProperty(isAttribute = true)
    String url;
}
