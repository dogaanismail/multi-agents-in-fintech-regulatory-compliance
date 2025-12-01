# Avro Schema Library

Centralized Avro schemas for all microservices in the AML fraud detection system.

## Overview

This library provides a single source of truth for all Avro schemas used across:
- **Java Spring Boot services** (payment-engine, risk-engine, etc.)
- **Python ML agents** (transaction-pattern-agent, customer-risk-agent, network-analysis-agent)
- **Python MARL orchestrator** (MADDPG coordination)

## Schemas

### Fraud Detection (`schemas/fraud/`)
- **FraudDetectionRequest.avsc** - Request from risk-engine to MARL orchestrator
- **FraudDetectionResponse.avsc** - Response from MARL orchestrator back to risk-engine

### Payment (`schemas/payment/`)
- **PaymentCreatedEvent.avsc** - Payment initiated event
- **PaymentCompletedEvent.avsc** - Payment successfully processed
- **PaymentBlockedEvent.avsc** - Payment blocked by fraud detection

## Quick Start

### 1. Start Infrastructure

```bash
# Start Kafka, Schema Registry, and Kafka UI
./scripts/start-infrastructure.sh

# Access Kafka UI: http://localhost:8080
# Access Schema Registry: http://localhost:8081
```

### 2. Validate & Register Schemas

```bash
# Validate all schemas have valid JSON syntax
./scripts/validate-schemas.sh

# Register schemas to Schema Registry
./scripts/register-schemas.sh
```

### 3. Generate Code

```bash
# Generate both Java and Python code
./scripts/generate-all.sh

# Or individually:
./scripts/generate-java.sh     # Java classes only
./scripts/generate-python.sh   # Python package only
```

## Usage

### Java (Spring Boot Services)

Add to your `build.gradle`:

```gradle
dependencies {
    // Local dependency
    implementation project(':libraries:avro-schema-library:java')
    
    // Kafka + Avro dependencies
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'io.confluent:kafka-avro-serializer:7.5.0'
    implementation 'org.apache.avro:avro:1.11.3'
}
```

Use in your code:

```java
import com.aml.fraud.FraudDetectionRequest;
import com.aml.fraud.TransactionFeatures;

FraudDetectionRequest request = FraudDetectionRequest.newBuilder()
    .setRequestId(UUID.randomUUID().toString())
    .setTransactionId("TXN-001")
    .setTransactionFeatures(TransactionFeatures.newBuilder()
        .setAmountReceived(1000.0)
        .setPaymentCurrency("USD")
        .build())
    .build();

kafkaTemplate.send("fraud.detection.requests", request);
```

### Python (ML Agents & MARL Orchestrator)

Install the package:

```bash
# From local directory
pip install -e libraries/avro-schema-library/python

# Or after publishing to PyPI
pip install avro-schema-library==1.0.0
```

Use in your code:

```python
from confluent_kafka import avro
from confluent_kafka.avro import AvroConsumer

consumer = AvroConsumer({
    'bootstrap.servers': 'kafka:9092',
    'group.id': 'marl-orchestrator',
    'schema.registry.url': 'http://schema-registry:8081'
})

consumer.subscribe(['fraud.detection.requests'])

msg = consumer.poll(timeout=1.0)
request = msg.value()  # Automatically deserialized from Avro
print(f"Transaction ID: {request['transaction_id']}")
```

## Development

### Generate Java Classes

```bash
cd java
./gradlew clean build
```

Generated classes will be in `java/build/generated-main-avro-java/`

### Build Python Package

```bash
cd python
python setup.py sdist bdist_wheel
pip install dist/avro_schema_library-1.0.0-py3-none-any.whl
```

### Register Schemas to Schema Registry

```bash
./scripts/register-schemas.sh
```

This will register all schemas to Confluent Schema Registry at `http://localhost:8081`

## Schema Evolution

When modifying schemas:

1. **Backward compatible** (add optional fields with defaults):
   - Increment minor version (1.0.0 → 1.1.0)
   - Old consumers can still read new data

2. **Breaking changes** (remove/rename fields):
   - Increment major version (1.0.0 → 2.0.0)
   - Requires coordinated deployment

3. **Test compatibility**:
   ```bash
   ./scripts/test-compatibility.sh schemas/fraud/FraudDetectionRequest.avsc
   ```

## Architecture Benefits

✅ **Single Source of Truth** - One schema definition for all services  
✅ **Type Safety** - Compile-time checking in Java, runtime validation in Python  
✅ **Version Control** - Git tracks all schema changes  
✅ **Code Generation** - Automatic class/dataclass generation  
✅ **Schema Registry Integration** - Automatic compatibility checking  

## Contributing

1. Create/modify schema in `schemas/` directory
2. Run `./scripts/generate-all.sh` to generate code
3. Test with both Java and Python services
4. Commit schema changes with descriptive message
5. Update version in `java/build.gradle` and `python/setup.py`

## License

MIT License - Ismail Dogan Master's Thesis Project
