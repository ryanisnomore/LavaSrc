package com.github.topi314.lavasrc.jiosaavn;

import com.github.topi314.lavasrc.plugin.config.JioSaavnConfig;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class JioSaavnAudioSourceManager implements AudioSourceManager, HttpConfigurable {
    
    private static final Logger log = LoggerFactory.getLogger(JioSaavnAudioSourceManager.class);
    
    private static final String SEARCH_PREFIX = "jssearch:";
    private static final String URL_PREFIX = "jiosaavn:";
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(www\\.)?jiosaavn\\.com/(song|album|playlist)/[a-zA-Z0-9-_]+(/[a-zA-Z0-9-_]+)?/?");
    
    private final HttpInterfaceManager httpInterfaceManager;
    private final JioSaavnConfig config;
    
    public JioSaavnAudioSourceManager(JioSaavnConfig config) {
        this.config = config;
        this.httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    }
    
    @Override
    public String getSourceName() {
        return "jiosaavn";
    }
    
    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                return this.search(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
            }
            
            if (reference.identifier.startsWith(URL_PREFIX)) {
                String url = reference.identifier.substring(URL_PREFIX.length());
                return this.getAudioItemFromUrl(url);
            }
            
            if (URL_PATTERN.matcher(reference.identifier).matches()) {
                return this.getAudioItemFromUrl(reference.identifier);
            }
            
            return null;
        } catch (IOException e) {
            throw new FriendlyException("Failed to load JioSaavn item", FriendlyException.Severity.SUSPICIOUS, e);
        }
    }
    
    private AudioItem getAudioItemFromUrl(String url) throws IOException {
        if (url.contains("/song/")) {
            return this.getTrack(url);
        } else if (url.contains("/album/") || url.contains("/playlist/")) {
            return this.getPlaylist(url);
        }
        return null;
    }
    
    private AudioTrack getTrack(String url) throws IOException {
        String apiUrl = String.format("%s/song?id=%s", 
            this.config.getApiURL(), 
            URLEncoder.encode(url, StandardCharsets.UTF_8)
        );
        
        JsonBrowser json = this.getJson(apiUrl);
        if (json == null || json.isNull()) {
            return null;
        }
        
        return new JioSaavnAudioTrack(this, json);
    }
    
    private AudioPlaylist getPlaylist(String url) throws IOException {
        boolean isAlbum = url.contains("/album/");
        String type = isAlbum ? "album" : "playlist";
        
        String apiUrl = String.format("%s/%s?id=%s&limit=%d", 
            this.config.getApiURL(), 
            type,
            URLEncoder.encode(url, StandardCharsets.UTF_8),
            isAlbum ? Integer.MAX_VALUE : this.config.getPlaylistTrackLimit()
        );
        
        JsonBrowser json = this.getJson(apiUrl);
        if (json == null || json.isNull()) {
            return null;
        }
        
        String name = json.get("name").text();
        List<AudioTrack> tracks = new ArrayList<>();
        
        JsonBrowser songs = json.get("songs");
        if (!songs.isNull()) {
            for (JsonBrowser song : songs.values()) {
                tracks.add(new JioSaavnAudioTrack(this, song));
            }
        }
        
        return new JioSaavnAudioPlaylist(name, tracks, isAlbum);
    }
    
    private AudioItem search(String query) throws IOException {
        String apiUrl = String.format("%s/search?query=%s", 
            this.config.getApiURL(), 
            URLEncoder.encode(query, StandardCharsets.UTF_8)
        );
        
        JsonBrowser json = this.getJson(apiUrl);
        if (json == null || json.isNull()) {
            return AudioReference.NO_TRACK;
        }
        
        List<AudioTrack> tracks = new ArrayList<>();
        JsonBrowser results = json.get("results");
        
        if (!results.isNull()) {
            for (JsonBrowser result : results.values()) {
                tracks.add(new JioSaavnAudioTrack(this, result));
            }
        }
        
        return new BasicAudioPlaylist("Search results for: " + query, tracks, null, true);
    }
    
    public JsonBrowser getJson(String url) throws IOException {
        HttpInterface httpInterface = this.httpInterfaceManager.getInterface();
        HttpGet request = new HttpGet(url);
        
        return HttpClientTools.fetchResponseAsJson(httpInterface, request);
    }
    
    public String getMediaUrl(String id) throws IOException {
        String apiUrl = String.format("%s/song?id=%s", 
            this.config.getApiURL(), 
            URLEncoder.encode(id, StandardCharsets.UTF_8)
        );
        
        JsonBrowser json = this.getJson(apiUrl);
        if (json == null || json.isNull()) {
            return null;
        }
        
        return json.get("media_url").text();
    }
    
    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }
    
    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
        // No special encoding needed
    }
    
    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new JioSaavnAudioTrack(trackInfo, this);
    }
    
    @Override
    public void shutdown() {
        // Clean up resources if needed
    }
    
    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        this.httpInterfaceManager.configureRequests(configurator);
    }
    
    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        this.httpInterfaceManager.configureBuilder(configurator);
    }
    
    public JioSaavnConfig getConfig() {
        return config;
    }
}
