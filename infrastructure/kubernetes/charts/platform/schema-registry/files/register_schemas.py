import json
import os
import sys
import time
import urllib.error
import urllib.request

SCHEMA_REGISTRY_URL = os.environ["SCHEMA_REGISTRY_URL"]
SCHEMAS_DIRECTORY = os.environ["SCHEMAS_DIRECTORY"]
SUBJECTS_FILE = os.environ["SUBJECTS_FILE"]
REGISTRY_WAIT_SECONDS = 300


def wait_for_schema_registry():
    deadline = time.monotonic() + REGISTRY_WAIT_SECONDS
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(f"{SCHEMA_REGISTRY_URL}/subjects", timeout=5):
                return
        except (urllib.error.URLError, ConnectionError):
            time.sleep(5)
    sys.exit(f"Schema Registry at {SCHEMA_REGISTRY_URL} did not become reachable")


def register_schema(subject, schema_file_name):
    with open(os.path.join(SCHEMAS_DIRECTORY, schema_file_name)) as schema_file:
        schema = json.dumps(json.load(schema_file))
    request = urllib.request.Request(
        f"{SCHEMA_REGISTRY_URL}/subjects/{subject}/versions",
        data=json.dumps({"schema": schema, "references": []}).encode(),
        headers={"Content-Type": "application/vnd.schemaregistry.v1+json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            schema_id = json.load(response)["id"]
            print(f"{subject} registered with id {schema_id}")
            return True
    except urllib.error.HTTPError as error:
        print(f"{subject} failed: {error.code} {error.read().decode()}")
        return False


def main():
    wait_for_schema_registry()
    with open(SUBJECTS_FILE) as subjects_file:
        subjects = json.load(subjects_file)
    results = [register_schema(entry["subject"], entry["schema"]) for entry in subjects]
    if not all(results):
        sys.exit(1)


if __name__ == "__main__":
    main()
