import os
import base64
import json

from azure.storage.queue import QueueClient
from flask import Flask

app = Flask(__name__)

CONNECTION_STRING = os.getenv('CONNECTION_STRING')
QUEUE_NAME = os.getenv('QUEUE_NAME')

queue = QueueClient.from_connection_string(
        conn_str=CONNECTION_STRING, queue_name=QUEUE_NAME)

@app.route("/")
def getTaskCount():
    stats = {
        'tasks': 0
    }

    response = queue.receive_message()

    if response is None:
        print("no message in queue")
        return stats
    
    decoded_content = base64.b64decode(response.content).decode()
    message_dict = json.loads(decoded_content)
    contentLength = message_dict['data']['contentLength']

    if contentLength > 1000000:
        stats['tasks'] = 5
    elif contentLength > 100000:
        stats['tasks'] = 3
    queue.delete_message(response)
    return stats
