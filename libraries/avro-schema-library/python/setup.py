from setuptools import setup, find_packages
import os

# Read README for long description
def read_readme():
    readme_path = os.path.join(os.path.dirname(__file__), '..', 'README.md')
    if os.path.exists(readme_path):
        with open(readme_path, 'r', encoding='utf-8') as f:
            return f.read()
    return ''

# Get all .avsc files
def get_schema_files():
    schema_files = []
    schemas_dir = os.path.join(os.path.dirname(__file__), '..', 'schemas')
    for root, dirs, files in os.walk(schemas_dir):
        for file in files:
            if file.endswith('.avsc'):
                schema_files.append(os.path.join(root, file))
    return schema_files

setup(
    name='avro-schema-library',
    version='1.0.0',
    description='Centralized Avro schemas for AML fraud detection system',
    long_description=read_readme(),
    long_description_content_type='text/markdown',
    author='Ismail Dogan',
    author_email='ismail.dogan@example.com',
    url='https://github.com/dogaanismail/multi-agents-in-fintech-regulatory-compliance',
    packages=find_packages(),
    install_requires=[
        'avro-python3>=1.10.2',
        'fastavro>=1.9.0',
    ],
    python_requires='>=3.8',
    include_package_data=True,
    package_data={
        'avro_schemas': ['../schemas/**/*.avsc'],
    },
    classifiers=[
        'Development Status :: 4 - Beta',
        'Intended Audience :: Developers',
        'Topic :: Software Development :: Libraries',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.8',
        'Programming Language :: Python :: 3.9',
        'Programming Language :: Python :: 3.10',
        'Programming Language :: Python :: 3.11',
    ],
    keywords='avro schema kafka fraud-detection aml',
)
