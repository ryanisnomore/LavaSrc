package com.github.topi314.lavasrc.plugin.config;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "jiosaavn")
@Component
public class JioSaavnConfig {
    private String apiURL = "https://ryan-jiosaavn.vercel.app/api";
    private int playlistTrackLimit = 50;
    private int recommendationsTrackLimit = 10;

    public String getApiURL() {
        return apiURL;
    }

    public void setApiURL(String apiURL) {
        this.apiURL = apiURL;
    }

    public int getPlaylistTrackLimit() {
        return playlistTrackLimit;
    }

    public void setPlaylistTrackLimit(int playlistTrackLimit) {
        this.playlistTrackLimit = playlistTrackLimit;
    }

    public int getRecommendationsTrackLimit() {
        return recommendationsTrackLimit;
    }

    public void setRecommendationsTrackLimit(int recommendationsTrackLimit) {
        this.recommendationsTrackLimit = recommendationsTrackLimit;
    }
}
