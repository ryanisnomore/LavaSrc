package com.github.topi314.lavasrc.jiosaavn;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JioSaavnAudioTrack extends DelegatedAudioTrack {
    
    private static final Logger log = LoggerFactory.getLogger(JioSaavnAudioTrack.class);
    
    private final JioSaavnAudioSourceManager sourceManager;
    
    public JioSaavnAudioTrack(JioSaavnAudioSourceManager sourceManager, JsonBrowser json) {
        this(createTrackInfo(json), sourceManager);
    }
    
    public JioSaavnAudioTrack(AudioTrackInfo trackInfo, JioSaavnAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }
    
    private static AudioTrackInfo createTrackInfo(JsonBrowser json) {
        return new AudioTrackInfo(
            json.get("name").text(),
            json.get("primary_artists").text(),
            json.get("duration").as(Long.class) * 1000L,
            json.get("id").text(),
            false,
            json.get("url").text()
        );
    }
    
    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        try (HttpInterface httpInterface = this.sourceManager.getHttpInterfaceManager().getInterface()) {
            String mediaUrl = this.sourceManager.getMediaUrl(this.getIdentifier());
            
            if (mediaUrl == null) {
                throw new FriendlyException("Failed to get media URL", FriendlyException.Severity.SUSPICIOUS, null);
            }
            
            log.debug("Starting JioSaavn track from URL: {}", mediaUrl);
            
            try (var stream = HttpClientTools.fetchResponse(httpInterface, new HttpGet(mediaUrl))) {
                processDelegate(executor, stream);
            }
        }
    }
    
    @Override
    public AudioSourceManager getSourceManager() {
        return this.sourceManager;
    }
    
    @Override
    protected AudioTrack makeShallowClone() {
        return new JioSaavnAudioTrack(this.trackInfo, this.sourceManager);
    }
}
