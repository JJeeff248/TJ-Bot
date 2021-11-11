package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("didyoumean")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DidYouMean {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
