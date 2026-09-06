package com.resumeai.service.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.scoring.weights")
public class ScoringWeights {

    private int atsParsing;
    private int keywordAlignment;
    private int structure;
    private int contentQuality;
    private int experienceRelevance;
    private int impactQuality;
    private int formatting;

    public int getAtsParsing() { return atsParsing; }
    public void setAtsParsing(int v) { this.atsParsing = v; }
    public int getKeywordAlignment() { return keywordAlignment; }
    public void setKeywordAlignment(int v) { this.keywordAlignment = v; }
    public int getStructure() { return structure; }
    public void setStructure(int v) { this.structure = v; }
    public int getContentQuality() { return contentQuality; }
    public void setContentQuality(int v) { this.contentQuality = v; }
    public int getExperienceRelevance() { return experienceRelevance; }
    public void setExperienceRelevance(int v) { this.experienceRelevance = v; }
    public int getImpactQuality() { return impactQuality; }
    public void setImpactQuality(int v) { this.impactQuality = v; }
    public int getFormatting() { return formatting; }
    public void setFormatting(int v) { this.formatting = v; }

    public int total() {
        return atsParsing + keywordAlignment + structure + contentQuality
                + experienceRelevance + impactQuality + formatting;
    }
}

