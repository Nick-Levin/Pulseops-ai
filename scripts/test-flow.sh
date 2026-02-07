#!/bin/bash

# PulseOps v3.0 - Integration Test Flow Script
# This script tests the complete flow of the PulseOps platform

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
API_KEY=""
INCIDENT_ID=""
EVIDENCE_ID=""

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${YELLOW}[STEP]${NC} $1"
}

# Check if services are running
check_services() {
    log_step "Checking if services are running..."
    
    # Check Gateway
    if ! curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/actuator/health" | grep -q "200\|404"; then
        log_error "Gateway service is not running on $GATEWAY_URL"
        log_info "Please start the services first:"
        log_info "  1. make infra-up"
        log_info "  2. make run-secrets (in terminal 1)"
        log_info "  3. make run-gateway (in terminal 2)"
        log_info "  4. make run-incident (in terminal 3)"
        log_info "  5. make run-evidence (in terminal 4)"
        log_info "  6. make run-activity (in terminal 5)"
        exit 1
    fi
    
    log_success "Gateway is accessible"
}

# Step 1: Create API Key
step_create_api_key() {
    log_step "Step 1: Creating API Key..."
    
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/keys" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Integration Test Key",
            "description": "Test key created by test-flow.sh"
        }')
    
    log_info "Response: $RESPONSE"
    
    # Extract API key from response
    API_KEY=$(echo "$RESPONSE" | grep -o '"apiKey":"[^"]*"' | cut -d'"' -f4)
    KEY_ID=$(echo "$RESPONSE" | grep -o '"keyId":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$API_KEY" ]; then
        log_error "Failed to create API key"
        exit 1
    fi
    
    log_success "API Key created: ${API_KEY:0:20}..."
    log_info "Key ID: $KEY_ID"
}

# Step 2: Verify API Key
step_verify_api_key() {
    log_step "Step 2: Verifying API Key..."
    
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/keys/verify" \
        -H "Content-Type: application/json" \
        -d "{\"apiKey\": \"$API_KEY\"}")
    
    log_info "Response: $RESPONSE"
    
    if echo "$RESPONSE" | grep -q '"valid":true'; then
        log_success "API Key is valid"
    else
        log_error "API Key verification failed"
        exit 1
    fi
}

# Step 3: Create Incident
step_create_incident() {
    log_step "Step 3: Creating Incident..."
    
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/incidents" \
        -H "X-API-Key: $API_KEY" \
        -H "Content-Type: application/json" \
        -d '{
            "title": "Test Incident from Integration Script",
            "description": "This incident was created by the test-flow.sh script",
            "severity": "high",
            "service": "test-service"
        }')
    
    log_info "Response: $RESPONSE"
    
    # Extract incident ID
    INCIDENT_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$INCIDENT_ID" ]; then
        log_error "Failed to create incident"
        exit 1
    fi
    
    log_success "Incident created with ID: $INCIDENT_ID"
}

# Step 4: Get Incident
step_get_incident() {
    log_step "Step 4: Retrieving Incident..."
    
    RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/v1/incidents/$INCIDENT_ID" \
        -H "X-API-Key: $API_KEY")
    
    log_info "Response: $RESPONSE"
    
    if echo "$RESPONSE" | grep -q "$INCIDENT_ID"; then
        log_success "Incident retrieved successfully"
    else
        log_error "Failed to retrieve incident"
        exit 1
    fi
}

# Step 5: Update Incident Status
step_update_status() {
    log_step "Step 5: Updating Incident Status..."
    
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/incidents/$INCIDENT_ID/status?newStatus=investigating" \
        -H "X-API-Key: $API_KEY")
    
    log_info "Response: $RESPONSE"
    
    if echo "$RESPONSE" | grep -q '"status":"investigating"'; then
        log_success "Incident status updated to 'investigating'"
    else
        log_error "Failed to update incident status"
        exit 1
    fi
}

# Step 6: List Incidents
step_list_incidents() {
    log_step "Step 6: Listing All Incidents..."
    
    RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/v1/incidents" \
        -H "X-API-Key: $API_KEY")
    
    log_info "Response: $RESPONSE"
    
    if echo "$RESPONSE" | grep -q "incidents"; then
        log_success "Incidents listed successfully"
    else
        log_error "Failed to list incidents"
        exit 1
    fi
}

# Step 7: Upload Evidence (using a temporary file)
step_upload_evidence() {
    log_step "Step 7: Uploading Evidence..."
    
    # Create a temporary test file
    TEMP_FILE=$(mktemp /tmp/pulseops-test-XXXXXX.txt)
    echo "This is a test evidence file created by test-flow.sh" > "$TEMP_FILE"
    
    RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/evidence" \
        -H "X-API-Key: $API_KEY" \
        -F "incidentId=$INCIDENT_ID" \
        -F "description=Test evidence file" \
        -F "file=@$TEMP_FILE")
    
    # Clean up temp file
    rm -f "$TEMP_FILE"
    
    log_info "Response: $RESPONSE"
    
    # Extract evidence ID
    EVIDENCE_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$EVIDENCE_ID" ]; then
        log_error "Failed to upload evidence"
        exit 1
    fi
    
    log_success "Evidence uploaded with ID: $EVIDENCE_ID"
}

# Step 8: List Evidence for Incident
step_list_evidence() {
    log_step "Step 8: Listing Evidence for Incident..."
    
    RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/v1/evidence/incident/$INCIDENT_ID" \
        -H "X-API-Key: $API_KEY")
    
    log_info "Response: $RESPONSE"
    
    if echo "$RESPONSE" | grep -q "$EVIDENCE_ID"; then
        log_success "Evidence listed successfully"
    else
        log_error "Failed to list evidence"
        exit 1
    fi
}

# Step 9: Query Activity
step_query_activity() {
    log_step "Step 9: Querying Activity Feed..."
    
    # Wait a moment for events to be processed
    sleep 2
    
    RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/v1/activity?incidentId=$INCIDENT_ID" \
        -H "X-API-Key: $API_KEY")
    
    log_info "Response: $RESPONSE"
    
    if echo "$RESPONSE" | grep -q "activities"; then
        log_success "Activity feed retrieved successfully"
    else
        log_error "Failed to retrieve activity feed"
        exit 1
    fi
}

# Step 10: Test SSE Connection
step_test_sse() {
    log_step "Step 10: Testing SSE Connection (5 seconds)..."
    
    log_info "Connecting to SSE stream..."
    
    # Test SSE connection - connect for 5 seconds then disconnect
    timeout 5 curl -N "$GATEWAY_URL/api/v1/activity/stream" \
        -H "X-API-Key: $API_KEY" \
        -H "Accept: text/event-stream" 2>/dev/null || true
    
    log_success "SSE connection test completed"
}

# Step 11: Clean up - Delete Incident
step_cleanup() {
    log_step "Step 11: Cleaning up - Deleting Test Incident..."
    
    RESPONSE=$(curl -s -X DELETE "$GATEWAY_URL/api/v1/incidents/$INCIDENT_ID" \
        -H "X-API-Key: $API_KEY")
    
    log_info "Response: $RESPONSE"
    
    log_success "Test incident deleted"
}

# Main execution
main() {
    echo "=============================================="
    echo "  PulseOps v3.0 - Integration Test Flow"
    echo "=============================================="
    echo ""
    
    # Check services
    check_services
    
    echo ""
    echo "Starting integration tests..."
    echo ""
    
    # Run all steps
    step_create_api_key
    echo ""
    step_verify_api_key
    echo ""
    step_create_incident
    echo ""
    step_get_incident
    echo ""
    step_update_status
    echo ""
    step_list_incidents
    echo ""
    step_upload_evidence
    echo ""
    step_list_evidence
    echo ""
    step_query_activity
    echo ""
    step_test_sse
    echo ""
    step_cleanup
    
    echo ""
    echo "=============================================="
    log_success "All integration tests passed!"
    echo "=============================================="
    echo ""
    echo "Summary:"
    echo "  - API Key created and verified"
    echo "  - Incident created, retrieved, and updated"
    echo "  - Evidence uploaded and listed"
    echo "  - Activity feed queried"
    echo "  - SSE connection tested"
    echo "  - Cleanup completed"
    echo ""
}

# Run main function
main "$@"
