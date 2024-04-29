# Import required libraries
from kafka import KafkaProducer
import random
import time
import datetime
import json
from time import sleep
from kafka_schema_registry import prepare_producer # https://pypi.org/project/kafka-schema-registry/

# Configuration
KAFKA_BOOTSTRAP_SERVERS = ['localhost:9092']
SCHEMA_REGISTRY_URL = 'http://localhost:8081'
TOPIC_NAME = 'transactions'
NUM_MESSAGES = 2000
SLEEP_INTERVAL = 1

# Avro Schema
TRANSACTION_SCHEMA = {
    "type": "record",
    "name": "Transaction",
    "fields": [
        {"name": "user_id", "type": "int"},
        {"name": "transaction_timestamp_millis", "type": "long"},
        {"name": "amount", "type": "float"},
        {"name": "currency", "type": "string"},
        {"name": "counterpart_id", "type": "int"},
        {"name": "ingestion_date", "type": "string"}
    ]
}

# Kafka Avro Producer with Schema Validation
data_producer = prepare_producer(
    KAFKA_BOOTSTRAP_SERVERS,
    SCHEMA_REGISTRY_URL,
    TOPIC_NAME,
    1,
    1,
    value_schema=TRANSACTION_SCHEMA
)

# Kafka Json Producer
fallback_producer = KafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)


def generate_timestamp_millis():
    current_time = time.time()
    seven_days_ago = current_time - (7 * 24 * 60 * 60)
    seven_days_ago_ms = int(seven_days_ago * 1000)
    random_timestamp = random.randint(seven_days_ago_ms, int(current_time * 1000))
    return random_timestamp


class DataGenerator:
    @staticmethod
    def get_orders_data():
        """
        Generate and return a dictionary with mock order data.
        """
        return {
            "user_id": random.randint(1000, 1010),
            "transaction_timestamp_millis": generate_timestamp_millis(),
            "amount": round(random.uniform(10, 1000), 2),
            "currency": "USD",
            "counterpart_id": random.randint(2000, 2010),
            "ingestion_date": str(datetime.date.today())
        }

    @staticmethod
    def produce_avro_message(producer, topic, data):
        """
        Produce an Avro message and send it to the Kafka topic.
        """
        producer.send(topic, data)


# Generate and send order data
for _ in range(NUM_MESSAGES):
    transaction_data = DataGenerator.get_orders_data()
    try:
        print("Transaction Payload: ", transaction_data)
        DataGenerator.produce_avro_message(data_producer, TOPIC_NAME, transaction_data)
    except Exception as e:
        print("Error in Payload: ", e)
        transaction_json = json.dumps(transaction_data).encode("utf-8")
        DataGenerator.produce_avro_message(fallback_producer, 'transactions_fallback', transaction_json)
    sleep(SLEEP_INTERVAL)