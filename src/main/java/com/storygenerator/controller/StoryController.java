package com.storygenerator.controller;

import com.storygenerator.model.*;
import com.storygenerator.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api")
public class StoryController {

    @Autowired private StoryGenerationService storyService;
    @Autowired private ContentFilterService contentFilter;

    @PostMapping("/story/generate")
    public ResponseEntity<?> generateStory(@RequestBody StoryRequest request) {
        if (!contentFilter.isInputSafe(request.getCharacterName()) ||
            !contentFilter.isInputSafe(request.getTheme())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", contentFilter.getUnsafeInputMessage(request.getLanguage()));
            return ResponseEntity.badRequest().body(error);
        }
        StoryResponse response = storyService.generateStory(
            request.getCharacterName(), request.getTheme(),
            request.getLanguage(), request.getAgeGroup());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/story/continue")
    public ResponseEntity<?> continueStory(@RequestBody InteractiveRequest request) {
        StoryResponse response = storyService.continueStory(
            request.getStoryId(), request.getChoiceId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/story/image")
    public ResponseEntity<?> generateFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "characterName", defaultValue = "") String characterName,
            @RequestParam(value = "ageGroup", defaultValue = "6-8") String ageGroup) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload an image"));
        }
        String imageName = image.getOriginalFilename();
        StoryResponse response = storyService.generateFromImage(imageName, language, characterName, ageGroup);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/themes")
    public ResponseEntity<?> getThemes(@RequestParam(value = "language", defaultValue = "en") String lang) {
        List<Map<String, String>> themes = new ArrayList<>();
        themes.add(Map.of("id","adventure","name",themeName(lang,"Adventure","रोमांच","Adventure ka Maza","অভিযান"),"emoji","🏔️"));
        themes.add(Map.of("id","fantasy","name",themeName(lang,"Fantasy","जादू","Jaadu ki Duniya","জাদু"),"emoji","✨"));
        themes.add(Map.of("id","space","name",themeName(lang,"Space","अंतरिक्ष","Space Adventure","মহাকাশ"),"emoji","🚀"));
        themes.add(Map.of("id","animals","name",themeName(lang,"Animals","जानवर","Animal Friends","পশুপাখি"),"emoji","🐾"));
        themes.add(Map.of("id","friendship","name",themeName(lang,"Friendship","दोस्ती","Dosti","বন্ধুত্ব"),"emoji","🤝"));
        themes.add(Map.of("id","mystery","name",themeName(lang,"Mystery","रहस्य","Mystery Solve Karo","রহস্য"),"emoji","🔍"));
        return ResponseEntity.ok(themes);
    }

    private String themeName(String lang, String en, String hi, String hinglish, String bn) {
        return switch (lang) {
            case "hi" -> hi;
            case "hinglish" -> hinglish;
            case "bn" -> bn;
            default -> en;
        };
    }
}
