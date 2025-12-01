#!/bin/bash
# Validate Avro schema syntax

set -e

SCHEMAS_DIR="$(cd "$(dirname "$0")/../schemas" && pwd)"

echo "🔍 Validating Avro schemas..."
echo ""

# Check if avro-tools is available
if ! command -v avro-tools &> /dev/null; then
    echo "⚠️  avro-tools not found. Installing..."
    echo "Run: pip install avro-tools"
    echo ""
fi

# Validate each schema file
error_count=0

for schema_file in $(find "$SCHEMAS_DIR" -name "*.avsc" -type f); do
    schema_name=$(basename "$schema_file")
    relative_path=$(echo "$schema_file" | sed "s|$SCHEMAS_DIR/||")
    
    echo -n "Validating $relative_path ... "
    
    # Check if file is valid JSON
    if jq empty "$schema_file" 2>/dev/null; then
        echo "✅"
    else
        echo "❌ Invalid JSON syntax"
        ((error_count++))
    fi
done

echo ""

if [ $error_count -eq 0 ]; then
    echo "✅ All schemas are valid!"
    exit 0
else
    echo "❌ Found $error_count invalid schema(s)"
    exit 1
fi
