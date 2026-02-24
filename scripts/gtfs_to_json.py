import os
import csv
import json
from typing import Dict, List, Any


def convert_gtfs_to_json(base_dir: str, cities: List[str], output_file: str):
    db: Dict[str, Any] = {"routes": [], "stops": [], "trips": []}

    for city in cities:
        city_dir = os.path.join(base_dir, city)

        # 1. Parse Routes
        routes_path = os.path.join(city_dir, "routes.txt")
        if os.path.exists(routes_path):
            with open(routes_path, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    db["routes"].append(
                        {
                            "id": f"{city}_{row.get('route_id')}",
                            "city": city,
                            "short_name": row.get("route_short_name", ""),
                            "long_name": row.get("route_long_name", ""),
                            "type": row.get("route_type", ""),
                        }
                    )

        # 2. Parse Stops
        stops_path = os.path.join(city_dir, "stops.txt")
        if os.path.exists(stops_path):
            with open(stops_path, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    db["stops"].append(
                        {
                            "id": f"{city}_{row.get('stop_id')}",
                            "city": city,
                            "name": row.get("stop_name", ""),
                            "lat": float(row.get("stop_lat", 0)),
                            "lon": float(row.get("stop_lon", 0)),
                        }
                    )

        # 3. Parse Trips (limit to 1000 per city for MVP performance)
        trips_path = os.path.join(city_dir, "trips.txt")
        if os.path.exists(trips_path):
            with open(trips_path, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                count = 0
                for row in reader:
                    if count >= 1000:
                        break
                    db["trips"].append(
                        {
                            "id": f"{city}_{row.get('trip_id')}",
                            "route_id": f"{city}_{row.get('route_id')}",
                            "headsign": row.get("trip_headsign", ""),
                            "city": city,
                        }
                    )
                    count += 1

    # Write the compiled db.json for json-server or similar mock APIs
    with open(output_file, "w", encoding="utf-8") as out:
        json.dump(db, out, ensure_ascii=False, indent=2)

    print(f"Data pipeline complete. Created {output_file} with:")
    print(f"   - {len(db['routes'])} routes")
    print(f"   - {len(db['stops'])} stops")
    print(f"   - {len(db['trips'])} trips")


if __name__ == "__main__":
    base_data_dir = os.path.join("data", "gtfs")
    cities_to_process = ["hyderabad", "kochi"]
    output_json_path = os.path.join("data", "db.json")

    # Ensure data directory exists
    os.makedirs(os.path.dirname(output_json_path), exist_ok=True)

    print("Starting GTFS MVP Data Pipeline...")
    convert_gtfs_to_json(base_data_dir, cities_to_process, output_json_path)
