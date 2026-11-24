# EsportsTeamRoom - Testing Results

## 14 Test Cases

|No.| Test Case     | Expected Result | Actual Result | Status |
|---|---            |---|---|---|
| 1 | Three clients | Three clients can connect to the server. | | |
| 2 | Broadcast     | A message is received by all connected clients. | | |
| 3 | Join/Leave    | Server announces when a client joins or leaves. | | |
| 4 | /name 	    | Client can change their nickname. | | |
| 5 | Duplicate names | Duplicate nicknames are rejected or handled correctly. | | |
| 6 | Blank message | Empty messages are not broadcast. | | |
| 7 | Bad IP/port | Invalid IP or port is handled without crashing. | | |
| 8 | Server off | Client displays an appropriate connection error. | | |
| 9 | /exit | Client disconnects using the /exit command. | | |
| 10 | Abrupt close | Server detects when a client closes unexpectedly. | | |
| 11 | Voting | Votes are processed and results are displayed correctly. | | |
| 12 | Ready | Ready status is communicated correctly. | | |
| 13 | Additional test | | | |
| 14 | Additional test | | | |

**Note:** Record the actual behavior observed during testing. Do not mark a test as PASS unless you have tested it.