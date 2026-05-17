#!/bin/bash
# Deploy SupportDesk Server to VPS

# Build the server
echo "Building server..."
./gradlew :server:shadowJar

# The JAR will be at: server/build/libs/server-all.jar
JAR_FILE="server/build/libs/server-all.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    exit 1
fi

echo "Server built successfully at $JAR_FILE"
echo ""
echo "To run the server locally:"
echo "  SUPPORTDESK_SERVER_PORT=8080 SUPPORTDESK_SERVER_HOST=0.0.0.0 java -jar $JAR_FILE"
echo ""
echo "To deploy to VPS:"
echo "  1. Copy $JAR_FILE to your VPS"
echo "  2. Set environment variables:"
echo "     - DATABASE_URL or SUPABASE_DATABASE_URL"
echo "     - SUPPORTDESK_AUTH_SECRET"
echo "     - SUPPORTDESK_SERVER_PORT (default: 8080)"
echo "     - SUPPORTDESK_SERVER_HOST (default: 127.0.0.1)"
echo "  3. Run: java -jar server-all.jar"
