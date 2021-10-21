package org.togetherjava.tjbot.commands.mathcommands.wolframalpha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("subpod")
@JsonIgnoreProperties(ignoreUnknown = true)
final class SubPod {
    @JacksonXmlProperty(isAttribute = true)
    private String title;

    @JsonProperty("img")
    private Image image;
    @JsonProperty("plaintext")
    private PlainText plainText;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public PlainText getPlainText() {
        return plainText;
    }

    public void setPlainText(PlainText plainText) {
        this.plainText = plainText;
    }
}
