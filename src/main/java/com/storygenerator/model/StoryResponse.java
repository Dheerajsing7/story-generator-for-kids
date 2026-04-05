package com.storygenerator.model;

import java.util.List;

public class StoryResponse {
    private String storyId;
    private String title;
    private String content;
    private List<StoryChoice> choices;
    private String language;
    private boolean complete;
    private List<StoryScene> scenes;
    private String imageUrl;
    private String characterName;
    private String theme;
    private String ageGroup;
    private String error;

    public StoryResponse() {}

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<StoryChoice> getChoices() { return choices; }
    public void setChoices(List<StoryChoice> choices) { this.choices = choices; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }
    public List<StoryScene> getScenes() { return scenes; }
    public void setScenes(List<StoryScene> scenes) { this.scenes = scenes; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
