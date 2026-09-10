rm -rf server/target 
mvn clean package -DskipTests -X 
sudo cp -r server/target/launcher.war ../hmdm-docker/volumes/webapps/ROOT.war
