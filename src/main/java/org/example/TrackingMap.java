package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

//TODO use tracking map from BigraphFramework
public class TrackingMap extends HashMap<String, String> {

    private String ruleName;
    public Set<String> links = new LinkedHashSet<>();

    public void addLinkNames(String... links) {
        this.links.addAll(Arrays.asList(links));
    }

    public boolean isLink(String link) {
        return this.links.contains(link);
    }

    public Set<String> getLinks() {
        return links;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public void setLinks(Set<String> links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TrackingMap map = (TrackingMap) o;
        return Objects.equals(ruleName, map.ruleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ruleName);
    }

    public static Set<TrackingMap> read(String filePath) {
        Set<TrackingMap> trackingMapSet = new LinkedHashSet<>();
        try {
            // Replace with the path to your JSON file
            FileReader fileReader = new FileReader(filePath);
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONObject jsonObject = new JSONObject(tokener);
            // Parse the JSON content
//            JSONObject jsonObject = new JSONObject(fileReader);
            // Iterate over the keys (rule names) in the JSON object
            Iterator<String> ruleNames = jsonObject.keys();
            while (ruleNames.hasNext()) {
                String ruleName = ruleNames.next();
//                System.out.println("\tRulename: " + ruleName);
                TrackingMap tmap = new TrackingMap();
                tmap.setRuleName(ruleName);
                JSONObject rule = jsonObject.getJSONObject(ruleName);
                JSONArray map = rule.getJSONArray("map");
                JSONArray links = rule.getJSONArray("links");
//                System.out.println("Rule " + ruleName + " Links: " + links.toString());
                tmap.setLinks(links.toList().stream().map(x -> (String) x).collect(Collectors.toSet()));
//                System.out.println("Rule " + ruleName + " Map:");
                for (int i = 0; i < map.length(); i++) {
                    JSONArray entry = map.getJSONArray(i);
//                    System.out.println("[" + entry.getString(0) + ", " + entry.optString(1, "") + "]");
                    tmap.put(entry.getString(0), entry.optString(1, ""));
                }
                trackingMapSet.add(tmap);
//                System.out.println("Size of trackingMapSet: " + trackingMapSet.size());
            }
            // Close the FileReader
            fileReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackingMapSet;
    }
}
