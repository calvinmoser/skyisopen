# Demo Data

Notes on how the demo mode data is collected and what lives here.

## Files

- `arrivals.json` — arrivals from AeroAPI, used to populate the demo arrivals table
- `track-history.json` — raw position track per flight keyed by `fa_flight_id`, used by `DemoService` for dead-reckoning

## Collection

Data is collected by running `DataCollector.java` (`main` method, no Spring context needed).
Requires an AeroAPI key in `backend/src/main/resources/password.properties`.

Fetches KIND arrivals for a **4-hour window starting 24 hours ago**, then fetches a raw position track for each flight.
Raw `FlightTrack` data is saved as-is; `DemoService` handles transformation at request time.

## Demo Loop

- **t0** = 2026-05-26 12:05:00 (`T0 = 1779816300000L`, earliest meaningful arrival in the dataset)
- **Loop duration** = 3 hours (hardcoded)
- **Virtual now** = `t0 + ((realNow - t0) % loopDuration)`

At any wall-clock time, the virtual position in the loop is `(realNow - t0) % loopDuration`. This means any page load or refresh shows proper in-progress flight data with no cold start. Flights that have landed in the current cycle sort to the bottom of the arrivals list with their `estimated_on` bumped to the next cycle.

## Progress Calculation

AeroAPI's `progress_percent` is time-based:

```
progress_percent = (now - actual_off) / (estimated_on - actual_off) * 100
```

In demo mode, `now` is the virtual timestamp (`t0 + elapsed`), `actual_off` maps to a flight's first track position, and `estimated_on` maps to its last.

### Endpoints

#### `GET /airports/{id}/flights/arrivals` → [schema](#arrivals-schema)

| Parameter | Type | Required | Notes |
|-----------|------|----------|-------|
| `id` | path | yes | Airport code. Prefer ICAO (e.g. `KIND`) |
| `start` | query | yes | ISO8601 date/datetime, inclusive. Compared against `scheduled_on`. |
| `end` | query | yes | ISO8601 date/datetime, exclusive. Compared against `scheduled_on`. |
| `airline` | query | no | Filter by airline (e.g. `UAL`). Mutually exclusive with `type`. |
| `type` | query | no | `General_Aviation` or `Airline`. Mutually exclusive with `airline`. |
| `max_pages` | query | no | Default 1, min 1. Upper limit on pages returned. |
| `cursor` | query | no | Opaque value for paging. |

#### `GET /flights/{id}/track` → [schema](#track-schema)

## Notes

---

## Arrivals Schema

**Response envelope**

| Field | Type | Notes |
|-------|------|-------|
| `links.next` | uri-reference | Cursor link to next page |
| `num_pages` | integer | Pages returned (min 1) |
| `arrivals` | array | See flight fields below |

**Each arrival object** (`*` = used by app)

| Field | Type | Notes |
|-------|------|-------|
| `ident` * | string | Operator code + flight number, or tail number for GA |
| `ident_icao` | string\|null | ICAO operator + flight number |
| `ident_iata` | string\|null | IATA operator + flight number |
| `actual_runway_off` | string\|null | Actual departure runway |
| `actual_runway_on` | string\|null | Actual arrival runway |
| `fa_flight_id` * | string | FlightAware unique flight ID |
| `operator` * | string\|null | ICAO operator code (falls back to IATA) |
| `operator_icao` | string\|null | |
| `operator_iata` | string\|null | |
| `flight_number` * | string\|null | Bare flight number |
| `registration` | string\|null | Tail number |
| `atc_ident` | string\|null | ATC ident if different from ident |
| `inbound_fa_flight_id` | string\|null | Previous leg's flight ID |
| `codeshares` | [string] | ICAO codeshares |
| `codeshares_iata` | [string] | IATA codeshares |
| `blocked` | boolean | Blocked from public view |
| `diverted` | boolean | |
| `cancelled` | boolean | |
| `position_only` | boolean | No flight plan available |
| `origin` * | object | See airport object below |
| `destination` | object | See airport object below |
| `departure_delay` | integer\|null | Seconds, negative = early |
| `arrival_delay` | integer\|null | Seconds, negative = early |
| `filed_ete` | integer\|null | Filed runway-to-runway duration (seconds) |
| `progress_percent` * | integer\|null | 0–100, null for position-only |
| `status` | string | Human-readable flight status |
| `aircraft_type` * | string\|null | ICAO type code (IATA fallback) |
| `route_distance` * | integer\|null | Filed route distance (statute miles) |
| `filed_airspeed` * | integer\|null | Filed IFR airspeed (knots) |
| `filed_altitude` | integer\|null | Filed IFR altitude (100s of feet) |
| `route` | string\|null | Textual route description |
| `baggage_claim` | string\|null | |
| `seats_cabin_business` | integer\|null | |
| `seats_cabin_coach` | integer\|null | |
| `seats_cabin_first` | integer\|null | |
| `gate_origin` | string\|null | |
| `gate_destination` | string\|null | |
| `terminal_origin` | string\|null | |
| `terminal_destination` | string\|null | |
| `type` | enum | `Airline` or `General_Aviation` |
| `scheduled_out` | datetime\|null | Scheduled gate departure |
| `estimated_out` | datetime\|null | |
| `actual_out` | datetime\|null | |
| `scheduled_off` | datetime\|null | Scheduled runway departure |
| `estimated_off` * | datetime\|null | |
| `actual_off` | datetime\|null | |
| `scheduled_on` | datetime\|null | Scheduled runway arrival |
| `estimated_on` * | datetime\|null | |
| `actual_on` | datetime\|null | |
| `scheduled_in` | datetime\|null | Scheduled gate arrival |
| `estimated_in` | datetime\|null | |
| `actual_in` | datetime\|null | |

**Airport object** (origin / destination)

| Field | Type |
|-------|------|
| `code` | string\|null |
| `code_icao` | string\|null |
| `code_iata` * | string\|null |
| `code_lid` | string\|null |
| `timezone` | string\|null |
| `name` | string\|null |
| `city` * | string\|null |
| `airport_info_url` | uri-reference\|null |

## Track Schema

**Parameters**

| Parameter | Type | Required | Notes |
|-----------|------|----------|-------|
| `id` | path | yes | `fa_flight_id` (e.g. `UAL1234-1234567890-airline-0123`) |
| `include_estimated_positions` | query | no | Default false. Include estimated positions. |
| `include_surface_positions` | query | no | Default false. Include surface (ground) positions. |

**Response** (`*` = used by app)

| Field | Type | Notes |
|-------|------|-------|
| `actual_distance` | integer\|null | Miles flown as of latest position. Includes origin→first point and last point→destination if complete. Estimated positions excluded from calculation. |
| `positions` | array | See position fields below |

**Each position object**

| Field | Type | Notes |
|-------|------|-------|
| `fa_flight_id` | string\|null | Only populated by `/flights/search/positions` |
| `altitude` * | integer | Hundreds of feet |
| `altitude_change` * | enum | `C` climbing, `D` descending, `-` level |
| `groundspeed` * | integer | Knots |
| `heading` * | integer\|null | 0–360 degrees |
| `latitude` * | number | |
| `longitude` * | number | |
| `timestamp` * | datetime | Time position was received |
| `update_type` * | enum\|null | `P`=projected, `O`=oceanic, `Z`=radar, `A`=ADS-B, `M`=multilateration, `D`=datalink, `X`=surface/near-surface, `S`=space-based, `V`=virtual |
