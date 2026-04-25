#!/bin/bash

CURRENT_PARENT_ID=1

for i in {2..22}
do
  echo "Attempting to create comment at depth level: $i"
  
  RESPONSE=$(curl -s -X POST http://localhost:8080/api/posts/1/comments \
  -H "Content-Type: application/json" \
  -d '{
      "authorId": 2,
      "authorType": "USER",
      "content": "Testing depth level '$i'",
      "parentCommentId": '$CURRENT_PARENT_ID'
  }')
  
  if echo "$RESPONSE" | grep -q "Too Many Requests"; then
      echo "BLOCKED! Vertical Cap successfully triggered."
      echo "Error: $RESPONSE"
      break
  fi
  
  CURRENT_PARENT_ID=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
  echo "Success. New Comment ID: $CURRENT_PARENT_ID"
  echo "-----------------------------------"
done
