package com.storygenerator.model;

public class StoryScene {
    private int sceneNumber;
    private String text;
    private String emoji;
    private String backgroundColor;

    public StoryScene() {}

    public StoryScene(int sceneNumber, String text, String emoji, String backgroundColor) {
        this.sceneNumber = sceneNumber;
        this.text = text;
        this.emoji = emoji;
        this.backgroundColor = backgroundColor;
    }

    public int getSceneNumber() { return sceneNumber; }
    public void setSceneNumber(int sceneNumber) { this.sceneNumber = sceneNumber; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
}
