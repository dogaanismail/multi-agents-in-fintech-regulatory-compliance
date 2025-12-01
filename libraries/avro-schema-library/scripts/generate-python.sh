#!/bin/bash
# Build Python package from Avro schemas

set -e

echo "📦 Building Python package from Avro schemas..."
echo ""

cd "$(dirname "$0")/../python"

# Clean previous builds
echo "Cleaning previous builds..."
rm -rf build/ dist/ *.egg-info/

# Build package
echo "Building Python package..."
python3 setup.py sdist bdist_wheel

echo ""
echo "✅ Python package built successfully!"
echo "📁 Location: python/dist/"
echo ""
echo "Built packages:"
ls -lh dist/

echo ""
echo "To install locally:"
echo "  pip install dist/avro_schema_library-1.0.0-py3-none-any.whl"
echo ""
echo "To install in editable mode (for development):"
echo "  pip install -e ."
