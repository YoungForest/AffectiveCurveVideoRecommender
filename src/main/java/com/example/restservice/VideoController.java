package com.example.restservice;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.FileSystemResource;
import java.io.File;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class VideoController {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	Gson gson;
	Logger logger = LoggerFactory.getLogger(VideoController.class);
	VideoController() {
		gson = new Gson();
	}

	List<Video> getVideos() {
		  /*
        CREATE VIEW video_view AS select * from shortpath left join videos on shortpath.name=videos.video_path
        video_view(name,length,id,created_at,updated_at,deleted_at,aweme_id,author_id,nickname,avatar,"desc",digg_count,comment_count,cover_path,video_path,share_url,is_download,status)'
         */

		List<Video> videos = jdbcTemplate.query("SELECT name, length, nickname, desc, video_path, cover_path FROM video_view",
				(resultSet, rowNum) ->
				{
					return new Video(resultSet.getString("name"),
							resultSet.getString("length"),
							resultSet.getString("nickname"),
							resultSet.getString("desc"),
							resultSet.getString("video_path"),
							resultSet.getString("cover_path")
					);
				});
		return  videos;
	}

	Map<String, Video> getVideoMap() {
		List<Video> videoList = getVideos();
		Map<String, Video> ans = new HashMap<>();
		for (Video v : videoList) {
			ans.put(v.name, v);
		}
		return ans;
	}

	@GetMapping("/video")
	public ResponseEntity<FileSystemResource> getVideo(@RequestParam(value = "id", defaultValue = "NoneId") String id, @RequestParam(value = "name") String name) {
		final String videoRoot = "/Users/yngsen/workspace/bishe-spider/douyin/";
		File file = new File(videoRoot + name);
		FileSystemResource fileResource = new FileSystemResource(file);
		String mimeType;
		if (name.contains(".mp4")) {
			mimeType = "video/mp4";
		} else if (name.contains(".jpeg")) {
			mimeType = "image/jpg";
		} else {
			mimeType = "application/octet-stream";
		}
		logger.info("getVideo " + id + " " + getVideoMap().get(name));
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename="+name)
				.contentType(MediaType.valueOf(mimeType)).body(fileResource);
	}

	@GetMapping("/cover")
	public ResponseEntity<FileSystemResource> getCover(@RequestParam(value = "id", defaultValue = "NoneId") String id, @RequestParam(value = "name") String name) {
		final String videoRoot = "/Users/yngsen/workspace/bishe-spider/douyin/";
		name = name.replace(".mp4", ".mp4.jpeg");
		File file = new File(videoRoot + name);
		FileSystemResource fileResource = new FileSystemResource(file);
		String mimeType = "image/jpg";
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename="+name)
				.contentType(MediaType.valueOf(mimeType)).body(fileResource);
	}

	@GetMapping("/recommendvideo")
	public String getRecommendVideo(@RequestParam(value = "id", defaultValue = "NoneId") String id, @RequestParam(value = "limits", defaultValue = "15" ) String limits) {
		List<Video> ret = getRandomElement(getVideos(), Integer.valueOf(limits));
		logger.info("getRecommendVideo " + id + " " + ret);
		return gson.toJson(ret);
	}

	@GetMapping("/videoinfo")
	public String getVideoInfo(@RequestParam(value = "id", defaultValue = "NoneId") String id, @RequestParam(value = "name", defaultValue = "" ) String name) {
		return gson.toJson(getVideos());
	}

	// Function select an element base on index and return
	// an element
	public List<Video> getRandomElement(List<Video> list,
										  int totalItems)
	{
		Random rand = new Random();

		// create a temporary list for storing
		// selected element
		List<Video> newList = new ArrayList<>();
		for (int i = 0; i < totalItems; i++) {

			// take a raundom index between 0 to size
			// of given List
			int randomIndex = rand.nextInt(list.size());

			// add element in temporary list
			newList.add(list.get(randomIndex));

			// Remove selected element from orginal list
			list.remove(randomIndex);
		}
		return newList;
	}
}
