#!/bin/bash
# Generate Java classes from Avro schemas

set -e

echo "🔨 Generating Java classes from Avro schemas..."
echo ""

cd "$(dirname "$0")/../java"

# Clean previous generated files
echo "Cleaning previous generated files..."
rm -rf build/generated-main-avro-java

# Generate using Gradle
echo "Running Gradle build..."
./gradlew clean generateAvro build

echo ""
echo "✅ Java classes generated successfully!"
echo "📁 Location: java/build/generated-main-avro-java/"
echo ""
echo "Generated classes:"
find build/generated-main-avro-java -name "*.java" -type f | sed 's|build/generated-main-avro-java/||' | sort
