#!/usr/bin/env bash
set -euo pipefail
BASE="http://127.0.0.1:8787/api/v1"
echo "Checking admin login"
TOKEN=$(curl -fsS -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"ciadmin","password":"TestOnlyPass_1234"}' | jq -r '.token')
test "${#TOKEN}" -eq 64
DAY=$(TZ=Asia/Tehran date -d '+1 day' +%F)
header=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')
invite=$(curl -fsS -X POST "$BASE/admin/invitations" "${header[@]}" -d '{"name":"Test customer 1","phone":"09120000001"}' | jq -r '.url')
invite2=$(curl -fsS -X POST "$BASE/admin/invitations" "${header[@]}" -d '{"name":"Test customer 2","phone":"09120000002"}' | jq -r '.url')
t1=${invite##*/}
t2=${invite2##*/}
curl -fsS "$BASE/invitations/$t1/availability?day=$DAY" | jq -e '.slots | index("10:00") != null'
one=$(curl -fsS -X POST "$BASE/invitations/$t1/requests" -H 'Content-Type: application/json' -d "{\"day\":\"$DAY\",\"time\":\"10:00\"}")
track=$(echo "$one" | jq -r '.tracking_token')
test "${#track}" -eq 64
conflict=$(curl -sS -o /tmp/conflict.json -w '%{http_code}' -X POST "$BASE/invitations/$t2/requests" -H 'Content-Type: application/json' -d "{\"day\":\"$DAY\",\"time\":\"10:00\"}")
test "$conflict" = "409"
requestId=$(curl -fsS "$BASE/admin/overview" "${header[@]}" | jq -r '.requests[0].id')
curl -fsS -X POST "$BASE/admin/requests/$requestId/approve" "${header[@]}" | jq -e '.status=="approved"'
curl -fsS "$BASE/booking-status/$track" | jq -e '.status=="approved"'
echo "Checking sync endpoint and conflict blocking"
curl -fsS "$BASE/admin/sync/snapshot" "${header[@]}" | jq -e '.count>=1'
startSeconds=$(TZ=Asia/Tehran date -d "$DAY 10:00:00" +%s)
busyMs="${startSeconds}000"
syncConflict=$(curl -sS -o /tmp/sync-conflict.json -w '%{http_code}' -X POST "$BASE/admin/sync/mutate" "${header[@]}" -d "{\"action\":\"create\",\"sync_key\":\"00000000-0000-4000-8000-000000000001\",\"name\":\"Mobile test\",\"phone\":\"\",\"starts_ms\":$busyMs,\"minutes\":60}")
test "$syncConflict" = "409"
# Create a separate mobile session and exercise update / cancellation with optimistic version.
openSeconds=$(TZ=Asia/Tehran date -d "$DAY 15:00:00" +%s)
openMs="${openSeconds}000"
created=$(curl -fsS -X POST "$BASE/admin/sync/mutate" "${header[@]}" -d "{\"action\":\"create\",\"sync_key\":\"00000000-0000-4000-8000-000000000002\",\"name\":\"Mobile test\",\"phone\":\"\",\"starts_ms\":$openMs,\"minutes\":60}")
remoteId=$(echo "$created" | jq -r '.remote_id')
test "$remoteId" -gt 0
updatedSeconds=$(TZ=Asia/Tehran date -d "$DAY 16:00:00" +%s)
updatedMs="${updatedSeconds}000"
curl -fsS -X POST "$BASE/admin/sync/mutate" "${header[@]}" -d "{\"action\":\"update\",\"remote_id\":$remoteId,\"expected_version\":1,\"sync_key\":\"00000000-0000-4000-8000-000000000002\",\"name\":\"Mobile test\",\"phone\":\"\",\"starts_ms\":$updatedMs,\"minutes\":60}" | jq -e '.version==2'
curl -fsS -X POST "$BASE/admin/sync/mutate" "${header[@]}" -d "{\"action\":\"cancel\",\"remote_id\":$remoteId,\"expected_version\":2,\"sync_key\":\"00000000-0000-4000-8000-000000000002\"}" | jq -e '.version==3'
echo "Booking integration smoke tests passed"
