package com.storygenerator.model;

public class StoryChoice {
    private String choiceId;
    private String text;
    private String emoji;

    public StoryChoice() {}

    public StoryChoice(String choiceId, String text, String emoji) {
        this.choiceId = choiceId;
        this.text = text;
        this.emoji = emoji;
    }

    public String getChoiceId() { return choiceId; }
    public void setChoiceId(String choiceId) { this.choiceId = choiceId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
}
