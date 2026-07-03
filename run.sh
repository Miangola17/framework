#!/bin/bash

echo "=== Compilation du framework ==="
mvn clean package

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation Maven"
    exit 1
fi

echo "✅ Compilation reussie"
echo ""
echo "=== Déploiement vers Tomcat ==="

# Configuration
TOMCAT_WEBAPPS="/home/gougoula/Téléchargements/apache-tomcat-10.0.16/webapps"
WAR_FILE="target/framework-1.0-SNAPSHOT.war"
DEPLOY_DIR="$TOMCAT_WEBAPPS/framework"

# Créer le répertoire de déploiement
mkdir -p "$DEPLOY_DIR"

# Copier le WAR
cp "$WAR_FILE" "$TOMCAT_WEBAPPS/"

# Extraire le WAR
cd "$DEPLOY_DIR"
jar -xf "$TOMCAT_WEBAPPS/$(basename $WAR_FILE)"

echo "✅ Déploiement terminé dans $DEPLOY_DIR"
echo ""
echo "=== Redémarrage de Tomcat ==="

cd /home/gougoula/Téléchargements/apache-tomcat-10.0.16
./bin/shutdown.sh
sleep 3
./bin/startup.sh

echo "✅ Tomcat redémarré"
echo ""
echo "=== Framework disponible sur http://localhost:8080/framework ==="
