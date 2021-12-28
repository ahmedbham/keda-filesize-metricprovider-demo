package com.example.kedafilesizemetricproviderdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

class Stats {
  int tasks;

  public int gettasks() {
    return tasks;
  }

  public void settasks(int value) {
    tasks = value;
  }
}

class myEventGridEvent {
  private String topic;
  private String subject;
  private String eventType;
  private String id;
  private Data data;
  private String dataVersion;
  private String metadataVersion;
  private String eventTime;

  public String getTopic() {
    return topic;
  }

  public void setTopic(String value) {
    this.topic = value;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String value) {
    this.subject = value;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String value) {
    this.eventType = value;
  }

  public String getID() {
    return id;
  }

  public void setID(String value) {
    this.id = value;
  }

  public Data getData() {
    return data;
  }

  public void setData(Data value) {
    this.data = value;
  }

  public String getDataVersion() {
    return dataVersion;
  }

  public void setDataVersion(String value) {
    this.dataVersion = value;
  }

  public String getMetadataVersion() {
    return metadataVersion;
  }

  public void setMetadataVersion(String value) {
    this.metadataVersion = value;
  }

  public String getEventTime() {
    return eventTime;
  }

  public void setEventTime(String value) {
    this.eventTime = value;
  }
}

class Data {
  private String api;
  private String requestId;
  private String eTag;
  private String contentType;
  private long contentLength;
  private String blobType;
  private String blobUrl;
  private String url;
  private String sequencer;
  private String identity;
  private StorageDiagnostics storageDiagnostics;
  private String clientRequestId;

  public String getAPI() {
    return api;
  }

  public void setAPI(String value) {
    this.api = value;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String value) {
    this.requestId = value;
  }

  public String geteTag() {
    return eTag;
  }

  public void seteTag(String value) {
    this.eTag = value;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String value) {
    this.contentType = value;
  }

  public long getContentLength() {
    return contentLength;
  }

  public void setContentLength(long value) {
    this.contentLength = value;
  }

  public String getBlobType() {
    return blobType;
  }

  public void setBlobType(String value) {
    this.blobType = value;
  }

  public String getblobUrl() {
    return blobUrl;
  }

  public void setblobUrl(String value) {
    this.blobUrl = value;
  }

  public String getURL() {
    return url;
  }

  public void setURL(String value) {
    this.url = value;
  }

  public String getSequencer() {
    return sequencer;
  }

  public void setSequencer(String value) {
    this.sequencer = value;
  }

  public String getIdentity() {
    return identity;
  }

  public void setIdentity(String value) {
    this.identity = value;
  }

  public StorageDiagnostics getStorageDiagnostics() {
    return storageDiagnostics;
  }

  public void setStorageDiagnostics(StorageDiagnostics value) {
    this.storageDiagnostics = value;
  }
  
  public String getClientRequestId() {
    return clientRequestId;
  }

  public void setClientRequestId(String value) {
    this.clientRequestId = value;
  }
}

class StorageDiagnostics {
  private String batchId;

  public String getBatchId() {
    return batchId;
  }

  public void setBatchdD(String value) {
    this.batchId = value;
  }
}

@RestController
public class FilesizeMetricProvider {

  @Value("${queue_name}")
  private String QUEUE_NAME;

  @Value("${connection_string}")
  private String CONNECTION_STRING;
    
  
  @GetMapping(value = "/")
  public Stats getBlobStats() {

    String filesize = "";
    Stats stats = new Stats();
    stats.tasks = 0;

    try {
      // Instantiate a QueueClient which will be
      // used to create and manipulate the queue
      QueueClient queueClient = new QueueClientBuilder()
          .connectionString(CONNECTION_STRING)
          .queueName(QUEUE_NAME)
          .buildClient();

      // Get the first queue message
      QueueMessageItem message = queueClient.receiveMessage();

      if (null != message) {
        System.out.println("Message retrieved: " + message);

        byte[] decodedBytes = Base64.getDecoder().decode(message.getBody().toBytes());
        String decodedString = new String(decodedBytes);

        ObjectMapper mapper = new ObjectMapper();
        var event = mapper.readValue(decodedString, myEventGridEvent.class);

        if (event.getEventType().equals("Microsoft.Storage.BlobCreated")) {
          var data = event.getData();
          if (null != data) {
            filesize = Long.toString(data.getContentLength());
            System.out.println("file size: " + filesize);
            if (Integer.valueOf(filesize) > 100000 && Integer.valueOf(filesize) < 1000000) {
              stats.tasks = 3;
              System.out.println("tasks set to three: " + stats.tasks);
            } else if (Integer.valueOf(filesize) >= 1000000) {
              stats.tasks = 5;
              System.out.println("tasks set to five: " + stats.tasks);
            }
          }

        }
        queueClient.deleteMessage(message.getMessageId(), message.getPopReceipt());
      }
      else {
        System.out.println("no new files in queue");
      }
    } catch (QueueStorageException e) {
      // Output the exception message and stack trace
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (JsonMappingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return stats;
  }
}
