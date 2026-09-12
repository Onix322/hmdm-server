#!/bin/bash


# ==========================================
# PATHS AND CONFIGURATION
# ==========================================
HMDM_DOCKER_DIR="$(cd "$(dirname "$0")/../hmdm-docker" 2>/dev/null && pwd)"

if [ ! -d "$HMDM_DOCKER_DIR" ]; then
    HMDM_DOCKER_DIR="/home/alex/Documents/Projects/hmdm-docker"
fi

if [ -f "$HMDM_DOCKER_DIR/docker-compose.yaml" ]; then
    HMDM_DOCKER_FILE="$HMDM_DOCKER_DIR/docker-compose.yaml"
elif [ -f "$HMDM_DOCKER_DIR/docker-compose.yml" ]; then
    HMDM_DOCKER_FILE="$HMDM_DOCKER_DIR/docker-compose.yml"
else
    echo "❌ docker-compose.yaml or docker-compose.yml not found in: $HMDM_DOCKER_DIR"
    exit 1
fi

# Flags
DO_BUILD=false
DO_CLEAN=false
DO_DEPLOY=false
FOLLOW_LOGS=false

# ==========================================
# ARGUMENT PARSING
# ==========================================
show_help() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -b, --build     Run Maven build (mvn clean package)"
    echo "  -c, --clean     Remove Tomcat cache directories"
    echo "  -d, --deploy    Deploy target/launcher.war to Docker Compose"
    echo "  -f, --follow    Follow live container logs after deployment"
    echo "  -a, --all       Run full pipeline: build, clean, deploy, and follow logs (-b -c -d -f)"
    echo "  -h, --help      Display this help message"
    echo ""
    echo "Note: If no flags are passed, the script defaults to running build and deploy (-b -d)."
    exit 0
}

# If no arguments provided, default to build + deploy
if [[ $# -eq 0 ]]; then
    DO_BUILD=true
    DO_DEPLOY=true
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        -b|--build)
            DO_BUILD=true
            shift
            ;;
        -c|--clean)
            DO_CLEAN=true
            shift
            ;;
        -d|--deploy)
            DO_DEPLOY=true
            shift
            ;;
        -f|--follow)
            FOLLOW_LOGS=true
            shift
            ;;
        -a|--all)
            DO_BUILD=true
            DO_CLEAN=true
            DO_DEPLOY=true
            FOLLOW_LOGS=true
            shift
            ;;
        -h|--help)
            show_help
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            ;;
    esac
done

CURRENT_DIR="$(pwd)"

# ==========================================
# STEP 1: MAVEN BUILD (-b)
# ==========================================
if [ "$DO_BUILD" = true ]; then
    echo "==> [1/4] Running Maven build..."
    rm -rf server/target 
    mvn clean package -DskipTests -X

    if [ $? -ne 0 ]; then
        echo "❌ Build failed! Aborting."
        exit 1
    fi
else
    echo "==> [1/4] Skipping Maven build..."
fi

# ==========================================
# STEP 2: CLEAR TOMCAT CACHE (-c)
# ==========================================
if [ "$DO_CLEAN" = true ]; then
    echo "==> [2/4] Stopping services & clearing Tomcat cache..."
    sudo docker compose -f "$HMDM_DOCKER_FILE" down 2>/dev/null

    sudo rm -rf "$HMDM_DOCKER_DIR/volumes/webapps/ROOT"
    sudo rm -rf "$HMDM_DOCKER_DIR/volumes/conf/Catalina/localhost/ROOT.xml"
    sudo rm -rf "$HMDM_DOCKER_DIR/volumes/work/Catalina/localhost"
    sudo rm -rf "$HMDM_DOCKER_DIR/volumes/temp"

    sudo mkdir -p "$HMDM_DOCKER_DIR/volumes/work/Catalina/localhost"
    sudo mkdir -p "$HMDM_DOCKER_DIR/volumes/temp"
else
    echo "==> [2/4] Skipping cache removal..."
fi

# ==========================================
# STEP 3: DEPLOY TO DOCKER (-d)
# ==========================================
if [ "$DO_DEPLOY" = true ]; then
    echo "==> [3/4] Deploying to Docker..."
    
    if [ ! -f "$CURRENT_DIR/server/target/launcher.war" ]; then
        echo "❌ File $CURRENT_DIR/server/target/launcher.war not found! Run with -b first."
        exit 1
    fi

    echo "--> Copying fresh ROOT.war to $HMDM_DOCKER_DIR/volumes/webapps/..."
    sudo cp "$CURRENT_DIR/server/target/launcher.war" "$HMDM_DOCKER_DIR/volumes/webapps/ROOT.war"

    cd "$HMDM_DOCKER_DIR" || exit 1

    if [ "$DO_CLEAN" = true ]; then
        sudo docker compose -f "$HMDM_DOCKER_FILE" build --no-cache
    fi

    sudo docker compose -f "$HMDM_DOCKER_FILE" up -d

    if [ $? -ne 0 ]; then
        echo "❌ Docker Compose execution failed!"
        exit 1
    fi

    echo "✅ Deployment completed successfully!"
else
    echo "==> [3/4] Skipping Docker deployment..."
fi

# ==========================================
# STEP 4: FOLLOW LOGS (-f)
# ==========================================
if [ "$FOLLOW_LOGS" = true ]; then
    echo "==> [4/4] Attaching to live container logs (Ctrl+C to exit)..."
    cd "$HMDM_DOCKER_DIR" || exit 1
    sudo docker compose -f "$HMDM_DOCKER_FILE" logs -f
else
    echo "==> [4/4] Done."
fi
