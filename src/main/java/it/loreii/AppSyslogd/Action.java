package it.loreii.AppSyslogd;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.mongodb.MongoClient;

import it.loreii.AppSyslogd.SyslogMsg.Data;

@RestController
public class Action {
	static final Logger logger = LoggerFactory.getLogger(Application.class);

	private static final int PAGE_SIZE = 15;
	private MongoOperations mongoOps = new MongoTemplate(new SimpleMongoDbFactory(new MongoClient(), "test"));

	private Gson gson = new Gson();

	private static Cache<Object, Object> cache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

	public Action() {
	}

	@Autowired
	private SyslogDataRepository repo;
	@Autowired
	private DictionaryRepository dict;

	/**
	 * Simple action for generating some syslog for debug
	 */
	@RequestMapping("/log")
	public void log() {
		logger.debug("debug message slf4j");
		logger.info("info message slf4j");
		logger.warn("warn message slf4j");
		logger.error("error message slf4j");
	}

	/**
	 * Fetch paginated no filter
	 */
	@RequestMapping("/list")
	public String list(@RequestParam(value = "page", defaultValue = "0") Integer page) {
		Page<Data> result = repo.findAll(new PageRequest(page, PAGE_SIZE));
		HashMap<String, Object> m = new HashMap<>();
		m.put("data", result.getContent());
		m.put("totalPages", result.getTotalPages());
		return gson.toJson(m);
	}

	/**
	 * Fetch paginated no filter
	 */
	@RequestMapping("/size")
	public String size() {
		long size = repo.count();
		return "{\"size\":" + size + "}";
	}

	HashMap<Integer, Long> stupidCache = new HashMap<>(); // LRU cache

	/**
	 * Query the data by some criteria
	 */
	@RequestMapping("/q")
	public String query(@RequestParam(value = "page", defaultValue = "0") Integer page,
			@RequestParam(value = "search") String search, @RequestParam(value = "hostname") String hostname,
			@RequestParam(value = "pri") Long pri, @RequestParam(value = "from") Long from,
			@RequestParam(value = "to") Long to) {
		HashMap<String, Object> m = new HashMap<>();
		if (StringUtils.isBlank(search) && StringUtils.isBlank(hostname) && pri == null && from == null && to == null) {
			Page<Data> result = repo.findAll(new PageRequest(page, PAGE_SIZE));
			result.nextPageable();
			m.put("data", result.getContent());
			m.put("totalPages", result.getTotalPages());
			return gson.toJson(m);
		} else {
			if (!StringUtils.isBlank(search)) {
				// full text search
				Page<Data> result = repo.findByText(search, new PageRequest(page, PAGE_SIZE));
				m.put("data", result.getContent());
				m.put("totalPages", result.getTotalPages());
				return gson.toJson(m);
			}
			Query query = new Query();

			if (!StringUtils.isBlank(hostname))
				query.addCriteria(Criteria.where("hostname").is(hostname));
			if (pri != null)
				query.addCriteria(Criteria.where("pri").is(pri));
			if (from != null || from != null) {
				query.addCriteria(Criteria.where("timestamp").gt(new Date(from * 1000)).lt(new Date(to * 1000)));
			}

			query.with(new Sort(Sort.Direction.ASC, "timestamp"));

			Long c;
			try {
				c = (Long) cache.get(query.toString(), new Callable<Long>() {
					public Long call() {
						return  mongoOps.count(query, Data.class);
					}
				});			
			} catch (ExecutionException e) {
				c = mongoOps.count(query, Data.class);
			}
			
			query.limit(PAGE_SIZE).skip(page * PAGE_SIZE);

			List<Data> result = mongoOps.find(query, Data.class);

			m.put("data", result);
			m.put("totalPages", c / PAGE_SIZE);
			return gson.toJson(m);
		}

	}

	/**
	 * Fetch suggestions for search
	 */
	@RequestMapping("/suggestions")
	public String suggestions(@RequestParam(value = "search") String search,
			@RequestParam(value = "type", defaultValue = "hostname") String type) {
		HashMap<String, Object> m = new HashMap<>();

		if (type.equalsIgnoreCase("")) {
			Page<Dictionary> result = dict.findByValueLike(search, new PageRequest(0, PAGE_SIZE)); // fixed page
			m.put("data", result.getContent());
			m.put("totalPages", result.getTotalPages());
		} else {
			List<Dictionary> result = dict.findByType(type); // fixed page
			m.put("data", result);
			m.put("totalPages", result.size());
		}

		return gson.toJson(m);
	}

	
}
