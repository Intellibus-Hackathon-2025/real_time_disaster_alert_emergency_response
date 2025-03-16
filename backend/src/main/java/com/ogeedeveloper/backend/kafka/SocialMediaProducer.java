//package com.ogeedeveloper.backend.kafka;
//
//import com.ogeedeveloper.backend.model.AlertType;
//import com.ogeedeveloper.backend.model.GeoPoint;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.catalina.util.RateLimiter;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.w3c.dom.css.Counter;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class SocialMediaProducer implements AlertProducer {
//    private KafkaProducer kafkaProducerService;
//
//    @Autowired(required = false)
//    private MeterRegistry meterRegistry;
//
//    @Value("${social.media.twitter.bearer.token}")
//    private String twitterBearerToken;
//
//    @Value("${social.media.facebook.app.id}")
//    private String facebookAppId;
//
//    @Value("${social.media.facebook.app.secret}")
//    private String facebookAppSecret;
//
//    @Value("${social.media.facebook.access.token}")
//    private String facebookAccessToken;
//
//    @Value("${social.media.reddit.client.id}")
//    private String redditClientId;
//
//    @Value("${social.media.reddit.client.secret}")
//    private String redditClientSecret;
//
//    @Value("${social.media.emergency.sites}")
//    private List<String> emergencySiteUrls;
//
//    @Value("${social.media.minimum.post.threshold:3}")
//    private int minimumPostThreshold;
//
//    @Value("${social.media.geo.precision:7}")
//    private int geoHashPrecision;
//
//    @Value("${app.state:MS}")
//    private String stateCode;
//
//    private static final String SOURCE_TYPE = "SOCIAL_MEDIA";
//    private static final String TOPIC = "social-media-alerts";
//    private final AtomicLong idGenerator = new AtomicLong(1000);
//
//    // Client instances
//    private TwitterApi twitterClient;
//    private Facebook facebookClient;
//    private RateLimiter redditRateLimiter;
//    private StanfordCoreNLP nlpPipeline;
//
//    // Thread pool for concurrent processing
//    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
//
//    // Cache for deduplication and trend analysisa
//    private final Cache<String, SocialPost> postCache = Caffeine.newBuilder()
//            .expireAfterWrite(6, TimeUnit.HOURS)
//            .maximumSize(10000)
//            .build();
//
//    // Store clusters for trend detection
//    private final Map<String, EmergencyCluster> emergencyClusters = new ConcurrentHashMap<>();
//
//    // Metrics
//    private Counter twitterPostsCounter;
//    private Counter facebookPostsCounter;
//    private Counter redditPostsCounter;
//    private Counter emergencySiteAlertsCounter;
//    private Counter alertsGeneratedCounter;
//
//    // Emergency keywords to monitor
//    private static final List<String> FLOOD_KEYWORDS = Arrays.asList(
//            "flooding", "flood", "water rising", "underwater", "evacuation",
//            "road underwater", "washed out", "flash flood", "river overflow",
//            "dam breach", "levee", "water rescue", "high water");
//
//    private static final List<String> TRAFFIC_KEYWORDS = Arrays.asList(
//            "road closed", "accident", "traffic jam", "detour", "highway blocked",
//            "bridge out", "road flooded", "stranded", "pile up", "collision",
//            "overturned", "jackknifed", "interstate closed");
//
//    private static final List<String> EMERGENCY_KEYWORDS = Arrays.asList(
//            "emergency", "help needed", "rescue", "trapped", "911", "evacuate",
//            "danger", "warning", "alert", "seeking shelter", "fire", "explosion",
//            "chemical spill", "gas leak", "shelter in place", "lockdown");
//
//    // Combine all keywords for initial filtering
//    private final List<String> ALL_KEYWORDS = new ArrayList<>();
//
//    // Map to store alert types
//    private final Map<String, AlertType> alertTypes = new HashMap<>();
//
//    // Store locations with name mappings for geocoding
//    private final Map<String, GeoPoint> locationMap = new HashMap<>();
//
//    @PostConstruct
//    public void initialize() {
//        // Initialize metrics
//        if (meterRegistry != null) {
//            twitterPostsCounter = meterRegistry.counter("social.media.posts.twitter");
//            facebookPostsCounter = meterRegistry.counter("social.media.posts.facebook");
//            redditPostsCounter = meterRegistry.counter("social.media.posts.reddit");
//            emergencySiteAlertsCounter = meterRegistry.counter("social.media.alerts.emergency.sites");
//            alertsGeneratedCounter = meterRegistry.counter("social.media.alerts.generated");
//        }
//
//        // Initialize Twitter client
//        try {
//            TwitterCredentialsBearer credentials = new TwitterCredentialsBearer(twitterBearerToken);
//            twitterClient = new TwitterApi(credentials);
//            log.info("Twitter client initialized successfully");
//        } catch (Exception e) {
//            log.error("Failed to initialize Twitter client", e);
//        }
//
//        // Initialize Facebook client
//        try {
//            ConfigurationBuilder cb = new ConfigurationBuilder();
//            cb.setDebugEnabled(true)
//                    .setOAuthAppId(facebookAppId)
//                    .setOAuthAppSecret(facebookAppSecret)
//                    .setOAuthAccessToken(facebookAccessToken);
//
//            FacebookFactory ff = new FacebookFactory(cb.build());
//            facebookClient = ff.getInstance();
//            log.info("Facebook client initialized successfully");
//        } catch (Exception e) {
//            log.error("Failed to initialize Facebook client", e);
//        }
//
//        // Initialize Reddit rate limiter (1 request per 2 seconds to comply with API limits)
//        redditRateLimiter = RateLimiter.create(0.5);
//
//        // Initialize NLP pipeline for entity extraction
//        try {
//            Properties props = new Properties();
//            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
//            nlpPipeline = new StanfordCoreNLP(props);
//            log.info("NLP pipeline initialized successfully");
//        } catch (Exception e) {
//            log.error("Failed to initialize NLP pipeline", e);
//        }
//
//        // Combine all keywords
//        ALL_KEYWORDS.addAll(FLOOD_KEYWORDS);
//        ALL_KEYWORDS.addAll(TRAFFIC_KEYWORDS);
//        ALL_KEYWORDS.addAll(EMERGENCY_KEYWORDS);
//
//        // Initialize alert types
//        alertTypes.put("FLOOD", AlertType.builder().id(1L).name("FLOOD").build());
//        alertTypes.put("TRAFFIC_INCIDENT", AlertType.builder().id(2L).name("TRAFFIC_INCIDENT").build());
//        alertTypes.put("GENERAL_EMERGENCY", AlertType.builder().id(3L).name("GENERAL_EMERGENCY").build());
//
//        // Initialize Mississippi locations
//        initializeLocationMap();
//
//        // Start background tasks for continuous monitoring
//        executorService.scheduleWithFixedDelay(this::monitorEmergencySites, 0, 15, TimeUnit.MINUTES);
//
//        log.info("SocialMediaProducer initialized successfully");
//    }
//
//    @PreDestroy
//    public void cleanup() {
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//        log.info("SocialMediaProducer shutdown complete");
//    }
//
//    @Override
//    @Scheduled(fixedRate = 120000) // Every 2 minutes
//    public void fetchAndPublishAlerts() {
//        log.info("Starting social media analysis for emergency trends");
//
//        try {
//            // Process each platform concurrently for efficiency
//            List<SocialPost> combinedPosts = new ArrayList<>();
//
//            // Collect Twitter posts
//            executorService.submit(() -> {
//                try {
//                    List<SocialPost> twitterPosts = fetchTwitterPosts();
//                    log.debug("Fetched {} Twitter posts related to emergencies", twitterPosts.size());
//                    if (twitterPostsCounter != null) {
//                        twitterPostsCounter.increment(twitterPosts.size());
//                    }
//                    combinedPosts.addAll(twitterPosts);
//                } catch (Exception e) {
//                    log.error("Error fetching Twitter posts", e);
//                }
//            });
//
//            // Collect Facebook posts
//            executorService.submit(() -> {
//                try {
//                    List<SocialPost> facebookPosts = fetchFacebookPosts();
//                    log.debug("Fetched {} Facebook posts related to emergencies", facebookPosts.size());
//                    if (facebookPostsCounter != null) {
//                        facebookPostsCounter.increment(facebookPosts.size());
//                    }
//                    combinedPosts.addAll(facebookPosts);
//                } catch (Exception e) {
//                    log.error("Error fetching Facebook posts", e);
//                }
//            });
//
//            // Collect Reddit posts
//            executorService.submit(() -> {
//                try {
//                    List<SocialPost> redditPosts = fetchRedditPosts();
//                    log.debug("Fetched {} Reddit posts related to emergencies", redditPosts.size());
//                    if (redditPostsCounter != null) {
//                        redditPostsCounter.increment(redditPosts.size());
//                    }
//                    combinedPosts.addAll(redditPosts);
//                } catch (Exception e) {
//                    log.error("Error fetching Reddit posts", e);
//                }
//            });
//
//            // Wait for all collections to finish
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//
//            // Analyze the combined posts for trends
//            List<EmergencyCluster> detectedClusters = analyzePosts(combinedPosts);
//
//            // Generate alerts for significant clusters
//            List<Alert> alerts = generateAlertsFromClusters(detectedClusters);
//
//            // Publish alerts
//            for (Alert alert : alerts) {
//                kafkaProducerService.sendAlert(TOPIC, alert);
//                log.info("Published social media based alert: {}", alert.getDetail());
//                if (alertsGeneratedCounter != null) {
//                    alertsGeneratedCounter.increment();
//                }
//            }
//
//            if (alerts.isEmpty()) {
//                log.debug("No significant emergency trends detected in social media");
//            }
//
//        } catch (Exception e) {
//            log.error("Error during social media analysis", e);
//        }
//    }
//
//    /**
//     * Fetches recent tweets related to emergencies.
//     */
//    private List<SocialPost> fetchTwitterPosts() {
//        if (twitterClient == null) {
//            return Collections.emptyList();
//        }
//
//        List<SocialPost> relevantPosts = new ArrayList<>();
//
//        try {
//            // Create query for relevant keywords in the target state
//            String query = String.join(" OR ", ALL_KEYWORDS) + " place:" + stateCode;
//
//            // Execute the search API call
//            Get2TweetsSearchRecentResponse response = twitterClient.tweets().searchRecent()
//                    .query(query)
//                    .maxResults(100)
//                    .tweetFields(Arrays.asList("created_at", "geo", "text", "author_id", "public_metrics"))
//                    .execute();
//
//            if (response == null || response.getData() == null) {
//                return Collections.emptyList();
//            }
//
//            // Process each tweet
//            for (Tweet tweet : response.getData()) {
//                SocialPost post = new SocialPost();
//                post.setId("twitter-" + tweet.getId());
//                post.setContent(tweet.getText());
//                post.setTimestamp(LocalDateTime.now()); // convert from Twitter date format
//                post.setPlatform("Twitter");
//                post.setUrl("https://twitter.com/i/web/status/" + tweet.getId());
//
//                // Extract location from tweet geo data or text
//                if (tweet.getGeo() != null && tweet.getGeo().getCoordinates() != null) {
//                    TweetGeo.CoordinatesTypeEnum type = tweet.getGeo().getCoordinates().getType();
//                    if (TweetGeo.CoordinatesTypeEnum.POINT.equals(type)) {
//                        List<Double> coordinates = tweet.getGeo().getCoordinates().getCoordinates();
//                        if (coordinates != null && coordinates.size() >= 2) {
//                            GeoPoint location = new GeoPoint(coordinates.get(0), coordinates.get(1));
//                            post.setLocation(location);
//                        }
//                    }
//                }
//
//                // If no direct geo data, try to extract from text
//                if (post.getLocation() == null) {
//                    post.setLocation(extractLocationFromText(tweet.getText()));
//                }
//
//                // Only include posts with location data and relevant emergency content
//                if (post.getLocation() != null && isEmergencyRelated(tweet.getText())) {
//                    // Get engagement metrics if available
//                    if (tweet.getPublicMetrics() != null) {
//                        post.setEngagementScore(
//                                tweet.getPublicMetrics().getLikeCount() +
//                                        tweet.getPublicMetrics().getRetweetCount() * 3 +
//                                        tweet.getPublicMetrics().getReplyCount() * 2
//                        );
//                    }
//
//                    relevantPosts.add(post);
//                }
//            }
//
//        } catch (ApiException e) {
//            log.error("Twitter API error: {} - {}", e.getCode(), e.getResponseBody(), e);
//        }
//
//        return relevantPosts;
//    }
//
//    /**
//     * Fetches recent Facebook posts related to emergencies.
//     */
//    private List<SocialPost> fetchFacebookPosts() {
//        if (facebookClient == null) {
//            return Collections.emptyList();
//        }
//
//        List<SocialPost> relevantPosts = new ArrayList<>();
//
//        try {
//            // Get posts from public pages of emergency agencies, news outlets, etc.
//            // This is a simplified approach as personal posts require user authentication
//            List<String> emergencyPages = Arrays.asList(
//                    "MississippiEmergencyManagementAgency",
//                    "NWSJacksonMS",
//                    "MississippiDOT",
//                    "MSStateHealth"
//            );
//
//            for (String page : emergencyPages) {
//                try {
//                    // Get recent posts from the page
//                    Reading reading = new Reading()
//                            .limit(20)
//                            .fields("id", "message", "created_time", "place", "coordinates", "reactions.summary(true)");
//
//                    List<Post> posts = facebookClient.getFeed(page, reading);
//
//                    for (Post post : posts) {
//                        if (post.getMessage() != null && isEmergencyRelated(post.getMessage())) {
//                            SocialPost socialPost = new SocialPost();
//                            socialPost.setId("facebook-" + post.getId());
//                            socialPost.setContent(post.getMessage());
//                            socialPost.setTimestamp(LocalDateTime.now()); // convert from FB date
//                            socialPost.setPlatform("Facebook");
//                            socialPost.setUrl("https://facebook.com/" + post.getId());
//
//                            // Extract location from post place or message text
//                            if (post.getPlace() != null && post.getPlace().getLocation() != null) {
//                                GeoPoint location = new GeoPoint(
//                                        post.getPlace().getLocation().getLongitude(),
//                                        post.getPlace().getLocation().getLatitude()
//                                );
//                                socialPost.setLocation(location);
//                            } else {
//                                socialPost.setLocation(extractLocationFromText(post.getMessage()));
//                            }
//
//                            // Check for engagement metrics
//                            if (post.getReactions() != null) {
//                                socialPost.setEngagementScore(post.getReactions().getCount());
//                            }
//
//                            if (socialPost.getLocation() != null) {
//                                relevantPosts.add(socialPost);
//                            }
//                        }
//                    }
//
//                    // Respect rate limits
//                    Thread.sleep(200);
//
//                } catch (FacebookException | InterruptedException e) {
//                    if (e instanceof InterruptedException) {
//                        Thread.currentThread().interrupt();
//                    }
//                    log.warn("Error fetching posts from Facebook page {}: {}", page, e.getMessage());
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("Error fetching Facebook posts", e);
//        }
//
//        return relevantPosts;
//    }
//
//    /**
//     * Fetches recent Reddit posts related to emergencies.
//     */
//    private List<SocialPost> fetchRedditPosts() {
//        List<SocialPost> relevantPosts = new ArrayList<>();
//
//        try {
//            // Subreddits relevant to Mississippi and emergencies
//            List<String> subreddits = Arrays.asList(
//                    "mississippi",
//                    "jacksonms",
//                    "biloxi",
//                    "hattiesburg",
//                    "weather"
//            );
//
//            Gson gson = new Gson();
//            OkHttpClient client = new OkHttpClient.Builder()
//                    .connectTimeout(30, TimeUnit.SECONDS)
//                    .readTimeout(30, TimeUnit.SECONDS)
//                    .build();
//
//            for (String subreddit : subreddits) {
//                // Respect Reddit's rate limits
//                redditRateLimiter.acquire();
//
//                // Build request for subreddit posts
//                String url = "https://www.reddit.com/r/" + subreddit + "/search.json?q=" +
//                        String.join("%20OR%20", ALL_KEYWORDS.stream()
//                                .map(kw -> kw.replace(" ", "%20"))
//                                .collect(Collectors.toList())) +
//                        "&sort=new&restrict_sr=on&limit=25";
//
//                Request request = new Request.Builder()
//                        .url(url)
//                        .header("User-Agent", "Advisory-App/1.0")
//                        .get()
//                        .build();
//
//                try (Response response = client.newCall(request).execute()) {
//                    if (!response.isSuccessful()) {
//                        log.warn("Reddit API returned: {}", response.code());
//                        continue;
//                    }
//
//                    String responseBody = response.body().string();
//                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
//
//                    if (jsonResponse.has("data") && jsonResponse.getAsJsonObject("data").has("children")) {
//                        JsonArray posts = jsonResponse.getAsJsonObject("data")
//                                .getAsJsonArray("children");
//
//                        for (int i = 0; i < posts.size(); i++) {
//                            JsonObject post = posts.get(i).getAsJsonObject().getAsJsonObject("data");
//
//                            if (post.has("title") && post.has("selftext")) {
//                                String title = post.get("title").getAsString();
//                                String text = post.get("selftext").getAsString();
//                                String fullText = title + " " + text;
//
//                                if (isEmergencyRelated(fullText)) {
//                                    SocialPost socialPost = new SocialPost();
//                                    socialPost.setId("reddit-" + post.get("id").getAsString());
//                                    socialPost.setContent(fullText);
//                                    // Convert reddit created timestamp to LocalDateTime
//                                    long createdUtc = post.get("created_utc").getAsLong();
//                                    socialPost.setTimestamp(
//                                            LocalDateTime.ofInstant(
//                                                    Instant.ofEpochSecond(createdUtc),
//                                                    ZoneId.systemDefault()
//                                            )
//                                    );
//                                    socialPost.setPlatform("Reddit");
//                                    socialPost.setUrl("https://reddit.com" + post.get("permalink").getAsString());
//
//                                    // Calculate engagement score
//                                    int score = post.get("score").getAsInt();
//                                    int numComments = post.get("num_comments").getAsInt();
//                                    socialPost.setEngagementScore(score + (numComments * 2));
//
//                                    // Extract location from text
//                                    socialPost.setLocation(extractLocationFromText(fullText));
//
//                                    if (socialPost.getLocation() != null) {
//                                        relevantPosts.add(socialPost);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("Error fetching Reddit posts", e);
//        }
//
//        return relevantPosts;
//    }
//
//    /**
//     * Monitors emergency-specific websites like weather services, DOT status pages, etc.
//     */
//    private void monitorEmergencySites() {
//        try {
//            for (String url : emergencySiteUrls) {
//                try {
//                    Document doc = Jsoup.connect(url)
//                            .userAgent("Advisory-App/1.0")
//                            .timeout(10000)
//                            .get();
//
//                    // Different sites have different structures, so try several common patterns
//                    Elements alerts = doc.select(".alert, .warning, .emergency, .notice, .advisory");
//
//                    for (Element alert : alerts) {
//                        String alertText = alert.text();
//
//                        if (isEmergencyRelated(alertText)) {
//                            // Extract location information
//                            GeoPoint location = extractLocationFromText(alertText);
//
//                            if (location != null) {
//                                SocialPost post = new SocialPost();
//                                post.setId("site-" + System.currentTimeMillis() + "-" +
//                                        Math.abs(alertText.hashCode()));
//                                post.setContent(alertText);
//                                post.setTimestamp(LocalDateTime.now());
//                                post.setPlatform("EmergencySite");
//                                post.setUrl(url);
//                                post.setLocation(location);
//                                post.setEngagementScore(10); // Higher base score for official sources
//
//                                // Add to cache for analysis
//                                postCache.put(post.getId(), post);
//
//                                // Increment counter
//                                if (emergencySiteAlertsCounter != null) {
//                                    emergencySiteAlertsCounter.increment();
//                                }
//                            }
//                        }
//                    }
//
//                    // Don't hammer the sites
//                    Thread.sleep(2000);
//
//                } catch (Exception e) {
//                    log.warn("Error monitoring emergency site {}: {}", url, e.getMessage());
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error in emergency site monitoring", e);
//        }
//    }
//
//    /**
//     * Analyzes collected posts to detect emergency clusters.
//     */
//    private List<EmergencyCluster> analyzePosts(List<SocialPost> posts) {
//        // Store posts in cache for deduplication and trend analysis
//        for (SocialPost post : posts) {
//            postCache.put(post.getId(), post);
//        }
//
//        // Group posts by geographic area (using geohash prefix)
//        Map<String, List<SocialPost>> postsByArea = new HashMap<>();
//
//        for (SocialPost post : posts) {
//            if (post.getLocation() != null) {
//                String geoHash = GeoHash.encodeHash(
//                        post.getLocation().getLatitude(),
//                        post.getLocation().getLongitude(),
//                        geoHashPrecision
//                );
//
//                // Use first 4 characters as area prefix for clustering
//                String areaPrefix = geoHash.substring(0, Math.min(4, geoHash.length()));
//
//                postsByArea.computeIfAbsent(areaPrefix, k -> new ArrayList<>()).add(post);
//            }
//        }
//
//        // Find clusters with significant activity
//        List<EmergencyCluster> significantClusters = new ArrayList<>();
//
//        for (Map.Entry<String, List<SocialPost>> entry : postsByArea.entrySet()) {
//            List<SocialPost> areaPosts = entry.getValue();
//
//            if (areaPosts.size() >= minimumPostThreshold) {
//                // Determine the type of emergency by keyword frequency
//                Map<String, Integer> keywordCounts = new HashMap<>();
//                keywordCounts.put("FLOOD", 0);
//                keywordCounts.put("TRAFFIC_INCIDENT", 0);
//                keywordCounts.put("GENERAL_EMERGENCY", 0);
//
//                for (SocialPost post : areaPosts) {
//                    String content = post.getContent().toLowerCase();
//
//                    // Count keyword occurrences
//                    for (String keyword : FLOOD_KEYWORDS) {
//                        if (content.contains(keyword.toLowerCase())) {
//                            keywordCounts.put("FLOOD", keywordCounts.get("FLOOD") + 1);
//                        }
//                    }
//
//                    for (String keyword : TRAFFIC_KEYWORDS) {
//                        if (content.contains(keyword.toLowerCase())) {
//                            keywordCounts.put("TRAFFIC_INCIDENT", keywordCounts.get("TRAFFIC_INCIDENT") + 1);
//                        }
//                    }
//
//                    for (String keyword : EMERGENCY_KEYWORDS) {
//                        if (content.contains(keyword.toLowerCase())) {
//                            keywordCounts.put("GENERAL_EMERGENCY", keywordCounts.get("GENERAL_EMERGENCY") + 1);
//                        }
//                    }
//                }
//
//                // Find the most frequent emergency type
//                String emergencyType = keywordCounts.entrySet().stream()
//                        .max(Map.Entry.comparingByValue())
//                        .map(Map.Entry::getKey)
//                        .orElse("GENERAL_EMERGENCY");
//
//                // Calculate average location
//                double totalLat = 0, totalLon = 0;
//                for (SocialPost post : areaPosts) {
//                    totalLat += post.getLocation().getLatitude();
//                    totalLon += post.getLocation().getLongitude();
//                }
//
//                GeoPoint avgLocation = new GeoPoint(
//                        totalLon / areaPosts.size(),
//                        totalLat / areaPosts.size()
//                );
//
//                // Calculate total engagement
//                int totalEngagement = areaPosts.stream()
//                        .mapToInt(SocialPost::getEngagementScore)
//                        .sum();
//
//                // Create the cluster
//                EmergencyCluster cluster = new EmergencyCluster();
//                cluster.setAreaCode(entry.getKey());
//                cluster.setEmergencyType(emergencyType);
//                cluster.setCenterLocation(avgLocation);
//                cluster.setPosts(areaPosts);
//                cluster.setEngagementScore(totalEngagement);
//                cluster.setLastUpdated(LocalDateTime.now());
//
//                // Set the nearest city or location name
//                cluster.setLocationName(getNearestCityName(avgLocation));
//
//                // Add to significant clusters if it meets the threshold
//                if (totalEngagement >= 5 || areaPosts.size() >= minimumPostThreshold) {
//                    significantClusters.add(cluster);
//
//                    // Update the stored clusters map
//                    emergencyClusters.put(entry.getKey(), cluster);
//                }
//            }
//        }
//
//        return significantClusters;
//    }
//
//    /**
//     * Generates alerts from detected emergency clusters.
//     */
//    private List<Alert> generateAlertsFromClusters(List<EmergencyCluster> clusters) {
//        List<Alert> alerts = new ArrayList<>();
//
//        for (EmergencyCluster cluster : clusters) {
//            // Only generate an alert if this is a new cluster or significantly updated
//            String clusterKey = cluster.getAreaCode();
//            EmergencyCluster existingCluster = emergencyClusters.get(clusterKey);
//
//            boolean isNewCluster = existingCluster == null;
//            boolean isUpdatedCluster = !isNewCluster &&
//                    (cluster.getEngagementScore() >= existingCluster.getEngagementScore() * 1.5 ||
//                            cluster.getPosts().size() >= existingCluster.getPosts().size() * 1.5);
//
//            if (isNewCluster || isUpdatedCluster) {
//                // Get alert type
//                AlertType alertType = alertTypes.getOrDefault(
//                        cluster.getEmergencyType(),
//                        alertTypes.get("GENERAL_EMERGENCY")
//                );
