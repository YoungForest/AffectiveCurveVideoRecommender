package com.example.restservice;

public class Video {
    private String name;
    private String nickName;
    private String videoPath;
    private String coverPath;
    private String description;

    Video (String _name, String _nickname, String _description, String _videoPath, String _coverPath) {
        name = _name;
        nickName = _nickname;
        description = _description;
        videoPath = _videoPath;
        coverPath = _coverPath;
    }

    @Override
    public String toString() {
        return "Video [name=" + name
                + ", nickname=" + nickName
                + ", description=" + description
                + ", videoPath=" + videoPath
                + ", coverPath=" + coverPath
                + "]";
    }
}