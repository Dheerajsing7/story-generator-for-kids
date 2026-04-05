package com.storygenerator.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ImageAnalysisService {

    // Maps image characteristics to story themes and elements
    private static final Map<String, String> KEYWORD_THEME_MAP = new HashMap<>();
    private static final Map<String, Map<String, String>> IMAGE_STORY_ELEMENTS = new HashMap<>();

    static {
        // File name keywords to theme mapping
        KEYWORD_THEME_MAP.put("cat", "animals");
        KEYWORD_THEME_MAP.put("dog", "animals");
        KEYWORD_THEME_MAP.put("bird", "animals");
        KEYWORD_THEME_MAP.put("animal", "animals");
        KEYWORD_THEME_MAP.put("pet", "animals");
        KEYWORD_THEME_MAP.put("fish", "animals");
        KEYWORD_THEME_MAP.put("rabbit", "animals");
        KEYWORD_THEME_MAP.put("horse", "animals");
        KEYWORD_THEME_MAP.put("elephant", "animals");
        KEYWORD_THEME_MAP.put("lion", "animals");
        KEYWORD_THEME_MAP.put("tiger", "animals");
        KEYWORD_THEME_MAP.put("butterfly", "animals");
        KEYWORD_THEME_MAP.put("forest", "adventure");
        KEYWORD_THEME_MAP.put("mountain", "adventure");
        KEYWORD_THEME_MAP.put("ocean", "adventure");
        KEYWORD_THEME_MAP.put("sea", "adventure");
        KEYWORD_THEME_MAP.put("river", "adventure");
        KEYWORD_THEME_MAP.put("beach", "adventure");
        KEYWORD_THEME_MAP.put("island", "adventure");
        KEYWORD_THEME_MAP.put("cave", "adventure");
        KEYWORD_THEME_MAP.put("castle", "fantasy");
        KEYWORD_THEME_MAP.put("dragon", "fantasy");
        KEYWORD_THEME_MAP.put("fairy", "fantasy");
        KEYWORD_THEME_MAP.put("magic", "fantasy");
        KEYWORD_THEME_MAP.put("unicorn", "fantasy");
        KEYWORD_THEME_MAP.put("wizard", "fantasy");
        KEYWORD_THEME_MAP.put("princess", "fantasy");
        KEYWORD_THEME_MAP.put("prince", "fantasy");
        KEYWORD_THEME_MAP.put("rainbow", "fantasy");
        KEYWORD_THEME_MAP.put("star", "space");
        KEYWORD_THEME_MAP.put("moon", "space");
        KEYWORD_THEME_MAP.put("planet", "space");
        KEYWORD_THEME_MAP.put("rocket", "space");
        KEYWORD_THEME_MAP.put("space", "space");
        KEYWORD_THEME_MAP.put("astronaut", "space");
        KEYWORD_THEME_MAP.put("sky", "space");
        KEYWORD_THEME_MAP.put("friend", "friendship");
        KEYWORD_THEME_MAP.put("kids", "friendship");
        KEYWORD_THEME_MAP.put("children", "friendship");
        KEYWORD_THEME_MAP.put("family", "friendship");
        KEYWORD_THEME_MAP.put("school", "friendship");
        KEYWORD_THEME_MAP.put("play", "friendship");
        KEYWORD_THEME_MAP.put("puzzle", "mystery");
        KEYWORD_THEME_MAP.put("detective", "mystery");
        KEYWORD_THEME_MAP.put("mystery", "mystery");
        KEYWORD_THEME_MAP.put("clue", "mystery");
        KEYWORD_THEME_MAP.put("map", "mystery");
        KEYWORD_THEME_MAP.put("treasure", "mystery");
        KEYWORD_THEME_MAP.put("key", "mystery");

        // Image-based story elements for different themes
        Map<String, String> animalElements = new HashMap<>();
        animalElements.put("en", "Looking at this wonderful picture, it reminds me of the magical bond between animals and nature. ");
        animalElements.put("hi", "इस अद्भुत तस्वीर को देखकर, यह हमें जानवरों और प्रकृति के जादुई बंधन की याद दिलाती है। ");
        animalElements.put("hinglish", "Yeh amazing picture dekhke lagta hai ki animals aur nature ka bond kitna magical hai! ");
        animalElements.put("bn", "এই অসাধারণ ছবিটা দেখে মনে হচ্ছে পশুপাখি আর প্রকৃতির মধ্যে কী জাদুকরী বন্ধন! ");
        IMAGE_STORY_ELEMENTS.put("animals", animalElements);

        Map<String, String> adventureElements = new HashMap<>();
        adventureElements.put("en", "This breathtaking image shows a world full of exciting adventures waiting to be explored! ");
        adventureElements.put("hi", "यह लुभावनी तस्वीर एक ऐसी दुनिया दिखाती है जो रोमांचक कारनामों से भरी है! ");
        adventureElements.put("hinglish", "Yeh picture dekho — ek aisi duniya hai jo exciting adventures se bhari hai! ");
        adventureElements.put("bn", "এই দারুণ ছবিটা এমন এক জগৎ দেখাচ্ছে যেটা রোমাঞ্চকর অভিযানে ভরপুর! ");
        IMAGE_STORY_ELEMENTS.put("adventure", adventureElements);

        Map<String, String> fantasyElements = new HashMap<>();
        fantasyElements.put("en", "This enchanting picture has a touch of magic that inspires a truly fantastical tale! ");
        fantasyElements.put("hi", "इस मंत्रमुग्ध करने वाली तस्वीर में जादू का स्पर्श है जो एक अद्भुत कहानी को प्रेरित करती है! ");
        fantasyElements.put("hinglish", "Yeh picture mein itna magic hai ki ek fantastic kahani ban sakti hai! ");
        fantasyElements.put("bn", "এই মন্ত্রমুগ্ধ ছবিতে এমন এক জাদুর ছোঁয়া আছে যা অসাধারণ গল্প তৈরি করে! ");
        IMAGE_STORY_ELEMENTS.put("fantasy", fantasyElements);

        Map<String, String> spaceElements = new HashMap<>();
        spaceElements.put("en", "Looking at this picture, one can almost feel the vastness of the universe calling out for exploration! ");
        spaceElements.put("hi", "इस तस्वीर को देखकर, लगभग ब्रह्मांड की विशालता महसूस हो सकती है जो खोज के लिए पुकार रही है! ");
        spaceElements.put("hinglish", "Yeh picture dekhke lagta hai jaise poora universe explore karne ke liye bula raha hai! ");
        spaceElements.put("bn", "এই ছবি দেখে মনে হয় মহাবিশ্বের বিশালতা যেন অভিযানের জন্য ডাকছে! ");
        IMAGE_STORY_ELEMENTS.put("space", spaceElements);

        Map<String, String> friendshipElements = new HashMap<>();
        friendshipElements.put("en", "This heartwarming picture captures the beautiful essence of friendship and togetherness! ");
        friendshipElements.put("hi", "यह दिल को छू लेने वाली तस्वीर दोस्ती और एकजुटता के सुंदर सार को दर्शाती है! ");
        friendshipElements.put("hinglish", "Yeh picture dil ko touch kar deti hai — dosti aur togetherness ka essence hai! ");
        friendshipElements.put("bn", "এই হৃদয়স্পর্শী ছবিটা বন্ধুত্ব আর একতার সুন্দর সারাংশ ধরে রেখেছে! ");
        IMAGE_STORY_ELEMENTS.put("friendship", friendshipElements);

        Map<String, String> mysteryElements = new HashMap<>();
        mysteryElements.put("en", "This mysterious picture holds many secrets, just waiting for a clever detective to uncover them! ");
        mysteryElements.put("hi", "इस रहस्यमय तस्वीर में कई राज छिपे हैं, बस एक होशियार जासूस का इंतजार है! ");
        mysteryElements.put("hinglish", "Yeh mysterious picture mein bohot secrets chhupe hain — bas ek smart detective chahiye! ");
        mysteryElements.put("bn", "এই রহস্যময় ছবিতে অনেক গোপন কথা লুকিয়ে আছে, শুধু একজন চতুর গোয়েন্দার অপেক্ষায়! ");
        IMAGE_STORY_ELEMENTS.put("mystery", mysteryElements);
    }

    public String analyzeImageForTheme(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return getRandomTheme();
        }

        String lowerName = imageName.toLowerCase();
        for (Map.Entry<String, String> entry : KEYWORD_THEME_MAP.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return getRandomTheme();
    }

    public String getImageStoryPrefix(String theme, String language) {
        Map<String, String> elements = IMAGE_STORY_ELEMENTS.getOrDefault(theme,
            IMAGE_STORY_ELEMENTS.get("adventure"));
        return elements.getOrDefault(language, elements.get("en"));
    }

    private String getRandomTheme() {
        String[] themes = {"adventure", "fantasy", "space", "animals", "friendship", "mystery"};
        return themes[new Random().nextInt(themes.length)];
    }
}
