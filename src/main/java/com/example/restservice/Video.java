package com.example.restservice;

public class Video {
    public String name;
    public int length;
    public String nickName;
    public String videoPath;
    public String coverPath;
    public String description;

    Video (String _name, String _length, String _nickname, String _description, String _videoPath, String _coverPath) {
        name = _name;
        length = Integer.valueOf(_length);
        nickName = _nickname;
        description = _description;
        videoPath = _videoPath;
        coverPath = _coverPath;
    }

    @Override
    public String toString() {
        return "Video [name=" + name
                + ", length=" + length
                + ", nickname=" + nickName
                + ", description=" + description
                + ", videoPath=" + videoPath
                + ", coverPath=" + coverPath
                + "]";
    }
}