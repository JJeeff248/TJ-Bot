package org.togetherjava.tjbot.commands.mathcommands.wolframalpha.misunderstoodqueries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("relatedexample")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelatedExample {

    @JacksonXmlProperty(isAttribute = true)
    String input;
    @JacksonXmlProperty(isAttribute = true, localName = "desc")
    String description;
    @JacksonXmlProperty(isAttribute = true)
    String category;
    @JacksonXmlProperty(isAttribute = true, localName = "categorythumb")
    String categoryThumb;
    @JacksonXmlProperty(isAttribute = true, localName = "categorypage")
    String categoryPage;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryThumb() {
        return categoryThumb;
    }

    public void setCategoryThumb(String categoryThumb) {
        this.categoryThumb = categoryThumb;
    }

    public String getCategoryPage() {
        return categoryPage;
    }

    public void setCategoryPage(String categoryPage) {
        this.categoryPage = categoryPage;
    }
}
