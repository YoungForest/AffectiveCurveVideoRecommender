package com.example.restservice;

import com.fastdtw.timeseries.TimeSeries;
import com.fastdtw.timeseries.TimeSeriesBase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import com.fastdtw.dtw.FastDTW;
import com.fastdtw.util.Distances;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class VideoController {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	Gson gson;
	Logger logger = LoggerFactory.getLogger(VideoController.class);
	Map<String, Set<Video>> user2seen;
	Map<Pair<String, String>, Double> distanceBetweenVideos;
	Map<String, List<List<Double>>> affectiveCurveMap;
	@Value("${recommend.mode}")
	private String recommendMode;
	@Value("${video.root}")
	private String videoRoot;
	VideoController() {
		user2seen = new HashMap<>();
		gson = new Gson();
		distanceBetweenVideos = new HashMap<>();
		Gson affectiveCurve = new Gson();
		try {
			Reader reader = Files.newBufferedReader(Paths.get("src/main/resources/predict-douyin-VA-video.json"));
			Type videoMapType = new TypeToken<Map<String, List<List<Double>>>>() {}.getType();
			affectiveCurveMap = gson.fromJson(reader, videoMapType);
			System.out.println(affectiveCurve);
			for (Map.Entry<String, List<List<Double>>> entry : affectiveCurveMap.entrySet()) {
				System.out.println(entry.getKey() + "=" + entry.getValue());
				for (List<Double> l : entry.getValue()) {
					for (Double d : l) {
						System.out.println(d);
					}
				}
			}
			for (Map.Entry<String, List<List<Double>>> entry : affectiveCurveMap.entrySet()) {
				TimeSeries t1 = getTimeSeriesFromListDouble(entry.getValue());
				for (Map.Entry<String, List<List<Double>>> entry2 : affectiveCurveMap.entrySet()) {
					TimeSeries t2 = getTimeSeriesFromListDouble(entry2.getValue());
					double distance = FastDTW.compare(t1, t2, 3, Distances.EUCLIDEAN_DISTANCE)
							.getDistance();
					distanceBetweenVideos.put(new Pair<String, String>(entry.getKey(), entry2.getKey()), distance);
				}
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	TimeSeries getTimeSeriesFromListDouble (List<List<Double>> curve){
		TimeSeriesBase.Builder tb = TimeSeriesBase.builder();
		int s1 = curve.get(0).size();
		for (int i = 0; i < s1; ++i) {
			tb.add(curve.get(0).get(i),curve.get(1).get(i));
		}
		TimeSeries ts = tb.build();
		return ts;
	}

	List<Video> getVideos() {
		  /*
        CREATE VIEW video_view AS select * from shortpath left join videos on shortpath.name=videos.video_path
        video_view(name,length,id,created_at,updated_at,deleted_at,aweme_id,author_id,nickname,avatar,"desc",digg_count,comment_count,cover_path,video_path,share_url,is_download,status)'
         */

		List<Video> videos = jdbcTemplate.query("SELECT name, length, nickname, desc, video_path, cover_path FROM video_view where cover_path is not null ",
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
		Video ret = getVideoMap().get(name);
		if (!user2seen.containsKey(id)) user2seen.put(id, new HashSet<>());
		user2seen.get(id).add(ret);
		logger.info("getVideo " + id + " " + ret);
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename="+name)
				.contentType(MediaType.valueOf(mimeType)).body(fileResource);
	}

	@GetMapping("/cover")
	public ResponseEntity<FileSystemResource> getCover(@RequestParam(value = "id", defaultValue = "NoneId") String id, @RequestParam(value = "name") String name) {
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
		List<Video> ret;
		int limit = Integer.valueOf(limits);
		logger.info("Seen records: ", user2seen.get(id));
		if (user2seen.get(id) == null) {
			logger.info("user2seen.get(id) is null");
		}
		Set<Video> seen = user2seen.get(id);
		if (recommendMode.equals("affective") && seen != null) {
			logger.info("Affective recommend.");
			// calculate sum distance to seen videos, pick the closest candidates
			Map<String, Double> distance = new HashMap<>();
			List<Video> candidates = getVideos();
			for (Video v : candidates) {
				distance.put(v.name, 0.0);
				for (Video see : seen) {
					distance.put(v.name, distance.get(v.name) + distanceBetweenVideos.get(new Pair<>(v.name, see.name)));
				}
			}
			candidates.sort((Video v1, Video v2) -> distance.get(v1.name).compareTo(distance.get(v2.name)));
			ret = new ArrayList<>();
			int j = 0;
			// pick 5 more video than needed
			for (int i = 0; j < limit + 5 && i < candidates.size(); ++i) {
				Video x = candidates.get(i);
				if (seen.contains(x)) continue;
				else {
					ret.add(x);
					++j;
				}
			}
			ret = getRandomElement(ret, limit);	// random resort
		} else {
			logger.info("Random recommend.");
			ret = getRandomElement(getVideos(), limit);
		}
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
