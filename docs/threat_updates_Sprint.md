# Threat Updates Sprint: Real-Time Online Alerts

This sprint focuses on integrating live, location-based threat data into the **SaveReach** application. The goal is to provide a "Current Situation" pulse that allows users to stay safe by monitoring authoritative data sources.

## Current Situation Sources (Online)

To enable real-time updates while online, we have identified the following primary data sources:

| Source | Type | Coverage | Format |
| :--- | :--- | :--- | :--- |
| **GDACS API** | Natural Disasters (Earthquakes, Floods, etc.) | Global | JSON, GeoJSON, XML |
| **NWS API** | Localized Emergency Alerts | United States | CAP (Common Alerting Protocol) |
| **USGS** | Earthquake Data | Global | GeoJSON |
| **NewsAPI.ai** | Event-Based intelligence (Crisis, Panic, Evacuation) | Global | API |
| **CAP Feeds** | Standardized Emergency Messaging | Regional (e.g. EC-JRC for EU) | XML |

### Proximity Logic
Building a "current situation" feature involves:
1.  **Geospatial Tracking**: Fetching the user's current GPS coordinates.
2.  **Point-in-Polygon Search**: Comparing user coordinates against "alert polygons" (the area impacted by a disaster).
3.  **Intensity Thresholds**: Filtering alerts by severity (e.g., only "Extreme" or "Severe").

## Recommended Agent Skills

I discovered the following skills using `npx skills find` that are highly relevant to this sprint:

1.  **Weather Intelligence**
    - `dpearson2699/swift-ios-skills@weatherkit` (464 installs)
    - Provides high-fidelity weather alerts and threat forecasting.

2.  **Alerting & Workflows**
    - `claude-office-skills/skills@weather automation` (321 installs)
    - Useful for setting up automated notification chains when a threshold is met.

3.  **Crisis Communications**
    - `erichowens/some_claude_skills@crisis-response-protocol` (47 installs)
    - `omer-metin/skills-for-antigravity@crisis-communications` (20 installs)
    - Protocol-based communication and crisis response automation.

4.  **Local Intelligence**
    - `psh355q-ui/szdi57465yt@emergency-news-agent` (5 installs)
    - Specialized news monitoring for local emergency contexts.

## Roadmap & Tasks

- [ ] **Infrastructure**: Implement `AlertProvider` to aggregate multiple online sources.
- [ ] **Data Logic**: Develop `LocationFilter` to match alerts with user coordinates.
- [ ] **UX/UI**: Retrofit the "Active Alerts" dashboard with online-fetched data.
- [ ] **Polish**: Implement micro-animations for "Live Pulses" and "Sector 7" 스타일 (Style).

---
*Created by Antigravity on April 6, 2026*
