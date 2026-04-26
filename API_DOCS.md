# API Documentation

## Process Control API

### Start Process
- **Endpoint**: `POST /process/start`
- **Description**: Starts a new analysis process
- **Response**:
```json
{
  "processId": "uuid",
  "status": "RUNNING",
  "progress": {
    "totalFiles": 10,
    "processedFiles": 0,
    "percentage": 0.0
  },
  "startedAt": "2024-01-15T10:30:00Z",
  "estimatedCompletion": "2024-01-15T10:32:00Z"
}
```

### Stop Process
- **Endpoint**: `POST /process/stop/{process_id}`
- **Description**: Stops a specific process

### Pause Process
- **Endpoint**: `POST /process/pause/{process_id}`
- **Description**: Pauses a specific process

### Resume Process
- **Endpoint**: `POST /process/resume/{process_id}`
- **Description**: Resumes a paused process

### Get Status
- **Endpoint**: `GET /process/status/{process_id}`
- **Description**: Queries the status of a process
- **Response**: Same as start response

### List Processes
- **Endpoint**: `GET /process/list`
- **Description**: Lists all processes and their states
- **Response**:
```json
[
  {
    "processId": "uuid",
    "status": "COMPLETED",
    ...
  }
]
```

### Get Results
- **Endpoint**: `GET /process/results/{process_id}`
- **Description**: Gets analysis results
- **Response**:
```json
{
  "processId": "uuid",
  "status": "COMPLETED",
  "progress": {...},
  "startedAt": "...",
  "results": {
    "totalWords": 1500,
    "totalLines": 75,
    "mostFrequentWords": ["the", "of", "and"],
    "filesProcessed": ["doc1.txt", "doc2.txt"],
    "summary": "Processed 10 files with 1500 words."
  }
}
```

## WebSocket Updates
- **Endpoint**: `/ws`
- **Topic**: `/topic/process/{process_id}`
- **Message**: ProcessVO JSON on status changes

## Error Handling
- 404: Process not found
- 500: Internal server error
- Validation errors for invalid inputs
