package com.storygenerator.model;

public class InteractiveRequest {
    private String storyId;
    private String choiceId;

    public InteractiveRequest() {}

    public InteractiveRequest(String storyId, String choiceId) {
        this.storyId = storyId;
        this.choiceId = choiceId;
    }

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }
    public String getChoiceId() { return choiceId; }
    public void setChoiceId(String choiceId) { this.choiceId = choiceId; }
}
