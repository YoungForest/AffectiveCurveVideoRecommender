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

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
// If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Video)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        Video c = (Video) o;

        // Compare the data members and return accordingly
        return c.name.equals(this.name);
    }
}