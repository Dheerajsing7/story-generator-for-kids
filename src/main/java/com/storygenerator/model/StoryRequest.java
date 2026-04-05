package com.storygenerator.model;

public class StoryRequest {
    private String characterName;
    private String theme;
    private String language;
    private String ageGroup;

    public StoryRequest() {}

    public StoryRequest(String characterName, String theme, String language) {
        this.characterName = characterName;
        this.theme = theme;
        this.language = language;
    }

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
}
