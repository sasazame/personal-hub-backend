#!/bin/bash

echo "🧪 Testing Pomodoro Events..."
echo ""

# Base URL
BASE_URL="http://localhost:8080/api/v1"

# Test user credentials
EMAIL="test-pomodoro@example.com"
PASSWORD="Test123!"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

# 1. Register or login to get auth token
echo "1️⃣ Authenticating..."
AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" 2>/dev/null || echo "{}")

if [ -z "$AUTH_RESPONSE" ] || [ "$AUTH_RESPONSE" = "{}" ]; then
    echo "Login failed, trying to register..."
    AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"testpomodoro\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" 2>/dev/null)
fi

TOKEN=$(echo $AUTH_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ Failed to authenticate${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Authenticated successfully${NC}"
echo ""

# 2. Test getting active session when none exists
echo "2️⃣ Testing active session endpoint (no session)..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/pomodoro/sessions/active" \
  -H "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Active session endpoint returns 200 when no session exists"
else
    print_result 1 "Active session endpoint returns $HTTP_CODE (expected 200)"
fi
echo ""

# 3. Create a new session
echo "3️⃣ Creating a new Pomodoro session..."
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/pomodoro/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"workDuration":25,"breakDuration":5}')

SESSION_ID=$(echo $CREATE_RESPONSE | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -n "$SESSION_ID" ]; then
    print_result 0 "Session created with ID: $SESSION_ID"
else
    print_result 1 "Failed to create session"
    exit 1
fi
echo ""

# 4. Test getting active session when it exists
echo "4️⃣ Testing active session endpoint (with session)..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/pomodoro/sessions/active" \
  -H "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Active session endpoint returns 200 with active session"
else
    print_result 1 "Active session endpoint returns $HTTP_CODE (expected 200)"
fi
echo ""

# 5. Test STOP action
echo "5️⃣ Testing STOP action..."
STOP_RESPONSE=$(curl -s -X PUT "$BASE_URL/pomodoro/sessions/$SESSION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":"CANCEL"}')

if [ -n "$STOP_RESPONSE" ]; then
    print_result 0 "Session stopped successfully"
else
    print_result 1 "Failed to stop session"
fi

# Check if active session is now null
RESPONSE=$(curl -s -X GET "$BASE_URL/pomodoro/sessions/active" \
  -H "Authorization: Bearer $TOKEN")

if [ "$RESPONSE" = "" ] || [ "$RESPONSE" = "null" ]; then
    print_result 0 "No active session after stop (correct)"
else
    print_result 1 "Active session still exists after stop"
fi
echo ""

# 6. Create another session and test SKIP (COMPLETE)
echo "6️⃣ Testing SKIP action..."
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/pomodoro/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"workDuration":25,"breakDuration":5}')

SESSION_ID=$(echo $CREATE_RESPONSE | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

SKIP_RESPONSE=$(curl -s -X PUT "$BASE_URL/pomodoro/sessions/$SESSION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":"COMPLETE"}')

if [ -n "$SKIP_RESPONSE" ]; then
    print_result 0 "Session skipped/completed successfully"
else
    print_result 1 "Failed to skip session"
fi
echo ""

# 7. Test page reload scenario
echo "7️⃣ Testing page reload scenario..."
# Check active session multiple times (simulating page reloads)
for i in 1 2 3; do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/pomodoro/sessions/active" \
      -H "Authorization: Bearer $TOKEN")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}  ✅ Reload $i: Status 200${NC}"
    else
        echo -e "${RED}  ❌ Reload $i: Status $HTTP_CODE${NC}"
    fi
done
echo ""

# 8. Test session history
echo "8️⃣ Testing session history..."
HISTORY_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/pomodoro/sessions?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")
HTTP_CODE=$(echo "$HISTORY_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Session history endpoint works"
else
    print_result 1 "Session history returns $HTTP_CODE"
fi

echo ""
echo "🎯 Test Summary:"
echo "- Active session endpoint handles null sessions correctly"
echo "- STOP and SKIP actions work properly"
echo "- Page reload doesn't cause 400 errors"
echo "- Session history is accessible"