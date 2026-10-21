package com.example.portshield;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable assessment and persistence model representing a security event.
 * Keeps original evidence and calculated score immutable after creation, while
 * allowing review_status and user notes to be updated in SQLite.
 */
public class SecurityEvent {

    public static final String SOURCE_SIMULATED = "SIMULATED";
    public static final String SOURCE_OBSERVED = "OBSERVED";

    public static final String REVIEW_PENDING = "PENDING";
    public static final String REVIEW_REVIEWED = "REVIEWED";

    private long id;
    private final String eventType;
    private final int riskScore;
    private final String riskLevel;
    private final String description;
    private final List<String> evidenceList;
    private final String recommendedAction;
    private final String confidenceIndicator;
    private final String eventSource;
    private final long timestamp;
    private String reviewStatus;
    private String notes;

    public SecurityEvent(
            long id,
            String eventType,
            int riskScore,
            String riskLevel,
            String description,
            List<String> evidenceList,
            String recommendedAction,
            String confidenceIndicator,
            String eventSource,
            long timestamp,
            String reviewStatus,
            String notes) {
        this.id = id;
        this.eventType = eventType != null ? eventType : "UNKNOWN_EVENT";
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskLevel = riskLevel != null ? riskLevel : "Unknown";
        this.description = description != null ? description : "";
        this.evidenceList = evidenceList != null ? new ArrayList<>(evidenceList) : new ArrayList<>();
        this.recommendedAction = recommendedAction != null ? recommendedAction : "";
        this.confidenceIndicator = confidenceIndicator != null ? confidenceIndicator : "Unverified";
        this.eventSource = eventSource != null ? eventSource : SOURCE_SIMULATED;
        this.timestamp = timestamp;
        this.reviewStatus = reviewStatus != null ? reviewStatus : REVIEW_PENDING;
        this.notes = notes != null ? notes : "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEvidenceList() {
        return new ArrayList<>(evidenceList);
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getConfidenceIndicator() {
        return confidenceIndicator;
    }

    public String getEventSource() {
        return eventSource;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Serializes the evidence list, recommended action, and confidence indicator into
     * a structured JSON string for storage in the SQLite evidence_json column.
     */
    public String toEvidenceJsonString() {
        JSONObject root = new JSONObject();
        try {
            JSONArray arr = new JSONArray();
            for (String item : evidenceList) {
                arr.put(item);
            }
            root.put("evidence", arr);
            root.put("recommended_action", recommendedAction);
            root.put("confidence", confidenceIndicator);
        } catch (JSONException e) {
            return "{}";
        }
        return root.toString();
    }

    /**
     * Helper class for unpacking evidence_json from SQLite.
     */
    public static class EvidencePayload {
        public final List<String> evidenceList;
        public final String recommendedAction;
        public final String confidenceIndicator;

        public EvidencePayload(List<String> evidenceList, String recommendedAction, String confidenceIndicator) {
            this.evidenceList = evidenceList;
            this.recommendedAction = recommendedAction;
            this.confidenceIndicator = confidenceIndicator;
        }
    }

    public static EvidencePayload parseEvidenceJson(String jsonString) {
        List<String> list = new ArrayList<>();
        String action = "Review connection state and disconnect untrusted USB accessories if unexpected.";
        String confidence = "Recorded assessment";
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new EvidencePayload(list, action, confidence);
        }
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray arr = root.optJSONArray("evidence");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.optString(i));
                }
            }
            action = root.optString("recommended_action", action);
            confidence = root.optString("confidence", confidence);
        } catch (JSONException e) {
            list.add(jsonString);
        }
        return new EvidencePayload(list, action, confidence);
    }
}