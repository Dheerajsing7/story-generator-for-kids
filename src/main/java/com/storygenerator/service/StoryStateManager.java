package com.storygenerator.service;

import com.storygenerator.model.StoryChoice;
import com.storygenerator.model.StoryScene;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoryStateManager {

    public static class StoryState {
        public String storyId;
        public String characterName;
        public String theme;
        public String language;
        public String currentSegment;
        public List<String> choiceHistory;
        public StringBuilder fullStory;

        public StoryState(String storyId, String characterName, String theme, String language) {
            this.storyId = storyId;
            this.characterName = characterName;
            this.theme = theme;
            this.language = language;
            this.currentSegment = "opening";
            this.choiceHistory = new ArrayList<>();
            this.fullStory = new StringBuilder();
        }
    }

    private final Map<String, StoryState> activeStories = new ConcurrentHashMap<>();

    public StoryState createStory(String characterName, String theme, String language) {
        String storyId = UUID.randomUUID().toString().substring(0, 8);
        StoryState state = new StoryState(storyId, characterName, theme, language);
        activeStories.put(storyId, state);
        return state;
    }

    public StoryState getStory(String storyId) {
        return activeStories.get(storyId);
    }

    public void updateSegment(String storyId, String segment, String choiceId) {
        StoryState state = activeStories.get(storyId);
        if (state != null) {
            state.currentSegment = segment;
            state.choiceHistory.add(choiceId);
        }
    }

    public void appendStoryText(String storyId, String text) {
        StoryState state = activeStories.get(storyId);
        if (state != null) {
            state.fullStory.append(text).append("\n\n");
        }
    }
}
