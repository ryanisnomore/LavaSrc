package com.github.topi314.lavasrc.jiosaavn;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public class JioSaavnAudioPlaylist implements AudioPlaylist {
    
    private final String name;
    private final List<AudioTrack> tracks;
    private final boolean isAlbum;
    
    public JioSaavnAudioPlaylist(String name, List<AudioTrack> tracks, boolean isAlbum) {
        this.name = name;
        this.tracks = tracks;
        this.isAlbum = isAlbum;
    }
    
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public List<AudioTrack> getTracks() {
        return this.tracks;
    }
    
    @Override
    public AudioTrack getSelectedTrack() {
        return null;
    }
    
    @Override
    public boolean isSearchResult() {
        return false;
    }
}
