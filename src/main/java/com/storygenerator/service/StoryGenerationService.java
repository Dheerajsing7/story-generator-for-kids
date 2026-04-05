package com.storygenerator.service;

import com.storygenerator.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class StoryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StoryGenerationService.class);

    @Autowired private StoryStateManager stateManager;
    @Autowired private ContentFilterService contentFilter;
    @Autowired private ImageAnalysisService imageService;
    @Autowired private OpenAIService openAIService;

    private static final Map<String, Map<String, Map<String, String>>> STORIES = new HashMap<>();
    private static final Map<String, Map<String, List<StoryChoice>>> CHOICES = new HashMap<>();
    private static final Map<String, Map<String, String>> TITLES = new HashMap<>();
    private static final String C = "{{CHARACTER}}";
    private static final String[] SCENE_COLORS = {"#FFE0B2","#B3E5FC","#C8E6C9","#F8BBD0","#D1C4E9","#FFF9C4"};
    private static final String[] SCENE_EMOJIS = {"🌟","🏰","🌈","🐾","🚀","🔮","🌺","⭐","🦋","🎪"};

    static { initStories(); }

    public StoryResponse generateStory(String characterName, String theme, String language) {
        return generateStory(characterName, theme, language, "6-8");
    }

    public StoryResponse generateStory(String characterName, String theme, String language, String ageGroup) {
        String name = contentFilter.sanitizeInput(characterName);
        if (name.isEmpty()) name = switch(language) { case "hi" -> "नन्हा हीरो"; case "hinglish" -> "Chhota Hero"; case "bn" -> "ছোট্ট বীর"; default -> "Little Hero"; };
        if (theme == null || !STORIES.containsKey(theme)) theme = "adventure";
        if (language == null) language = "en";
        if (ageGroup == null) ageGroup = "6-8";

        // Try OpenAI first
        String aiText = null;
        try {
            aiText = openAIService.generateStory(name, theme, language, ageGroup);
        } catch (Exception e) {
            log.warn("OpenAI failed, falling back to templates: {}", e.getMessage());
        }

        StoryStateManager.StoryState state = stateManager.createStory(name, theme, language);

        if (aiText != null && !aiText.isBlank()) {
            // Use OpenAI-generated story
            aiText = contentFilter.filterStoryContent(aiText);
            stateManager.appendStoryText(state.storyId, aiText);
            StoryResponse resp = new StoryResponse();
            resp.setStoryId(state.storyId);
            resp.setTitle(getTitle(theme, language).replace(C, name));
            resp.setContent(aiText);
            resp.setChoices(Collections.emptyList());
            resp.setLanguage(language);
            resp.setComplete(true);
            resp.setCharacterName(name);
            resp.setTheme(theme);
            resp.setAgeGroup(ageGroup);
            resp.setScenes(textToScenes(aiText));
            return resp;
        }

        // Fallback to local templates
        String text = getStoryText(theme, "opening", language).replace(C, name);
        text = contentFilter.filterStoryContent(text);
        stateManager.appendStoryText(state.storyId, text);

        StoryResponse resp = new StoryResponse();
        resp.setStoryId(state.storyId);
        resp.setTitle(getTitle(theme, language).replace(C, name));
        resp.setContent(text);
        resp.setChoices(getChoicesForSegment(theme, "opening", language, name));
        resp.setLanguage(language);
        resp.setComplete(false);
        resp.setCharacterName(name);
        resp.setTheme(theme);
        resp.setAgeGroup(ageGroup);
        resp.setScenes(textToScenes(text));
        return resp;
    }

    public StoryResponse continueStory(String storyId, String choiceId) {
        StoryStateManager.StoryState state = stateManager.getStory(storyId);
        if (state == null) return errorResponse("Story not found", "en");
        String seg = state.currentSegment;
        String nextSeg = seg.equals("opening") ? "middle_" + choiceId : "ending_" + choiceId;
        stateManager.updateSegment(storyId, nextSeg, choiceId);

        String text = getStoryText(state.theme, nextSeg, state.language).replace(C, state.characterName);
        text = contentFilter.filterStoryContent(text);
        stateManager.appendStoryText(storyId, text);
        boolean isEnding = nextSeg.startsWith("ending");

        StoryResponse resp = new StoryResponse();
        resp.setStoryId(storyId);
        resp.setTitle(getTitle(state.theme, state.language).replace(C, state.characterName));
        resp.setContent(text);
        resp.setChoices(isEnding ? Collections.emptyList() : getChoicesForSegment(state.theme, nextSeg, state.language, state.characterName));
        resp.setLanguage(state.language);
        resp.setComplete(isEnding);
        resp.setCharacterName(state.characterName);
        resp.setTheme(state.theme);
        resp.setScenes(textToScenes(text));
        return resp;
    }

    public StoryResponse generateFromImage(String imageName, String language, String characterName) {
        return generateFromImage(imageName, language, characterName, "6-8");
    }

    public StoryResponse generateFromImage(String imageName, String language, String characterName, String ageGroup) {
        String theme = imageService.analyzeImageForTheme(imageName);
        String prefix = imageService.getImageStoryPrefix(theme, language);
        StoryResponse resp = generateStory(characterName, theme, language, ageGroup);
        resp.setContent(prefix + resp.getContent());
        resp.setScenes(textToScenes(resp.getContent()));
        return resp;
    }

    private String getStoryText(String theme, String segment, String lang) {
        return STORIES.getOrDefault(theme, STORIES.get("adventure"))
                .getOrDefault(segment, STORIES.get(theme).get("opening"))
                .getOrDefault(lang, STORIES.get(theme).get("opening").get("en"));
    }

    private String getTitle(String theme, String lang) {
        return TITLES.getOrDefault(theme, TITLES.get("adventure")).getOrDefault(lang, "Story Time!");
    }

    private List<StoryChoice> getChoicesForSegment(String theme, String segment, String lang, String name) {
        String key = segment.startsWith("middle") ? "middle" : "opening";
        List<StoryChoice> templates = CHOICES.getOrDefault(theme, CHOICES.get("adventure"))
                .getOrDefault(key + "_" + lang, Collections.emptyList());
        List<StoryChoice> result = new ArrayList<>();
        for (StoryChoice c : templates) {
            result.add(new StoryChoice(c.getChoiceId(), c.getText().replace(C, name), c.getEmoji()));
        }
        return result;
    }

    private List<StoryScene> textToScenes(String text) {
        String[] sentences = text.split("(?<=[.!?।])\\s+");
        List<StoryScene> scenes = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            if (!sentences[i].trim().isEmpty()) {
                scenes.add(new StoryScene(i + 1, sentences[i].trim(),
                    SCENE_EMOJIS[i % SCENE_EMOJIS.length], SCENE_COLORS[i % SCENE_COLORS.length]));
            }
        }
        return scenes;
    }

    private StoryResponse errorResponse(String msg, String lang) {
        StoryResponse r = new StoryResponse();
        r.setContent(msg); r.setComplete(true); r.setChoices(Collections.emptyList());
        r.setScenes(Collections.emptyList()); r.setLanguage(lang);
        return r;
    }

    private static void initStories() {
        // ADVENTURE
        Map<String, Map<String, String>> adv = new HashMap<>();
        put(adv,"opening","en","Once upon a time, in a land of towering mountains and shimmering seas, there lived a brave young explorer named "+C+". One golden morning, while exploring near an ancient oak tree, "+C+" discovered something extraordinary — a crumpled treasure map hidden inside a hollow branch! The map showed three mysterious paths leading to a legendary treasure. "+C+"'s heart raced with excitement as they studied the faded markings on the old parchment. \"This is going to be the greatest adventure ever!\" "+C+" whispered with sparkling eyes. Which path should they follow?");
        put(adv,"opening","hi","बहुत समय पहले, ऊंचे पहाड़ों और चमचमाते समुद्रों की भूमि में, "+C+" नाम का एक बहादुर युवा खोजकर्ता रहता था। एक सुनहरी सुबह, एक प्राचीन बरगद के पेड़ के पास खोज करते हुए, "+C+" को कुछ अद्भुत मिला — एक खोखली शाखा के अंदर छिपा एक पुराना खजाने का नक्शा! नक्शे में एक महान खजाने तक जाने वाले तीन रहस्यमय रास्ते दिखाई दे रहे थे। "+C+" का दिल उत्साह से धड़कने लगा। \"यह अब तक का सबसे शानदार रोमांच होगा!\" "+C+" ने चमकती आँखों से कहा। कौन सा रास्ता चुनना चाहिए?");
        put(adv,"middle_a","en",C+" bravely chose the Mountain Path and began climbing through misty peaks. The air grew cooler with each step, and magical clouds swirled around like cotton candy. After hours of climbing, "+C+" reached a hidden cave entrance, glowing with golden light! Inside sat a friendly dragon named Sparkle, guarding the treasure. \"Welcome, brave explorer!\" Sparkle said warmly. \"You've proven your courage by climbing so high. But tell me, what will you do with the treasure?\"");
        put(adv,"middle_a","hi",C+" ने बहादुरी से पहाड़ी रास्ता चुना और धुंधली चोटियों के बीच चढ़ाई शुरू कर दी। हर कदम के साथ हवा ठंडी होती गई, और जादुई बादल रुई की मिठाई की तरह चारों ओर घूमने लगे। घंटों की चढ़ाई के बाद, "+C+" एक छिपी हुई गुफा के प्रवेश द्वार तक पहुँचा, जो सुनहरी रोशनी से चमक रही थी! अंदर स्पार्कल नाम का एक दोस्ताना ड्रैगन बैठा था। \"स्वागत है, बहादुर खोजकर्ता!\" स्पार्कल ने प्यार से कहा। \"तुमने इतनी ऊंचाई पर चढ़कर अपनी हिम्मत साबित कर दी है। लेकिन बताओ, तुम खजाने का क्या करोगे?\"");
        put(adv,"middle_b","en",C+" followed the River Path along sparkling blue waters. Colorful fish jumped and played alongside, and friendly otters waved their tiny paws. The river led to a magnificent waterfall with a rainbow arching across it! Behind the waterfall, "+C+" found a secret grotto filled with glittering crystals and a golden chest. A wise old owl named Hoot appeared on a branch above. \"Well done, young explorer!\" Hoot said. \"This treasure has been waiting for someone with a kind heart. How will you share this gift?\"");
        put(adv,"middle_b","hi",C+" चमचमाते नीले पानी के साथ नदी के रास्ते पर चल पड़ा। रंगीन मछलियाँ कूदती और खेलती रहीं, और दोस्ताना ऊदबिलाव अपने छोटे पंजे हिलाते रहे। नदी एक शानदार झरने तक ले गई जिसके ऊपर एक इंद्रधनुष बना हुआ था! झरने के पीछे, "+C+" को चमकते क्रिस्टलों से भरी एक गुप्त गुफा मिली। एक बुद्धिमान बूढ़ा उल्लू प्रकट हुआ। \"शाबाश, नन्हे खोजकर्ता!\" उल्लू ने कहा। \"यह खजाना एक दयालु दिल वाले किसी व्यक्ति का इंतजार कर रहा था।\"");
        put(adv,"middle_c","en",C+" ventured into the enchanted Forest Path. Glowing mushrooms lit the way, and friendly fireflies danced around like tiny stars. Deep in the forest, "+C+" discovered an ancient tree house hidden among the tallest trees. Inside, a kind fairy named Luna was waiting with a warm smile. \"I knew you would come, "+C+"!\" Luna said, her wings shimmering like moonlight. \"The greatest treasure is here, but it is not gold or gems. It is something far more precious. What do you think it could be?\"");
        put(adv,"middle_c","hi",C+" जादुई जंगल के रास्ते में आगे बढ़ा। चमकते मशरूमों ने रास्ता रोशन किया, और दोस्ताना जुगनू छोटे सितारों की तरह नाचते रहे। जंगल में गहराई में, "+C+" को सबसे ऊंचे पेड़ों के बीच छिपा एक प्राचीन पेड़ का घर मिला। अंदर, लूना नाम की एक दयालु परी गर्मजोशी से मुस्कुराते हुए इंतजार कर रही थी। \"मुझे पता था कि तुम आओगे, "+C+"!\" लूना ने कहा। \"सबसे बड़ा खजाना यहाँ है, लेकिन यह सोना या रत्न नहीं है। यह कुछ और भी कीमती है।\"");
        put(adv,"ending_a","en","\"I want to share the treasure with everyone in my village!\" said "+C+" with the biggest smile. Sparkle the dragon was so impressed that golden tears of joy rolled down her cheeks. \"That is the most wonderful answer I have ever heard!\" she said. Together, "+C+" and Sparkle flew down the mountain carrying the treasure chest. When they returned to the village, "+C+" shared the golden coins with every family. The village threw the grandest celebration ever, with music, dancing, and Sparkle lighting up the sky with her rainbow fire! From that day on, "+C+" was known as the kindest explorer in all the land, and Sparkle became their best friend forever. And they lived happily ever after! 🌟✨🐉");
        put(adv,"ending_a","hi","\"मैं खजाना अपने गाँव के सभी लोगों के साथ बाँटना चाहता हूँ!\" "+C+" ने सबसे बड़ी मुस्कान के साथ कहा। स्पार्कल ड्रैगन इतना प्रभावित हुआ कि खुशी के सुनहरे आँसू उसके गालों पर लुढ़क गए। \"यह सबसे अद्भुत जवाब है!\" उसने कहा। "+C+" और स्पार्कल ने मिलकर खजाने का संदूक लेकर पहाड़ से उड़ान भरी। गाँव में सबसे भव्य उत्सव मनाया गया! उस दिन से, "+C+" को पूरी दुनिया का सबसे दयालु खोजकर्ता माना जाने लगा। 🌟✨🐉");
        put(adv,"ending_b","en",""+C+" decided to use the treasure to build a school where all children could learn and grow. The wise owl Hoot became the principal, and animals from all over the forest came to teach and learn together! The school had a library full of magical books, a garden of singing flowers, and a playground among the clouds. "+C+" became the youngest teacher, sharing stories of adventure with every student. The school became the most wonderful place in the whole kingdom! And "+C+" learned the greatest lesson of all — that sharing knowledge is the most precious treasure. The End! 📚🦉🌈");
        put(adv,"ending_b","hi",""+C+" ने खजाने का उपयोग एक स्कूल बनाने के लिए किया जहाँ सभी बच्चे सीख सकें और बढ़ सकें। बुद्धिमान उल्लू प्रधानाध्यापक बन गया! स्कूल में जादुई किताबों से भरा पुस्तकालय, गाने वाले फूलों का बगीचा, और बादलों के बीच एक खेल का मैदान था। "+C+" सबसे कम उम्र का शिक्षक बन गया। "+C+" ने सबसे बड़ा सबक सीखा — ज्ञान बाँटना सबसे कीमती खजाना है! समाप्त! 📚🦉🌈");
        put(adv,"opening","hinglish","Ek time ki baat hai, jahan bade-bade mountains aur shiny seas the, wahan ek brave young explorer rehta tha jiska naam tha "+C+". Ek sunheri morning, ek ancient purane oak tree ke paas explore karte hue, "+C+" ko kuch extraordinary mila — ek hollow branch ke andar ek purana treasure map! Map mein teen mysterious paths dikhaye the jo ek legendary treasure tak le jaate the. "+C+" ka dil excitement se dhadakne laga. \"Yeh toh ab tak ka sabse greatest adventure hoga!\" "+C+" ne sparkling eyes ke saath kaha. Kaunsa path follow karein?");
        put(adv,"opening","bn","অনেক দিন আগে, উঁচু পাহাড় আর ঝলমলে সমুদ্রের দেশে, "+C+" নামে এক সাহসী তরুণ অভিযাত্রী ছিল। এক সোনালী সকালে, একটা পুরোনো ওক গাছের কাছে ঘুরতে গিয়ে, "+C+" অসাধারণ কিছু খুঁজে পেল — একটা ফাঁপা ডালের মধ্যে লুকানো একটা পুরোনো ধনের মানচিত্র! মানচিত্রে তিনটে রহস্যময় পথ দেখাচ্ছিল যেগুলো এক কিংবদন্তি ধনভান্ডারে যায়। "+C+" এর বুক উত্তেজনায় ধড়ফড় করতে লাগল। \"এটা হবে সেরা অভিযান!\" "+C+" চকচকে চোখে ফিসফিস করল। কোন পথে যাওয়া উচিত?");
        put(adv,"middle_a","hinglish",C+" ne bravely Mountain Path choose kiya aur misty peaks ke beech chadhai shuru kar di. Har step ke saath hawa thandi hoti gayi, aur magical clouds cotton candy jaisi ghoomne lagi. Ghanton ki climbing ke baad, "+C+" ek hidden cave entrance tak pahuncha jo golden light se glow kar rahi thi! Andar ek friendly dragon baitha tha jiska naam Sparkle tha. \"Welcome, brave explorer!\" Sparkle ne warmly kaha. \"Tumne itni height climb karke apni courage prove kar di hai. Par batao, treasure ka kya karoge?\"");
        put(adv,"middle_a","bn",C+" সাহসের সাথে পাহাড়ি পথ বেছে নিল আর কুয়াশাচ্ছন্ন চূড়ার মধ্যে দিয়ে উঠতে শুরু করল। প্রতিটা পদক্ষেপে বাতাস ঠান্ডা হতে লাগল, আর জাদুকরী মেঘ তুলোর মিছরির মতো ঘুরপাক খেতে লাগল। ঘণ্টার পর ঘণ্টা চড়ার পর, "+C+" একটা লুকানো গুহার মুখে পৌঁছাল, যেটা সোনালী আলোয় জ্বলছিল! ভেতরে স্পার্কল নামে এক বন্ধুবৎসল ড্রাগন বসে ছিল। \"স্বাগতম, সাহসী অভিযাত্রী!\" স্পার্কল উষ্ণভাবে বলল। \"তুমি এত উঁচুতে উঠে তোমার সাহস প্রমাণ করেছ। কিন্তু বলো, ধনভান্ডার দিয়ে কী করবে?\"");
        put(adv,"middle_b","hinglish",C+" sparkling blue waters ke saath River Path follow karne laga. Colorful fish kuudte aur khelte rahe, aur friendly otters apne chhote paws hilate rahe. River ek magnificent waterfall tak le gayi jiske upar ek rainbow ban raha tha! Waterfall ke peeche, "+C+" ko glittering crystals aur ek golden chest wali secret grotto mili. Ek wise old owl Hoot upar ek branch pe appear hua. \"Well done, young explorer!\" Hoot ne kaha. \"Yeh treasure ek kind heart waale insaan ka wait kar raha tha. Tum is gift ko kaise share karoge?\"");
        put(adv,"middle_b","bn",C+" ঝলমলে নীল জলের ধারে ধারে নদীর পথ ধরে চলল। রঙিন মাছ লাফাচ্ছিল আর খেলছিল, বন্ধুবৎসল ভোঁদড়রা তাদের ছোট্ট থাবা নাড়ছিল। নদী একটা দারুণ জলপ্রপাতে গিয়ে পৌঁছাল যার ওপরে একটা রংধনু! জলপ্রপাতের পেছনে, "+C+" একটা গোপন গুহা খুঁজে পেল যেটা চকচকে স্ফটিক আর একটা সোনার বাক্সে ভর্তি। হুট নামে এক জ্ঞানী বুড়ো পেঁচা দেখা দিল। \"সাবাশ, তরুণ অভিযাত্রী!\" হুট বলল। \"এই ধন একটা দয়ালু হৃদয়ের মানুষের অপেক্ষায় ছিল।\"");
        put(adv,"middle_c","hinglish",C+" enchanted Forest Path mein aage badha. Glowing mushrooms ne raasta roshan kiya, aur friendly fireflies chhote stars ki tarah naachti rahin. Forest mein deep andar, "+C+" ko tallest trees ke beech ek ancient tree house mila. Andar, Luna naam ki ek kind fairy warm smile ke saath wait kar rahi thi. \"Mujhe pata tha ki tum aaoge, "+C+"!\" Luna ne kaha. \"Sabse bada treasure yahaan hai, par yeh gold ya gems nahi hai. Yeh kuch aur bhi precious hai. Tumhe kya lagta hai yeh kya ho sakta hai?\"");
        put(adv,"middle_c","bn",C+" মন্ত্রমুগ্ধ বনপথে এগিয়ে চলল। জ্বলজ্বলে মাশরুম পথ আলো করে দিল, আর বন্ধুসুলভ জোনাকিরা ছোট্ট তারার মতো নাচতে লাগল। গভীর বনের মধ্যে, "+C+" সবচেয়ে উঁচু গাছগুলোর মাঝে লুকানো একটা পুরোনো গাছবাড়ি খুঁজে পেল। ভেতরে, লুনা নামে এক দয়ালু পরী উষ্ণ হাসি নিয়ে অপেক্ষা করছিল। \"আমি জানতাম তুমি আসবে, "+C+"!\" লুনা বলল। \"সবচেয়ে বড় ধন এখানে আছে, কিন্তু এটা সোনা বা রত্ন নয়। এটা আরও মূল্যবান কিছু।\"");
        put(adv,"ending_a","hinglish","\"Main treasure apne gaon ke sabhi logon ke saath share karna chahta hoon!\" "+C+" ne sabse badi smile ke saath kaha. Sparkle dragon itna impressed hua ki khushi ke golden tears uske cheeks par roll ho gaye. \"Yeh sabse wonderful answer hai!\" usne kaha. "+C+" aur Sparkle ne milkar treasure chest lekar mountain se fly kiya. Gaon mein sabse grand celebration hua, music, dancing, aur Sparkle ne sky ko rainbow fire se light up kar diya! Us din se, "+C+" ko sabse kindest explorer maana gaya. And they lived happily ever after! 🌟✨🐉");
        put(adv,"ending_a","bn","\"আমি ধনভান্ডার আমার গ্রামের সবার সাথে ভাগ করে নিতে চাই!\" "+C+" সবচেয়ে বড় হাসি দিয়ে বলল। স্পার্কল ড্রাগন এতই মুগ্ধ হল যে আনন্দের সোনালী অশ্রু তার গালে গড়িয়ে পড়ল। \"এটা সবচেয়ে চমৎকার উত্তর!\" সে বলল। "+C+" আর স্পার্কল মিলে ধনের বাক্স নিয়ে পাহাড় থেকে উড়ে এল। গ্রামে সবচেয়ে জমকালো উৎসব হল! সেদিন থেকে, "+C+" কে সবচেয়ে দয়ালু অভিযাত্রী হিসেবে জানা গেল। 🌟✨🐉");
        put(adv,"ending_b","hinglish",""+C+" ne treasure use karke ek school banana decide kiya jahan saare bacche seekh sakein. Wise owl Hoot principal ban gaya! School mein magical books ki library, singing flowers ka garden, aur clouds ke beech ek playground tha. "+C+" sabse youngest teacher ban gaya. "+C+" ne sabse bada lesson seekha — knowledge share karna sabse precious treasure hai! The End! 📚🦉🌈");
        put(adv,"ending_b","bn",""+C+" ধনভান্ডার দিয়ে একটা স্কুল তৈরি করার সিদ্ধান্ত নিল যেখানে সব শিশু শিখতে পারবে। জ্ঞানী পেঁচা হুট প্রধান শিক্ষক হল! স্কুলে জাদুকরী বইয়ের লাইব্রেরি, গান গাওয়া ফুলের বাগান, আর মেঘের মাঝে খেলার মাঠ ছিল। "+C+" সবচেয়ে কম বয়সী শিক্ষক হল। "+C+" সবচেয়ে বড় শিক্ষা পেল — জ্ঞান ভাগ করে নেওয়াই সবচেয়ে মূল্যবান ধন! শেষ! 📚🦉🌈");
        STORIES.put("adventure", adv);
        Map<String, String> advT = new HashMap<>();
        advT.put("en", C+"'s Grand Adventure"); advT.put("hi", C+" का महान रोमांच");
        advT.put("hinglish", C+" ka Grand Adventure"); advT.put("bn", C+" এর মহান অভিযান");
        TITLES.put("adventure", advT);
        Map<String, List<StoryChoice>> advC = new HashMap<>();
        advC.put("opening_en", Arrays.asList(new StoryChoice("a","Take the Mountain Path through misty peaks","🏔️"),new StoryChoice("b","Follow the River Path along sparkling waters","🌊"),new StoryChoice("c","Explore the enchanted Forest Path","🌲")));
        advC.put("opening_hi", Arrays.asList(new StoryChoice("a","धुंधली चोटियों से पहाड़ी रास्ता लो","🏔️"),new StoryChoice("b","चमकते पानी के साथ नदी का रास्ता अपनाओ","🌊"),new StoryChoice("c","जादुई जंगल का रास्ता खोजो","🌲")));
        advC.put("opening_hinglish", Arrays.asList(new StoryChoice("a","Mountain Path le lo misty peaks ke beech","🏔️"),new StoryChoice("b","River Path follow karo sparkling waters ke saath","🌊"),new StoryChoice("c","Enchanted Forest Path explore karo","🌲")));
        advC.put("opening_bn", Arrays.asList(new StoryChoice("a","কুয়াশাচ্ছন্ন চূড়ায় পাহাড়ি পথ নাও","🏔️"),new StoryChoice("b","ঝলমলে জলের ধারে নদীর পথ ধরো","🌊"),new StoryChoice("c","মন্ত্রমুগ্ধ বনপথ অন্বেষণ করো","🌲")));
        advC.put("middle_en", Arrays.asList(new StoryChoice("a","Share the treasure with everyone","💝"),new StoryChoice("b","Build something wonderful for all","🏫")));
        advC.put("middle_hi", Arrays.asList(new StoryChoice("a","खजाना सबके साथ बाँटो","💝"),new StoryChoice("b","सबके लिए कुछ अद्भुत बनाओ","🏫")));
        advC.put("middle_hinglish", Arrays.asList(new StoryChoice("a","Treasure sabke saath share karo","💝"),new StoryChoice("b","Sabke liye kuch wonderful banao","🏫")));
        advC.put("middle_bn", Arrays.asList(new StoryChoice("a","ধন সবার সাথে ভাগ করে নাও","💝"),new StoryChoice("b","সবার জন্য কিছু চমৎকার তৈরি করো","🏫")));
        CHOICES.put("adventure", advC);

        // FANTASY
        Map<String, Map<String, String>> fan = new HashMap<>();
        put(fan,"opening","en","In a magical kingdom beyond the clouds, where flowers could sing and trees could dance, there lived a young wizard named "+C+". "+C+" had just received their very first magic wand — a beautiful staff made of starlight and moonbeams! But there was a problem: the kingdom's colors were fading away, and everything was turning gray. The wise elder said only three magical ingredients could restore the colors. "+C+" knew what had to be done!");
        put(fan,"opening","hi","बादलों के पार एक जादुई राज्य में, जहाँ फूल गा सकते थे और पेड़ नाच सकते थे, "+C+" नाम का एक युवा जादूगर रहता था। "+C+" को अभी-अभी अपनी पहली जादू की छड़ी मिली थी — तारों की रोशनी और चाँदनी से बनी एक सुंदर छड़ी! लेकिन एक समस्या थी: राज्य के रंग फीके पड़ रहे थे। बुद्धिमान बुजुर्ग ने कहा कि केवल तीन जादुई सामग्रियाँ रंगों को वापस ला सकती हैं। "+C+" जानता था कि क्या करना है!");
        put(fan,"middle_a","en",C+" flew on a magical carpet to the Rainbow Garden, where the first ingredient — a petal from the Singing Rose — was guarded by a gentle giant named Bumble. \"To earn this petal, you must make me laugh!\" Bumble said. "+C+" told the funniest joke ever, and Bumble's laughter shook the whole garden! He happily gave "+C+" the glowing petal. With the first ingredient in hand, "+C+" felt the magic growing stronger!");
        put(fan,"middle_a","hi",C+" एक जादुई कालीन पर उड़कर इंद्रधनुष बगीचे में गया, जहाँ पहली सामग्री — गाने वाले गुलाब की पंखुड़ी — बंबल नाम के एक सौम्य दानव के पास थी। \"यह कमाने के लिए तुम्हें मुझे हँसाना होगा!\" बंबल ने कहा। "+C+" ने सबसे मज़ेदार चुटकुला सुनाया और बंबल की हँसी ने पूरा बगीचा हिला दिया! उसने खुशी-खुशी चमकती पंखुड़ी दे दी!");
        put(fan,"middle_b","en",C+" journeyed to the Crystal Caves, where the second ingredient — a drop of Moonlight Dew — shimmered on the ceiling. The caves were full of friendly crystal creatures who offered to help. A crystal butterfly named Glimmer led "+C+" through a maze of sparkling tunnels to the highest chamber. There, "+C+" carefully collected the precious moonlight dew in a tiny golden vial!");
        put(fan,"middle_b","hi",C+" क्रिस्टल गुफाओं की यात्रा पर गया, जहाँ दूसरी सामग्री — चाँदनी ओस की एक बूँद — छत पर चमक रही थी। गुफाएँ दोस्ताना क्रिस्टल प्राणियों से भरी थीं। ग्लिमर नाम की एक क्रिस्टल तितली ने "+C+" को चमकती सुरंगों से होकर सबसे ऊपरी कक्ष तक पहुँचाया!");
        put(fan,"middle_c","en",C+" visited the Whispering Woods to find the third ingredient — a feather from the Phoenix of Dawn. The Phoenix only appeared at sunrise, so "+C+" waited patiently through the night, making friends with the woodland creatures. When dawn broke, the magnificent Phoenix appeared in a burst of golden flames! She gently offered "+C+" one of her radiant feathers.");
        put(fan,"middle_c","hi",C+" फुसफुसाते जंगलों में तीसरी सामग्री खोजने गया — भोर के फीनिक्स का एक पंख। फीनिक्स केवल सूर्योदय पर दिखाई देता था, इसलिए "+C+" ने धैर्यपूर्वक रात भर इंतज़ार किया। जब भोर हुई, शानदार फीनिक्स सुनहरी लपटों में प्रकट हुआ और "+C+" को एक चमकदार पंख दिया!");
        put(fan,"ending_a","en",C+" mixed all three magical ingredients together and waved the starlight wand! A spectacular rainbow explosion lit up the entire kingdom! Colors burst forth everywhere — the grass turned emerald green, the sky became sapphire blue, and flowers bloomed in every color imaginable! The whole kingdom cheered as "+C+" danced with joy. The wise elder crowned "+C+" as the Kingdom's Guardian of Colors. Every morning, "+C+" would paint the sunrise with their magic wand, making sure the kingdom was always the most beautiful and colorful place in all the worlds! 🌈✨🎨");
        put(fan,"ending_a","hi",""+C+" ने तीनों जादुई सामग्रियों को मिलाया और तारों की छड़ी लहराई! एक शानदार इंद्रधनुष विस्फोट ने पूरे राज्य को रोशन कर दिया! हर जगह रंग बिखर गए! पूरे राज्य ने "+C+" का जयकारा लगाया। "+C+" को राज्य का रंगों का रक्षक बनाया गया। हर सुबह, "+C+" अपनी जादू की छड़ी से सूर्योदय को रंगता था! 🌈✨🎨");
        put(fan,"ending_b","en",C+" decided to teach everyone in the kingdom how to make their own colors using kindness and imagination! Each person received a tiny bit of magic, and soon the whole kingdom was painting, creating, and sharing colors with each other. "+C+" opened a School of Magic Arts where creatures of all kinds could learn to express themselves through colorful magic. The kingdom became a place where creativity and kindness made every day brighter than the last! The End! 🎭🌟💫");
        put(fan,"ending_b","hi",""+C+" ने सबको दयालुता और कल्पना से अपने रंग बनाना सिखाने का फैसला किया! हर किसी को थोड़ा सा जादू मिला। "+C+" ने एक जादुई कला स्कूल खोला जहाँ सभी प्राणी रंगीन जादू सीख सकते थे। राज्य ऐसी जगह बन गया जहाँ रचनात्मकता हर दिन को और रोशन बनाती थी! समाप्त! 🎭🌟💫");
        put(fan,"opening","hinglish","Clouds ke paar ek magical kingdom mein, jahan flowers gaa sakte the aur trees naach sakte the, wahan "+C+" naam ka ek young wizard rehta tha. "+C+" ko abhi-abhi apni pehli magic wand mili thi — starlight aur moonbeams se bani ek beautiful staff! Par ek problem thi: kingdom ke colors fade ho rahe the, sab kuch gray ho raha tha. Wise elder ne kaha ki sirf teen magical ingredients se colors wapas aa sakte hain. "+C+" ko pata tha kya karna hai!");
        put(fan,"opening","bn","মেঘের ওপারে এক জাদুকরী রাজ্যে, যেখানে ফুল গান গাইতে পারত আর গাছ নাচতে পারত, "+C+" নামে এক তরুণ জাদুকর থাকত। "+C+" সবে তার প্রথম জাদুদণ্ড পেয়েছিল — তারার আলো আর চাঁদের কিরণ দিয়ে তৈরি এক সুন্দর দণ্ড! কিন্তু একটা সমস্যা ছিল: রাজ্যের রঙ ফিকে হয়ে যাচ্ছিল। জ্ঞানী প্রবীণ বললেন শুধু তিনটে জাদুকরী উপাদান রঙ ফিরিয়ে আনতে পারে। "+C+" জানত কী করতে হবে!");
        put(fan,"middle_a","hinglish",C+" ek magical carpet pe udkar Rainbow Garden mein gaya, jahan pehla ingredient — Singing Rose ki ek petal — Bumble naam ke ek gentle giant ke paas thi. \"Yeh petal earn karne ke liye tumhe mujhe hasaana hoga!\" Bumble ne kaha. "+C+" ne sabse funny joke sunaya, aur Bumble ki laughter ne poora garden hila diya! Usne khushi se glowing petal de di!");
        put(fan,"middle_a","bn",C+" একটা জাদুকরী গালিচায় চড়ে রংধনু বাগানে গেল, যেখানে প্রথম উপাদান — গান গাওয়া গোলাপের একটা পাপড়ি — বাম্বল নামে এক সদয় দৈত্যর কাছে ছিল। \"এটা পেতে তোমাকে আমাকে হাসাতে হবে!\" বাম্বল বলল। "+C+" সবচেয়ে মজার কৌতুক বলল, আর বাম্বলের হাসিতে পুরো বাগান কেঁপে উঠল! সে খুশি হয়ে জ্বলজ্বলে পাপড়ি দিয়ে দিল!");
        put(fan,"middle_b","hinglish",C+" Crystal Caves ki journey pe gaya, jahan doosra ingredient — Moonlight Dew ki ek drop — ceiling pe shimmer kar rahi thi. Caves friendly crystal creatures se bhari thin. Glimmer naam ki ek crystal butterfly ne "+C+" ko sparkling tunnels ke maze se highest chamber tak pahunchaya. Wahan, "+C+" ne carefully precious moonlight dew ek tiny golden vial mein collect ki!");
        put(fan,"middle_b","bn",C+" স্ফটিক গুহার দিকে যাত্রা করল, যেখানে দ্বিতীয় উপাদান — চাঁদের আলোর শিশির — ছাদে ঝলমল করছিল। গুহা বন্ধুসুলভ স্ফটিক প্রাণীতে ভর্তি ছিল। গ্লিমার নামে এক স্ফটিক প্রজাপতি "+C+" কে ঝকঝকে সুড়ঙ্গের গোলকধাঁধা দিয়ে সবচেয়ে উঁচু কক্ষে নিয়ে গেল!");
        put(fan,"middle_c","hinglish",C+" Whispering Woods mein teesra ingredient dhundne gaya — Dawn ke Phoenix ka ek feather. Phoenix sirf sunrise pe dikhta tha, toh "+C+" ne patiently raat bhar wait kiya, woodland creatures ke saath friends banate hue. Jab dawn aayi, magnificent Phoenix golden flames mein appear hua! Usne gently "+C+" ko apna ek radiant feather offer kiya.");
        put(fan,"middle_c","bn",C+" ফিসফিস বনে তৃতীয় উপাদান খুঁজতে গেল — ভোরের ফিনিক্সের একটা পালক। ফিনিক্স শুধু সূর্যোদয়ে দেখা দেয়, তাই "+C+" ধৈর্য ধরে সারারাত অপেক্ষা করল। ভোর হতেই, দুর্দান্ত ফিনিক্স সোনালী শিখায় দেখা দিল আর "+C+" কে একটা উজ্জ্বল পালক দিল!");
        put(fan,"ending_a","hinglish",C+" ne teeno magical ingredients mix kiye aur starlight wand wave ki! Ek spectacular rainbow explosion ne poore kingdom ko light up kar diya! Har jagah colors burst ho gaye! Poore kingdom ne "+C+" ka cheers kiya. "+C+" ko Kingdom's Guardian of Colors bana diya gaya. Har subah, "+C+" apni magic wand se sunrise paint karta tha! 🌈✨🎨");
        put(fan,"ending_a","bn",C+" তিনটে জাদুকরী উপাদান মেশাল আর তারার দণ্ড ঘোরাল! এক দুর্দান্ত রংধনু বিস্ফোরণে পুরো রাজ্য আলোকিত হল! সবদিকে রঙ ছড়িয়ে পড়ল! পুরো রাজ্য "+C+" এর জয়ধ্বনি দিল। "+C+" কে রাজ্যের রঙের রক্ষক করা হল। প্রতি সকালে, "+C+" তার জাদুদণ্ড দিয়ে সূর্যোদয় রাঙাত! 🌈✨🎨");
        put(fan,"ending_b","hinglish",C+" ne sabko kindness aur imagination se apne colors banana sikhane ka decide kiya! Har ek ko thoda sa magic mila. "+C+" ne ek School of Magic Arts khola jahan sabhi creatures colorful magic seekh sakte the. Kingdom ek aisi jagah ban gayi jahan creativity har din ko aur brighter banati thi! The End! 🎭🌟💫");
        put(fan,"ending_b","bn",C+" সবাইকে দয়া আর কল্পনা দিয়ে নিজের রঙ তৈরি করতে শেখানোর সিদ্ধান্ত নিল! প্রত্যেকে একটু জাদু পেল। "+C+" একটা জাদুকরী কলা স্কুল খুলল যেখানে সব প্রাণী রঙিন জাদু শিখতে পারত। রাজ্য এমন জায়গা হয়ে গেল যেখানে সৃজনশীলতা প্রতিটা দিনকে আরও উজ্জ্বল করত! শেষ! 🎭🌟💫");
        STORIES.put("fantasy", fan);
        Map<String, String> fanT = new HashMap<>();
        fanT.put("en", C+"'s Magical Quest"); fanT.put("hi", C+" की जादुई खोज");
        fanT.put("hinglish", C+" ki Magical Quest"); fanT.put("bn", C+" এর জাদুকরী অভিযান");
        TITLES.put("fantasy", fanT);
        Map<String, List<StoryChoice>> fanC = new HashMap<>();
        fanC.put("opening_en", Arrays.asList(new StoryChoice("a","Visit the Rainbow Garden","🌹"),new StoryChoice("b","Journey to the Crystal Caves","💎"),new StoryChoice("c","Explore the Whispering Woods","🌳")));
        fanC.put("opening_hi", Arrays.asList(new StoryChoice("a","इंद्रधनुष बगीचे में जाओ","🌹"),new StoryChoice("b","क्रिस्टल गुफाओं की यात्रा करो","💎"),new StoryChoice("c","फुसफुसाते जंगलों की खोज करो","🌳")));
        fanC.put("opening_hinglish", Arrays.asList(new StoryChoice("a","Rainbow Garden visit karo","🌹"),new StoryChoice("b","Crystal Caves ki journey karo","💎"),new StoryChoice("c","Whispering Woods explore karo","🌳")));
        fanC.put("opening_bn", Arrays.asList(new StoryChoice("a","রংধনু বাগানে যাও","🌹"),new StoryChoice("b","স্ফটিক গুহায় যাত্রা করো","💎"),new StoryChoice("c","ফিসফিস বন অন্বেষণ করো","🌳")));
        fanC.put("middle_en", Arrays.asList(new StoryChoice("a","Use magic to restore all colors at once","🌈"),new StoryChoice("b","Teach everyone to create their own colors","🎨")));
        fanC.put("middle_hi", Arrays.asList(new StoryChoice("a","जादू से सभी रंग एक साथ वापस लाओ","🌈"),new StoryChoice("b","सभी को अपने रंग बनाना सिखाओ","🎨")));
        fanC.put("middle_hinglish", Arrays.asList(new StoryChoice("a","Magic se saare colors ek saath restore karo","🌈"),new StoryChoice("b","Sabko apne colors banana sikhao","🎨")));
        fanC.put("middle_bn", Arrays.asList(new StoryChoice("a","জাদু দিয়ে সব রঙ একসাথে ফিরিয়ে আনো","🌈"),new StoryChoice("b","সবাইকে নিজের রঙ তৈরি করতে শেখাও","🎨")));
        CHOICES.put("fantasy", fanC);

        // SPACE
        Map<String, Map<String, String>> spc = new HashMap<>();
        put(spc,"opening","en","3... 2... 1... BLAST OFF! Young astronaut "+C+" zoomed into space aboard the magnificent starship 'Cosmic Wonder'! The mission: to find the legendary Star of Friendship that could connect all the planets in the galaxy. As the Earth grew smaller in the window, "+C+" gazed at billions of twinkling stars. The ship's friendly AI assistant, Astro, announced: \"Captain "+C+", I've detected three possible locations for the Star of Friendship!\"");
        put(spc,"opening","hi","3... 2... 1... उड़ान! युवा अंतरिक्ष यात्री "+C+" शानदार अंतरिक्ष यान 'कॉस्मिक वंडर' पर अंतरिक्ष में उड़ गया! मिशन: दोस्ती का महान तारा खोजना जो आकाशगंगा के सभी ग्रहों को जोड़ सके। जैसे-जैसे पृथ्वी खिड़की में छोटी होती गई, "+C+" ने अरबों टिमटिमाते तारों को देखा। यान के दोस्ताना AI सहायक एस्ट्रो ने घोषणा की: \"कैप्टन "+C+", मैंने तीन संभावित स्थान खोजे हैं!\"");
        put(spc,"opening","hinglish","3... 2... 1... BLAST OFF! Young astronaut "+C+" magnificent starship 'Cosmic Wonder' pe space mein zoom kar gaya! Mission tha: legendary Star of Friendship dhundna jo galaxy ke saare planets ko connect kar sake. Jaise-jaise Earth window mein chhoti hoti gayi, "+C+" ne billions of twinkling stars ko dekha. Ship ke friendly AI assistant Astro ne announce kiya: \"Captain "+C+", maine teen possible locations detect ki hain!\"");
        put(spc,"opening","bn","3... 2... 1... উড্ডয়ন! তরুণ মহাকাশচারী "+C+" চমৎকার মহাকাশযান 'কসমিক ওয়ান্ডার'-এ চড়ে মহাকাশে ছুটে গেল! মিশন: কিংবদন্তি বন্ধুত্বের তারা খুঁজে বের করা যেটা গ্যালাক্সির সব গ্রহকে জুড়ে দিতে পারে। পৃথিবী জানালায় ছোট হতে থাকলে, "+C+" কোটি কোটি মিটমিটে তারার দিকে তাকাল। জাহাজের বন্ধুবৎসল এআই সহকারী অ্যাস্ট্রো ঘোষণা করল: \"ক্যাপ্টেন "+C+", আমি তিনটে সম্ভাব্য অবস্থান খুঁজে পেয়েছি!\"");
        put(spc,"middle_a","en","Captain "+C+" steered toward the Candy Planet, a world made entirely of sweets! Mountains of chocolate, rivers of caramel, and clouds of cotton candy surrounded the ship. The Candy Planet's friendly aliens, the Sweetlings, were round and colorful like gumdrops. Their leader, Chief Lollipop, told "+C+": \"The Star of Friendship passed through here! It left a trail of sparkles heading toward the center of our planet!\" "+C+" followed the glittering trail deep underground!");
        put(spc,"middle_a","hi","कैप्टन "+C+" ने कैंडी ग्रह की ओर उड़ान भरी, एक ऐसी दुनिया जो पूरी तरह मिठाइयों से बनी थी! चॉकलेट के पहाड़, कारमेल की नदियाँ, और रुई की मिठाई के बादल! कैंडी ग्रह के दोस्ताना एलियन, स्वीटलिंग्स, गोल और रंगीन थे। उनके नेता ने "+C+" को बताया: \"दोस्ती का तारा यहाँ से गुजरा था!\" "+C+" ने चमकते निशानों का पीछा किया!");
        put(spc,"middle_b","en","Captain "+C+" headed for the Music Moon, where everything made beautiful sounds! Craters played like drums, geysers whistled melodies, and the ground hummed gentle lullabies. The Moon Musicians, beings made of pure sound waves, greeted "+C+" with a symphony. \"The Star of Friendship loves music!\" they sang. \"Play the most beautiful song from your heart, and it will appear!\" "+C+" closed their eyes and hummed their favorite melody!");
        put(spc,"middle_b","hi","कैप्टन "+C+" संगीत चंद्रमा की ओर गया, जहाँ हर चीज़ सुंदर आवाज़ें बनाती थी! गड्ढे ढोल जैसे बजते थे और ज़मीन लोरियाँ गुनगुनाती थी। संगीत प्राणियों ने "+C+" का स्वागत किया। \"दोस्ती का तारा संगीत से प्यार करता है!\" उन्होंने गाया। \"अपने दिल से सबसे सुंदर गीत गाओ!\" "+C+" ने आँखें बंद कीं और अपनी पसंदीदा धुन गुनगुनाई!");
        put(spc,"middle_c","en","Captain "+C+" flew to the Rainbow Nebula, the most colorful place in the entire universe! Swirling clouds of every color imaginable danced around the ship. Inside the nebula, "+C+" found a floating garden tended by Star Sprites — tiny glowing beings who cultivated cosmic flowers. \"The Star of Friendship grows here, like a flower!\" the eldest Sprite explained. \"But it only blooms when someone tells it why friendship matters!\"");
        put(spc,"middle_c","hi","कैप्टन "+C+" इंद्रधनुष नेबुला की ओर उड़ा, ब्रह्मांड का सबसे रंगीन स्थान! हर रंग के घूमते बादल यान के चारों ओर नाचते रहे। नेबुला के अंदर, "+C+" को तारा स्प्राइट्स का एक तैरता बगीचा मिला। \"दोस्ती का तारा यहाँ एक फूल की तरह उगता है!\" सबसे बुजुर्ग स्प्राइट ने समझाया। \"लेकिन यह तभी खिलता है जब कोई बताए कि दोस्ती क्यों मायने रखती है!\"");
        put(spc,"ending_a","en",""+C+" held the brilliant Star of Friendship high above their head! It burst into millions of tiny stars that zoomed to every planet in the galaxy, creating a sparkling network connecting all worlds. Every alien species could now talk to each other and share their wonderful cultures! "+C+" became known as the Galaxy's Greatest Friend-Maker. Back on Earth, "+C+" could look up at the sky every night and see the twinkling network of friendship spanning the stars! The End! 🚀⭐🌌");
        put(spc,"ending_a","hi",""+C+" ने चमकते दोस्ती के तारे को ऊपर उठाया! यह लाखों छोटे तारों में बदल गया जो हर ग्रह तक पहुँचे, सभी दुनियाओं को जोड़ने वाला एक चमकता नेटवर्क बनाते हुए! "+C+" पूरी आकाशगंगा का सबसे बड़ा दोस्त-निर्माता बन गया। पृथ्वी पर वापस, "+C+" हर रात आसमान में दोस्ती का टिमटिमाता नेटवर्क देख सकता था! समाप्त! 🚀⭐🌌");
        put(spc,"ending_b","en",""+C+" realized that the Star of Friendship was not just one star — it was inside every heart that cared about others! "+C+" taught all the aliens this beautiful truth, and soon every planet created their own tradition of kindness. The Galaxy Friendship Festival became the biggest celebration in the universe, where beings from every world gathered to share stories, food, and laughter. Captain "+C+" was awarded the Golden Constellation Medal for uniting the galaxy through love! The End! 💫🌍🤝");
        put(spc,"ending_b","hi",""+C+" ने महसूस किया कि दोस्ती का तारा सिर्फ एक तारा नहीं — यह हर दयालु दिल में था! "+C+" ने सभी एलियंस को यह सुंदर सच सिखाया। गैलेक्सी फ्रेंडशिप फेस्टिवल ब्रह्मांड का सबसे बड़ा उत्सव बन गया! कैप्टन "+C+" को गोल्डन कॉन्स्टेलेशन मेडल से सम्मानित किया गया! समाप्त! 💫🌍🤝");
        put(spc,"middle_a","hinglish","Captain "+C+" ne Candy Planet ki taraf steering kiya, ek aisi duniya jo poori tarah sweets se bani thi! Chocolate ke mountains, caramel ki rivers, aur cotton candy ke clouds ne ship ko ghera! Candy Planet ke friendly aliens, Sweetlings, gumdrops jaisi gol aur colorful thin. Unke leader Chief Lollipop ne "+C+" ko bataya: \"Star of Friendship yahan se guzra tha! Isne sparkles ki trail chhodi hai jo planet ke center ki taraf ja rahi hai!\" "+C+" ne glittering trail follow ki deep underground!");
        put(spc,"middle_a","bn","ক্যাপ্টেন "+C+" ক্যান্ডি গ্রহের দিকে এগিয়ে গেল, এমন এক জগৎ যেটা পুরোটাই মিষ্টি দিয়ে তৈরি! চকলেটের পাহাড়, ক্যারামেলের নদী, আর তুলোর মিছরির মেঘ জাহাজকে ঘিরে ধরল! ক্যান্ডি গ্রহের বন্ধুবৎসল এলিয়েন সুইটলিংরা গামড্রপের মতো গোল আর রঙিন ছিল। তাদের নেতা চিফ ললিপপ "+C+" কে বলল: \"বন্ধুত্বের তারা এখান দিয়ে গেছে! এটা আমাদের গ্রহের কেন্দ্রে চকচকে পথ রেখে গেছে!\" "+C+" ঝলমলে পথ ধরে মাটির গভীরে নেমে গেল!");
        put(spc,"middle_b","hinglish","Captain "+C+" Music Moon ki taraf gaya, jahan har cheez beautiful sounds banati thi! Craters drums jaisi bajte the, geysers melodies whistle karte the, aur ground gentle lullabies hum karti thi. Moon Musicians ne "+C+" ka symphony ke saath welcome kiya. \"Star of Friendship ko music bahut pasand hai!\" unhone gaya. \"Apne dil se sabse beautiful song play karo, aur yeh appear ho jayega!\" "+C+" ne eyes band karke apni favorite melody hum ki!");
        put(spc,"middle_b","bn","ক্যাপ্টেন "+C+" মিউজিক মুনের দিকে গেল, যেখানে সবকিছু সুন্দর শব্দ করত! গর্তগুলো ড্রামের মতো বাজত, গিজার সুর শিস দিত, আর মাটি মৃদু ঘুমপাড়ানি গান গুনগুন করত। মুন মিউজিশিয়ানরা "+C+" কে সিম্ফনি দিয়ে অভ্যর্থনা জানাল। \"বন্ধুত্বের তারা সঙ্গীত ভালোবাসে!\" তারা গেয়ে উঠল। \"তোমার হৃদয় থেকে সবচেয়ে সুন্দর গান বাজাও!\" "+C+" চোখ বন্ধ করে প্রিয় সুর গুনগুন করল!");
        put(spc,"middle_c","hinglish","Captain "+C+" Rainbow Nebula ki taraf fly kiya, poore universe ki sabse colorful jagah! Har imaginable color ke swirling clouds ship ke around dance kar rahe the. Nebula ke andar, "+C+" ko Star Sprites ka floating garden mila — tiny glowing beings jo cosmic flowers grow karti thin. \"Star of Friendship yahan ek flower ki tarah ugta hai!\" sabse eldest Sprite ne explain kiya. \"Par yeh tabhi bloom hota hai jab koi bataye ki friendship kyun matter karti hai!\"");
        put(spc,"middle_c","bn","ক্যাপ্টেন "+C+" রেইনবো নেবুলার দিকে উড়ে গেল, পুরো মহাবিশ্বের সবচেয়ে রঙিন জায়গা! প্রতিটা রঙের ঘূর্ণায়মান মেঘ জাহাজের চারপাশে নাচছিল। নেবুলার ভেতরে, "+C+" একটা ভাসমান বাগান পেল যেটা স্টার স্প্রাইটরা দেখাশোনা করত। \"বন্ধুত্বের তারা এখানে ফুলের মতো জন্মায়!\" সবচেয়ে বয়স্ক স্প্রাইট বলল। \"কিন্তু এটা তখনই ফোটে যখন কেউ বলে বন্ধুত্ব কেন গুরুত্বপূর্ণ!\"");
        put(spc,"ending_a","hinglish",""+C+" ne brilliant Star of Friendship ko apne head ke upar high hold kiya! Yeh millions of tiny stars mein burst ho gaya jo galaxy ke har planet tak zoom kar gaye, sabhi worlds ko connect karne wala ek sparkling network banaate hue! Har alien species ab ek doosre se baat kar sakti thi! "+C+" ko Galaxy's Greatest Friend-Maker ke naam se jaana gaya. Earth pe wapas, "+C+" har raat sky mein friendship ka twinkling network dekh sakta tha! The End! 🚀⭐🌌");
        put(spc,"ending_a","bn",""+C+" উজ্জ্বল বন্ধুত্বের তারাকে মাথার ওপরে উঁচু করে ধরল! এটা লক্ষ লক্ষ ছোট্ট তারায় ভেঙে গ্যালাক্সির প্রতিটা গ্রহে ছুটে গেল, সমস্ত জগৎকে জুড়ে দেওয়া একটা ঝলমলে নেটওয়ার্ক তৈরি করল! "+C+" কে গ্যালাক্সির শ্রেষ্ঠ বন্ধু-নির্মাতা হিসেবে জানা গেল। পৃথিবীতে ফিরে, "+C+" প্রতি রাতে আকাশে বন্ধুত্বের মিটমিটে নেটওয়ার্ক দেখতে পেত! শেষ! 🚀⭐🌌");
        put(spc,"ending_b","hinglish",""+C+" ne realize kiya ki Star of Friendship sirf ek star nahi tha — yeh har us dil ke andar tha jo doosron ki care karta tha! "+C+" ne sabhi aliens ko yeh beautiful truth sikhaya. Galaxy Friendship Festival universe ka sabse bada celebration ban gaya! Captain "+C+" ko Golden Constellation Medal se award kiya gaya! The End! 💫🌍🤝");
        put(spc,"ending_b","bn",""+C+" বুঝতে পারল যে বন্ধুত্বের তারা শুধু একটা তারা নয় — এটা ছিল প্রতিটা হৃদয়ের মধ্যে যেটা অন্যদের যত্ন নেয়! "+C+" সমস্ত এলিয়েনদের এই সুন্দর সত্য শেখাল। গ্যালাক্সি ফ্রেন্ডশিপ ফেস্টিভ্যাল মহাবিশ্বের সবচেয়ে বড় উৎসব হয়ে গেল! ক্যাপ্টেন "+C+" কে গোল্ডেন কনস্টেলেশন মেডাল দেওয়া হল! শেষ! 💫🌍🤝");
        STORIES.put("space", spc);
        Map<String, String> spcT = new HashMap<>();
        spcT.put("en", "Captain "+C+"'s Space Voyage"); spcT.put("hi", "कैप्टन "+C+" की अंतरिक्ष यात्रा");
        spcT.put("hinglish", "Captain "+C+" ki Space Voyage"); spcT.put("bn", "ক্যাপ্টেন "+C+" এর মহাকাশ যাত্রা");
        TITLES.put("space", spcT);
        Map<String, List<StoryChoice>> spcC = new HashMap<>();
        spcC.put("opening_en", Arrays.asList(new StoryChoice("a","Fly to the Candy Planet","🍬"),new StoryChoice("b","Head for the Music Moon","🎵"),new StoryChoice("c","Explore the Rainbow Nebula","🌈")));
        spcC.put("opening_hi", Arrays.asList(new StoryChoice("a","कैंडी ग्रह पर उड़ान भरो","🍬"),new StoryChoice("b","संगीत चंद्रमा की ओर जाओ","🎵"),new StoryChoice("c","इंद्रधनुष नेबुला की खोज करो","🌈")));
        spcC.put("opening_hinglish", Arrays.asList(new StoryChoice("a","Candy Planet pe fly karo","🍬"),new StoryChoice("b","Music Moon ki taraf jao","🎵"),new StoryChoice("c","Rainbow Nebula explore karo","🌈")));
        spcC.put("opening_bn", Arrays.asList(new StoryChoice("a","ক্যান্ডি গ্রহে ওড়ো","🍬"),new StoryChoice("b","মিউজিক মুনে যাও","🎵"),new StoryChoice("c","রেইনবো নেবুলা অন্বেষণ করো","🌈")));
        spcC.put("middle_en", Arrays.asList(new StoryChoice("a","Use the Star to connect all planets","⭐"),new StoryChoice("b","Teach everyone that friendship lives in every heart","💖")));
        spcC.put("middle_hi", Arrays.asList(new StoryChoice("a","तारे से सभी ग्रहों को जोड़ो","⭐"),new StoryChoice("b","सबको सिखाओ कि दोस्ती हर दिल में है","💖")));
        spcC.put("middle_hinglish", Arrays.asList(new StoryChoice("a","Star se saare planets connect karo","⭐"),new StoryChoice("b","Sabko sikhao ki dosti har dil mein hai","💖")));
        spcC.put("middle_bn", Arrays.asList(new StoryChoice("a","তারা দিয়ে সব গ্রহ জুড়ে দাও","⭐"),new StoryChoice("b","সবাইকে শেখাও যে বন্ধুত্ব প্রতিটা হৃদয়ে আছে","💖")));
        CHOICES.put("space", spcC);

        // ANIMALS - reuses adventure structure with animal theme
        copyThemeWithOverride4("animals", "adventure",
            "In a beautiful meadow where sunflowers grew as tall as houses, "+C+" discovered they could talk to animals! A wise tortoise, a playful monkey, and a brave little mouse asked "+C+" for help — their magical friendship tree was losing its leaves!",
            "एक खूबसूरत मैदान में जहाँ सूरजमुखी घरों जितने ऊँचे उगते थे, "+C+" ने पाया कि वह जानवरों से बात कर सकता है! एक बुद्धिमान कछुआ, एक चंचल बंदर, और एक बहादुर छोटा चूहा ने "+C+" से मदद माँगी — उनका जादुई दोस्ती का पेड़ अपने पत्ते खो रहा था!",
            "Ek beautiful meadow mein jahan sunflowers gharon jitne tall the, "+C+" ne discover kiya ki woh animals se baat kar sakta hai! Ek wise tortoise, ek playful monkey, aur ek brave chhoti mouse ne "+C+" se help maangi — unka magical friendship tree apne leaves kho raha tha!",
            "এক সুন্দর মাঠে যেখানে সূর্যমুখী বাড়ির মতো লম্বা, "+C+" আবিষ্কার করল যে সে পশুপাখির সাথে কথা বলতে পারে! একটা জ্ঞানী কচ্ছপ, একটা চঞ্চল বাঁদর, আর একটা সাহসী ছোট্ট ইঁদুর "+C+" এর কাছে সাহায্য চাইল — তাদের জাদুকরী বন্ধুত্বের গাছ পাতা হারাচ্ছিল!",
            C+"'s Animal Friends", C+" के जानवर दोस्त", C+" ke Animal Friends", C+" এর পশু বন্ধুরা",
            new String[]{"🐢 Help the wise tortoise first","🐒 Play with the monkey to find clues","🐭 Follow the brave mouse underground"},
            new String[]{"🐢 पहले बुद्धिमान कछुए की मदद करो","🐒 सुराग खोजने के लिए बंदर के साथ खेलो","🐭 बहादुर चूहे के साथ ज़मीन के नीचे जाओ"},
            new String[]{"🐢 Pehle wise tortoise ki help karo","🐒 Monkey ke saath khelo clues dhundne ke liye","🐭 Brave mouse ke saath underground jao"},
            new String[]{"🐢 আগে জ্ঞানী কচ্ছপকে সাহায্য করো","🐒 সূত্র খুঁজতে বাঁদরের সাথে খেলো","🐭 সাহসী ইঁদুরের সাথে মাটির নিচে যাও"});

        // FRIENDSHIP
        copyThemeWithOverride4("friendship", "adventure",
            "It was "+C+"'s first day at a brand new school! Everything felt strange and different. But then "+C+" noticed three other new students who also looked nervous. "+C+" had an idea — what if they all became friends and explored the school together?",
            "आज "+C+" का एक बिल्कुल नए स्कूल में पहला दिन था! सब कुछ अजीब और अलग लगा। लेकिन फिर "+C+" ने तीन और नए छात्रों को देखा जो भी घबराए हुए लग रहे थे। "+C+" के मन में एक विचार आया — क्या होगा अगर वे सब दोस्त बन जाएँ?",
            "Aaj "+C+" ka ek brand new school mein pehla din tha! Sab kuch strange aur different lag raha tha. Par phir "+C+" ne teen aur naye students dekhe jo bhi nervous lag rahe the. "+C+" ke mann mein ek idea aaya — kya hoga agar sab friends ban jaayein aur school ko saath mein explore karein?",
            "আজ "+C+" এর একদম নতুন স্কুলে প্রথম দিন! সবকিছু অচেনা আর আলাদা লাগছিল। কিন্তু তখন "+C+" আরো তিনজন নতুন ছাত্রকে দেখল যারাও নার্ভাস দেখাচ্ছিল। "+C+" এর একটা আইডিয়া এল — যদি ওরা সবাই বন্ধু হয়ে যায় আর একসাথে স্কুল ঘুরে দেখে?",
            C+"'s Friendship Story", C+" की दोस्ती की कहानी", C+" ki Friendship Story", C+" এর বন্ধুত্বের গল্প",
            new String[]{"🎨 Invite the shy artist to draw together","📚 Help the bookworm find the library","⚽ Ask the sporty kid to play"},
            new String[]{"🎨 शर्मीले कलाकार को साथ में चित्र बनाने के लिए बुलाओ","📚 किताबी कीड़े को पुस्तकालय खोजने में मदद करो","⚽ खिलाड़ी बच्चे को खेलने के लिए कहो"},
            new String[]{"🎨 Shy artist ko saath mein draw karne ke liye invite karo","📚 Bookworm ko library dhundne mein help karo","⚽ Sporty kid ko khelne ke liye bolo"},
            new String[]{"🎨 লাজুক শিল্পীকে একসাথে ছবি আঁকতে ডাকো","📚 বইপোকাকে লাইব্রেরি খুঁজতে সাহায্য করো","⚽ ক্রীড়াবিদ বন্ধুকে খেলতে বলো"});

        // MYSTERY
        copyThemeWithOverride4("mystery", "adventure",
            "Detective "+C+" received a mysterious letter with golden sparkles! The letter said: 'The Rainbow Crystal has disappeared from the town museum! Only the cleverest detective can find it.' "+C+" put on their detective hat and magnifying glass. Three clues were found at the scene!",
            "जासूस "+C+" को सुनहरी चमक वाला एक रहस्यमय पत्र मिला! पत्र में लिखा था: 'इंद्रधनुषी क्रिस्टल शहर के संग्रहालय से गायब हो गया है! केवल सबसे होशियार जासूस इसे ढूंढ सकता है।' "+C+" ने अपनी जासूसी टोपी और आवर्धक कांच पहन लिया। मौके पर तीन सुराग मिले!",
            "Detective "+C+" ko golden sparkles wala ek mysterious letter mila! Letter mein likha tha: 'Rainbow Crystal town ke museum se gayab ho gaya hai! Sirf sabse clever detective hi ise dhoondh sakta hai.' "+C+" ne apni detective hat aur magnifying glass pehan li. Scene pe teen clues mile!",
            "গোয়েন্দা "+C+" সোনালী চকচকে একটা রহস্যময় চিঠি পেল! চিঠিতে লেখা ছিল: 'রেইনবো ক্রিস্টাল শহরের জাদুঘর থেকে উধাও হয়ে গেছে! শুধু সবচেয়ে চতুর গোয়েন্দাই এটা খুঁজে বের করতে পারবে।' "+C+" গোয়েন্দা টুপি আর আতশকাচ পরে নিল। ঘটনাস্থলে তিনটে সূত্র পাওয়া গেল!",
            "Detective "+C+"'s Mystery", "जासूस "+C+" का रहस्य", "Detective "+C+" ka Mystery", "গোয়েন্দা "+C+" এর রহস্য",
            new String[]{"🔍 Follow the trail of glitter","🗝️ Investigate the locked room","👣 Track the mysterious footprints"},
            new String[]{"🔍 चमक के निशानों का पीछा करो","🗝️ बंद कमरे की जाँच करो","👣 रहस्यमय पैरों के निशानों का पता लगाओ"},
            new String[]{"🔍 Glitter ki trail follow karo","🗝️ Locked room investigate karo","👣 Mysterious footprints track karo"},
            new String[]{"🔍 চকমকির পথ অনুসরণ করো","🗝️ তালাবন্ধ ঘর তদন্ত করো","👣 রহস্যময় পায়ের ছাপ খুঁজে বের করো"});
    }

    private static void put(Map<String, Map<String, String>> theme, String segment, String lang, String text) {
        theme.computeIfAbsent(segment, k -> new HashMap<>()).put(lang, text);
    }


    private static void copyThemeWithOverride4(String newTheme, String baseTheme,
            String openingEn, String openingHi, String openingHinglish, String openingBn,
            String titleEn, String titleHi, String titleHinglish, String titleBn,
            String[] choicesEn, String[] choicesHi, String[] choicesHinglish, String[] choicesBn) {
        Map<String, Map<String, String>> base = STORIES.get(baseTheme);
        Map<String, Map<String, String>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> e : base.entrySet()) {
            copy.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        copy.get("opening").put("en", openingEn);
        copy.get("opening").put("hi", openingHi);
        if (openingHinglish != null) copy.get("opening").put("hinglish", openingHinglish);
        if (openingBn != null) copy.get("opening").put("bn", openingBn);
        STORIES.put(newTheme, copy);

        Map<String, String> titles = new HashMap<>();
        titles.put("en", titleEn); titles.put("hi", titleHi);
        if (titleHinglish != null) titles.put("hinglish", titleHinglish);
        if (titleBn != null) titles.put("bn", titleBn);
        TITLES.put(newTheme, titles);

        Map<String, List<StoryChoice>> choices = new HashMap<>();
        String[] ids = {"a","b","c"};
        choices.put("opening_en", parseChoices(choicesEn, ids));
        choices.put("opening_hi", parseChoices(choicesHi, ids));
        if (choicesHinglish != null) choices.put("opening_hinglish", parseChoices(choicesHinglish, ids));
        if (choicesBn != null) choices.put("opening_bn", parseChoices(choicesBn, ids));
        choices.put("middle_en", CHOICES.get(baseTheme).get("middle_en"));
        choices.put("middle_hi", CHOICES.get(baseTheme).get("middle_hi"));
        if (CHOICES.get(baseTheme).containsKey("middle_hinglish")) choices.put("middle_hinglish", CHOICES.get(baseTheme).get("middle_hinglish"));
        if (CHOICES.get(baseTheme).containsKey("middle_bn")) choices.put("middle_bn", CHOICES.get(baseTheme).get("middle_bn"));
        CHOICES.put(newTheme, choices);
    }

    private static List<StoryChoice> parseChoices(String[] raw, String[] ids) {
        List<StoryChoice> list = new ArrayList<>();
        for (int i = 0; i < raw.length && i < 3; i++) {
            String emoji = raw[i].substring(0, raw[i].indexOf(' '));
            list.add(new StoryChoice(ids[i], raw[i].substring(raw[i].indexOf(' ')+1), emoji));
        }
        return list;
    }
}
