# Resilience: Database Schema (Room Entities)

## 1. Household & Profiles

### `HOUSEHOLD_MEMBER`
Primary entity for sensitive family data.
- `id`: PK (UUID)
- `name`: String
- `relation`: Enum (Parent, Child, Spouse, Other)
- `blood_group`: String (Restricted Enum)
- `allergies`: String (Categorized)
- `medications`: JSON List or separate table
- `is_dependent`: Boolean
- `emergency_notes`: String (Encrypted)

### `HOUSEHOLD`
Group metadata if multiple households are supported.
- `id`: PK (UUID)
- `group_name`: String
- `last_synced`: Long (Timestamp)

---

## 2. Safety & Location

### `MEETING_POINT`
Crucial for reunification.
- `id`: PK (UUID)
- `household_id`: FK
- `type`: Enum (Primary, Backup, Outside-Area)
- `label`: String (e.g., "Park Bench")
- `description`: String (Plain text directions)
- `latitude`: Double?
- `longitude`: Double?
- `is_verified`: Boolean

### `CHECKIN_LOG`
History of safety status.
- `id`: PK (UUID)
- `member_id`: FK
- `timestamp`: Long
- `status`: Enum (Safe, Need Help, Unknown)
- `last_lat`: Double?
- `last_long`: Double?
- `message_sent`: Boolean

---

## 3. Playbooks & Content

### `PLAYBOOK`
The survival guides.
- `id`: PK (UUID)
- `title`: String
- `category`: Enum (Medical, Shelter, Water, etc.)
- `content_markdown`: String
- `is_essential`: Boolean
- `source_id`: FK

### `PLAYBOOK_SOURCE_METADATA`
Attribution and trust data.
- `id`: PK (UUID)
- `org_name`: String (e.g., WHO)
- `verification_date`: Long
- `signature`: String (Digital signature for integrity)
- `source_url`: String

### `CONTENT_VERSION`
Tracking updates for offline sync.
- `id`: PK (UUID)
- `component`: Enum (App, Playbook, Maps)
- `version_code`: Int
- `release_date`: Long

---

## 4. Resource Inventory

### `RESOURCE_INVENTORY`
- `id`: PK (UUID)
- `category`: Enum (Water, Food, Fuel, Medicine, Batteries)
- `item_name`: String
- `quantity`: Float
- `unit`: String (Liters, Days, Units)
- `burn_rate`: Float (Usage per day per person)
- `expiry_date`: Long?
- `last_updated`: Long

---

## 5. System & Sync

### `SYNC_STATE`
Tracking background WorkManager status.
- `id`: PK (UUID)
- `entity_type`: String
- `last_success`: Long
- `retry_count`: Int
- `is_pending`: Boolean
